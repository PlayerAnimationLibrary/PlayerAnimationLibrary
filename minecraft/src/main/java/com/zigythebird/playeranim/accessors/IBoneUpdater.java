package com.zigythebird.playeranim.accessors;

import com.google.j2objc.annotations.J2ObjCIncompatible;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

/**
 * Use to implement custom transformations using mixins, for example BC applies bends here
 */
@J2ObjCIncompatible
public interface IBoneUpdater {
    default void pal$updatePart(AvatarAnimManager emote, ModelPart part, PlayerAnimBone bone) {}
    default void pal$resetAll(@Nullable AvatarAnimManager emote) {}
}
