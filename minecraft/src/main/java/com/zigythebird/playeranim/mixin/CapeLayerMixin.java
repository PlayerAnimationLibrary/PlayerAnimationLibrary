package com.zigythebird.playeranim.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin extends RenderLayer<AvatarRenderState, PlayerModel> {
    public CapeLayerMixin(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Inject (
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
            )
    )
    private void render(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, AvatarRenderState avatarRenderState, float f, float g, CallbackInfo ci) {
        AvatarAnimManager emote = ((IAvatarAnimationState) avatarRenderState).playerAnimLib$getAnimManager();
        if (emote != null && emote.isActive()) getParentModel().body.translateAndRotate(poseStack);
    }
}
