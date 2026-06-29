package com.zigythebird.playeranimcore.network;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeStack;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.AnimationFormat;
import com.zigythebird.playeranimcore.loading.PlayerAnimatorLoader;
import io.netty.buffer.ByteBuf;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.parser.ast.FloatExpression;
import team.unnamed.mocha.runtime.IsConstantExpression;
import team.unnamed.mocha.util.ExprBytesUtils;
import team.unnamed.mocha.util.network.ProtocolUtils;
import team.unnamed.mocha.util.network.VarIntUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.zigythebird.playeranimcore.molang.MolangLoader.MOCHA_ENGINE;

final class AnimationBinaryV6 {

    static void write(ByteBuf buf, int version, Animation animation) {
        Map<String, Object> data = animation.data().data();
        writeHeader(buf, animation, data);

        VarIntUtils.writeVarInt(buf, animation.boneAnimations().size());
        for (Map.Entry<String, BoneAnimation> entry : animation.boneAnimations().entrySet()) {
            ProtocolUtils.writeString(buf, entry.getKey());
            writeBoneAnimation(buf, entry.getValue(), version);
        }

        AnimationBinary.writeEventKeyframes(buf, animation.keyFrames());
        NetworkUtils.writeMap(buf, animation.bones(), ProtocolUtils::writeString, (byteBuf, bone) -> NetworkUtils.writeCustomBone(byteBuf, bone, version));
        NetworkUtils.writeMap(buf, animation.parents(), ProtocolUtils::writeString, ProtocolUtils::writeString);
    }

    static Animation read(ByteBuf buf, int version) {
        ExtraAnimationData data = new ExtraAnimationData();

        int flags = VarIntUtils.readVarInt(buf);
        boolean shouldPlayAgain = HeaderFlag.SHOULD_PLAY_AGAIN.test(flags);
        boolean isHoldOnLastFrame = HeaderFlag.HOLD_ON_LAST_FRAME.test(flags);
        AnimationFormat format = HeaderFlag.PLAYER_ANIMATOR.test(flags) ? AnimationFormat.PLAYER_ANIMATOR : AnimationFormat.GECKOLIB;

        data.put(ExtraAnimationData.FORMAT_KEY, format);
        if (HeaderFlag.APPLY_BEND.test(flags)) data.put(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY, true);
        if (!HeaderFlag.EASE_BEFORE.test(flags)) data.put(ExtraAnimationData.EASING_BEFORE_KEY, false);

        float length = buf.readFloat();

        Animation.LoopType loopType;
        if (shouldPlayAgain) {
            if (isHoldOnLastFrame) {
                loopType = Animation.LoopType.HOLD_ON_LAST_FRAME;
            } else {
                loopType = Animation.LoopType.returnToTickLoop(buf.readFloat());
            }
        } else {
            loopType = Animation.LoopType.PLAY_ONCE;
        }

        if (HeaderFlag.HAS_BEGIN_TICK.test(flags)) data.put(ExtraAnimationData.BEGIN_TICK_KEY, buf.readFloat());
        if (HeaderFlag.HAS_END_TICK.test(flags)) data.put(ExtraAnimationData.END_TICK_KEY, buf.readFloat());

        data.put(ExtraAnimationData.UUID_KEY, NetworkUtils.readUuid(buf));

        boolean isPlayerAnimator = format == AnimationFormat.PLAYER_ANIMATOR;
        Map<String, BoneAnimation> boneAnimations = NetworkUtils.readMap(buf, ProtocolUtils::readString,
                buf1 -> readBoneAnimation(buf1, isPlayerAnimator, version));

        Animation.Keyframes keyFrames = AnimationBinary.readEventKeyframes(buf);
        Map<String, CustomModelBone> pivotBones = NetworkUtils.readMap(buf, ProtocolUtils::readString, byteBuf -> NetworkUtils.readCustomBone(byteBuf, version));
        Map<String, String> parents = NetworkUtils.readMap(buf, ProtocolUtils::readString, ProtocolUtils::readString);

        return new Animation(data, length, loopType, boneAnimations, keyFrames, pivotBones, parents);
    }

    private static void writeHeader(ByteBuf buf, Animation animation, Map<String, Object> data) {
        boolean shouldPlayAgain = animation.loopType().shouldPlayAgain(null, animation);
        boolean isHoldOnLastFrame = animation.loopType() == Animation.LoopType.HOLD_ON_LAST_FRAME;
        AnimationFormat format = (AnimationFormat) data.getOrDefault(ExtraAnimationData.FORMAT_KEY, AnimationFormat.GECKOLIB);
        boolean applyBend = (boolean) data.getOrDefault(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY, false);
        boolean easeBefore = (boolean) data.getOrDefault(ExtraAnimationData.EASING_BEFORE_KEY, true);
        float beginTick = (float) data.getOrDefault(ExtraAnimationData.BEGIN_TICK_KEY, Float.NaN);
        float endTick = (float) data.getOrDefault(ExtraAnimationData.END_TICK_KEY, Float.NaN);

        int flags = 0;
        flags = HeaderFlag.SHOULD_PLAY_AGAIN.set(flags, shouldPlayAgain);
        flags = HeaderFlag.HOLD_ON_LAST_FRAME.set(flags, isHoldOnLastFrame);
        flags = HeaderFlag.PLAYER_ANIMATOR.set(flags, format == AnimationFormat.PLAYER_ANIMATOR);
        flags = HeaderFlag.APPLY_BEND.set(flags, applyBend);
        flags = HeaderFlag.EASE_BEFORE.set(flags, easeBefore);
        flags = HeaderFlag.HAS_BEGIN_TICK.set(flags, !Float.isNaN(beginTick));
        flags = HeaderFlag.HAS_END_TICK.set(flags, !Float.isNaN(endTick));

        VarIntUtils.writeVarInt(buf, flags);
        buf.writeFloat(animation.length());
        if (shouldPlayAgain && !isHoldOnLastFrame) {
            buf.writeFloat(animation.loopType().restartFromTick(null, animation));
        }
        if (HeaderFlag.HAS_BEGIN_TICK.test(flags)) buf.writeFloat(beginTick);
        if (HeaderFlag.HAS_END_TICK.test(flags)) buf.writeFloat(endTick);
        NetworkUtils.writeUuid(buf, animation.uuid());
    }

    private static void writeBoneAnimation(ByteBuf buf, BoneAnimation bone, int version) {
        int presenceFlags = 0;
        for (BoneChannel ch : BoneChannel.VALUES) {
            if (!ch.getKeyframes(bone).isEmpty()) {
                presenceFlags |= ch.mask;
            }
        }
        VarIntUtils.writeVarInt(buf, presenceFlags);
        for (BoneChannel ch : BoneChannel.VALUES) {
            if ((presenceFlags & ch.mask) != 0) {
                writeKeyframeList(buf, ch.getKeyframes(bone), version);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static BoneAnimation readBoneAnimation(ByteBuf buf, boolean shouldStartFromDefault, int version) {
        int presenceFlags = VarIntUtils.readVarInt(buf);
        List<Keyframe>[] lists = new List[BoneChannel.VALUES.length];
        for (BoneChannel ch : BoneChannel.VALUES) {
            lists[ch.ordinal()] = (presenceFlags & ch.mask) != 0
                    ? readKeyframeList(buf, shouldStartFromDefault, ch.isScale, version)
                    : new ArrayList<>(0);
        }
        return new BoneAnimation(
                new KeyframeStack(lists[0], lists[1], lists[2]),
                new KeyframeStack(lists[3], lists[4], lists[5]),
                new KeyframeStack(lists[6], lists[7], lists[8]),
                lists[9]
        );
    }

    private static void writeKeyframeList(ByteBuf buf, List<Keyframe> keyframes, int version) {
        VarIntUtils.writeVarInt(buf, keyframes.size());
        for (Keyframe keyframe : keyframes) {
            writeKeyframe(keyframe, buf, version);
        }
    }

    private static void writeKeyframe(Keyframe keyframe, ByteBuf buf, int version) {
        List<Expression> endValue = keyframe.endValue();
        boolean isConstant = endValue.size() == 1 && IsConstantExpression.test(endValue.getFirst());
        boolean hasEasingArgs = false;
        for (List<Expression> inner : keyframe.easingArgs()) {
            if (!inner.isEmpty()) {
                hasEasingArgs = true;
                break;
            }
        }

        int flags = 0;
        if (isConstant) flags |= KeyframeFlag.IS_CONSTANT.mask;
        if (hasEasingArgs) flags |= KeyframeFlag.HAS_EASING_ARGS.mask;
        if (keyframe.length() == 0.0f) flags |= KeyframeFlag.LENGTH_ZERO.mask;
        else if (keyframe.length() == 1.0f) flags |= KeyframeFlag.LENGTH_ONE.mask;
        VarIntUtils.writeVarInt(buf, KeyframeFlag.pack(keyframe.easingType().id, flags, version));

        if (isConstant) {
            buf.writeFloat(MOCHA_ENGINE.eval(endValue));
        } else {
            ExprBytesUtils.writeExpressions(endValue, buf);
        }

        if ((flags & (KeyframeFlag.LENGTH_ZERO.mask | KeyframeFlag.LENGTH_ONE.mask)) == 0) {
            buf.writeFloat(keyframe.length());
        }

        if (hasEasingArgs) {
            ProtocolUtils.writeList(buf, keyframe.easingArgs(), ExprBytesUtils::writeExpressions);
        }
    }

    private static List<Keyframe> readKeyframeList(ByteBuf buf, boolean shouldStartFromDefault, boolean isScale, int version) {
        int count = VarIntUtils.readVarInt(buf);
        List<Keyframe> list = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            int combined = VarIntUtils.readVarInt(buf);
            int easingId = KeyframeFlag.unpackEasing(combined, version);
            int flags = KeyframeFlag.unpackFlags(combined, version);
            boolean isConstant = (flags & KeyframeFlag.IS_CONSTANT.mask) != 0;
            boolean hasEasingArgs = (flags & KeyframeFlag.HAS_EASING_ARGS.mask) != 0;

            List<Expression> endValue;
            if (isConstant) {
                endValue = List.of(FloatExpression.of(buf.readFloat()));
            } else {
                endValue = ExprBytesUtils.readExpressions(buf);
            }

            float length;
            if ((flags & KeyframeFlag.LENGTH_ZERO.mask) != 0) length = 0.0f;
            else if ((flags & KeyframeFlag.LENGTH_ONE.mask) != 0) length = 1.0f;
            else length = buf.readFloat();

            List<Expression> startValue = list.isEmpty()
                    ? (shouldStartFromDefault ? (isScale ? PlayerAnimatorLoader.ONE : PlayerAnimatorLoader.ZERO) : endValue)
                    : list.getLast().endValue();
            EasingType easingType = EasingType.fromId((byte) easingId);
            List<List<Expression>> easingArgs;
            if (hasEasingArgs) {
                easingArgs = ProtocolUtils.readList(buf, ExprBytesUtils::readExpressions);
            } else if (shouldStartFromDefault && i > 0) {
                easingArgs = Collections.singletonList(new ArrayList<>(0));
            } else {
                easingArgs = new ArrayList<>(0);
            }

            list.add(new Keyframe(length, startValue, endValue, easingType, easingArgs));
        }

        return list;
    }
}
