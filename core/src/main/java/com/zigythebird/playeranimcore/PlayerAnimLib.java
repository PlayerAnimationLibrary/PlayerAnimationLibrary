package com.zigythebird.playeranimcore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.j2objc.annotations.ReflectionSupport;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.loading.AnimationLoader;
import com.zigythebird.playeranimcore.loading.KeyFrameLoader;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Map;

@ReflectionSupport(ReflectionSupport.Level.FULL)
public class PlayerAnimLib {
    public static final String MOD_ID = "player_animation_library";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Type ANIMATIONS_MAP_TYPE = new TypeToken<Map<String, Animation>>() {}.getType();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Animation.Keyframes.class, new KeyFrameLoader())
            .registerTypeAdapter(Animation.class, new AnimationLoader())
            .registerTypeAdapter(ANIMATIONS_MAP_TYPE, new UniversalAnimLoader())
            .disableHtmlEscaping()
            .create();
}
