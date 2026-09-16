package com.zigythebird.playeranimcore.util;

import com.google.gson.*;
import com.google.j2objc.annotations.ReflectionSupport;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Json helper class for various json functions
 */
@ReflectionSupport(ReflectionSupport.Level.FULL)
public final class JsonUtil {
    private JsonUtil() {}

    /**
     * Converts a {@link JsonArray} to a {@link List} of elements of a pre-determined type
     *
     * @param array The {@code JsonArray} to convert
     * @param elementTransformer Transformation function that converts a {@link JsonElement} to the intended output object
     */
    public static <T> List<T> jsonArrayToList(@Nullable JsonArray array, Function<JsonElement, T> elementTransformer) {
        if (array == null)
            return new ObjectArrayList<>();

        List<T> list = new ObjectArrayList<>(array.size());

        for (JsonElement element : array) {
            list.add(elementTransformer.apply(element));
        }

        return list;
    }

    @Nullable
    @Contract("_,_,!null->!null;_,_,null->_")
    public static String getAsString(JsonObject json, String memberName, @Nullable String fallback) {
        return json.has(memberName) ? convertToString(json.get(memberName), memberName) : fallback;
    }

    public static boolean convertToBoolean(JsonElement json, String memberName) {
        if (json.isJsonPrimitive()) {
            return json.getAsBoolean();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a Boolean, was " + getType(json));
        }
    }

    public static boolean getAsBoolean(JsonObject json, String memberName, boolean fallback) {
        return json.has(memberName) ? convertToBoolean(json.get(memberName), memberName) : fallback;
    }

    public static float convertToFloat(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            return json.getAsFloat();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a Float, was " + getType(json));
        }
    }

    public static float getAsFloat(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToFloat(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Float");
        }
    }

    public static JsonObject convertToJsonObject(JsonElement json, String memberName) {
        if (json.isJsonObject()) {
            return json.getAsJsonObject();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a JsonObject, was " + getType(json));
        }
    }

    @Nullable
    @Contract("_,_,!null->!null;_,_,null->_")
    public static JsonObject getAsJsonObject(JsonObject json, String memberName, @Nullable JsonObject fallback) {
        return json.has(memberName) ? convertToJsonObject(json.get(memberName), memberName) : fallback;
    }

    public static String convertToString(JsonElement json, String memberName) {
        if (json.isJsonPrimitive()) {
            return json.getAsString();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a string, was " + getType(json));
        }
    }

    public static String getAsString(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToString(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a string");
        }
    }

    public static JsonArray convertToJsonArray(JsonElement json, String memberName) {
        if (json.isJsonArray()) {
            return json.getAsJsonArray();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a JsonArray, was " + getType(json));
        }
    }

    public static JsonArray getAsJsonArray(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToJsonArray(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a JsonArray");
        }
    }

    public static String getType(@Nullable JsonElement json) {
        String string = String.valueOf(json);
        if (json == null) {
            return "null (missing)";
        } else if (json.isJsonNull()) {
            return "null (json)";
        } else if (json.isJsonArray()) {
            return "an array (" + string + ")";
        } else if (json.isJsonObject()) {
            return "an object (" + string + ")";
        } else {
            if (json.isJsonPrimitive()) {
                JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
                if (jsonPrimitive.isNumber()) {
                    return "a number (" + string + ")";
                }

                if (jsonPrimitive.isBoolean()) {
                    return "a boolean (" + string + ")";
                }
            }

            return string;
        }
    }
}
