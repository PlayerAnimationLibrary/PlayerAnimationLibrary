package com.zigythebird.playeranim.accessors;

import com.google.j2objc.annotations.J2ObjCIncompatible;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import net.minecraft.resources.Identifier;

@J2ObjCIncompatible
public interface IAnimatedAvatar {
    AvatarAnimManager playerAnimLib$getAnimManager();
    IAnimation playerAnimLib$getAnimation(Identifier id);
}
