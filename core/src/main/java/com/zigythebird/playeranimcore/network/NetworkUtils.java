package com.zigythebird.playeranimcore.network;

import com.zigythebird.playeranimcore.math.Vec3f;
import io.netty.buffer.ByteBuf;
import org.redlance.mocha.parser.util.network.VarIntUtils;

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
