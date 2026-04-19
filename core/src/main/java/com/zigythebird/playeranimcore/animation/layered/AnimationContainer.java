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
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A container to make swapping animation object easier
 * It will clone the behaviour of the held animation
 * <p>
 * you can put endless AnimationContainer into each other
 * @param <T> Nullable animation
 */
public class AnimationContainer<T extends IAnimation> implements IAnimation {
    @Nullable
    protected T anim;

    public AnimationContainer(@Nullable T anim) {
        this.anim = anim;
    }

    public AnimationContainer() {
        this.anim = null;
    }

    public void setAnim(@Nullable T newAnim) {
        this.anim = newAnim;
    }

    public @Nullable T getAnim() {
        return this.anim;
    }

    @Override
    public boolean isActive() {
        return anim != null && anim.isActive();
    }

    @Override
    public void tick(AnimationData state) {
        if (anim != null) anim.tick(state);
    }

    @Override
    public void get3DTransform(@NotNull PlayerAnimBone bone) {
        if (anim != null) anim.get3DTransform(bone);
    }

    @Override
    public void setupAnim(AnimationData state) {
        if (this.anim != null) this.anim.setupAnim(state);
    }

    @Override
    public @NotNull FirstPersonMode getFirstPersonMode() {
        return anim != null ? anim.getFirstPersonMode() : FirstPersonMode.NONE;
    }

    @Override
    public @NotNull FirstPersonConfiguration getFirstPersonConfiguration() {
        return anim != null ? anim.getFirstPersonConfiguration() : IAnimation.super.getFirstPersonConfiguration();
    }

    @Override
    public String toString() {
        return "AnimationContainer{" +
                "anim=" + anim +
                '}';
    }
}
