package io.github.kosmx.emotes.testing.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimtests.framework.TestImages;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
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

    @Test
    @DisplayName("Bones without a model keep the UUIDs they had before bones could carry one")
    public void pivotOnlyUuidTest() throws IOException {
        Assertions.assertEquals(UUID.fromString("0003dac4-5039-7a0a-0007-c1691681e9b0"), EmoteDataHashingTest.loadAnimation("/assets/player_animation_library/player_animations/pivot_placement_test_one.json").get());
        Assertions.assertEquals(UUID.fromString("00036d33-b44f-5c05-0007-064116d8f4cf"), EmoteDataHashingTest.loadAnimation("/assets/player_animation_library/player_animations/test_more_complex_emote_bezier.json").get());
    }

    @Test
    @DisplayName("A textured model gets the same UUID on every load")
    public void texturedModelUuidTest() throws IOException {
        Animation model1 = EmoteDataHashingTest.loadAnimation("/cmm_bone_test.json");
        Animation model2 = EmoteDataHashingTest.loadAnimation("/cmm_bone_test.json");

        Assertions.assertEquals(model1, model2, "The texture should not make two loads unequal");
        Assertions.assertEquals(model1.get(), model2.get(), "The uuid should be same");
    }

    @Test
    @DisplayName("Models that differ only in their texture get different UUIDs")
    public void retexturedModelUuidTest() throws IOException {
        JsonObject json;
        try (InputStream is = EmoteDataHashingTest.class.getResourceAsStream("/cmm_bone_test.json")) {
            json = JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(is))).getAsJsonObject();
        }
        Animation model = UniversalAnimLoader.loadAnimations(json.deepCopy()).values().iterator().next();

        // The same size as the model's own, so only the pixels differ
        int[] black = new int[16 * 16];
        Arrays.fill(black, 0xFF000000);
        String otherTexture = Base64.getEncoder().encodeToString(TestImages.png(16, 16, black));
        json.getAsJsonObject("model").getAsJsonObject("cmm").addProperty("texture", otherTexture);
        Animation retextured = UniversalAnimLoader.loadAnimations(json).values().iterator().next();

        Assertions.assertNotEquals(model, retextured, "Another texture should make the models unequal");
        Assertions.assertNotEquals(model.get(), retextured.get(), "Another texture should give another uuid");
    }

    public static final UUID ANIMATION_UUID = UUID.fromString("0003a26e-3bf7-b554-0007-698deefeb9da");

    public static Animation loadAnimation() throws IOException {
        return loadAnimation("/bye-bye-bye.json");
    }

    public static Animation loadAnimation(String name) throws IOException {
        try (InputStream is = EmoteDataHashingTest.class.getResourceAsStream(name)) {
            return UniversalAnimLoader.loadAnimations(is).values().iterator().next();
        }
    }
}
