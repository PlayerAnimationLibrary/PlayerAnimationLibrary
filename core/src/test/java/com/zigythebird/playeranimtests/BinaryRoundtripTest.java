package com.zigythebird.playeranimtests;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.enums.TransformType;
import com.zigythebird.playeranimcore.network.AnimationBinary;
import com.zigythebird.playeranimtests.framework.AnimationsProvider;
import com.zigythebird.playeranimtests.framework.TestAnimationController;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.EnumSet;

/**
 * For every animation × every {@link AnimationBinary} version, assert that
 * serializing and re-reading produces a controller whose per-tick bone
 * trajectory matches the original. Exercises version-specific workarounds
 * (axis negation, easing rewrites, v6 bit-packing) end-to-end.
 */
public class BinaryRoundtripTest {

    @DisplayName("AnimationBinary roundtrip")
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AnimationsProvider.class)
    public void roundtrip(Animation animation) {
        float length = animation.length();
        EnumSet<TransformType> toAssert = EnumSet.of(TransformType.POSITION, TransformType.ROTATION, TransformType.SCALE);
        for (int version = 1; version <= AnimationBinary.getCurrentVersion(); version++) {
            ByteBuf buf = Unpooled.buffer();
            try {
                AnimationBinary.write(buf, version, animation);
                Animation decoded = AnimationBinary.read(buf, version);
                TestAnimationController.playing(animation).captureAgainst(
                        TestAnimationController.playing(decoded), length,
                        animation.getNameOrId() + " v" + version, toAssert);
            } finally {
                buf.release();
            }
        }
    }
}
