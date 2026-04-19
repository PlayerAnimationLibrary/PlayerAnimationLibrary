/*
 * MIT License
 *
 * Copyright (c) 2024 GeckoLib
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.zigythebird.playeranimcore.animation;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.CustomInstructionKeyframeData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.ParticleKeyframeData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.SoundKeyframeData;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimcore.math.Vec3f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A compiled animation instance for use by the {@link AnimationController}
 * <p>
 * Modifications or extensions of a compiled Animation are not supported, and therefore an instance of <code>Animation</code> is considered final and immutable
 */
public record Animation(ExtraAnimationData data, float length, LoopType loopType, Map<String, BoneAnimation> boneAnimations, Keyframes keyFrames, Map<String, Vec3f> bones, Map<String, String> parents) implements Supplier<UUID> {
    public record Keyframes(SoundKeyframeData[] sounds, ParticleKeyframeData[] particles, CustomInstructionKeyframeData[] customInstructions) {
        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(sounds), Arrays.hashCode(particles), Arrays.hashCode(customInstructions));
        }
    }

    static Animation generateWaitAnimation(float length) {
        return new Animation(new ExtraAnimationData(ExtraAnimationData.NAME_KEY, "internal.wait"), length, LoopType.PLAY_ONCE,
                Collections.emptyMap(), UniversalAnimLoader.NO_KEYFRAMES, new HashMap<>(), new HashMap<>());
    }

    public boolean isPlayingAt(float tick) {
        return loopType.shouldPlayAgain(null, this) || tick < length() && tick > 0;
    }

    @Nullable
    public BoneAnimation getBone(String id) {
        return this.boneAnimations.get(id);
    }

    public Optional<BoneAnimation> getBoneOptional(String id) {
        return Optional.ofNullable(getBone(id));
    }

    /**
     * Loop type functional interface to define post-play handling for a given animation
     * <p>
     * Custom loop types are supported by extending this class and providing the extended class instance as the loop type for the animation
     */
    @FunctionalInterface
    public interface LoopType {
        Map<String, LoopType> LOOP_TYPES = new ConcurrentHashMap<>(4);

        LoopType DEFAULT = new LoopType() {
            @Override
            public boolean shouldPlayAgain(@Nullable AnimationController controller, Animation currentAnimation) {
                return currentAnimation.loopType().shouldPlayAgain(controller, currentAnimation);
            }

            @Override
            public float restartFromTick(@Nullable AnimationController controller, Animation currentAnimation) {
                return currentAnimation.loopType().restartFromTick(controller, currentAnimation);
            }
        };
        LoopType PLAY_ONCE = register("play_once", register("false", (controller, currentAnimation) -> false));
        LoopType HOLD_ON_LAST_FRAME = register("hold_on_last_frame", (controller, currentAnimation) -> {
            if (controller != null) controller.pause();
            return true;
        });
        LoopType LOOP = register("loop", register("true", (controller, currentAnimation) -> true));

        /**
         * Override in a custom instance to dynamically decide whether an animation should repeat or stop
         *
         * @param controller The {@link AnimationController} playing the current animation
         * @param currentAnimation The current animation that just played
         * @return Whether the animation should play again, or stop
         */
        boolean shouldPlayAgain(@Nullable AnimationController controller, Animation currentAnimation);

        /**
         * Override in a custom instance to dynamically decide where an animation should start after looping.
         *
         * @param controller The {@link AnimationController} playing the current animation
         * @param currentAnimation The current animation that just played
         * @return The tick the animation starts from after looping.
         */
        default float restartFromTick(@Nullable AnimationController controller, Animation currentAnimation) {
            return 0;
        }

        static LoopType returnToTickLoop(float tick) {
            return new LoopType() {
                @Override
                public boolean shouldPlayAgain(@Nullable AnimationController controller, Animation currentAnimation) {
                    return true;
                }

                @Override
                public float restartFromTick(@Nullable AnimationController controller, Animation currentAnimation) {
                    return tick;
                }
            };
        }

        /**
         * Retrieve a LoopType instance based on a {@link JsonElement}
         * <p>
         * Returns either {@link LoopType#PLAY_ONCE} or {@link LoopType#LOOP} based on a boolean or string element type,
         * or any other registered loop type with a matching type string
         *
         * @param json The <code>loop</code> {@link JsonElement} to attempt to parse
         * @return A usable LoopType instance
         */
        static LoopType fromJson(JsonElement json) {
            if (json == null || !json.isJsonPrimitive())
                return PLAY_ONCE;

            JsonPrimitive primitive = json.getAsJsonPrimitive();

            if (primitive.isBoolean())
                return primitive.getAsBoolean() ? LOOP : PLAY_ONCE;

            if (primitive.isString())
                return fromString(primitive.getAsString());

            return PLAY_ONCE;
        }

        static LoopType fromString(String name) {
            return LOOP_TYPES.getOrDefault(name, PLAY_ONCE);
        }

        /**
         * Register a LoopType with Player Animation Library for handling loop functionality of animations
         * <p>
         * <b><u>MUST be called during mod construct</u></b>
         * <p>
         *
         * @param name The name of the loop type
         * @param loopType The loop type to register
         * @return The registered {@code LoopType}
         */
        static LoopType register(String name, LoopType loopType) {
            LOOP_TYPES.put(name, loopType);

            return loopType;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Animation animation)) return false;
        return Float.compare(length, animation.length) == 0 && Objects.equals(keyFrames, animation.keyFrames) && Objects.equals(bones, animation.bones) && Objects.equals(parents, animation.parents) && Objects.equals(boneAnimations, animation.boneAnimations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(length, boneAnimations, keyFrames, bones, parents);
    }

    private UUID generateUuid() {
        return generateUuid(
                Float.floatToIntBits(length),
                boneAnimations.hashCode(),
                keyFrames.hashCode(),
                bones.hashCode(),
                parents.hashCode()
        );
    }

    private static UUID generateUuid(int... hashes) {
        long mostSigBits = 17L;
        long leastSigBits = 31L;
        for (int hash : hashes) {
            mostSigBits = 31L * mostSigBits + hash;
            leastSigBits = 37L * leastSigBits + hash;
        }
        return new UUID(mostSigBits, leastSigBits);
    }

    public UUID uuid() {
        if (!data().has(ExtraAnimationData.UUID_KEY)) {
            data().put(ExtraAnimationData.UUID_KEY, generateUuid());
        } else if (data().getRaw(ExtraAnimationData.UUID_KEY) instanceof String str) {
            data().put(ExtraAnimationData.UUID_KEY,  UUID.fromString(str));
        }
        return data().<UUID>get(ExtraAnimationData.UUID_KEY).orElseThrow();
    }

    @Override
    public @NotNull String toString() {
        return "Animation{" +
                "data=" + data +
                ", length=" + length +
                '}';
    }

    @Override
    public UUID get() {
        return uuid();
    }

    @NotNull
    public String getNameOrId() {
        return Objects.requireNonNullElseGet(data().name(), () -> uuid().toString());
    }
}
