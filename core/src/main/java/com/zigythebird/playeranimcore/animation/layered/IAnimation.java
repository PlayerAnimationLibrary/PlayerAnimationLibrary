/*
 * MIT License
 *
 * Copyright (c) 2022 KosmX
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

package com.zigythebird.playeranimcore.animation.layered;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.CustomBone;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface IAnimation {
    FirstPersonConfiguration DEFAULT_FIRST_PERSON_CONFIG = new FirstPersonConfiguration();

    /**
     * Animation tick, on lag-free client 20 [tick/sec]
     * This is called even when the animation is inactive
     */
    default void tick(AnimationData state) {}

    /**
     * Called before rendering a character
     * Only called when the animation is active.
     * @param state Current animation state which can be used to get the player and the current partial tick.
     */
    default void setupAnim(AnimationData state) {}

    /**
     * Is the animation currently active?
     */
    boolean isActive();

    /**
     * Called every frame for each bone allowing you to transform them to your liking.
     * Only called when the animation is active.
     * @param bone the bone being currently animated.
     */
    void get3DTransform(@NotNull PlayerAnimBone bone);

    default PlayerAnimBone get3DTransform(@NotNull String name) {
        PlayerAnimBone bone = new PlayerAnimBone(name);
        get3DTransform(bone);
        return bone;
    }

    /**
     * Keep in mind that modifiers can't affect the first-person mode, at least not by default.
     */
    @NotNull
    default FirstPersonMode getFirstPersonMode() {
        return FirstPersonMode.NONE;
    }

    /**
     * Keep in mind that modifiers can't affect the first-person configuration, at least not by default.
     */
    @NotNull
    default FirstPersonConfiguration getFirstPersonConfiguration() {
        return DEFAULT_FIRST_PERSON_CONFIG;
    }

    /**
     * Return true if the animation should be removed.
     */
    default boolean canRemove() {
        return false;
    }

    default void collectModels(Consumer<CustomBone> consumer) {}
}
