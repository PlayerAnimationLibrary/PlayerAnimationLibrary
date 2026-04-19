package com.zigythebird.playeranim.mixin;

import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.accessors.IBoneUpdater;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.unnamed.mocha.runtime.standard.MochaMath;

//Set the priority high cause why not!
@Mixin(value = PlayerCapeModel.class, priority = 2001)
public class PlayerCapeModelMixin implements IBoneUpdater {
    @Shadow
    @Final
    private ModelPart cape;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void setupAnim(AvatarRenderState avatarRenderState, CallbackInfo ci) {
        AvatarAnimManager emote = ((IAvatarAnimationState)avatarRenderState).playerAnimLib$getAnimManager();
        if (emote != null && emote.isActive()) {
            PlayerAnimBone bone = RenderUtil.copyVanillaPart(this.cape, new PlayerAnimBone("cape"));

            bone.rotation.x -= MochaMath.PI;
            bone.rotation.z -= MochaMath.PI;
            bone.rotation.x *= -1;
            bone.rotation.y *= -1;
            emote.get3DTransform(bone);
            bone.rotation.x *= -1;
            bone.rotation.y *= -1;
            bone.rotation.x += MochaMath.PI;
            bone.rotation.z += MochaMath.PI;

            this.pal$updatePart(emote, this.cape, bone);
        } else {
            this.pal$resetAll(emote);
        }
    }

    @Override
    public void pal$updatePart(AvatarAnimManager emote, ModelPart part, PlayerAnimBone bone) {
        RenderUtil.translatePartToCape(part, bone, part.getInitialPose());
    }

    @Override
    public void pal$resetAll(@Nullable AvatarAnimManager emote) {
        // no-op
    }
}
