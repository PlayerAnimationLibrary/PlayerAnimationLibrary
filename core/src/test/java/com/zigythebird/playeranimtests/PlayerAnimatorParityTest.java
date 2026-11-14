package com.zigythebird.playeranimtests;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.TransformType;
import com.zigythebird.playeranimcore.network.LegacyAnimationBinary;
import com.zigythebird.playeranimtests.framework.AnimationsProvider;
import com.zigythebird.playeranimtests.framework.LegacyPlayerAdapter;
import com.zigythebird.playeranimtests.framework.Snapshots;
import com.zigythebird.playeranimtests.framework.TestAnimationController;
import dev.kosmx.playerAnim.core.data.AnimationBinary;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import io.github.kosmx.emotes.testing.common.EmoteDataHashingTest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * Cross-library behavior check: our controller vs the original
 * {@code KeyframeAnimationPlayer} (playeranimator fork). Both consume the
 * same bytes produced by {@code LegacyAnimationBinary.write} (molang already
 * collapsed) — one side re-reads via our {@code LegacyAnimationBinary.read},
 * the other via {@code dev.kosmx}'s {@code AnimationBinary.read}. Per-tick
 * bone trajectories must match within {@link Snapshots#DEFAULT_EPSILON}.
 */
public class PlayerAnimatorParityTest {

    @DisplayName("playeranimator parity over legacy binary")
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AnimationsProvider.class)
    public void parity(Animation animation) throws IOException {
        // Custom pivots and parent edges are smuggled through the legacy format
        // behind {@code @pal@pivot@} / {@code @pal@parent@} prefixes, which KAP
        // reads as harmless dummy bones — it can't reconstruct the pivot effect
        // on bone positions, so parity is fundamentally unreachable for animations
        // that rely on them.
        if (!animation.bones().isEmpty() || !animation.parents().isEmpty()) return;

        // KAP's CATMULLROM is a degenerate `n + 2` wrapped in easeInOut (the spline
        // formula's `t` factors were hard-coded as `1`); it evaluates to 1 at f=0
        // and jumps straight to the `after` keyframe's value. Our implementation
        // is the proper centripetal spline, so parity is unreachable here.
        if (usesUnsupportedEasing(animation)) return;

        // Torso bend propagation ({@link ExtraAnimationData#APPLY_BEND_TO_OTHER_BONES_KEY}) lives in
        // playeranimator's renderer, not in KAP - it hands out the raw per-part transforms and never
        // rotates the upper body around the bend - so the bones our controller propagates onto have
        // nothing to be compared against.
        if (propagatesBendToAnimatedBone(animation)) return;

        forEachLegacyVersion(animation, (our, their, label, toAssert) ->
                TestAnimationController.playing(our).captureAgainst(new LegacyPlayerAdapter(their), label, toAssert)
        );
    }

    @DisplayName("playeranimator loop parity over legacy binary")
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AnimationsProvider.class)
    public void loopParity(Animation animation) throws IOException {
        assertLoopParity(animation);
    }

    /**
     * A library emote whose restart tick is itself a keyframe, the case where a loop jumped back
     * a keyframe on every wrap instead of blending into it.
     */
    @Test
    public void loopParityWhenRestartIsAKeyframe() throws IOException {
        assertLoopParity(EmoteDataHashingTest.loadAnimation("/SPE_Ankha_Dance.json"));
    }

    /** Restarts between two keyframes, so only part of the keyframe it falls in belongs to the seam. */
    @Test
    public void loopParityWhenRestartIsBetweenKeyframes() throws IOException {
        assertLoopParity(EmoteDataHashingTest.loadAnimation("/MIEM_blowjob.json"));
    }

    /**
     * KAP loops by jumping from past its end tick back to the return tick and blends the last
     * keyframe into the first one after it; our controller has to land on the same poses across
     * several wraps. Other formats restart plainly, so only playeranimator loops have a reference.
     */
    private static void assertLoopParity(Animation animation) throws IOException {
        Animation.LoopType loopType = animation.loopType();
        if (!animation.data().isAnimationPlayerAnimatorFormat() || loopType == Animation.LoopType.HOLD_ON_LAST_FRAME ||
                !loopType.shouldPlayAgain(null, animation)) return;

        // Same reasons as in {@link #parity}.
        if (!animation.bones().isEmpty() || !animation.parents().isEmpty()) return;
        if (usesUnsupportedEasing(animation)) return;

        // With no keyframe at or past the return tick, KAP blends such an axis into its default
        // value over the whole loop; ours keeps holding the last keyframe there, as it always has.
        if (endsAxisBeforeReturnTick(animation)) return;

        forEachLegacyVersion(animation, (our, their, label, toAssert) -> {
            // KAP hands out the raw per-part transforms, see {@link #parity}; without the propagation
            // ours are the same raw transforms, and the loop does not depend on it.
            our.data().data().remove(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY);

            float period = our.length() - our.loopType().restartFromTick(null, our);
            TestAnimationController.looping(our).captureAgainst(
                    new LegacyPlayerAdapter(their, true), label, toAssert, our.length() + LOOPS * period, LOOP_EPSILON
            );
        });
    }

    /** How many wraps a loop is followed through after its first pass. */
    private static final int LOOPS = 3;

    /**
     * Tighter than {@link Snapshots#DEFAULT_EPSILON}: a seam blended over the wrong stretch
     * misplaces a pose by hundredths, and both sides stay within thousandths of each other otherwise.
     */
    private static final float LOOP_EPSILON = 0.01f;

    /**
     * Writes {@code animation} with every legacy binary version and hands both readings to
     * {@code check}: ours through {@code LegacyAnimationBinary.read}, KAP's through its own reader.
     */
    private static void forEachLegacyVersion(Animation animation, LegacyReadings check) throws IOException {
        for (int version = 1; version <= LegacyAnimationBinary.getCurrentVersion(); version++) {
            EnumSet<TransformType> toAssert = version < 3 ? Snapshots.NO_SCALE : Snapshots.ALL;

            ByteBuf buf = Unpooled.buffer(LegacyAnimationBinary.calculateSize(animation, version));
            try {
                LegacyAnimationBinary.write(animation, buf, version);
                int writerIndex = buf.writerIndex();
                KeyframeAnimation their = AnimationBinary.read(buf.nioBuffer(0, writerIndex), version);
                Animation our = LegacyAnimationBinary.read(buf, version);

                check.accept(our, their, animation.getNameOrId() + " v" + version, toAssert);
            } finally {
                buf.release();
            }
        }
    }

    @FunctionalInterface
    private interface LegacyReadings {
        void accept(Animation our, KeyframeAnimation their, String label, EnumSet<TransformType> toAssert);
    }

    private static boolean propagatesBendToAnimatedBone(Animation animation) {
        BoneAnimation torso = animation.boneAnimations().get("torso");
        if (torso == null || torso.bendKeyFrames().isEmpty() ||
                animation.data().getNullable(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY) != Boolean.TRUE) return false;

        return TestAnimationController.TOP_BONES.stream().anyMatch(animation.boneAnimations()::containsKey);
    }

    public static boolean usesUnsupportedEasing(Animation animation) {
        return animation.boneAnimations().values().stream().flatMap(PlayerAnimatorParityTest::allKeyframes)
                .anyMatch(k -> k.easingType() == EasingType.CATMULLROM || k.easingType() == EasingType.BEZIER);
    }

    /** The legacy binary hands our restart tick to KAP as its return tick, one tick later. */
    private static boolean endsAxisBeforeReturnTick(Animation animation) {
        float returnTick = animation.loopType().restartFromTick(null, animation) + 1;
        return animation.boneAnimations().values().stream().flatMap(PlayerAnimatorParityTest::keyframeLists)
                .anyMatch(keyframes -> !keyframes.isEmpty() && Keyframe.getLastKeyframeTime(keyframes) < returnTick);
    }

    private static Stream<Keyframe> allKeyframes(BoneAnimation bone) {
        return keyframeLists(bone).flatMap(List::stream);
    }

    private static Stream<List<Keyframe>> keyframeLists(BoneAnimation bone) {
        return Stream.of(
                bone.rotationKeyFrames().xKeyframes(), bone.rotationKeyFrames().yKeyframes(), bone.rotationKeyFrames().zKeyframes(),
                bone.positionKeyFrames().xKeyframes(), bone.positionKeyFrames().yKeyframes(), bone.positionKeyFrames().zKeyframes(),
                bone.scaleKeyFrames().xKeyframes(), bone.scaleKeyFrames().yKeyframes(), bone.scaleKeyFrames().zKeyframes(),
                bone.bendKeyFrames()
        );
    }
}
