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
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * It is a representation of your pose on the frame.
 * Override {@link IAnimation#setupAnim} and set the pose there.
 * Must set the isActive variable to true in the tick method for this to work!
 * But remember to set it to false when inactive.
 */
public abstract class PlayerAnimationFrame implements IAnimation {
    protected boolean isActive = false;

    protected PlayerBone head = new PlayerBone();
    protected PlayerBone body = new PlayerBone();
    protected PlayerBone rightArm = new PlayerBone();
    protected PlayerBone leftArm = new PlayerBone();
    protected PlayerBone rightLeg = new PlayerBone();
    protected PlayerBone leftLeg = new PlayerBone();
    protected PlayerBone rightItem = new PlayerBone();
    protected PlayerBone leftItem = new PlayerBone();

    HashMap<String, PlayerBone> parts = new HashMap<>();

    public PlayerAnimationFrame() {
        parts.put("head", head);
        parts.put("body", body);
        parts.put("right_arm", rightArm);
        parts.put("left_arm", leftArm);
        parts.put("right_leg", rightLeg);
        parts.put("left_leg", leftLeg);
        parts.put("right_item", rightItem);
        parts.put("left_item", leftItem);
    }

    @Override
    public void tick(AnimationData state) {
        IAnimation.super.tick(state);
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    /**
     * Reset every part, those parts won't influence the animation
     * Don't use it if you don't want to set every part in every frame
     */
    public void resetPose() {
        for (Map.Entry<String, PlayerBone> entry: parts.entrySet()) {
            entry.getValue().setToInitialPose();
        }
    }

    /**
     * Sets all parts to their default values so no vanilla animations or animations from mother mods are applied.
     */
    public void enableAll() {
        for (Map.Entry<String, PlayerBone> entry: parts.entrySet()) {
            entry.getValue().enableAll();
        }
    }
    
    @Override
    public void get3DTransform(@NotNull PlayerAnimBone bone) {
        PlayerBone part = parts.get(bone.getName());
        if (part != null) part.applyToBone(bone);
    }

    //TODO Maybe replace this with smth better
    public static class PlayerBone {
        public Float offsetPosX = null;
        public Float offsetPosY = null;
        public Float offsetPosZ = null;

        public Float rotX = null;
        public Float rotY = null;
        public Float rotZ = null;

        public Float scaleX = null;
        public Float scaleY = null;
        public Float scaleZ = null;
        
        public PlayerBone() {
            super();
        }

        /**
         * Makes part no longer influence the animation in any way.
         * Other animations will be free to influence this bone unless you change some values.
         */
        public void setToInitialPose() {
            this.rotX = null;
            this.rotY = null;
            this.rotZ = null;

            this.offsetPosX = null;
            this.offsetPosY = null;
            this.offsetPosZ = null;

            this.scaleX = null;
            this.scaleY = null;
            this.scaleZ = null;
        }

        /**
         * Sets all parts to their default values so no vanilla animations or animations from mother mods are applied.
         */
        public void enableAll() {
            this.rotX = 0F;
            this.rotY = 0F;
            this.rotZ = 0F;

            this.offsetPosX = 0F;
            this.offsetPosY = 0F;
            this.offsetPosZ = 0F;

            this.scaleX = 1F;
            this.scaleY = 1F;
            this.scaleZ = 1F;
        }

        public PlayerAnimBone applyToBone(PlayerAnimBone bone) {
            if (offsetPosX != null)
                bone.position.x = offsetPosX;
            if (offsetPosY != null)
                bone.position.y = offsetPosY;
            if (offsetPosZ != null)
                bone.position.z = offsetPosZ;

            if (rotX != null)
                bone.rotation.x = rotX;
            if (rotY != null)
                bone.rotation.y = rotY;
            if (rotZ != null)
                bone.rotation.z = rotZ;

            if (scaleX != null)
                bone.scale.x = scaleX;
            if (scaleY != null)
                bone.scale.y = scaleY;
            if (scaleZ != null)
                bone.scale.z = scaleZ;

            return bone;
        }
    }

    @Override
    public String toString() {
        return "PlayerAnimationFrame{" +
                "isActive=" + isActive +
                ", parts=" + parts +
                '}';
    }
}
