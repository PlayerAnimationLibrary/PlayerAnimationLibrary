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
import com.mojang.math.Axis;
import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<S extends LivingEntityRenderState> {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;scale(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void doTranslations(S livingEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (livingEntityRenderState instanceof IAvatarAnimationState avatarRenderState) {
            var animationPlayer = avatarRenderState.playerAnimLib$getAnimManager();
            if (animationPlayer != null && animationPlayer.isActive()) {
                avatarRenderState.playerAnimLib$getAnimManager().handleAnimations(animationPlayer.getTickDelta(), false, avatarRenderState.playerAnimLib$isFirstPersonPass());
                poseStack.scale(-1.0F, -1.0F, 1.0F);

                if (avatarRenderState.playerAnimLib$isFirstPersonPass()) {
                    poseStack.translate(0.0F, livingEntityRenderState.eyeHeight, 0.0F);
                    if (animationPlayer.isFirstPersonFollowsCamera()) {
                        poseStack.mulPose(Axis.YP.rotationDegrees(-livingEntityRenderState.yRot));
                        poseStack.mulPose(Axis.XP.rotationDegrees(-livingEntityRenderState.xRot));
                    }
                    poseStack.translate(0.0F, -livingEntityRenderState.eyeHeight, 0.0F);
                }

                //These are additive properties
                PlayerAnimBone body = animationPlayer.get3DTransform(new PlayerAnimBone("body"));

                poseStack.translate(-body.getPosX()/16, body.getPosY()/16 + 0.75, body.getPosZ()/16);
                body.rotX *= -1;
                body.rotY *= -1;
                RenderUtil.rotateMatrixAroundBone(poseStack, body);
                poseStack.scale(body.getScaleX(), body.getScaleY(), body.getScaleZ());

                poseStack.translate(0, -0.75, 0);

                poseStack.scale(-1.0F, -1.0F, 1.0F);
            }
        }
    }
}
