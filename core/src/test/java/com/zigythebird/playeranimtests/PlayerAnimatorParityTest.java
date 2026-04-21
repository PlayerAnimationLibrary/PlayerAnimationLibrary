package com.zigythebird.playeranimtests;

import com.zigythebird.playeranimcore.animation.Animation;
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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.DisplayName;
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
        if (usesCatmullRom(animation)) return;

        for (int version = 1; version <= LegacyAnimationBinary.getCurrentVersion(); version++) {
            EnumSet<TransformType> toAssert = version < 3 ? Snapshots.NO_SCALE : Snapshots.ALL;

            ByteBuf buf = Unpooled.buffer(LegacyAnimationBinary.calculateSize(animation, version));
            try {
                LegacyAnimationBinary.write(animation, buf, version);
                int writerIndex = buf.writerIndex();
                KeyframeAnimation their = AnimationBinary.read(buf.nioBuffer(0, writerIndex), version);
                Animation our = LegacyAnimationBinary.read(buf, version);

                TestAnimationController.playing(our).captureAgainst(
                        new LegacyPlayerAdapter(their), animation.getNameOrId() + " v" + version, toAssert
                );
            } finally {
                buf.release();
            }
        }
    }

    private static boolean usesCatmullRom(Animation animation) {
        return animation.boneAnimations().values().stream().flatMap(PlayerAnimatorParityTest::allKeyframes)
                .anyMatch(k -> k.easingType() == EasingType.CATMULLROM);
    }

    private static Stream<Keyframe> allKeyframes(BoneAnimation bone) {
        return Stream.of(
                bone.rotationKeyFrames().xKeyframes(), bone.rotationKeyFrames().yKeyframes(), bone.rotationKeyFrames().zKeyframes(),
                bone.positionKeyFrames().xKeyframes(), bone.positionKeyFrames().yKeyframes(), bone.positionKeyFrames().zKeyframes(),
                bone.scaleKeyFrames().xKeyframes(), bone.scaleKeyFrames().yKeyframes(), bone.scaleKeyFrames().zKeyframes(),
                bone.bendKeyFrames()
        ).flatMap(List::stream);
    }
}
