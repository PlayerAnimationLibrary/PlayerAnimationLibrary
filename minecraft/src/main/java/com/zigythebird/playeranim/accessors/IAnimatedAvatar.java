package com.zigythebird.playeranim.accessors;

import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import net.minecraft.resources.Identifier;

public interface IAnimatedAvatar {
    AvatarAnimManager playerAnimLib$getAnimManager();
    IAnimation playerAnimLib$getAnimation(Identifier id);
}
