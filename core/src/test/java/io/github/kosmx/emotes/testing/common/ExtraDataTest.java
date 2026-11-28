package io.github.kosmx.emotes.testing.common;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationImage;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.network.LegacyAnimationBinary;
import com.zigythebird.playeranimtests.framework.TestImages;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redlance.smalt.MediaException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ExtraDataTest {
    @Test
    @DisplayName("Extra data test")
    public void extraDataTest() throws IOException {
        Animation animation = EmoteDataHashingTest.loadAnimation();

        Assertions.assertInstanceOf(String.class, animation.data().getNullable("author"));
        Assertions.assertInstanceOf(String.class, animation.data().getNullable("name"));
        Assertions.assertInstanceOf(String.class, animation.data().getNullable("description"));

        Assertions.assertInstanceOf(List.class, animation.data().get("bages").orElseGet(ArrayList::new));
        List<?> badges = animation.data().getList("bages");
        for (Object badge : badges) {
            Assertions.assertInstanceOf(String.class, badge);
        }
    }

    @Test
    @DisplayName("Icons decode from PNG and JPEG XL once, and give back the file they were read from")
    public void icons() {
        int[] pixels = {0xFF112233, 0x80445566};
        byte[] png = TestImages.png(2, 1, pixels);
        byte[] jxl = TestImages.jxl(2, 1, pixels);
        ExtraAnimationData data = new ExtraAnimationData();
        data.put("png", Base64.getEncoder().encodeToString(png));
        data.put("jxl", Base64.getEncoder().encodeToString(jxl));

        AnimationImage fromPng = data.getImage("png");
        Assertions.assertEquals(fromPng, data.getImage("jxl"), "both files hold the same pixels");
        Assertions.assertSame(fromPng, data.getImage("png"), "the image is decoded once");
        Assertions.assertArrayEquals(png, data.getBinary("png"));
        Assertions.assertArrayEquals(jxl, data.getBinary("jxl"));
    }

    @Test
    @DisplayName("An icon of more pixels than the textures of a model may have is refused")
    public void iconBudget() {
        ExtraAnimationData data = new ExtraAnimationData("icon", TestImages.png(2049, 2048, new int[2049 * 2048]));
        Assertions.assertThrows(MediaException.LimitExceeded.class, () -> data.getImage("icon"));
    }

    @Test
    @DisplayName("Apply bend to other bones survives the legacy binary")
    public void testApplyBendToOtherBones() throws IOException {
        Animation animation = EmoteDataHashingTest.loadAnimation("/MIEM_blowjob.json");
        Assertions.assertTrue(animation.data().has(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY));
        Assertions.assertTrue((Boolean) animation.data().getRaw(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY));

        for (int version = 1; version <= LegacyAnimationBinary.getCurrentVersion(); version++) {
            int len = LegacyAnimationBinary.calculateSize(animation, version);
            ByteBuf byteBuf = Unpooled.buffer(len);
            LegacyAnimationBinary.write(animation, byteBuf, version);
            Assertions.assertEquals(len, byteBuf.writerIndex(), "Incorrect size calculator at version " + version);

            Animation readed = LegacyAnimationBinary.read(byteBuf, version);
            Assertions.assertTrue((Boolean) readed.data().getRaw(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY),
                    "flag lost at version " + version);
            Assertions.assertEquals(animation.getBone("torso").bendKeyFrames(), readed.getBone("torso").bendKeyFrames(),
                    "torso bend lost at version " + version);
            byteBuf.release();
        }
    }
}
