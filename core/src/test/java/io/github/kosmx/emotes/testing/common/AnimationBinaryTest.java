package io.github.kosmx.emotes.testing.common;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.network.AnimationBinary;
import com.zigythebird.playeranimcore.network.LegacyAnimationBinary;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

/**
 * Test network data sending and receiving
 */
public class AnimationBinaryTest {
    private static final List<String> V1_BONES = List.of("head", "body", "right_arm", "left_arm", "right_leg", "left_leg");

    @Test
    @DisplayName("New binary format")
    public void newBinaryTest() throws IOException {
        Animation animation = EmoteDataHashingTest.loadAnimation();

        for (int version = 1; version <= AnimationBinary.getCurrentVersion(); version++) {
            ByteBuf byteBuf = Unpooled.buffer();
            AnimationBinary.write(byteBuf, version, animation);

            Animation readed = AnimationBinary.read(byteBuf, version);
            Assertions.assertEquals(animation.boneAnimations(), readed.boneAnimations(), "animation reads incorrectly at version " + version);
        }
    }

    @Test
    @DisplayName("Legacy binary format")
    public void legacyBinaryTest() throws IOException {
        Animation animation = EmoteDataHashingTest.loadAnimation();

        for (int version = 1; version <= LegacyAnimationBinary.getCurrentVersion(); version++) {
            int len = LegacyAnimationBinary.calculateSize(animation, version);
            ByteBuf byteBuf = Unpooled.buffer(len);
            LegacyAnimationBinary.write(animation, byteBuf, version);
            Assertions.assertEquals(len, byteBuf.writerIndex(), "Incorrect size calculator at version " + version);

            Assertions.assertTrue(byteBuf.readableBytes() > 0, "animation reads incorrectly at version " + version);

            Animation readed = LegacyAnimationBinary.read(byteBuf, version);

            if (version < 2) {
                // V1 only supports 6 hardcoded bones — compare only those
                for (String bone : V1_BONES) {
                    BoneAnimation orig = animation.boneAnimations().get(bone);
                    BoneAnimation read = readed.boneAnimations().get(bone);
                    if (orig != null) {
                        Assertions.assertEquals(orig, read, "bone '" + bone + "' reads incorrectly at version " + version);
                    }
                }
            } else {
                Assertions.assertEquals(animation.boneAnimations(), readed.boneAnimations(), "animation reads incorrectly at version " + version);
            }
            byteBuf.release();
        }
    }
}
