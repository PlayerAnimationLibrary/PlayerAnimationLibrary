package com.zigythebird.playeranimcore.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zigythebird.playeranimcore.animation.AnimationImage;
import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimcore.util.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.redlance.mocha.parser.util.network.ProtocolUtils;
import org.redlance.mocha.parser.util.network.VarIntUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class NetworkUtils {
    /**
     * Shared by all textures of one animation.
     */
    public static final long MAX_TEXTURE_PIXELS = 2048L * 2048L;

    public static <K, V> Map<K, V> readMap(ByteBuf buf, Function<ByteBuf, K> keyReader, Function<ByteBuf, V> valueReader) {
        int count = ProtocolUtils.readCount(buf);
        Map<K, V> map = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            K key = keyReader.apply(buf);
            V value = valueReader.apply(buf);
            map.put(key, value);
        }
        return map;
    }

    public static <K, V> void writeMap(ByteBuf buf, Map<K, V> map, BiConsumer<ByteBuf, K> keyWriter, BiConsumer<ByteBuf, V> valueWriter) {
        VarIntUtils.writeVarInt(buf, map.size());
        for (var entry : map.entrySet()) {
            keyWriter.accept(buf, entry.getKey());
            valueWriter.accept(buf, entry.getValue());
        }
    }

    public static Map<String, CustomModelBone> readCustomBones(ByteBuf buf, int version) {
        int count = ProtocolUtils.readCount(buf);
        Map<String, CustomModelBone> bones = new HashMap<>(count);
        long texturePixels = 0;
        for (int i = 0; i < count; i++) {
            String name = ProtocolUtils.readString(buf);
            CustomModelBone bone = readCustomBone(buf, version, MAX_TEXTURE_PIXELS - texturePixels);
            texturePixels += bone.texturePixels();
            bones.put(name, bone);
        }
        return bones;
    }

    /**
     * boneFlags (varint): bit0=hasTexture, bit1=isJpegXl (else PNG), bit2=hasElements
     */
    public static CustomModelBone readCustomBone(ByteBuf buf, int version, long maxTexturePixels) {
        Vec3f pivot = readVec3f(buf);

        AnimationImage texture = null;
        JsonArray elements = null;

        if (version >= 7) {
            int boneFlags = VarIntUtils.readVarInt(buf);

            if ((boneFlags & 1) != 0) {
                texture = AnimationImage.read(buf, ProtocolUtils.readCount(buf), (boneFlags & 2) != 0, maxTexturePixels);
            }

            if ((boneFlags & 4) != 0) {
                int count = ProtocolUtils.readCount(buf);
                elements = new JsonArray(count);
                for (int i = 0; i < count; i++) {
                    elements.add(CustomModelUtils.readBlockElement(buf, version));
                }
            }
        }

        return new CustomModelBone(pivot, texture, elements);
    }

    public static void writeCustomBone(ByteBuf buf, CustomModelBone bone, int version) {
        writeVec3f(buf, bone.pivot());

        if (version >= 7) {
            AnimationImage.Encoded texture = bone.texture() == null ? null : bone.texture().network();

            int boneFlags = 0;
            if (texture != null) {
                boneFlags |= 1;
                if (texture.jxl()) boneFlags |= 2;
            }
            if (bone.elements() != null) boneFlags |= 4;
            VarIntUtils.writeVarInt(buf, boneFlags);

            if (texture != null) {
                VarIntUtils.writeVarInt(buf, texture.bytes().length);
                buf.writeBytes(texture.bytes());
            }

            if (bone.elements() != null) {
                VarIntUtils.writeVarInt(buf, bone.elements().size());
                for (JsonElement el : bone.elements()) {
                    CustomModelUtils.writeBlockElement(buf, el.getAsJsonObject(), version);
                }
            }
        }
    }

    /**
     * Refuses at load what could not be sent, and gives the bone the elements receivers get.
     */
    public static JsonArray toNetworkElements(JsonArray elements) {
        ByteBuf buf = Unpooled.buffer();
        try {
            JsonArray carried = new JsonArray(elements.size());
            for (JsonElement element : elements) {
                CustomModelUtils.writeBlockElement(buf, JsonUtil.convertToJsonObject(element, "element"), AnimationBinary.getCurrentVersion());
                carried.add(CustomModelUtils.readBlockElement(buf, AnimationBinary.getCurrentVersion()));
            }
            return carried;
        } finally {
            buf.release();
        }
    }

    public static Vec3f readVec3f(ByteBuf buf) {
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        return new Vec3f(x, y, z);
    }

    public static void writeVec3f(ByteBuf buf, Vec3f vec3f) {
        buf.writeFloat(vec3f.x());
        buf.writeFloat(vec3f.y());
        buf.writeFloat(vec3f.z());
    }

    public static UUID readUuid(ByteBuf buf) {
        long msb = buf.readLong();
        long lsb = buf.readLong();
        return new UUID(msb, lsb);
    }

    public static void writeUuid(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }
}
