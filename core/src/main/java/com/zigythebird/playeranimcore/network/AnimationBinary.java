package com.zigythebird.playeranimcore.network;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeStack;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.CustomInstructionKeyframeData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.ParticleKeyframeData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.SoundKeyframeData;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.AnimationFormat;
import com.zigythebird.playeranimcore.enums.TransformType;
import com.zigythebird.playeranimcore.loading.PlayerAnimatorLoader;
import com.zigythebird.playeranimcore.math.Vec3f;
import io.netty.buffer.ByteBuf;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.parser.ast.FloatExpression;
import team.unnamed.mocha.runtime.IsConstantExpression;
import team.unnamed.mocha.util.ExprBytesUtils;
import team.unnamed.mocha.util.network.ProtocolUtils;
import team.unnamed.mocha.util.network.VarIntUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.zigythebird.playeranimcore.molang.MolangLoader.MOCHA_ENGINE;

@SuppressWarnings("unused")
public final class AnimationBinary {
    /**
     * Version 1: Initial Release
     * Version 2: Added support for animations that don't apply the torso bend to other bones + easeBefore
     * Version 3: No change client side, but the server won't send some animations to versions lower than 3 due to the possibility of a crash.
     * Version 4: Fixed some issues with the body bone.
     * Version 5: Fixed the Y position axis on items being negated.
     * Version 6: Compact binary format - bit-packed header, presence flags for bone axes, compact keyframe encoding.
     */
    public static int getCurrentVersion() {
        return 6;
    }

    public static void write(ByteBuf buf, Animation animation) {
        AnimationBinary.write(buf, getCurrentVersion(), animation);
    }

    public static void write(ByteBuf buf, int version, Animation animation) {
        if (version >= 6) {
            AnimationBinaryV6.write(buf, version, animation);
            return;
        }

        Map<String, Object> data = animation.data().data();
        boolean applyBendToOtherBones = (boolean) data.getOrDefault(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY, false);
        if (version < 3 && applyBendToOtherBones && animation.boneAnimations().containsKey("torso")
                && !animation.boneAnimations().get("torso").bendKeyFrames().isEmpty()) {
            applyBendToOtherBones = false;
        }
        buf.writeFloat(animation.length());
        boolean shouldPlayAgain = animation.loopType().shouldPlayAgain(null, animation);
        buf.writeBoolean(shouldPlayAgain);
        if (shouldPlayAgain) {
            if (animation.loopType() == Animation.LoopType.HOLD_ON_LAST_FRAME) {
                buf.writeBoolean(true);
            } else {
                buf.writeBoolean(false);
                buf.writeFloat(animation.loopType().restartFromTick(null, animation));
            }
        }
        buf.writeByte(((AnimationFormat)data.getOrDefault(ExtraAnimationData.FORMAT_KEY, AnimationFormat.GECKOLIB)).id);
        buf.writeFloat((float) data.getOrDefault(ExtraAnimationData.BEGIN_TICK_KEY, Float.NaN));
        buf.writeFloat((float) data.getOrDefault(ExtraAnimationData.END_TICK_KEY, Float.NaN));
        if (version > 1) {
            buf.writeBoolean(applyBendToOtherBones);
            buf.writeBoolean((boolean) data.getOrDefault(ExtraAnimationData.EASING_BEFORE_KEY, true));
        }
        NetworkUtils.writeUuid(buf, animation.uuid()); // required by emotecraft to stop animations
        VarIntUtils.writeVarInt(buf, animation.boneAnimations().size());
        for (Map.Entry<String, BoneAnimation> entry : animation.boneAnimations().entrySet()) {
            ProtocolUtils.writeString(buf, entry.getKey());
            writeBoneAnimation(buf, entry.getValue(), version < 4 && entry.getKey().equals("body"), version < 5 && LegacyAnimationBinary.ITEM_BONE.test(entry.getKey()));
        }

        writeEventKeyframes(buf, animation.keyFrames());
        NetworkUtils.writeMap(buf, animation.bones(), ProtocolUtils::writeString, NetworkUtils::writeVec3f);
        NetworkUtils.writeMap(buf, animation.parents(), ProtocolUtils::writeString, ProtocolUtils::writeString);
    }

    public static void writeBoneAnimation(ByteBuf buf, BoneAnimation bone, boolean isBody, boolean isItem) {
        writeKeyframeStack(buf, bone.rotationKeyFrames(), isBody, false, TransformType.ROTATION);
        writeKeyframeStack(buf, bone.positionKeyFrames(), isBody, isItem, TransformType.POSITION);
        writeKeyframeStack(buf, bone.scaleKeyFrames(), false, false, TransformType.SCALE);
        ProtocolUtils.writeList(buf, bone.bendKeyFrames(), AnimationBinary::writeKeyframe);
    }

    public static void writeKeyframeStack(ByteBuf buf, KeyframeStack stack, boolean isBody, boolean isItem, TransformType type) {
        ProtocolUtils.writeList(buf, isBody ? negateKeyframes(stack.xKeyframes()) : stack.xKeyframes(), AnimationBinary::writeKeyframe);
        ProtocolUtils.writeList(buf, isItem || (isBody && type == TransformType.ROTATION) ? negateKeyframes(stack.yKeyframes()) : stack.yKeyframes(), AnimationBinary::writeKeyframe);
        ProtocolUtils.writeList(buf, stack.zKeyframes(), AnimationBinary::writeKeyframe);
    }

    public static void writeKeyframe(Keyframe keyframe, ByteBuf buf) {
        buf.writeFloat(keyframe.length());
        ExprBytesUtils.writeExpressions(keyframe.endValue(), buf);
        buf.writeByte(keyframe.easingType().id);
        ProtocolUtils.writeList(buf, keyframe.easingArgs(), ExprBytesUtils::writeExpressions);
    }

    static void writeEventKeyframes(ByteBuf buf, Animation.Keyframes keyFrames) {
        // Sounds
        VarIntUtils.writeVarInt(buf, keyFrames.sounds().length);
        for (SoundKeyframeData soundKeyframe : keyFrames.sounds()) {
            buf.writeFloat(soundKeyframe.getStartTick());
            ProtocolUtils.writeString(buf, soundKeyframe.getSound());
        }

        // Particles
        VarIntUtils.writeVarInt(buf, keyFrames.particles().length);
        for (ParticleKeyframeData particleKeyframe : keyFrames.particles()) {
            buf.writeFloat(particleKeyframe.getStartTick());
            ProtocolUtils.writeString(buf, particleKeyframe.getEffect());
            ProtocolUtils.writeString(buf, particleKeyframe.getLocator());
            ProtocolUtils.writeString(buf, particleKeyframe.script());
        }

        // Instructions
        VarIntUtils.writeVarInt(buf, keyFrames.customInstructions().length);
        for (CustomInstructionKeyframeData instructionKeyframe : keyFrames.customInstructions()) {
            buf.writeFloat(instructionKeyframe.getStartTick());
            ProtocolUtils.writeString(buf, instructionKeyframe.getInstructions());
        }
    }

    public static Animation read(ByteBuf buf) {
        return AnimationBinary.read(buf, getCurrentVersion());
    }

    public static Animation read(ByteBuf buf, int version) {
        if (version >= 6) {
            return AnimationBinaryV6.read(buf, version);
        }

        float length = buf.readFloat();
        Animation.LoopType loopType = Animation.LoopType.PLAY_ONCE;
        if (buf.readBoolean()) {
            if (buf.readBoolean()) loopType = Animation.LoopType.HOLD_ON_LAST_FRAME;
            else loopType = Animation.LoopType.returnToTickLoop(buf.readFloat());
        }
        ExtraAnimationData data = new ExtraAnimationData();
        AnimationFormat format = AnimationFormat.fromId(buf.readByte());
        data.put(ExtraAnimationData.FORMAT_KEY, format);
        float beginTick = buf.readFloat();
        float endTick = buf.readFloat();
        if (!Float.isNaN(beginTick))
            data.put(ExtraAnimationData.BEGIN_TICK_KEY, beginTick);
        if (!Float.isNaN(endTick))
            data.put(ExtraAnimationData.END_TICK_KEY, endTick);
        if (version > 1) {
            boolean applyBendToOtherBones = buf.readBoolean();
            boolean easeBefore = buf.readBoolean();
            if (applyBendToOtherBones)
                data.put(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY, true);
            if (!easeBefore)
                data.put(ExtraAnimationData.EASING_BEFORE_KEY, false);
        } else data.put(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY, true);

        data.put(ExtraAnimationData.UUID_KEY, NetworkUtils.readUuid(buf)); // required by emotecraft to stop animations
        Map<String, BoneAnimation> boneAnimations = NetworkUtils.readMap(buf, ProtocolUtils::readString, buf1 -> readBoneAnimation(buf1, format == AnimationFormat.PLAYER_ANIMATOR));

        if (version < 4 && boneAnimations.containsKey("body")) {
            BoneAnimation body = boneAnimations.get("body");
            body.positionKeyFrames().xKeyframes().replaceAll(AnimationBinary::negateKeyframeExpressions);
            body.rotationKeyFrames().xKeyframes().replaceAll(AnimationBinary::negateKeyframeExpressions);
            body.rotationKeyFrames().yKeyframes().replaceAll(AnimationBinary::negateKeyframeExpressions);
        }

        if (version < 5) {
            if (boneAnimations.containsKey("right_item"))
                boneAnimations.get("right_item").positionKeyFrames().yKeyframes().replaceAll(AnimationBinary::negateKeyframeExpressions);
            if (boneAnimations.containsKey("left_item"))
                boneAnimations.get("left_item").positionKeyFrames().yKeyframes().replaceAll(AnimationBinary::negateKeyframeExpressions);
        }

        Animation.Keyframes keyFrames = readEventKeyframes(buf);
        Map<String, Vec3f> pivotBones = NetworkUtils.readMap(buf, ProtocolUtils::readString, NetworkUtils::readVec3f);
        Map<String, String> parents = NetworkUtils.readMap(buf, ProtocolUtils::readString, ProtocolUtils::readString);

        return new Animation(data, length, loopType, boneAnimations, keyFrames, pivotBones, parents);
    }

    public static BoneAnimation readBoneAnimation(ByteBuf buf, boolean shouldStartFromDefault) {
        KeyframeStack rotationKeyFrames = readKeyframeStack(buf, shouldStartFromDefault, false);
        KeyframeStack positionKeyFrames = readKeyframeStack(buf, shouldStartFromDefault, false);
        KeyframeStack scaleKeyFrames = readKeyframeStack(buf, shouldStartFromDefault, true);
        List<Keyframe> bendKeyFrames = readKeyframeList(buf, shouldStartFromDefault, false);

        return new BoneAnimation(rotationKeyFrames, positionKeyFrames, scaleKeyFrames, bendKeyFrames);
    }

    public static KeyframeStack readKeyframeStack(ByteBuf buf, boolean shouldStartFromDefault, boolean isScale) {
        List<Keyframe> xKeyframes = readKeyframeList(buf, shouldStartFromDefault, isScale);
        List<Keyframe> yKeyframes = readKeyframeList(buf, shouldStartFromDefault, isScale);
        List<Keyframe> zKeyframes = readKeyframeList(buf, shouldStartFromDefault, isScale);

        return new KeyframeStack(xKeyframes, yKeyframes, zKeyframes);
    }

    public static List<Keyframe> readKeyframeList(ByteBuf buf, boolean shouldStartFromDefault, boolean isScale) {
        int count = VarIntUtils.readVarInt(buf);
        List<Keyframe> list = new ArrayList<>(count);

        for (int i = 0; i < count; ++i) {
            float length = buf.readFloat();

            List<Expression> endValue = ExprBytesUtils.readExpressions(buf);
            List<Expression> startValue = list.isEmpty() ? (shouldStartFromDefault ?(isScale ? PlayerAnimatorLoader.ONE : PlayerAnimatorLoader.ZERO) : endValue) : list.getLast().endValue();
            EasingType easingType = EasingType.fromId(buf.readByte());
            List<List<Expression>> easingArgs = ProtocolUtils.readList(buf, ExprBytesUtils::readExpressions);

            list.add(new Keyframe(length, startValue, endValue, easingType, easingArgs));
        }

        return list;
    }

    static Animation.Keyframes readEventKeyframes(ByteBuf buf) {
        // Sounds
        int soundCount = VarIntUtils.readVarInt(buf);
        SoundKeyframeData[] sounds = new SoundKeyframeData[soundCount];
        for (int i = 0; i < soundCount; i++) {
            float startTick = buf.readFloat();
            String sound = ProtocolUtils.readString(buf);
            sounds[i] = new SoundKeyframeData(startTick, sound);
        }

        // Particles
        int particleCount = VarIntUtils.readVarInt(buf);
        ParticleKeyframeData[] particles = new ParticleKeyframeData[particleCount];
        for (int i = 0; i < particleCount; i++) {
            float startTick = buf.readFloat();
            String effect = ProtocolUtils.readString(buf);
            String locator = ProtocolUtils.readString(buf);
            String script = ProtocolUtils.readString(buf);
            particles[i] = new ParticleKeyframeData(startTick, effect, locator, script);
        }

        // Instructions
        int customInstructionCount = VarIntUtils.readVarInt(buf);
        CustomInstructionKeyframeData[] customInstructions = new CustomInstructionKeyframeData[customInstructionCount];
        for (int i = 0; i < customInstructionCount; i++) {
            float startTick = buf.readFloat();
            String instructions = ProtocolUtils.readString(buf);
            customInstructions[i] = new CustomInstructionKeyframeData(startTick, instructions);
        }

        return new Animation.Keyframes(sounds, particles, customInstructions);
    }

    private static List<Keyframe> negateKeyframes(List<Keyframe> keyframes) {
        keyframes = new ArrayList<>(keyframes);
        keyframes.replaceAll(AnimationBinary::negateKeyframeExpressions);
        return keyframes;
    }

    private static Keyframe negateKeyframeExpressions(Keyframe keyframe) {
        keyframe = new Keyframe(keyframe.length(), new ArrayList<>(keyframe.startValue()), new ArrayList<>(keyframe.endValue()), keyframe.easingType(), keyframe.easingArgs());
        negateKeyframeExpressions(keyframe.startValue());
        negateKeyframeExpressions(keyframe.endValue());
        return keyframe;
    }

    private static void negateKeyframeExpressions(List<Expression> expressions) {
        if (expressions.size() == 1 && IsConstantExpression.test(expressions.getFirst())) {
            expressions.set(0, FloatExpression.of(-MOCHA_ENGINE.eval(expressions)));
        }
    }
}
