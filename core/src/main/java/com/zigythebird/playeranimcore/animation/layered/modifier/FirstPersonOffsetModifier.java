package com.zigythebird.playeranimcore.animation.layered.modifier;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import org.jetbrains.annotations.NotNull;

public class FirstPersonOffsetModifier extends AbstractModifier {
    private boolean isFirstPersonPass;
    public float offset;

    public FirstPersonOffsetModifier() {
        this(1.92f);
    }

    public FirstPersonOffsetModifier(float offset) {
        this.offset = offset;
    }

    @Override
    public void setupAnim(AnimationData state) {
        super.setupAnim(state);
        this.isFirstPersonPass = state.isFirstPersonPass();
    }

    @Override
    public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
        bone = super.get3DTransform(bone);
        if (this.isFirstPersonPass && "body".equals(bone.getName()))
            bone.positionY += this.offset;
        return bone;
    }
}
