package com.zigythebird.playeranimcore.network;

import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeStack;
import com.zigythebird.playeranimcore.enums.Axis;

import java.util.List;
import java.util.function.Function;

enum BoneChannel {
    ROTATION_X(BoneAnimation::rotationKeyFrames, Axis.X, false),
    ROTATION_Y(BoneAnimation::rotationKeyFrames, Axis.Y, false),
    ROTATION_Z(BoneAnimation::rotationKeyFrames, Axis.Z, false),
    POSITION_X(BoneAnimation::positionKeyFrames, Axis.X, false),
    POSITION_Y(BoneAnimation::positionKeyFrames, Axis.Y, false),
    POSITION_Z(BoneAnimation::positionKeyFrames, Axis.Z, false),
    SCALE_X(BoneAnimation::scaleKeyFrames, Axis.X, true),
    SCALE_Y(BoneAnimation::scaleKeyFrames, Axis.Y, true),
    SCALE_Z(BoneAnimation::scaleKeyFrames, Axis.Z, true),
    BEND(null, null, false);

    static final BoneChannel[] VALUES = values();

    final int mask = 1 << ordinal();
    final Function<BoneAnimation, KeyframeStack> stackAccessor;
    final Axis axis;
    final boolean isScale;

    BoneChannel(Function<BoneAnimation, KeyframeStack> stackAccessor, Axis axis, boolean isScale) {
        this.stackAccessor = stackAccessor;
        this.axis = axis;
        this.isScale = isScale;
    }

    List<Keyframe> getKeyframes(BoneAnimation bone) {
        if (this == BEND) return bone.bendKeyFrames();
        return stackAccessor.apply(bone).getKeyFramesForAxis(axis);
    }
}
