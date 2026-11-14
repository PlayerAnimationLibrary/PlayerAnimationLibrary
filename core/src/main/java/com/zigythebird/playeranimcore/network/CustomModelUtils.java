package com.zigythebird.playeranimcore.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.zigythebird.playeranimcore.util.JsonUtil;
import io.netty.buffer.ByteBuf;
import org.redlance.mocha.parser.util.network.ProtocolUtils;
import org.redlance.mocha.parser.util.network.VarIntUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

class CustomModelUtils {
    private static final String[] DIRECTION_NAMES = {"down", "up", "north", "south", "west", "east"};
    private static final String[] AXIS_NAMES = {"x", "y", "z"};

    /**
     * ── BlockElement ──
     * elementFlags (varint):
     *    bit 0: hasFrom          (almost always set → low bit)
     *    bit 1: hasTo            (almost always set → low bit)
     *    bit 2: hasRotation
     *    bit 3: hasShadeDirectionOverride
     *    bits 4-7: lightEmission (0-15)
     */
    static void writeBlockElement(ByteBuf buf, JsonObject element, int version) {
        boolean hasFrom = element.has("from");
        boolean hasTo = element.has("to");
        boolean hasRotation = element.has("rotation");
        boolean hasShadeDirectionOverride = element.has("shade_direction_override");
        int lightEmission = JsonUtil.getAsInt(element, "light_emission", 0);
        if (lightEmission < 0 || lightEmission > 15) {
            throw new JsonParseException("Expected 'light_emission' to be an Integer between (inclusive) 0 and 15");
        }

        int flags = 0;
        if (hasFrom) flags |= 1;
        if (hasTo) flags |= 2;
        if (hasRotation) flags |= 4;
        if (hasShadeDirectionOverride) flags |= 8;
        flags |= lightEmission << 4;
        VarIntUtils.writeVarInt(buf, flags);

        if (hasFrom) writeJsonVec3f(buf, element, "from");
        if (hasTo) writeJsonVec3f(buf, element, "to");
        if (hasShadeDirectionOverride) {
            String direction = JsonUtil.getAsString(element, "shade_direction_override");
            int index = indexOf(DIRECTION_NAMES, direction);
            if (index == -1) throw new JsonParseException("Unknown shade direction override: " + direction);
            VarIntUtils.writeVarInt(buf, index);
        }

        JsonObject faces = JsonUtil.getAsJsonObject(element, "faces", null);
        int facesBitmask = 0;
        if (faces != null) {
            for (String name : faces.keySet()) {
                int direction = indexOf(DIRECTION_NAMES, name);
                if (direction == -1) throw new JsonParseException("Unknown facing: " + name);
                facesBitmask |= 1 << direction;
            }
        }
        VarIntUtils.writeVarInt(buf, facesBitmask);

        for (int d = 0; d < 6; d++) {
            if ((facesBitmask & (1 << d)) == 0) continue;
            writeBlockElementFace(buf, JsonUtil.convertToJsonObject(faces.get(DIRECTION_NAMES[d]), DIRECTION_NAMES[d]), version);
        }

        if (hasRotation) {
            writeBlockElementRotation(buf, JsonUtil.convertToJsonObject(element.get("rotation"), "rotation"), version);
        }
    }

    static JsonObject readBlockElement(ByteBuf buf, int version) {
        JsonObject element = new JsonObject();

        int flags = VarIntUtils.readVarInt(buf);
        boolean hasFrom = (flags & 1) != 0;
        boolean hasTo = (flags & 2) != 0;
        boolean hasRotation = (flags & 4) != 0;
        boolean hasShadeDirectionOverride = (flags & 8) != 0;
        int lightEmission = (flags >> 4) & 0xF;

        if (hasFrom) element.add("from", readJsonVec3f(buf));
        if (hasTo) element.add("to", readJsonVec3f(buf));
        if (hasShadeDirectionOverride) element.addProperty("shade_direction_override", DIRECTION_NAMES[VarIntUtils.readVarInt(buf)]);
        if (lightEmission != 0) element.addProperty("light_emission", lightEmission);

        int facesBitmask = VarIntUtils.readVarInt(buf);
        if (facesBitmask != 0) {
            JsonObject faces = new JsonObject();
            for (int d = 0; d < 6; d++) {
                if ((facesBitmask & (1 << d)) == 0) continue;
                faces.add(DIRECTION_NAMES[d], readBlockElementFace(buf, version));
            }
            element.add("faces", faces);
        }

        if (hasRotation) {
            element.add("rotation", readBlockElementRotation(buf, version));
        }

        return element;
    }

    /**
     * ── BlockElementFace ──
     * faceFlags (varint):
     *    bit 0: hasTexture
     *    bit 1: hasUVs
     *    bit 2: hasTintIndex
     *    bits 3-5: cullForDirection (0-5=dir, 7=none)
     *    bit 6: hasRotation
     */
    private static void writeBlockElementFace(ByteBuf buf, JsonObject face, int version) {
        boolean hasTexture = face.has("texture");
        boolean hasUVs = face.has("uv");
        int tintIndex = JsonUtil.getAsInt(face, "tintindex", -1);
        boolean hasTint = tintIndex != -1;

        int cullDir = 7;
        if (face.has("cullface")) {
            int direction = indexOf(DIRECTION_NAMES, JsonUtil.getAsString(face, "cullface"));
            if (direction != -1) cullDir = direction;
        }
        boolean hasRotation = face.has("rotation");

        int faceFlags = (hasTexture ? 1 : 0) | (hasUVs ? (1 << 1) : 0) | (hasTint ? (1 << 2) : 0)
                | ((cullDir & 0x7) << 3) | (hasRotation ? (1 << 6) : 0);
        VarIntUtils.writeVarInt(buf, faceFlags);

        if (hasTexture) ProtocolUtils.writeString(buf, JsonUtil.getAsString(face, "texture"));
        if (hasTint) VarIntUtils.writeVarInt(buf, tintIndex);

        if (hasUVs) {
            JsonArray uv = JsonUtil.getAsJsonArray(face, "uv");
            if (uv.size() != 4) throw new JsonParseException("Expected 4 uv values, found: " + uv.size());
            buf.writeFloat(JsonUtil.convertToFloat(uv.get(0), "minU"));
            buf.writeFloat(JsonUtil.convertToFloat(uv.get(1), "minV"));
            buf.writeFloat(JsonUtil.convertToFloat(uv.get(2), "maxU"));
            buf.writeFloat(JsonUtil.convertToFloat(uv.get(3), "maxV"));
        }

        if (hasRotation) VarIntUtils.writeVarInt(buf, JsonUtil.convertToInt(face.get("rotation"), "rotation"));
    }

    private static JsonObject readBlockElementFace(ByteBuf buf, int version) {
        int faceFlags = VarIntUtils.readVarInt(buf);
        boolean hasTexture = (faceFlags & 1) != 0;
        boolean hasUVs = (faceFlags & (1 << 1)) != 0;
        boolean hasTint = (faceFlags & (1 << 2)) != 0;
        int cullDir = (faceFlags >> 3) & 0x7;
        boolean hasRotation = (faceFlags & (1 << 6)) != 0;

        JsonObject face = new JsonObject();

        // Unlike ProtocolUtils.readString, keeps "", which Minecraft takes as a texture name
        if (hasTexture) face.addProperty("texture", buf.readSlice(VarIntUtils.readVarInt(buf)).toString(StandardCharsets.UTF_8));
        if (cullDir < 6) face.addProperty("cullface", DIRECTION_NAMES[cullDir]);
        if (hasTint) face.addProperty("tintindex", VarIntUtils.readVarInt(buf));

        if (hasUVs) {
            JsonArray uv = new JsonArray(4);
            uv.add(buf.readFloat());
            uv.add(buf.readFloat());
            uv.add(buf.readFloat());
            uv.add(buf.readFloat());
            face.add("uv", uv);
        }

        if (hasRotation) face.addProperty("rotation", VarIntUtils.readVarInt(buf));

        return face;
    }

    /**
     * ── BlockElementRotation ──
     * rotFlags (varint): bit0=isEuler, bit1=rescale
     */
    private static void writeBlockElementRotation(ByteBuf buf, JsonObject rotation, int version) {
        writeJsonVec3f(buf, rotation, "origin");

        // MC logic: euler when axis/angle are absent
        boolean isEuler = !rotation.has("axis") && !rotation.has("angle");
        if (isEuler && !rotation.has("x") && !rotation.has("y") && !rotation.has("z")) {
            throw new JsonParseException("Missing rotation value, expected either 'axis' and 'angle' or 'x', 'y' and 'z'");
        }
        boolean rescale = JsonUtil.getAsBoolean(rotation, "rescale", false);

        int rotFlags = 0;
        if (isEuler) rotFlags |= 1;
        if (rescale) rotFlags |= 2;
        VarIntUtils.writeVarInt(buf, rotFlags);

        if (isEuler) {
            buf.writeFloat(JsonUtil.getAsFloat(rotation, "x", 0f));
            buf.writeFloat(JsonUtil.getAsFloat(rotation, "y", 0f));
            buf.writeFloat(JsonUtil.getAsFloat(rotation, "z", 0f));
        } else {
            String axisName = JsonUtil.getAsString(rotation, "axis");
            int axis = indexOf(AXIS_NAMES, axisName.toLowerCase(Locale.ROOT));
            if (axis == -1) throw new JsonParseException("Invalid rotation axis: " + axisName);
            VarIntUtils.writeVarInt(buf, axis);
            buf.writeFloat(JsonUtil.getAsFloat(rotation, "angle"));
        }
    }

    private static JsonObject readBlockElementRotation(ByteBuf buf, int version) {
        JsonObject rotation = new JsonObject();
        rotation.add("origin", readJsonVec3f(buf));

        int rotFlags = VarIntUtils.readVarInt(buf);
        boolean isEuler = (rotFlags & 1) != 0;
        boolean rescale = (rotFlags & 2) != 0;

        if (isEuler) {
            float x = buf.readFloat();
            float y = buf.readFloat();
            float z = buf.readFloat();
            rotation.addProperty("x", x);
            rotation.addProperty("y", y);
            rotation.addProperty("z", z);
        } else {
            int axis = VarIntUtils.readVarInt(buf);
            float angle = buf.readFloat();
            rotation.addProperty("axis", AXIS_NAMES[axis]);
            rotation.addProperty("angle", angle);
        }

        if (rescale) rotation.addProperty("rescale", true);

        return rotation;
    }

    private static void writeJsonVec3f(ByteBuf buf, JsonObject object, String key) {
        JsonArray arr = JsonUtil.getAsJsonArray(object, key);
        if (arr.size() != 3) throw new JsonParseException("Expected 3 " + key + " values, found: " + arr.size());
        buf.writeFloat(JsonUtil.convertToFloat(arr.get(0), key + "[0]"));
        buf.writeFloat(JsonUtil.convertToFloat(arr.get(1), key + "[1]"));
        buf.writeFloat(JsonUtil.convertToFloat(arr.get(2), key + "[2]"));
    }

    private static JsonArray readJsonVec3f(ByteBuf buf) {
        JsonArray arr = new JsonArray(3);
        arr.add(buf.readFloat());
        arr.add(buf.readFloat());
        arr.add(buf.readFloat());
        return arr;
    }

    private static int indexOf(String[] names, String name) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) return i;
        }
        return -1;
    }
}
