package com.zigythebird.playeranimcore.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.zigythebird.playeranimcore.enums.AnimationFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redlance.platformtools.webp.decoder.DecodedImage;

import java.io.IOException;
import java.util.*;

public record ExtraAnimationData(Map<String, Object> data) {
    public static final String NAME_KEY = "name";
    public static final String UUID_KEY = "uuid";
    public static final String FORMAT_KEY = "format";
    public static final String BEGIN_TICK_KEY = "beginTick";
    public static final String END_TICK_KEY = "endTick";
    public static final String EASING_BEFORE_KEY = "easeBeforeKeyframe";
    public static final String APPLY_BEND_TO_OTHER_BONES_KEY = "applyBendToOtherBones";
    public static final String PARTICLE_EFFECTS_KEY = "particleEffects";
    public static final String DISABLE_AXIS_IF_NOT_MODIFIED = "disableAxisIfNotModified";

    public ExtraAnimationData(String key, Object value) {
        this(new HashMap<>(Collections.singletonMap(key, value)));
    }

    public ExtraAnimationData() {
        this(new HashMap<>(1)); // Mutable, 1 for name
    }

    @Nullable
    public String name() {
        Object data = data().get(NAME_KEY);
        String name;
        if (data instanceof JsonObject jsonObject) {
            name = jsonObject.get("fallback").getAsString();
        } else name = (String) data;
        return name != null ? name.toLowerCase(Locale.ROOT).replace("\"", "").replace(" ", "_") : null;
    }

    public boolean has(String name) {
        return data().containsKey(name);
    }

    public byte @Nullable [] getBinary(String name) {
        Object obj = getRaw(name);
        if (obj == null) return null;
        return convertToBytes(name, obj);
    }

    private byte @Nullable [] convertToBytes(String name, Object obj) {
        if (obj instanceof DecodedImage image) {
            try {
                return image.toPng();
            } catch (IOException e) {
                return null;
            }
        }
        if (obj instanceof String str) {
            try {
                put(name, obj = Base64.getDecoder().decode(str));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        if (obj instanceof byte[] bytes) {
            return bytes;
        }
        return null;
    }

    public @Nullable DecodedImage getImage(String name) throws IOException {
        Object obj = getRaw(name);
        if (obj == null) return null;

        if (obj instanceof DecodedImage image) {
            return image;
        } else {
            byte[] data = convertToBytes(name, obj);
            if (data == null) return null;
            DecodedImage image = DecodedImage.fromPng(data);
            put(name, image);
            return image;
        }
    }

    public Object getRaw(String name) {
        return data().get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        Object obj = getRaw(key);
        if (obj == null) return Optional.empty();

        try {
            return Optional.of((T) obj);
        } catch (Throwable ignored) {}

        return Optional.empty();
    }

    public <T> T getNullable(String key) {
        return this.<T>get(key).orElse(null);
    }

    public List<?> getList(String key) {
        Object obj = getRaw(key);
        return switch (obj) {
            case null -> Collections.emptyList();
            case JsonArray json -> json.asList();
            case List<?> list -> list;
            default -> throw new ClassCastException(obj.getClass().getName());
        };
    }

    public void put(String name, Object object) {
        data.put(name, object);
    }

    public void fromJson(JsonObject node, boolean root) {
        for (Map.Entry<String, JsonElement> entry : node.entrySet()) {
            String key = entry.getKey();
            if (root && ("version".equalsIgnoreCase(key) || "emote".equalsIgnoreCase(key))) continue;
            data().put(key, getValue(entry.getValue()));
        }
    }

    public Object getValue(JsonElement element) {
        if (element instanceof JsonPrimitive p) {
            if (p.isBoolean()) {
                return p.getAsBoolean();
            } else if (p.isString()) {
                return p.getAsString();
            } else if (p.isNumber()) {
                return p.getAsFloat();
            }
        }
        if (element instanceof JsonArray array) {
            List<Object> list = new ArrayList<>(array.size());
            for (JsonElement element1 : array) {
                list.add(getValue(element1));
            }
            return list;
        }
        return element.toString();
    }

    public ExtraAnimationData copy() {
        return new ExtraAnimationData(new HashMap<>(data()));
    }

    public boolean isDisableAxisIfNotModified() {
        return this.<Boolean>get(DISABLE_AXIS_IF_NOT_MODIFIED).orElse(true);
    }

    public boolean isAnimationPlayerAnimatorFormat() {
        return this.<AnimationFormat>get(ExtraAnimationData.FORMAT_KEY).orElse(null) == AnimationFormat.PLAYER_ANIMATOR;
    }

    @Override
    public @NotNull String toString() {
        return this.data.toString();
    }
}
