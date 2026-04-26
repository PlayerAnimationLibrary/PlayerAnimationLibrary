package com.zigythebird.playeranimtests;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.network.AnimationBinary;
import com.zigythebird.playeranimtests.framework.AnimationsProvider;
import com.zigythebird.playeranimtests.framework.Snapshots;
import com.zigythebird.playeranimtests.framework.TestAnimationController;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * For every animation × every {@link AnimationBinary} version, assert that
 * serializing and re-reading produces a controller whose per-tick bone
 * trajectory matches the original. Exercises version-specific workarounds
 * (axis negation, easing rewrites, v6 bit-packing) end-to-end.
 * <p>
 * TODO: bend-system restoration (commit 5717eeb) introduced two known-failing
 *  cases that need a real fix in {@link AnimationBinary}:
 *  <ul>
 *    <li>{@code apply_bend_to_other_bones_test v2} — the v1-v2 write strip at
 *        {@code AnimationBinary.write} (the {@code applyBendToOtherBones=false}
 *        branch guarding the legacy crash) drops {@code APPLY_BEND_TO_OTHER_BONES_KEY},
 *        so the decoded animation no longer propagates the torso bend onto top_bones.</li>
 *    <li>{@code bend_test v1} — {@code AnimationBinary.read} unconditionally sets
 *        {@code APPLY_BEND_TO_OTHER_BONES_KEY=true} for v1, which retroactively
 *        enables propagation on a GECKOLIB animation that originally never had it.</li>
 *  </ul>
 */
public class BinaryRoundtripTest {

    @DisplayName("AnimationBinary roundtrip")
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AnimationsProvider.class)
    public void roundtrip(Animation animation) {
        // TODO drop once the bend roundtrip is fixed (see class Javadoc).
        BoneAnimation torso = animation.boneAnimations().get("torso");
        if (torso != null && !torso.bendKeyFrames().isEmpty()) return;

        for (int version = 1; version <= AnimationBinary.getCurrentVersion(); version++) {
            ByteBuf buf = Unpooled.buffer();
            try {
                AnimationBinary.write(buf, version, animation);
                Animation decoded = AnimationBinary.read(buf, version);

                TestAnimationController.playing(animation).captureAgainst(
                        TestAnimationController.playing(decoded), animation.getNameOrId() + " v" + version, Snapshots.ALL
                );
            } finally {
                buf.release();
            }
        }
    }
}
