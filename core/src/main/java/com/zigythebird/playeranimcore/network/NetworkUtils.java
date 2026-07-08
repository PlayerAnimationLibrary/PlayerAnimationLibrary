package com.zigythebird.playeranimcore.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.math.Vec3f;
import io.netty.buffer.ByteBuf;
import org.redlance.platformtools.webp.decoder.DecodedImage;
import org.redlance.platformtools.webp.decoder.PlatformWebPDecoder;
import org.redlance.platformtools.webp.encoder.PlatformWebPEncoder;
import team.unnamed.mocha.util.network.VarIntUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class NetworkUtils {
    public static <K, V> Map<K, V> readMap(ByteBuf buf, Function<ByteBuf, K> keyReader, Function<ByteBuf, V> valueReader) {
        int count = VarIntUtils.readVarInt(buf);
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

    /**
     * boneFlags (varint): bit0=hasTexture, bit1=isWebP, bit2=hasElements
     */
    public static CustomModelBone readCustomBone(ByteBuf buf, int version) {
        Vec3f pivot = readVec3f(buf);

        DecodedImage texture = null;
        JsonArray elements = null;

        if (version >= 7) {
            int boneFlags = VarIntUtils.readVarInt(buf);

            if ((boneFlags & 1) != 0) {
                boolean isWebP = (boneFlags & 2) != 0;

                int len = VarIntUtils.readVarInt(buf);
                byte[] rawTexture = new byte[len];
                buf.readBytes(rawTexture);

                try {
                    if (isWebP) {
                        texture = PlatformWebPDecoder.INSTANCE.decode(rawTexture);
                    } else {
                        texture = DecodedImage.fromPng(rawTexture);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            if ((boneFlags & 4) != 0) {
                int count = VarIntUtils.readVarInt(buf);
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
            boolean isWebPEncoderAvailable = PlatformWebPEncoder.INSTANCE.isAvailable();

            int boneFlags = 0;
            if (bone.texture() != null) {
                boneFlags |= 1;
                if (isWebPEncoderAvailable) boneFlags |= 2;
            }
            if (bone.elements() != null) boneFlags |= 4;
            VarIntUtils.writeVarInt(buf, boneFlags);

            if (bone.texture() != null) {
                byte[] imageData;
                try {
                    if (isWebPEncoderAvailable) {
                        imageData = PlatformWebPEncoder.INSTANCE.encodeLossless(bone.texture());
                    } else {
                        imageData = bone.texture().toPng();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                VarIntUtils.writeVarInt(buf, imageData.length);
                buf.writeBytes(imageData);
            }

            if (bone.elements() != null) {
                VarIntUtils.writeVarInt(buf, bone.elements().size());
                for (JsonElement el : bone.elements()) {
                    CustomModelUtils.writeBlockElement(buf, el.getAsJsonObject(), version);
                }
            }
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
