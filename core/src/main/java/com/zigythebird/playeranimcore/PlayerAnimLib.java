package com.zigythebird.playeranimcore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.loading.AnimationLoader;
import com.zigythebird.playeranimcore.loading.KeyFrameLoader;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimcore.math.Vec3f;
import org.redlance.platformtools.webp.WebPDiagnostics;
import org.redlance.platformtools.webp.decoder.DecodedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Map;

public class PlayerAnimLib {
    public static final String MOD_ID = "player_animation_library";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Type ANIMATIONS_MAP_TYPE = new TypeToken<Map<String, Animation>>() {}.getType();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(CustomModelBone.class, new CustomModelBone.Deserializer())
            .registerTypeAdapter(DecodedImage.class, new CustomModelBone.ImageDeserializer())
            .registerTypeAdapter(Vec3f.class, new Vec3f.Deserializer())
            .registerTypeAdapter(Animation.Keyframes.class, new KeyFrameLoader())
            .registerTypeAdapter(Animation.class, new AnimationLoader())
            .registerTypeAdapter(ANIMATIONS_MAP_TYPE, new UniversalAnimLoader())
            .disableHtmlEscaping()
            .create();

    static {
        WebPDiagnostics.summary(PlayerAnimLib.LOGGER::info);
    }
}
