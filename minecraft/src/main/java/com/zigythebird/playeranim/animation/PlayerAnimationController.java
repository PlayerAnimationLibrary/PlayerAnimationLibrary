package com.zigythebird.playeranim.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.HumanoidAnimationController;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.mocha.MochaEngine;

import java.util.function.Function;

public class PlayerAnimationController extends HumanoidAnimationController {
    protected final Avatar avatar;

    /**
     * Instantiates a new {@code AnimationController}
     *
     * @param avatar           The object that will be animated by this controller
     * @param animationHandler The {@link AnimationStateHandler} animation state handler responsible for deciding which animations to play
     */
    public PlayerAnimationController(Avatar avatar, AnimationStateHandler animationHandler) {
        this(avatar, animationHandler, MolangLoader::createNewEngine);
    }

    /**
     * Instantiates a new {@code AnimationController}
     *
     * @param avatar           The object that will be animated by this controller
     * @param animationHandler The {@link AnimationStateHandler} animation state handler responsible for deciding which animations to play
     * @param molangRuntime    A function that provides the MoLang runtime engine for this animation controller when applied
     */
    public PlayerAnimationController(Avatar avatar, AnimationStateHandler animationHandler, Function<AnimationController, MochaEngine<AnimationController>> molangRuntime) {
        super(animationHandler, molangRuntime);
        this.avatar = avatar;
    }

    public Avatar getAvatar() {
        return this.avatar;
    }

    public boolean triggerAnimation(Identifier newAnimation, float startAnimFrom) {
        if (PlayerAnimResources.hasAnimation(newAnimation)) {
            triggerAnimation(PlayerAnimResources.getAnimation(newAnimation), startAnimFrom);
            return true;
        }
        PlayerAnimLibMod.LOGGER.error("Could not find animation with the name:" + newAnimation);
        return false;
    }

    public boolean triggerAnimation(Identifier newAnimation) {
        return triggerAnimation(newAnimation, 0);
    }

    public boolean replaceAnimationWithFade(@NotNull AbstractFadeModifier fadeModifier, @Nullable Identifier newAnimation, boolean fadeFromNothing) {
        if (PlayerAnimResources.hasAnimation(newAnimation)) {
            replaceAnimationWithFade(fadeModifier, PlayerAnimResources.getAnimation(newAnimation), fadeFromNothing);
            return true;
        }
        return false;
    }

    public boolean replaceAnimationWithFade(@NotNull AbstractFadeModifier fadeModifier, @Nullable Identifier newAnimation) {
        return replaceAnimationWithFade(fadeModifier, newAnimation, true);
    }

    /**
     * Get the position of a bone in the world in the form of a PoseStack.
     */
    public @Nullable PoseStack getBoneWorldPositionPoseStack(String name, float tickDelta, Vec3 cameraPos) {
        if (!this.activeBones.containsKey(name)) return null;
        PoseStack poseStack = new PoseStack();
        Vec3f pivot = getBonePosition(name);
        Vec3 position = avatar.getPosition(tickDelta).subtract(cameraPos).add(pivot.x(), pivot.y(), pivot.z());
        poseStack.translate(position.x(), position.y(), position.z());
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - Mth.lerp(tickDelta, avatar.yBodyRotO, avatar.yBodyRot)));
        RenderUtil.translateMatrixToBone(poseStack, this.activeBones.get(name));
        return poseStack;
    }
}
