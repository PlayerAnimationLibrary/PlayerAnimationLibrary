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
import com.zigythebird.playeranim.accessors.IAnimatedPlayer;
import com.zigythebird.playeranim.animation.PlayerAnimManager;
import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerRenderer.class, priority = 2000)
public abstract class PlayerRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void modifyRenderState(AbstractClientPlayer entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        PlayerAnimManager animation = ((IAnimatedPlayer)entity).playerAnimLib$getAnimManager();
        animation.setTickDelta(partialTicks);
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("RETURN"))
    private void applyBodyTransforms(AbstractClientPlayer player, PoseStack poseStack, float f, float bodyYaw, float tickDelta, float scale, CallbackInfo ci){
        var animationPlayer = ((IAnimatedPlayer)player).playerAnimLib$getAnimManager();
        if (animationPlayer != null && animationPlayer.isActive()) {
            ((IAnimatedPlayer)player).playerAnimLib$getAnimProcessor().handleAnimations(animationPlayer.getTickDelta(), false);

            if (FirstPersonMode.isFirstPersonPass() && player == Minecraft.getInstance().cameraEntity) {
                float pitch = player.getViewXRot(tickDelta);
                float headYaw = player.getViewYRot(tickDelta);
                poseStack.translate(0.0F, player.getEyeHeight(), 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw - headYaw));
                if (animationPlayer.isFirstPersonFollowPitch()) poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
                poseStack.translate(0.0F, -player.getEyeHeight(), 0.0F);
            }

            //These are additive properties
            PlayerAnimBone body = animationPlayer.get3DTransform(new PlayerAnimBone("body"));

            poseStack.scale(body.getScaleX(), body.getScaleY(), body.getScaleZ());
            poseStack.translate(-body.getPosX()/16, body.getPosY()/16 + 0.75, body.getPosZ()/16);
            body.rotX *= -1;
            body.rotY *= -1;
            RenderUtil.rotateMatrixAroundBone(poseStack, body);

            poseStack.translate(0, -0.75, 0);
        }
    }
}
