/*package com.zigythebird.playeranim.animation.layered.modifier;

import com.zigythebird.playeranim.animation.AnimationData;
import com.zigythebird.playeranim.bones.PlayerAnimBone;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

//TODO
public class HeadBoundCameraModifier extends AbstractModifier {
    float tickDelta;

    public HeadBoundCameraModifier() {}

    @Override
    public void setupAnim(AnimationData state) {
        tickDelta = state.getPartialTick();
        super.setupAnim(state);
    }

    @Override
    public PlayerAnimBone get3DCameraTransform(Camera camera, PlayerAnimBone bone) {
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            AbstractClientPlayer player = Minecraft.getInstance().player;
            float f = Mth.lerp(tickDelta, player.yBodyRotO, player.yBodyRot);
            return host.get3DTransform(bone).addRot(0, f * 0.017453292F, 0);
        }
        return bone;
    }
}*/
