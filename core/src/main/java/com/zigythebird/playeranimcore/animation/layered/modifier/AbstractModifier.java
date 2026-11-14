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

package com.zigythebird.playeranimcore.animation.layered.modifier;

import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.layered.AnimationContainer;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.bones.CustomBone;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public abstract class AbstractModifier extends AnimationContainer<IAnimation> {
    protected IAnimation host;

    public void setHost(IAnimation host) {
        this.host = host;
    }

    /**
     * Could return null if someone makes their own custom way of playing animations.
     */
    @Nullable
    protected AnimationController getController() {
        if (host instanceof AnimationController controller) return controller;
        return null;
    }

    public AbstractModifier() {}

    /**
     * Return true if the modifier should be removed.
     */
    @Override
    public boolean canRemove() {
        return false;
    }

    /**
     * The modifier poses the bones of the animation it holds.
     */
    @Override
    public void collectModels(BiConsumer<IAnimation, CustomBone> consumer) {
        if (this.anim != null) this.anim.collectModels((owner, bone) -> consumer.accept(this, bone));
    }
}
