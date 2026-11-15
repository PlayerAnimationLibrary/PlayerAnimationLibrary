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

import com.google.j2objc.annotations.AutoreleasePool;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimcore.util.MatrixUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.redlance.mocha.runtime.MolangInterpreter;

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

    /**
     * The torso bend as a transform for the {@link #top_bones}, only up to date while {@link #applyTorsoBend} is set.
     */
    private final Matrix4f torsoBendMatrix = new Matrix4f();
    private boolean applyTorsoBend;

    /**
     * Instantiates a new {@code AnimationController}
     *
     * @param animationHandler The {@link AnimationStateHandler} animation state handler responsible for deciding which animations to play
     * @param molangRuntime    A function that provides the MoLang runtime engine for this animation controller when applied
     */
    public HumanoidAnimationController(AnimationStateHandler animationHandler, Function<AnimationController, MolangInterpreter<AnimationController>> molangRuntime) {
        this(animationHandler, BONE_POSITIONS, molangRuntime);
    }

    /**
     * Instantiates a new {@code AnimationController}
     *
     * @param animationHandler The {@link AnimationStateHandler} animation state handler responsible for deciding which animations to play
     * @param bonePositions    Map of bones and their pivots
     * @param molangRuntime    A function that provides the MoLang runtime engine for this animation controller when applied
     */
    public HumanoidAnimationController(AnimationStateHandler animationHandler, Map<String, Vec3f> bonePositions, Function<AnimationController, MolangInterpreter<AnimationController>> molangRuntime) {
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
    @AutoreleasePool
    public void process(AnimationData state) {
        super.process(state);
        PlayerAnimBone torso = bones.get("torso");
        this.applyTorsoBend = Math.abs(torso.bend) > 0.001 && this.currentAnimation != null && this.currentAnimation.animation().data().getNullable(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY) == Boolean.TRUE;
        if (!this.applyTorsoBend) return;

        // The bend takes place in the space of the torso, but the bones come with their parents already applied,
        // so the torso is taken off them for the bend and put back after it
        MatrixUtil.prepMatrixForBone(this.torsoBendMatrix.identity(), torso, getBonePosition("torso"));
        Matrix4f torsoInverse = this.torsoBendMatrix.invert(new Matrix4f());
        // A torso scaled down to nothing leaves no space to bend in
        this.applyTorsoBend = torsoInverse.isFinite();
        this.torsoBendMatrix.translate(0, 18, 0).rotateX(torso.bend).translate(0, -18, 0).mul(torsoInverse);
    }

    @Override
    public PlayerAnimBone get3DTransformRaw(@NotNull PlayerAnimBone bone) {
        bone = super.get3DTransformRaw(bone);
        String name = bone.getName();
        if (this.applyTorsoBend && this.top_bones.contains(name)) {
            MatrixUtil.applyMatrixToBone(bone, new Matrix4f(this.torsoBendMatrix), getBonePosition(name));
        }
        return bone;
    }
}
