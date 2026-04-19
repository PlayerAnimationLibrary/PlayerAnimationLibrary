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

package com.zigythebird.playeranim.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.accessors.IBoneUpdater;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(value = PlayerModel.class, priority = 2001)//Apply after NotEnoughAnimation's inject
public class PlayerModelMixin extends HumanoidModel<AvatarRenderState> implements IBoneUpdater {
    @Unique
    private final PlayerAnimBone pal$head = new PlayerAnimBone("head");
    @Unique
    private final PlayerAnimBone pal$torso = new PlayerAnimBone("torso");
    @Unique
    private final PlayerAnimBone pal$rightArm = new PlayerAnimBone("right_arm");
    @Unique
    private final PlayerAnimBone pal$leftArm = new PlayerAnimBone("left_arm");
    @Unique
    private final PlayerAnimBone pal$rightLeg = new PlayerAnimBone("right_leg");
    @Unique
    private final PlayerAnimBone pal$leftLeg = new PlayerAnimBone("left_leg");

    public PlayerModelMixin(ModelPart root, Function<Identifier, RenderType> renderType) {
        super(root, renderType);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At(value = "HEAD"))
    private void setDefaultBeforeRender(AvatarRenderState avatarRenderState, CallbackInfo ci){
        this.head.visible = true; // For some reason only the head doesn't get reset
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At(value = "RETURN"))
    private void setupPlayerAnimation(AvatarRenderState avatarRenderState, CallbackInfo ci) {
        if (avatarRenderState instanceof IAvatarAnimationState state && state.playerAnimLib$getAnimManager() != null) {
            AvatarAnimManager emote = state.playerAnimLib$getAnimManager();

            if (emote.isActive()) {
                pal$updatePart(emote, this.head, pal$head);
                pal$updatePart(emote, this.rightArm, pal$rightArm);
                pal$updatePart(emote, this.leftArm, pal$leftArm);
                pal$updatePart(emote, this.rightLeg, pal$rightLeg);
                pal$updatePart(emote, this.leftLeg, pal$leftLeg);
                pal$updatePart(emote, this.body, pal$torso);
            } else {
                pal$resetAll(emote);
            }

            if (state.playerAnimLib$isFirstPersonPass()) {
                // Hiding all parts, because they should not be visible in first person
                this.head.visible = false;
                this.body.visible = false;
                this.leftLeg.visible = false;
                this.rightLeg.visible = false;
                // Showing arms based on configuration
                FirstPersonConfiguration config = emote.getFirstPersonConfiguration();
                this.rightArm.visible = config.isShowRightArm();
                this.leftArm.visible = config.isShowLeftArm();
            }
        } else {
            pal$resetAll(null);
        }
    }

    @Override
    public void pal$updatePart(AvatarAnimManager emote, ModelPart part, PlayerAnimBone bone) {
        RenderUtil.copyVanillaPart(part, bone);
        emote.updatePart(part, bone);
    }

    @WrapWithCondition(method = "translateToHand(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private boolean translateToHand(ModelPart modelPart, PoseStack poseStack, @Local(argsOnly = true) AvatarRenderState avatarRenderState) {
        if (avatarRenderState instanceof IAvatarAnimationState state && state.playerAnimLib$getAnimManager() != null && state.playerAnimLib$getAnimManager().isActive()) {
            poseStack.translate(modelPart.x / 16.0F, modelPart.y / 16.0F, modelPart.z / 16.0F);
            if (modelPart.xRot != 0.0F || modelPart.yRot != 0.0F || modelPart.zRot != 0.0F) {
                RenderUtil.rotateZYX(poseStack.last(), modelPart.zRot, modelPart.yRot, modelPart.xRot);
            }
            poseStack.translate(0, (modelPart.yScale - 1) * 0.609375, (modelPart.zScale - 1) * 0.0625);

            return false;
        }
        return true;
    }

    @Override
    public void pal$resetAll(@Nullable AvatarAnimManager emote) {
        // no-op
    }
}
