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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranim.animation.MinecraftCustomBone;
import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.PlayerAnimLib;
import com.zigythebird.playeranimcore.animation.HumanoidAnimationController;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.math.Vec3f;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> {
    @Shadow
    public abstract M getModel();

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;scale(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void doTranslations(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        if (state instanceof IAvatarAnimationState avatarRenderState) {
            var animationPlayer = avatarRenderState.playerAnimLib$getAnimManager();
            if (animationPlayer != null && animationPlayer.isActive()) {
                avatarRenderState.playerAnimLib$getAnimManager().handleAnimations(animationPlayer.getTickDelta(), false, avatarRenderState.playerAnimLib$isFirstPersonPass());
                poseStack.scale(-1.0F, -1.0F, 1.0F);

                //These are additive properties
                PlayerAnimBone body = animationPlayer.get3DTransform("body");

                poseStack.translate(-body.position.x/16, body.position.y/16 + 0.75, body.position.z/16);
                body.rotation.x *= -1;
                body.rotation.y *= -1;
                RenderUtil.rotateMatrixAroundBone(poseStack, body);
                poseStack.scale(body.scale.x, body.scale.y, body.scale.z);

                poseStack.translate(0, -0.75, 0);

                poseStack.scale(-1.0F, -1.0F, 1.0F);
            }
        }
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
                    shift = At.Shift.AFTER
            )
    )
    public void pal$renderCustomModels(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        if (!(state instanceof IAvatarAnimationState avatarRenderState)) return;

        AvatarAnimManager animationPlayer = avatarRenderState.playerAnimLib$getAnimManager();
        if (animationPlayer == null || !animationPlayer.isActive()) return;

        int lightCoords = state.lightCoords;

        poseStack.pushPose();

        poseStack.translate(0, 1.501F, 0);
        poseStack.scale(-1, -1, -1);

        AtomicInteger numberOfPushes = new AtomicInteger();

        animationPlayer.collectModels(bone -> {
            poseStack.pushPose();
            numberOfPushes.addAndGet(1);
            Vec3f pivot = bone.getPivot().div(16);
            poseStack.translate(pivot.x() - bone.position.x() / 16f, pivot.y() + bone.position.y() / 16f, pivot.z() - bone.position.z() / 16f);
            RenderUtil.applyBoneRotationScale(poseStack, bone);
            poseStack.translate(-pivot.x() - 0.5f, -pivot.y(), -pivot.z() - 0.5f);

            if (!(bone instanceof MinecraftCustomBone mcBone) || !mcBone.hasModel()) return;

            QuadCollection bakedPart = mcBone.getGeometry();

            submitNodeCollector.submitCustomGeometry(
                    poseStack, mcBone.getRenderType(),

                    (pose, buffer) -> {
                        QuadInstance instance = new QuadInstance();
                        instance.setLightCoords(lightCoords);
                        instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

                        for (BakedQuad quad : bakedPart.getQuads(null)) {
                            buffer.putBakedQuad(pose, quad, instance);
                        }
                        for (Direction dir : ModelBlockRenderer.DIRECTIONS) {
                            for (BakedQuad quad : bakedPart.getQuads(dir)) {
                                buffer.putBakedQuad(pose, quad, instance);
                            }
                        }
                    }
            );
        }, (modelPartName) -> {
            poseStack.pushPose();
            numberOfPushes.addAndGet(1);

            ModelPart modelPart = null;
            Vec3f pivot = null;
            
            if (this.getModel() instanceof PlayerModel playerModel) {
                //TODO Add cape
                //TODO Maybe add a way to register custom bones
                pivot = HumanoidAnimationController.BONE_POSITIONS.getOrDefault(modelPartName, null);
                switch (modelPartName) {
                    case "head" -> modelPart = playerModel.head;
                    case "torso" -> modelPart = playerModel.body;
                    case "right_arm" -> modelPart = playerModel.rightArm;
                    case "left_arm" -> modelPart = playerModel.leftArm;
                    case "right_leg" -> modelPart = playerModel.rightLeg;
                    case "left_leg" -> modelPart = playerModel.leftLeg;
                }
            }
            
            if (modelPart != null && pivot != null) {
                pivot = pivot.div(16);
                PartPose initialPose = modelPart.getInitialPose();
                poseStack.translate(pivot.x() - (modelPart.x - initialPose.x()) / 16f,
                        pivot.y() - (modelPart.y - initialPose.y()) / 16f,
                        pivot.z() - (modelPart.z - initialPose.z()) / 16f);
                RenderUtil.rotateZYX(poseStack.last(), modelPart.xRot, modelPart.yRot - initialPose.yRot(), modelPart.zRot);
                if (modelPart.xScale != 1 || modelPart.yScale != 1 || modelPart.zScale != 1)
                    poseStack.scale(modelPart.xScale, modelPart.yScale, modelPart.zScale);
                poseStack.translate(-pivot.x() - 0.5f, -pivot.y(), -pivot.z() - 0.5f);
            }
        }, () -> {
            if (numberOfPushes.get() > 0) {
                numberOfPushes.getAndDecrement();
                poseStack.popPose();
            }
            else {
                PlayerAnimLib.LOGGER.error("During model collection an animation popped a bone when all bones had already been popped.");
            }
        });

        for (int i = numberOfPushes.get(); i > 0; i--) {
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
