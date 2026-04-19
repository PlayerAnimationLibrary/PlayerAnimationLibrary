package com.zigythebird.playeranimcore.animation.layered;

import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.bones.ToggleablePlayerAnimBone;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record AnimationSnapshot(Map<String, ToggleablePlayerAnimBone> snapshots) implements IAnimation {
    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void get3DTransform(@NotNull PlayerAnimBone bone) {
        if (snapshots.containsKey(bone.getName())) {
            bone.copyOtherBoneIfNotDisabled(snapshots.get(bone.getName()));
        }
    }

    @Override
    public @NotNull String toString() {
        return "AnimationSnapshot{" +
                "snapshots=" + snapshots +
                '}';
    }
}
