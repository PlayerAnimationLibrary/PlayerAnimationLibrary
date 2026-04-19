/*
 * MIT License
 *
 * Copyright (c) 2022 KosmX
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.zigythebird.playeranimcore.network;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.AnimationFormat;
import com.zigythebird.playeranimcore.loading.PlayerAnimatorLoader;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimcore.math.Vec3f;
import io.netty.buffer.ByteBuf;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.parser.ast.FloatExpression;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

import static com.zigythebird.playeranimcore.molang.MolangLoader.MOCHA_ENGINE;

/**
 * Utility class to convert animation data to a binary format.
 * Includes a size predictor, using {@link java.nio.ByteBuffer}
 * Does <b>not</b> pack extraData, that must be done manually
 */
@SuppressWarnings("unused")
public final class LegacyAnimationBinary {
    public static final Predicate<String> BEND_BONE = name -> !name.equals("head") && !name.equals("left_item") && !name.equals("right_item");
    public static final Predicate<String> ITEM_BONE = name -> name.equals("left_item") || name.equals("right_item");

    /**
     * Write the animation into the ByteBuffer.
     * Versioning:
     * 1. Emotecraft 2.1 features
     * 2. New animation format for Animation library - including enable states, dynamic parts
     * Format type 1 takes less data, but only works for standard models and unable to send data for disabled states
     * @param animation animation
     * @param buf       target byteBuf
     * @param version   Binary version
     */
    public static void write(Animation animation, ByteBuf buf, int version) {
        buf.writeInt(animation.data().<Float>get(ExtraAnimationData.BEGIN_TICK_KEY).orElse(0F).intValue());
        int endTick = animation.data().<Float>get(ExtraAnimationData.END_TICK_KEY).orElse(animation.length()).intValue();
        buf.writeInt(endTick);
        buf.writeInt((int) animation.length());
        if (animation.loopType() == Animation.LoopType.HOLD_ON_LAST_FRAME) {
            putBoolean(buf, true);
            buf.writeInt(endTick);
        } else {
            putBoolean(buf, animation.loopType().shouldPlayAgain(null, animation));
            buf.writeInt((int)animation.loopType().restartFromTick(null, animation) + 1);
        }
        boolean easeBefore = animation.data().<Boolean>get(ExtraAnimationData.EASING_BEFORE_KEY)
                .orElse(animation.data().data().getOrDefault(ExtraAnimationData.FORMAT_KEY, AnimationFormat.GECKOLIB) == AnimationFormat.GECKOLIB);
        putBoolean(buf, easeBefore);
        putBoolean(buf, false); //NSFW tag
        buf.writeByte(keyframeSize(version));
        if (version >= 2) {
            buf.writeInt(animation.boneAnimations().size());
            for (Map.Entry<String, BoneAnimation> part : animation.boneAnimations().entrySet()) {
                putString(buf, UniversalAnimLoader.restorePlayerBoneName(part.getKey()));
                writePart(buf, part.getKey(), part.getValue(), version, easeBefore);
            }
        } else {
            writePart(buf, "head", animation.getBone("head"), version, easeBefore);
            writePart(buf, "body", animation.getBone("body"), version, easeBefore);
            writePart(buf, "right_arm", animation.getBone("right_arm"), version, easeBefore);
            writePart(buf, "left_arm", animation.getBone("left_arm"), version, easeBefore);
            writePart(buf, "right_leg", animation.getBone("right_leg"), version, easeBefore);
            writePart(buf, "left_leg", animation.getBone("left_leg"), version, easeBefore);
        }
        NetworkUtils.writeUuid(buf, animation.uuid());
    }

    /**
     * Write the animation into the ByteBuffer using the latest format version
     * @param animation animation
     * @param buf       target byteBuf
     */
    public static void write(Animation animation, ByteBuf buf) {
        write(animation, buf, getCurrentVersion());
    }

    private static void writePart(ByteBuf buf, String name, BoneAnimation part, int version, boolean easeBefore) {
        if (part == null) {
            int i = 6;
            if (BEND_BONE.test(name)) i += 2;
            if (version >= 3) i += 3;
            for (; i > 0; i--) {
                if (version >= 2) {
                    putBoolean(buf, false);
                    buf.writeInt(0);
                } else buf.writeInt(-1);
            }
            return;
        }
        Vec3f def = PlayerAnimatorLoader.getDefaultValues(name);
        boolean isItem = ITEM_BONE.test(name);
        boolean isBody = name.equals("body");
        boolean isCape = name.equals("cape");
        writeKeyframes(buf, part.positionKeyFrames().xKeyframes(), def.x(), version, easeBefore, isBody, isItem || isCape || isBody);
        writeKeyframes(buf, isItem ? part.positionKeyFrames().zKeyframes() : part.positionKeyFrames().yKeyframes(), def.y(), version, easeBefore, isBody, false);
        writeKeyframes(buf, isItem ? part.positionKeyFrames().yKeyframes() : part.positionKeyFrames().zKeyframes(), def.z(), version, easeBefore, isBody, isCape);
        writeKeyframes(buf, part.rotationKeyFrames().xKeyframes(), version, easeBefore, isItem || isCape || isBody);
        writeKeyframes(buf, isItem ? part.rotationKeyFrames().zKeyframes() : part.rotationKeyFrames().yKeyframes(), version, easeBefore, isItem || isBody);
        writeKeyframes(buf, isItem ? part.rotationKeyFrames().yKeyframes() : part.rotationKeyFrames().zKeyframes(), version, easeBefore, isItem || isCape);
        if (BEND_BONE.test(name)) {
            //Marking the no longer supported Y axis bend keyframes as non-existent
            if (version >= 2) {
                putBoolean(buf, false);
                buf.writeInt(0);
            } else {
                buf.writeInt(-1);
            }
            writeKeyframes(buf, part.bendKeyFrames(), version, easeBefore, false);
        }
        if (version >= 3) {
            writeKeyframes(buf, part.scaleKeyFrames().xKeyframes(), version, easeBefore, false);
            writeKeyframes(buf, part.scaleKeyFrames().yKeyframes(), version, easeBefore, false);
            writeKeyframes(buf, part.scaleKeyFrames().zKeyframes(), version, easeBefore, false);
        }
    }

    private static void writeKeyframes(ByteBuf buf, List<Keyframe> part, int version, boolean easeBefore, boolean negate) {
        writeKeyframes(buf, part, 0f, version, easeBefore, false, negate);
    }

    private static void writeKeyframes(ByteBuf buf, List<Keyframe> part, float def, int version, boolean easeBefore, boolean div, boolean negate) {
        if (version >= 2) {
            putBoolean(buf, !part.isEmpty());
        }

        int keyframeCount = part.size();
        if (!easeBefore) keyframeCount -= 1;
        buf.writeInt(keyframeCount);
        if (keyframeCount <= 0) return;

        float tickAccumulator = 0;
        for (int i = 0; i < keyframeCount; i++) {
            Keyframe move = part.get(i);

            tickAccumulator += move.length();
            buf.writeInt((int) Math.floor(tickAccumulator));

            buf.writeFloat(((MOCHA_ENGINE.eval(move.endValue()) * (negate ? -1 : 1)) + def) / (div ? 16f : 1f));

            EasingType easingToWrite;
            List<Expression> easingArgsToWrite = Collections.emptyList();

            if (easeBefore) {
                easingToWrite = move.easingType();
                if (move.easingArgs() != null && !move.easingArgs().isEmpty() && !move.easingArgs().getFirst().isEmpty()) {
                    easingArgsToWrite = move.easingArgs().getFirst();
                }
            } else {
                Keyframe nextMove = part.get(i + 1);
                easingToWrite = nextMove.easingType();
                if (nextMove.easingArgs() != null && !nextMove.easingArgs().isEmpty() && !nextMove.easingArgs().getFirst().isEmpty()) {
                    easingArgsToWrite = nextMove.easingArgs().getFirst();
                }
            }

            buf.writeByte(easingToWrite.id);

            if (version >= 4) {
                if (easingArgsToWrite != null && !easingArgsToWrite.isEmpty()) {
                    buf.writeFloat(MOCHA_ENGINE.eval(easingArgsToWrite));
                } else {
                    buf.writeFloat(Float.NaN);
                }
            }
        }
    }

    public static Animation read(ByteBuf buf) throws IOException {
        return read(buf, getCurrentVersion());
    }

    /**
     * Read keyframe animation from binary data.
     * Creates a Bool extra property with validation data with name <code>valid</code>
     * @param buf       byteBuf
     * @param version   format version (not stored in binary)
     * @return          KeyframeAnimation
     * @throws IOException if encounters invalid data
     */
    public static Animation read(ByteBuf buf, int version) throws IOException {
        ExtraAnimationData data = new ExtraAnimationData();

        int beginTick = buf.readInt();
        data.put(ExtraAnimationData.BEGIN_TICK_KEY, (float) beginTick);

        int endTick = Math.max(buf.readInt(), beginTick + 1);
        if (endTick <= 0) throw new IOException("endTick must be bigger than 0");
        data.put(ExtraAnimationData.END_TICK_KEY, (float) endTick);

        int stopTick = buf.readInt();

        boolean isLooped = getBoolean(buf);
        int returnTick = Math.max(0, buf.readInt() - 1);
        Animation.LoopType loopType = Animation.LoopType.PLAY_ONCE;
        if (isLooped) {
            if (returnTick > endTick) {
                throw new IOException("The returnTick has to be a non-negative value smaller than the endTick value");
            }
            if (returnTick == 0) loopType = Animation.LoopType.LOOP;
            else loopType = Animation.LoopType.returnToTickLoop(returnTick);
        }
        if (loopType == Animation.LoopType.PLAY_ONCE) {
            endTick = stopTick <= endTick ? endTick + 3 : stopTick; // https://github.com/KosmX/minecraftPlayerAnimator/blob/1.21/coreLib/src/main/java/dev/kosmx/playerAnim/core/data/KeyframeAnimation.java#L80
        }

        boolean easeBefore = getBoolean(buf);
        data.put(ExtraAnimationData.EASING_BEFORE_KEY, easeBefore);
        getBoolean(buf); //Ignored NSFW tag
        int keyframeSize = buf.readByte();
        if (keyframeSize <= 0) throw new IOException("keyframe size must be greater than 0, current: " + keyframeSize);
        Map<String, BoneAnimation> boneAnimations = new HashMap<>();
        if (version >= 2) {
            int count = buf.readInt();
            for (int i = 0; i < count; i++) {
                String name = UniversalAnimLoader.getCorrectPlayerBoneName(getString(buf));
                boneAnimations.put(name, readPart(buf, name, new BoneAnimation(), version, keyframeSize, easeBefore));
            }
        } else {
            boneAnimations.put("head", readPart(buf, "head", new BoneAnimation(), version, keyframeSize, easeBefore));
            boneAnimations.put("body", readPart(buf, "body", new BoneAnimation(), version, keyframeSize, easeBefore));
            boneAnimations.put("right_arm", readPart(buf, "right_arm", new BoneAnimation(), version, keyframeSize, easeBefore));
            boneAnimations.put("left_arm", readPart(buf, "left_arm", new BoneAnimation(), version, keyframeSize, easeBefore));
            boneAnimations.put("right_leg", readPart(buf, "right_leg", new BoneAnimation(), version, keyframeSize, easeBefore));
            boneAnimations.put("left_leg", readPart(buf, "left_leg", new BoneAnimation(), version, keyframeSize, easeBefore));
        }
        // Remove empty phantom bones (v1 always creates 6 bones even if they have no data)
        boneAnimations.values().removeIf(bone -> !bone.hasKeyframes());

        BoneAnimation body = boneAnimations.get("body");
        if (body != null && !body.bendKeyFrames().isEmpty()) {
            BoneAnimation torso = boneAnimations.computeIfAbsent("torso", name -> new BoneAnimation());
            torso.bendKeyFrames().addAll(body.bendKeyFrames());
            body.bendKeyFrames().clear();
            data.put(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY, true);
        }
        data.put(ExtraAnimationData.UUID_KEY, NetworkUtils.readUuid(buf));
        data.put(ExtraAnimationData.FORMAT_KEY, AnimationFormat.PLAYER_ANIMATOR);

        return new Animation(data, endTick, loopType, boneAnimations, UniversalAnimLoader.NO_KEYFRAMES, new HashMap<>(), new HashMap<>());
    }

    private static BoneAnimation readPart(ByteBuf buf, String name, BoneAnimation part, int version, int keyframeSize, boolean easeBefore) {
        Vec3f def = PlayerAnimatorLoader.getDefaultValues(name);
        boolean isBody = name.equals("body");
        boolean isItem = ITEM_BONE.test(name);
        boolean isCape = name.equals("cape");
        readKeyframes(buf, part.positionKeyFrames().xKeyframes(), def.x(), version, keyframeSize, isBody, isItem || isCape || isBody, PlayerAnimatorLoader.ZERO);
        readKeyframes(buf, part.positionKeyFrames().yKeyframes(), def.y(), version, keyframeSize, isBody, false, PlayerAnimatorLoader.ZERO);
        readKeyframes(buf, part.positionKeyFrames().zKeyframes(), def.z(), version, keyframeSize, isBody, isCape, PlayerAnimatorLoader.ZERO);
        readKeyframes(buf, part.rotationKeyFrames().xKeyframes(), version, keyframeSize, isItem || isCape || isBody, PlayerAnimatorLoader.ZERO);
        readKeyframes(buf, part.rotationKeyFrames().yKeyframes(), version, keyframeSize, isItem || isBody, PlayerAnimatorLoader.ZERO);
        readKeyframes(buf, part.rotationKeyFrames().zKeyframes(), version, keyframeSize, isItem || isCape, PlayerAnimatorLoader.ZERO);
        if (BEND_BONE.test(name)) {
            readKeyframes(buf, new ArrayList<>(), version, keyframeSize, false, PlayerAnimatorLoader.ZERO); // Discarded since no Y axis bend support
            readKeyframes(buf, part.bendKeyFrames(), version, keyframeSize, false, PlayerAnimatorLoader.ZERO);
        }
        if (version >= 3) {
            readKeyframes(buf, part.scaleKeyFrames().xKeyframes(), version, keyframeSize, false, PlayerAnimatorLoader.ONE);
            readKeyframes(buf, part.scaleKeyFrames().yKeyframes(), version, keyframeSize, false, PlayerAnimatorLoader.ONE);
            readKeyframes(buf, part.scaleKeyFrames().zKeyframes(), version, keyframeSize, false, PlayerAnimatorLoader.ONE);
        }
        if (!easeBefore) {
            PlayerAnimatorLoader.correctEasings(part.positionKeyFrames());
            PlayerAnimatorLoader.correctEasings(part.rotationKeyFrames());
            PlayerAnimatorLoader.correctEasings(part.scaleKeyFrames());
            PlayerAnimatorLoader.correctEasings(part.bendKeyFrames());
        }
        if (isItem) {
            PlayerAnimatorLoader.swapTheZYAxis(part.positionKeyFrames());
            PlayerAnimatorLoader.swapTheZYAxis(part.rotationKeyFrames());
        }
        return part;
    }

    private static void readKeyframes(ByteBuf buf, List<Keyframe> part, int version, int keyframeSize, boolean negate, List<Expression> fallback) {
        readKeyframes(buf, part, (float) 0, version, keyframeSize, false, negate, fallback);
    }

    private static void readKeyframes(ByteBuf buf, List<Keyframe> part, float def, int version, int keyframeSize, boolean mul, boolean negate, List<Expression> fallback) {
        int length;
        boolean enabled;
        if (version >= 2) {
            enabled = getBoolean(buf);
            length = buf.readInt();
        } else {
            length = buf.readInt();
            enabled = length >= 0;
        }

        if (!enabled) {
            if (length > 0) {
                buf.readerIndex(buf.readerIndex() + length * keyframeSize);
            }
            part.clear();
            return;
        }

        int lastTick = 0;
        for (int i = 0; i < length; i++) {
            Keyframe prevKeyframe = part.isEmpty() ? null : part.getLast();
            int currentPos = buf.readerIndex();

            int tick = buf.readInt();
            float keyframeLength = (float)tick - lastTick;
            lastTick = tick;

            List<Expression> expression = Collections.singletonList(FloatExpression.of((buf.readFloat() - def) * (mul ? 16 : 1) * (negate ? -1 : 1)));
            EasingType easingType = EasingType.fromId(buf.readByte());
            Float easingArg = null;

            if (version >= 4) {
                easingArg = buf.readFloat();

                if (Float.isNaN(easingArg)) {
                    easingArg = null;
                }
            }

            part.add(new Keyframe(keyframeLength, prevKeyframe == null ? fallback : prevKeyframe.endValue(), expression, easingType,
                    easingArg == null ? Collections.singletonList(Collections.emptyList()) :
                            Collections.singletonList(Collections.singletonList(FloatExpression.of(easingArg)))));
            buf.readerIndex(currentPos + keyframeSize);
        }
    }

    /**
     * Current animation binary version
     * @return version
     */
    public static int getCurrentVersion() {
        return 4;
    }

    public static int calculateSize(Animation animation) {
        return calculateSize(animation, getCurrentVersion());
    }

    public static int calculateSize(Animation animation, int version) {
        //I will create less efficient loops, but these will be more easily fixable
        int size = 36;//The header makes xx bytes IIIBIBBBLL
        boolean easeBefore = animation.data().<Boolean>get(ExtraAnimationData.EASING_BEFORE_KEY)
                .orElse(animation.data().data().getOrDefault(ExtraAnimationData.FORMAT_KEY, AnimationFormat.GECKOLIB) == AnimationFormat.GECKOLIB);
        if (version < 2) {
            size += partSize(animation.getBone("head"), false, version, easeBefore);
            size += partSize(animation.getBone("body"), true, version, easeBefore);
            size += partSize(animation.getBone("right_arm"), true, version, easeBefore);
            size += partSize(animation.getBone("left_arm"), true, version, easeBefore);
            size += partSize(animation.getBone("right_leg"), true, version, easeBefore);
            size += partSize(animation.getBone("left_leg"), true, version, easeBefore);
        } else {
            size += 4;
            for (Map.Entry<String, BoneAnimation> entry : animation.boneAnimations().entrySet()) {
                size += stringSize(UniversalAnimLoader.restorePlayerBoneName(entry.getKey())) + partSize(entry.getValue(), BEND_BONE.test(entry.getKey()), version, easeBefore);
            }
        }
        //The size of an empty emote is 230 bytes.
        //But that makes the size to be 230 + keyframes count*9 bytes.
        //46 axes, including bends for every body-part except head.
        return size;
    }

    private static int stringSize(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        return bytes.length + 4;
    }

    private static int partSize(BoneAnimation part, boolean bendable, int version, boolean easeBefore) {
        if (part == null) {
            int i = 6;
            if (bendable) i += 2;
            if (version >= 3) i += 3;
            return i * (version >= 2 ? 5 : 4);
        }
        int size = 0;
        size += axisSize(part.positionKeyFrames().xKeyframes(), version, easeBefore);
        size += axisSize(part.positionKeyFrames().yKeyframes(), version, easeBefore);
        size += axisSize(part.positionKeyFrames().zKeyframes(), version, easeBefore);
        size += axisSize(part.rotationKeyFrames().xKeyframes(), version, easeBefore);
        size += axisSize(part.rotationKeyFrames().yKeyframes(), version, easeBefore);
        size += axisSize(part.rotationKeyFrames().zKeyframes(), version, easeBefore);
        if (bendable) {
            size += version >= 2 ? 5 : 4;
            size += axisSize(part.bendKeyFrames(), version, easeBefore);
        }
        if (version >= 3) {
            size += axisSize(part.scaleKeyFrames().xKeyframes(), version, easeBefore);
            size += axisSize(part.scaleKeyFrames().yKeyframes(), version, easeBefore);
            size += axisSize(part.scaleKeyFrames().zKeyframes(), version, easeBefore);
        }
        return size;
    }

    private static int axisSize(List<Keyframe> axis, int version, boolean easeBefore) {
        return Math.max(0, (axis.size() - (easeBefore ? 0 : 1)))*keyframeSize(version) + (version >= 2 ? 5 : 4);// count*IFB + I (for count)
    }

    /**
     * Size needed for one keyframe
     */
    private static byte keyframeSize(int version) {
        return version < 4 ? (byte) 9 /* 4 (int) + 4 (float) + 1 (byte) */ : (byte) 13; /* + 4 (float) */
    }

    /**
     * Writes a bool value into byteBuffer, using one byte per bool
     * @param byteBuffer buf
     * @param bl         bool
     */
    public static void putBoolean(ByteBuf byteBuffer, boolean bl){
        byteBuffer.writeByte((byte) (bl ? 1 : 0));
    }

    /**
     * Reads a bool value from byteBuffer
     * @param buf buf
     * @return    bool
     */
    public static boolean getBoolean(ByteBuf buf) {
        return buf.readByte() != (byte) 0;
    }

    /**
     * Writes a binary string into byteBuf
     * first 4 bytes for size, then string data
     * @param buf buf
     * @param str str
     */
    public static void putString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    /**
     * Reads string from buf, see {@link LegacyAnimationBinary#putString(ByteBuf, String)}
     * @param buf buf
     * @return str
     */
    public static String getString(ByteBuf buf) {
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}