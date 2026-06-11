package com.zigythebird.playeranimcore.util;

import com.google.gson.JsonObject;
import com.google.j2objc.annotations.ReflectionSupport;
import com.zigythebird.playeranimcore.PlayerAnimLib;

@ReflectionSupport(ReflectionSupport.Level.FULL)
public class ParticleEffectUtils {
    public static String parseIdentifier(String raw) {
        return getIdentifier(PlayerAnimLib.GSON.fromJson(raw, JsonObject.class));
    }

    public static String getIdentifier(JsonObject obj) {
        return obj.getAsJsonObject("particle_effect")
                .getAsJsonObject("description")
                .get("identifier").getAsString();
    }
}
