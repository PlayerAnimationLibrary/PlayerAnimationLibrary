package com.zigythebird.playeranimcore.animation;

import com.google.gson.*;
import com.zigythebird.playeranimcore.math.Vec3f;
import org.jetbrains.annotations.Nullable;
import org.redlance.platformtools.webp.decoder.DecodedImage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;

public record CustomModelBone(Vec3f pivot, @Nullable DecodedImage texture, @Nullable JsonArray elements) {
    public static class Deserializer implements JsonDeserializer<CustomModelBone> {
        @Override
        public CustomModelBone deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            return new CustomModelBone(
                    ctx.deserialize(obj.get("pivot"), Vec3f.class),
                    obj.has("texture") ? ctx.deserialize(obj.get("texture"), DecodedImage.class) : null,
                    obj.getAsJsonArray("elements")
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
}
