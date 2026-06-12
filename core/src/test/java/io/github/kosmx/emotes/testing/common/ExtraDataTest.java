package io.github.kosmx.emotes.testing.common;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.network.AnimationBinary;
import com.zigythebird.playeranimcore.network.LegacyAnimationBinary;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
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
    public void testApplyBendToOtherBones() throws IOException {
        Animation animation = EmoteDataHashingTest.loadAnimation("/MIEM_blowjob.json");
        Assertions.assertTrue(animation.data().has(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY));
        Assertions.assertTrue((Boolean) animation.data().getRaw(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY));

        for (int version = 1; version <= LegacyAnimationBinary.getCurrentVersion(); version++) {
            ByteBuf byteBuf = Unpooled.buffer();
            LegacyAnimationBinary.write(animation, byteBuf, version);

            Animation readed = LegacyAnimationBinary.read(byteBuf, version);
            Assertions.assertFalse(readed.data().has(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY));
        }
    }
}
