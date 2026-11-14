package com.zigythebird.playeranimcore.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimcore.util.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.redlance.platformtools.webp.decoder.DecodedImage;
import org.redlance.platformtools.webp.decoder.PlatformWebPDecoder;
import org.redlance.platformtools.webp.encoder.PlatformWebPEncoder;
import org.redlance.mocha.parser.util.network.ProtocolUtils;
import org.redlance.mocha.parser.util.network.VarIntUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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

    public static Map<String, CustomModelBone> readCustomBones(ByteBuf buf, int version) {
        int count = VarIntUtils.readVarInt(buf);
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
     * boneFlags (varint): bit0=hasTexture, bit1=isWebP, bit2=hasElements
     */
    public static CustomModelBone readCustomBone(ByteBuf buf, int version, long maxTexturePixels) {
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
                    int[] size = isWebP ? PlatformWebPDecoder.INSTANCE.getInfo(rawTexture) : getPngSize(rawTexture);
                    if ((long) size[0] * size[1] > maxTexturePixels) {
                        throw new IllegalArgumentException("Texture of " + size[0] + "x" + size[1] + " is over the " + maxTexturePixels + " pixels left for this animation");
                    }

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

    private static int[] getPngSize(byte[] png) throws IOException {
        ImageReader reader = ImageIO.getImageReadersByFormatName("png").next();
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(png))) {
            reader.setInput(input);
            return new int[]{reader.getWidth(0), reader.getHeight(0)};
        } finally {
            reader.dispose();
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
