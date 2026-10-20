package com.zigythebird.playeranimcore.math;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Objects;

public record Vec3f(float x, float y, float z) {
    public static final Vec3f ZERO = new Vec3f(0f, 0f, 0f);
    public static final Vec3f ONE = new Vec3f(1f, 1f, 1f);

    /**
     * Scale the vector
     *
     * @param scalar scalar
     * @return scaled vector
     */
    public Vec3f mul(float scalar) {
        return new Vec3f(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public Vec3f div(float scalar) {
        return new Vec3f(this.x / scalar, this.y / scalar, this.z / scalar);
    }

    /**
     * Add two vectors
     *
     * @param other other vector
     * @return sum vector
     */
    public Vec3f add(Vec3f other) {
        return new Vec3f(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vec3f(float x1, float y1, float z1))) return false;
        return Objects.equals(x, x1) && Objects.equals(y, y1) && Objects.equals(z, z1);
    }

    @Override
    public @NotNull String toString() {
        return "Vec3f[" + this.x + "; " + this.y + "; " + this.z + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    public static class Deserializer implements JsonDeserializer<Vec3f> {
        @Override
        public Vec3f deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonArray array = json.getAsJsonArray();
            return new Vec3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
        }
    }
}
