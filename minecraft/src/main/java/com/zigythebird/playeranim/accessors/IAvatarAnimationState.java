package com.zigythebird.playeranim.accessors;

import com.zigythebird.playeranim.animation.AvatarAnimManager;

/**
 * Extension of PlayerRenderState
 */
public interface IAvatarAnimationState {
    boolean playerAnimLib$isFirstPersonPass();
    void playerAnimLib$setFirstPersonPass(boolean value);

    // AnimationApplier animationApplier
    void playerAnimLib$setAnimManager(AvatarAnimManager manager);
    AvatarAnimManager playerAnimLib$getAnimManager();
}
