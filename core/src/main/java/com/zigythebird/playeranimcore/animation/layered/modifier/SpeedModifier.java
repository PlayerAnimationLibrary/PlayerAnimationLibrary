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

import com.zigythebird.playeranimcore.animation.AnimationData;

/**
 * Modifies the animation speed.
 * The speed parameter is a multiplier so for example, 2 means twice the speed, the animation will take half as long.
 * <code>length = 1/speed</code>
 */
public class SpeedModifier extends AbstractModifier {
    public float speed;

    private float delta = 0;

    private float shiftedDelta = 0;

    public SpeedModifier(float speed) {
        if (!Float.isFinite(speed)) throw new IllegalArgumentException("Speed must be a finite number");
        this.speed = speed;
    }

    @Override
    public void tick(AnimationData state) {
        float delta = 1f - this.delta;
        this.delta = 0;
        step(delta, state);
    }

    @Override
    public void setupAnim(AnimationData state) {
        float delta = state.getPartialTick() - this.delta; //this should stay positive
        this.delta = state.getPartialTick();
        step(delta, state);
        super.setupAnim(state);
    }

    protected void step(float delta, AnimationData state) {
        delta *= speed;
        delta += shiftedDelta;
        while (delta > 1) {
            delta -= 1;
            super.tick(state);
        }
        state.setPartialTick(delta);
        this.shiftedDelta = delta;
    }

    @Override
    public String toString() {
        return "SpeedModifier{" +
                "speed=" + speed +
                ", anim=" + anim +
                '}';
    }
}
