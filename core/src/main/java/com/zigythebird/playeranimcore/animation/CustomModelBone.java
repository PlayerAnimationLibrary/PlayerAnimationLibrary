package com.zigythebird.playeranimcore.animation;

import com.google.gson.*;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimcore.network.NetworkUtils;
import com.zigythebird.playeranimcore.util.JsonUtil;
import org.jetbrains.annotations.Nullable;
import org.redlance.smalt.MediaException;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Objects;

public record CustomModelBone(Vec3f pivot, @Nullable AnimationImage texture, @Nullable JsonArray elements) {
    public static class Deserializer implements JsonDeserializer<CustomModelBone> {
        @Override
        public CustomModelBone deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            JsonArray elements = JsonUtil.getAsJsonArray(obj, "elements", null);
            return new CustomModelBone(
                    ctx.deserialize(JsonUtil.getAsJsonArray(obj, "pivot"), Vec3f.class),
                    obj.has("texture") ? ctx.deserialize(obj.get("texture"), AnimationImage.class) : null,
                    elements == null || elements.isEmpty() ? null : NetworkUtils.toNetworkElements(elements)
            );
        }
    }

    public static class ImageDeserializer implements JsonDeserializer<AnimationImage> {
        @Override
        public AnimationImage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            try {
                return AnimationImage.decode(Base64.getDecoder().decode(json.getAsString()), NetworkUtils.MAX_TEXTURE_PIXELS);
            } catch (MediaException | IllegalArgumentException e) {
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
     * A bone without texture and elements hashes as its pivot alone, keeping the UUIDs from before bones had models.
     */
    @Override
    public int hashCode() {
        return 31 * (31 * Objects.hashCode(this.texture) + Objects.hashCode(this.elements)) + this.pivot.hashCode();
    }
}
