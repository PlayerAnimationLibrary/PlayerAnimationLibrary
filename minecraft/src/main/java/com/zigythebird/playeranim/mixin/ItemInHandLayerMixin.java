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

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {
    @Unique
    private final PlayerAnimBone playerAnimLib$rightItem = new PlayerAnimBone("right_item");
    @Unique
    private final PlayerAnimBone playerAnimLib$leftItem = new PlayerAnimBone("left_item");

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V", ordinal = 0))
    private void changeItemLocation(ArmedEntityRenderState renderState, ItemStackRenderState itemStackRenderState, ItemStack itemStack, HumanoidArm arm, PoseStack matrices, SubmitNodeCollector submitNodeCollector, int i, CallbackInfo ci, @Share("pal_active") LocalBooleanRef active) {
        if (renderState instanceof IAvatarAnimationState state && state.playerAnimLib$getAnimManager() != null && state.playerAnimLib$getAnimManager().isActive()) {
            AvatarAnimManager anim = state.playerAnimLib$getAnimManager();
            active.set(true);

            PlayerAnimBone bone;

            if (arm == HumanoidArm.LEFT) bone = playerAnimLib$leftItem;
            else bone = playerAnimLib$rightItem;

            bone.setToInitialPose();
            anim.get3DTransform(bone);

            matrices.translate(bone.position.x/16, -bone.position.y/16, bone.position.z/16);
        }
        else active.set(false);
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void changeItemRotationAndScale(ArmedEntityRenderState renderState, ItemStackRenderState itemStackRenderState, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, CallbackInfo ci, @Share("pal_active") LocalBooleanRef active) {
        if (active.get()) {
            PlayerAnimBone bone;

            if (arm == HumanoidArm.LEFT) bone = playerAnimLib$leftItem;
            else bone = playerAnimLib$rightItem;

            if (bone.rotation.z != 0)
                poseStack.mulPose(Axis.ZP.rotation(-bone.rotation.y));

            if (bone.rotation.y != 0)
                poseStack.mulPose(Axis.YP.rotation(-bone.rotation.z));

            if (bone.rotation.x != 0)
                poseStack.mulPose(Axis.XP.rotation(-bone.rotation.x));

            poseStack.scale(bone.scale.x, bone.scale.y, bone.scale.z);
        }
    }
}
