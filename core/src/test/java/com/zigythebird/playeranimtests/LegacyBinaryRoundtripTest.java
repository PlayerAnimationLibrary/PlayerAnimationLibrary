package com.zigythebird.playeranimtests;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.enums.TransformType;
import com.zigythebird.playeranimcore.network.LegacyAnimationBinary;
import com.zigythebird.playeranimtests.framework.AnimationsProvider;
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
 */
public class LegacyBinaryRoundtripTest {
    @DisplayName("LegacyAnimationBinary roundtrip")
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AnimationsProvider.class)
    public void roundtrip(Animation animation) throws IOException {
        float length = animation.length();
        for (int version = 1; version <= LegacyAnimationBinary.getCurrentVersion(); version++) {
            // Scale was added to LegacyAnimationBinary in v3; earlier versions drop it entirely.
            EnumSet<TransformType> toAssert = version < 3
                    ? EnumSet.of(TransformType.POSITION, TransformType.ROTATION)
                    : EnumSet.of(TransformType.POSITION, TransformType.ROTATION, TransformType.SCALE);
            int len = LegacyAnimationBinary.calculateSize(animation, version);
            ByteBuf buf = Unpooled.buffer(len);
            try {
                LegacyAnimationBinary.write(animation, buf, version);
                Animation decoded = LegacyAnimationBinary.read(buf, version);
                // Drive `decoded` as the iterating side so dropped bones
                // (v1: only the 6 hardcoded base bones; v2+: any bone named
                // in the animation) are excluded from the per-tick comparison.
                TestAnimationController.playing(decoded).captureAgainst(
                        TestAnimationController.playing(animation), length,
                        animation.getNameOrId() + " v" + version, toAssert);
            } finally {
                buf.release();
            }
        }
    }
}
