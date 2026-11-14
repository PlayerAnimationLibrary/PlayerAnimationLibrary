package com.zigythebird.playeranimcore.animation;

import com.google.gson.*;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimcore.network.NetworkUtils;
import com.zigythebird.playeranimcore.util.JsonUtil;
import org.jetbrains.annotations.Nullable;
import org.redlance.platformtools.webp.decoder.DecodedImage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public record CustomModelBone(Vec3f pivot, @Nullable DecodedImage texture, @Nullable JsonArray elements) {
    public static class Deserializer implements JsonDeserializer<CustomModelBone> {
        @Override
        public CustomModelBone deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            JsonArray elements = JsonUtil.getAsJsonArray(obj, "elements", null);
            return new CustomModelBone(
                    ctx.deserialize(JsonUtil.getAsJsonArray(obj, "pivot"), Vec3f.class),
                    obj.has("texture") ? ctx.deserialize(obj.get("texture"), DecodedImage.class) : null,
                    elements == null || elements.isEmpty() ? null : NetworkUtils.toNetworkElements(elements)
            );
        }
    }

    public static class ImageDeserializer implements JsonDeserializer<DecodedImage> {
        @Override
        public DecodedImage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            try {
                return DecodedImage.fromPng(Base64.getDecoder().decode(json.getAsString()));
            } catch (IOException e) {
                throw new JsonParseException(e);
            }
        }
    }

    public boolean hasModelData() {
        return this.texture() != null && this.elements() != null && !this.elements().isEmpty();
    }

    public long texturePixels() {
        return this.texture == null ? 0 : (long) this.texture.width() * this.texture.height();
    }

    /**
     * An image compares by identity, so the texture is compared by its pixels.
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof CustomModelBone(Vec3f otherPivot, DecodedImage otherTexture, JsonArray otherElements)
                && this.pivot.equals(otherPivot) && samePixels(this.texture, otherTexture) && Objects.equals(this.elements, otherElements);
    }

    /**
     * A bone without texture and elements hashes as its pivot alone, keeping the UUIDs from before bones had models.
     */
    @Override
    public int hashCode() {
        int textureHash = this.texture == null ? 0 : Arrays.hashCode(this.texture.argb());
        return 31 * (31 * textureHash + Objects.hashCode(this.elements)) + this.pivot.hashCode();
    }

    private static boolean samePixels(@Nullable DecodedImage a, @Nullable DecodedImage b) {
        if (a == null || b == null) return a == b;
        return a.width() == b.width() && a.height() == b.height() && Arrays.equals(a.argb(), b.argb());
    }
}
