package com.zigythebird.playeranimcore.loading;

import com.google.gson.*;
import com.zigythebird.playeranimcore.PlayerAnimLib;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.CustomInstructionKeyframeData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.ParticleKeyframeData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.SoundKeyframeData;
import com.zigythebird.playeranimcore.util.JsonUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UniversalAnimLoader implements JsonDeserializer<Map<String, Animation>> {
    public static final Animation.Keyframes NO_KEYFRAMES = new Animation.Keyframes(new SoundKeyframeData[0], new ParticleKeyframeData[0], new CustomInstructionKeyframeData[0]);

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("([A-Z])");
    private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_(.)");

    public static Map<String, Animation> loadAnimations(InputStream resource) throws IOException {
        try (Reader reader = new InputStreamReader(resource)) {
            return loadAnimations(PlayerAnimLib.GSON.fromJson(reader, JsonObject.class));
        }
    }

    public static Map<@NotNull String, Animation> loadAnimations(JsonObject json) {
        if (json.has("animations")) {
            Map<String, Animation> animationMap = PlayerAnimLib.GSON.fromJson(json.get("animations"), PlayerAnimLib.ANIMATIONS_MAP_TYPE);
            if (json.has("model")) {
                Map<String, CustomModelBone> bones = UniversalAnimLoader.getModel(JsonUtil.getAsJsonObject(json, "model", new JsonObject()), PlayerAnimLib.GSON::fromJson);
                for (Animation animation : animationMap.values()) {
                    if (animation.bones().isEmpty()) {
                        animation.bones().putAll(bones);
                    }
                }
            }
            if (json.has("parents")) {
                Map<String, String> parents = UniversalAnimLoader.getParents(JsonUtil.getAsJsonObject(json, "parents", new JsonObject()));
                for (Animation animation : animationMap.values()) {
                    if (animation.parents().isEmpty()) {
                        animation.parents().putAll(parents);
                    }
                }
            }
            return animationMap;
        } else {
            Animation animation = PlayerAnimatorLoader.GSON.fromJson(json, Animation.class);
            return Collections.singletonMap(animation.getNameOrId(), animation);
        }
    }

    @Override
    public Map<String, Animation> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        Map<String, Animation> animations = new Object2ObjectOpenHashMap<>(obj.size());

        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            try {
                Animation animation = context.deserialize(entry.getValue().getAsJsonObject(), Animation.class);
                if (!animation.data().has(ExtraAnimationData.NAME_KEY)) { // Fallback to name only
                    animation.data().put(ExtraAnimationData.NAME_KEY, entry.getKey());
                }
                animations.put(entry.getKey(), animation);
            } catch (Exception ex) {
                PlayerAnimLib.LOGGER.error("Unable to parse animation: {}", entry.getKey(), ex);
            }
        }

        return animations;
    }

    public static Map<String, String> getParents(JsonObject parentsObj) {
        Map<String, String> parents = new HashMap<>(parentsObj.size());
        for (Map.Entry<String, JsonElement> entry : parentsObj.entrySet()) {
            parents.put(UniversalAnimLoader.getCorrectPlayerBoneName(entry.getKey()), entry.getValue().getAsString());
        }
        return parents;
    }

    public static Map<String, CustomModelBone> getModel(JsonObject modelObj, JsonDeserializationContext ctx) {
        Map<String, CustomModelBone> bones = new HashMap<>(modelObj.size());
        for (Map.Entry<String, JsonElement> entry : modelObj.entrySet()) {
            bones.put(entry.getKey(), ctx.deserialize(entry.getValue(), CustomModelBone.class));
        }
        return bones;
    }

    public static String getCorrectPlayerBoneName(String name) {
        return UPPERCASE_PATTERN.matcher(name).replaceAll("_$1").toLowerCase(Locale.ROOT);
    }

    public static String restorePlayerBoneName(String name) {
        StringBuilder result = new StringBuilder();
        String lowerCase = name.toLowerCase(Locale.ROOT);

        Matcher matcher = UNDERSCORE_PATTERN.matcher(lowerCase);
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(lowerCase, lastEnd, matcher.start());
            result.append(Character.toUpperCase(matcher.group(1).charAt(0)));
            lastEnd = matcher.end();
        }
        result.append(lowerCase.substring(lastEnd));
        return result.toString();
    }
}
