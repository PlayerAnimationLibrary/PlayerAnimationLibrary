package com.zigythebird.playeranim.accessors;

import com.google.j2objc.annotations.J2ObjCIncompatible;
import com.zigythebird.playeranim.animation.AvatarAnimManager;

/**
 * Extension of PlayerRenderState
 */
@J2ObjCIncompatible
public interface IAvatarAnimationState {
    boolean playerAnimLib$isFirstPersonPass();
    void playerAnimLib$setFirstPersonPass(boolean value);

    // AnimationApplier animationApplier
    void playerAnimLib$setAnimManager(AvatarAnimManager manager);
    AvatarAnimManager playerAnimLib$getAnimManager();
}
