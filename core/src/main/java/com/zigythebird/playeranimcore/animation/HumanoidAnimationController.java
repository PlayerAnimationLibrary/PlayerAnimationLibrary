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

import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimcore.util.MatrixUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import team.unnamed.mocha.MochaEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Map.entry;

public class HumanoidAnimationController extends AnimationController {
    /**
     * Bone pivot point positions used to apply custom pivot point translations.
     */
    public static final Map<String, Vec3f> BONE_POSITIONS = Map.ofEntries(
            entry("right_item", new Vec3f(6, 12, -2)),
            entry("left_item", new Vec3f(-6, 12, -2)),
            entry("right_arm", new Vec3f(5, 22, 0)),
            entry("left_arm", new Vec3f(-5, 22, 0)),
            entry("left_leg", new Vec3f(-2f, 12, 0f)),
            entry("right_leg", new Vec3f(2f, 12, 0f)),
            entry("torso", new Vec3f(0, 24, 0)),
            entry("head", new Vec3f(0, 24, 0)),
            entry("body", new Vec3f(0, 12, 0)),
            entry("cape", new Vec3f(0, 24, 2)),
            entry("elytra", new Vec3f(0, 24, 2))
    );

    /**
     * Used for applying torso bend to bones like the head.
     */
    protected List<String> top_bones;

    private float torsoBend;
    private float torsoBendYPosMultiplier;
    private float torsoBendZPosMultiplier;
    private int torsoBendSign;

    /**
     * Instantiates a new {@code AnimationController}
     *
     * @param animationHandler The {@link AnimationStateHandler} animation state handler responsible for deciding which animations to play
     * @param molangRuntime    A function that provides the MoLang runtime engine for this animation controller when applied
     */
    public HumanoidAnimationController(AnimationStateHandler animationHandler, Function<AnimationController, MochaEngine<AnimationController>> molangRuntime) {
        this(animationHandler, BONE_POSITIONS, molangRuntime);
    }

    /**
     * Instantiates a new {@code AnimationController}
     *
     * @param animationHandler The {@link AnimationStateHandler} animation state handler responsible for deciding which animations to play
     * @param bonePositions    Map of bones and their pivots
     * @param molangRuntime    A function that provides the MoLang runtime engine for this animation controller when applied
     */
    public HumanoidAnimationController(AnimationStateHandler animationHandler, Map<String, Vec3f> bonePositions, Function<AnimationController, MochaEngine<AnimationController>> molangRuntime) {
        super(animationHandler, bonePositions, molangRuntime);
    }

    @Override
    public void registerBones() {
        this.top_bones = new ArrayList<>();

        this.registerPlayerAnimBone("body");
        this.registerTopPlayerAnimBone("right_arm");
        this.registerTopPlayerAnimBone("left_arm");
        this.registerPlayerAnimBone("right_leg");
        this.registerPlayerAnimBone("left_leg");
        this.registerTopPlayerAnimBone("head");
        this.registerPlayerAnimBone("torso");
        this.registerPlayerAnimBone("right_item");
        this.registerPlayerAnimBone("left_item");
        this.registerTopPlayerAnimBone("cape");
        this.registerPlayerAnimBone("elytra");
    }

    public void registerTopPlayerAnimBone(String name) {
        this.top_bones.add(name);
        this.registerPlayerAnimBone(name);
    }

    @Override
    public void process(AnimationData state) {
        super.process(state);
        /* TODO Commented out until we decide what we want to do with bends
        this.torsoBend = bones.get("torso").getBend();
        float absBend = Math.abs(this.torsoBend);
        if (absBend > 0.001 && (this.currentAnimation != null && this.currentAnimation.animation().data().getNullable(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY) == Boolean.TRUE)) {
            this.torsoBendSign = (int) Math.signum(this.torsoBend);
            this.torsoBendYPosMultiplier = (float) -(1 - Math.cos(absBend));
            this.torsoBendZPosMultiplier = (float) (1 - Math.sin(absBend));
        } else this.torsoBendSign = 0;
         */
    }

    @Override
    public PlayerAnimBone get3DTransformRaw(@NotNull PlayerAnimBone bone) {
        bone = super.get3DTransformRaw(bone);
        String name = bone.getName();
        if (this.torsoBendSign != 0 && this.top_bones.contains(name)) {
            Matrix4f matrix4f = new Matrix4f();
            matrix4f.translate(0, 16, 0);
            matrix4f.rotateX(this.torsoBend);
            matrix4f.translate(0, -16, 0);
            MatrixUtil.applyMatrixToBone(bone, matrix4f, getBonePosition(name));
        }
        return bone;
    }
}
