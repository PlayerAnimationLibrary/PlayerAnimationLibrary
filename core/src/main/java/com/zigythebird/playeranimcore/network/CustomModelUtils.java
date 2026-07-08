package com.zigythebird.playeranimcore.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import team.unnamed.mocha.util.network.ProtocolUtils;
import team.unnamed.mocha.util.network.VarIntUtils;

class CustomModelUtils {
    private static final String[] DIRECTION_NAMES = {"down", "up", "north", "south", "west", "east"};
    private static final String[] AXIS_NAMES = {"x", "y", "z"};

    /**
     * ── BlockElement ──
     * elementFlags (varint):
     *    bit 0: hasFrom          (almost always set → low bit)
     *    bit 1: hasTo            (almost always set → low bit)
     *    bit 2: hasRotation
     *    bit 3: noShade          (bit set = shade is false, default true → 0)
     *    bits 4-7: lightEmission (0-15)
     */
    static void writeBlockElement(ByteBuf buf, JsonObject element, int version) {
        boolean hasFrom = element.has("from");
        boolean hasTo = element.has("to");
        boolean hasRotation = element.has("rotation");
        boolean noShade = element.has("shade") && !element.get("shade").getAsBoolean();
        int lightEmission = element.has("light_emission") ? element.get("light_emission").getAsInt() : 0;

        int flags = 0;
        if (hasFrom) flags |= 1;
        if (hasTo) flags |= 2;
        if (hasRotation) flags |= 4;
        if (noShade) flags |= 8;
        flags |= (lightEmission & 0xF) << 4;
        VarIntUtils.writeVarInt(buf, flags);

        if (hasFrom) writeJsonVec3f(buf, element.getAsJsonArray("from"));
        if (hasTo) writeJsonVec3f(buf, element.getAsJsonArray("to"));

        JsonObject faces = element.has("faces") ? element.getAsJsonObject("faces") : null;
        int facesBitmask = 0;
        if (faces != null) {
            for (int d = 0; d < 6; d++) {
                if (faces.has(DIRECTION_NAMES[d])) facesBitmask |= (1 << d);
            }
        }
        VarIntUtils.writeVarInt(buf, facesBitmask);

        for (int d = 0; d < 6; d++) {
            if ((facesBitmask & (1 << d)) == 0) continue;
            writeBlockElementFace(buf, faces.getAsJsonObject(DIRECTION_NAMES[d]), version);
        }

        if (hasRotation) {
            writeBlockElementRotation(buf, element.getAsJsonObject("rotation"), version);
        }
    }

    static JsonObject readBlockElement(ByteBuf buf, int version) {
        JsonObject element = new JsonObject();

        int flags = VarIntUtils.readVarInt(buf);
        boolean hasFrom = (flags & 1) != 0;
        boolean hasTo = (flags & 2) != 0;
        boolean hasRotation = (flags & 4) != 0;
        boolean noShade = (flags & 8) != 0;
        int lightEmission = (flags >> 4) & 0xF;

        if (hasFrom) element.add("from", readJsonVec3f(buf));
        if (hasTo) element.add("to", readJsonVec3f(buf));
        if (noShade) element.addProperty("shade", false);
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
        int tintIndex = face.has("tintindex") ? face.get("tintindex").getAsInt() : -1;
        boolean hasTint = tintIndex != -1;

        int cullDir = 7;
        if (face.has("cullface")) {
            String cullName = face.get("cullface").getAsString();
            for (int i = 0; i < 6; i++) {
                if (DIRECTION_NAMES[i].equals(cullName)) { cullDir = i; break; }
            }
        }
        boolean hasRotation = face.has("rotation");

        int faceFlags = (hasTexture ? 1 : 0) | (hasUVs ? (1 << 1) : 0) | (hasTint ? (1 << 2) : 0)
                | ((cullDir & 0x7) << 3) | (hasRotation ? (1 << 6) : 0);
        VarIntUtils.writeVarInt(buf, faceFlags);

        if (hasTexture) ProtocolUtils.writeString(buf, face.get("texture").getAsString());
        if (hasTint) VarIntUtils.writeVarInt(buf, tintIndex);

        if (hasUVs) {
            JsonArray uv = face.getAsJsonArray("uv");
            buf.writeFloat(uv.get(0).getAsFloat());
            buf.writeFloat(uv.get(1).getAsFloat());
            buf.writeFloat(uv.get(2).getAsFloat());
            buf.writeFloat(uv.get(3).getAsFloat());
        }

        if (hasRotation) VarIntUtils.writeVarInt(buf, face.get("rotation").getAsInt());
    }

    private static JsonObject readBlockElementFace(ByteBuf buf, int version) {
        int faceFlags = VarIntUtils.readVarInt(buf);
        boolean hasTexture = (faceFlags & 1) != 0;
        boolean hasUVs = (faceFlags & (1 << 1)) != 0;
        boolean hasTint = (faceFlags & (1 << 2)) != 0;
        int cullDir = (faceFlags >> 3) & 0x7;
        boolean hasRotation = (faceFlags & (1 << 6)) != 0;

        JsonObject face = new JsonObject();

        if (hasTexture) face.addProperty("texture", ProtocolUtils.readString(buf));
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
        writeJsonVec3f(buf, rotation.getAsJsonArray("origin"));

        // MC logic: euler when axis/angle are absent
        boolean isEuler = !rotation.has("axis") && !rotation.has("angle");
        boolean rescale = rotation.has("rescale") && rotation.get("rescale").getAsBoolean();

        int rotFlags = 0;
        if (isEuler) rotFlags |= 1;
        if (rescale) rotFlags |= 2;
        VarIntUtils.writeVarInt(buf, rotFlags);

        if (isEuler) {
            buf.writeFloat(rotation.has("x") ? rotation.get("x").getAsFloat() : 0f);
            buf.writeFloat(rotation.has("y") ? rotation.get("y").getAsFloat() : 0f);
            buf.writeFloat(rotation.has("z") ? rotation.get("z").getAsFloat() : 0f);
        } else {
            String axisName = rotation.get("axis").getAsString();
            int axis = 0;
            for (int i = 0; i < 3; i++) {
                if (AXIS_NAMES[i].equals(axisName)) { axis = i; break; }
            }
            VarIntUtils.writeVarInt(buf, axis);
            buf.writeFloat(rotation.get("angle").getAsFloat());
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

    private static void writeJsonVec3f(ByteBuf buf, JsonArray arr) {
        buf.writeFloat(arr.get(0).getAsFloat());
        buf.writeFloat(arr.get(1).getAsFloat());
        buf.writeFloat(arr.get(2).getAsFloat());
    }

    private static JsonArray readJsonVec3f(ByteBuf buf) {
        JsonArray arr = new JsonArray(3);
        arr.add(buf.readFloat());
        arr.add(buf.readFloat());
        arr.add(buf.readFloat());
        return arr;
    }
}
