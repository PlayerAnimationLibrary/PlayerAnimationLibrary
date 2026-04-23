package io.github.kosmx.emotes.testing.common;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;

public class EmoteDataHashingTest {
    @RepeatedTest(10)
    @DisplayName("emoteData hashing test")
    public void hashAndEqualsTest() throws IOException {
        Animation emote1 = EmoteDataHashingTest.loadAnimation();
        Animation emote2 = EmoteDataHashingTest.loadAnimation();

        Assertions.assertEquals(emote1, emote2, "EmoteData should equal with the a perfect copy"); //Object are not the same, but should be equal
        Assertions.assertEquals(emote1.hashCode(), emote2.hashCode(), "The hash should be same");

        Assertions.assertEquals(ANIMATION_UUID, emote1.get(), "The uuid should be same");
        Assertions.assertEquals(ANIMATION_UUID, emote2.get(), "The uuid should be same");

        emote1.boneAnimations().entrySet().iterator().next().getValue()
                .positionKeyFrames().xKeyframes().add(new Keyframe(1, Collections.emptyList(), Collections.emptyList(), EasingType.CONSTANT));

        Assertions.assertNotEquals(emote1, emote2, "After any change these should be NOT equals");
        Assertions.assertNotEquals(emote1.hashCode(), emote2.hashCode(), "After any change these should have different hash");
    }

    public static final UUID ANIMATION_UUID = UUID.fromString("0003a26e-3bf7-b554-0007-698deefeb9da");
    public static Animation loadAnimation() throws IOException {
        try (InputStream is = EmoteDataHashingTest.class.getResourceAsStream("/bye-bye-bye.json")) {
            return UniversalAnimLoader.loadAnimations(is).values().iterator().next();
        }
    }
}
