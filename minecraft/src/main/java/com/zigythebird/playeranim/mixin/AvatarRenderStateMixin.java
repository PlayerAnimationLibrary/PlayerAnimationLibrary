package com.zigythebird.playeranim.mixin;

import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements IAvatarAnimationState {
    @Unique
    boolean playerAnimLib$isFirstPersonPass = false;

    @Unique
    AvatarAnimManager playerAnimLib$avatarAnimManager = null;

    @Override
    public boolean playerAnimLib$isFirstPersonPass() {
        return playerAnimLib$isFirstPersonPass;
    }

    @Override
    public void playerAnimLib$setFirstPersonPass(boolean value) {
        playerAnimLib$isFirstPersonPass = value;
    }

    @Override
    public void playerAnimLib$setAnimManager(AvatarAnimManager manager) {
        this.playerAnimLib$avatarAnimManager = manager;
    }

    @Override
    public @NotNull AvatarAnimManager playerAnimLib$getAnimManager() {
        return this.playerAnimLib$avatarAnimManager;
    }
}

