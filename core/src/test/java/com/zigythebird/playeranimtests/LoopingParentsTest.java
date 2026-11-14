package com.zigythebird.playeranimtests;

import com.google.gson.JsonParser;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimtests.framework.TestAnimationController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Looping parents arrive from files and the network as they are. a, b and c have a pivot only.
 */
public class LoopingParentsTest {

    @DisplayName("Looping parents play")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"a\": \"b\", \"b\": \"a\"}",
            "{\"a\": \"a\"}",
            "{\"c\": \"a\", \"a\": \"b\", \"b\": \"a\"}",
            "{\"head\": \"torso\", \"torso\": \"head\"}",
            "{\"head\": \"head\"}",
            "{\"a\": \"head\", \"head\": \"a\"}"
    })
    public void loopingParents(String parents) {
        Animation animation = UniversalAnimLoader.loadAnimations(JsonParser.parseString("""
                {
                  "model": {"a": {"pivot": [0, 24, 0]}, "b": {"pivot": [0, 12, 0]}, "c": {"pivot": [0, 0, 0]}},
                  "parents": %s,
                  "animations": {"looping_parents": {
                    "loop": true,
                    "bones": {
                      "a": {"rotation": {"0.0": [0, 0, 0], "1.0": [30, 0, 0]}},
                      "head": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 45, 0]}}
                    }
                  }}
                }
                """.formatted(parents)).getAsJsonObject()).get("looping_parents");

        TestAnimationController controller = TestAnimationController.looping(animation);
        AnimationData data = new AnimationData(0, 0, false);
        Assertions.assertDoesNotThrow(() -> {
            for (int tick = 0; tick < 5; tick++) {
                controller.setupAnim(data);
                controller.tick(data);
            }
        });
    }
}
