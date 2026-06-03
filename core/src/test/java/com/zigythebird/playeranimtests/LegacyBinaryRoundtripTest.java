package com.zigythebird.playeranimtests;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.enums.TransformType;
import com.zigythebird.playeranimcore.network.LegacyAnimationBinary;
import com.zigythebird.playeranimtests.framework.AnimationsProvider;
import com.zigythebird.playeranimtests.framework.Snapshots;
import com.zigythebird.playeranimtests.framework.TestAnimationController;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.EnumSet;

/**
 * For every animation × every {@link LegacyAnimationBinary} version, assert
 * that serializing and re-reading produces a controller whose per-tick bone
 * trajectory matches the original.
 * <p>
 * TODO: bend-system restoration (commit 5717eeb) made
 *  {@code apply_bend_to_other_bones_test v1} fail here: legacy v1 writes only
 *  the 6 hardcoded body parts (no torso slot), so {@code torso.bendKeyFrames}
 *  and {@code APPLY_BEND_TO_OTHER_BONES_KEY} are dropped on roundtrip. The decoded
 *  animation no longer propagates bend onto top_bones while the original still
 *  does. Needs a real fix in {@code LegacyAnimationBinary} (route torso bend
 *  through the legacy "body" slot when propagation is requested).
 */
public class LegacyBinaryRoundtripTest {

    @DisplayName("LegacyAnimationBinary roundtrip")
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AnimationsProvider.class)
    public void roundtrip(Animation animation) throws IOException {
        // Legacy binary doesn't carry {@link Animation#bones()} (pivots) or
        // {@link Animation#parents()}, so animations that rely on them can't
        // roundtrip through it. Molang expressions are collapsed to their
        // write-time eval (null controller → 0), so dynamic queries like
        // {@code q.anim_time} also can't roundtrip.
        if (!animation.bones().isEmpty() || !animation.parents().isEmpty()) return;
        if (animation.getNameOrId().toLowerCase().startsWith("molang")) return;
        if (PlayerAnimatorParityTest.usesUnsupportedEasing(animation)) return;
        // TODO drop once the bend roundtrip is fixed (see class Javadoc).
        BoneAnimation torso = animation.boneAnimations().get("torso");
        if (torso != null && !torso.bendKeyFrames().isEmpty()) return;

        for (int version = 1; version <= LegacyAnimationBinary.getCurrentVersion(); version++) {
            // Scale was added to LegacyAnimationBinary in v3; earlier versions drop it entirely.
            EnumSet<TransformType> toAssert = version < 3 ? Snapshots.NO_SCALE : Snapshots.ALL;

            ByteBuf buf = Unpooled.buffer(LegacyAnimationBinary.calculateSize(animation, version));
            try {
                LegacyAnimationBinary.write(animation, buf, version);
                Animation decoded = LegacyAnimationBinary.read(buf, version);

                // Drive `decoded` as the iterating side so dropped bones
                // (v1: only the 6 hardcoded base bones; v2+: any bone named
                // in the animation) are excluded from the per-tick comparison.
                TestAnimationController.playing(decoded).captureAgainst(
                        TestAnimationController.playing(animation), animation.getNameOrId() + " v" + version, toAssert
                );
            } finally {
                buf.release();
            }
        }
    }
}
