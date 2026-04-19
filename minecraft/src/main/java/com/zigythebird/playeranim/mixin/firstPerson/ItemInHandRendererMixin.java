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

package com.zigythebird.playeranim.mixin.firstPerson;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranim.accessors.IAnimatedAvatar;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void disableDefaultItemIfNeeded(float f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LocalPlayer localPlayer, int i, CallbackInfo ci) {
        if (localPlayer instanceof IAnimatedAvatar player && player.playerAnimLib$getAnimManager().isActive()
                && player.playerAnimLib$getAnimManager().getFirstPersonMode() == FirstPersonMode.THIRD_PERSON_MODEL) {
                ci.cancel();
        }
    }

    /* AW needed, I may do it later
    @Redirect(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;evaluateWhichHandsToRender(Lnet/minecraft/client/player/LocalPlayer;)Lnet/minecraft/client/renderer/ItemInHandRenderer$HandRenderSelection;"))
    private ItemInHandRenderer.HandRenderSelection selectHandsToRender(LocalPlayer localPlayer) {

        return null;
    }*/

    @Inject(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"), cancellable = true)
    private void cancelItemRender(LivingEntity entity, ItemStack itemStack, ItemDisplayContext transformType, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, CallbackInfo ci) {
        if (entity instanceof IAnimatedAvatar player && player.playerAnimLib$getAnimManager().isActive() && entity == Minecraft.getInstance().getCameraEntity()
                && !Minecraft.getInstance().gameRenderer.getMainCamera().isDetached()
                && player.playerAnimLib$getAnimManager().getFirstPersonMode() == FirstPersonMode.THIRD_PERSON_MODEL) {
            var config = player.playerAnimLib$getAnimManager().getFirstPersonConfiguration();
            if (transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                if (!config.isShowRightItem()) {
                    ci.cancel();
                }
            } else {
                if (!config.isShowLeftItem()) {
                    ci.cancel();
                }
            }
        }
    }
}
