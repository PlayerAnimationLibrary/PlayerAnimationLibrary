package com.zigythebird.playeranim.animation.layered.modifier;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranimcore.animation.layered.modifier.MirrorModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import org.jetbrains.annotations.NotNull;

/**Apply the mirror modifier only if the client has their main arm set as the left one*/
public class MirrorIfLeftHandModifier extends MirrorModifier {
    public MirrorIfLeftHandModifier() {
        super();
    }

    @Override
    public void get3DTransform(@NotNull PlayerAnimBone bone) {
        if (getController() instanceof PlayerAnimationController controller && controller.getAvatar() == Minecraft.getInstance().player && Minecraft.getInstance().options.mainHand().get() == HumanoidArm.LEFT) return;
        super.get3DTransform(bone);
    }
}
