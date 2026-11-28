package com.zigythebird.playeranimtests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.AnimationStack;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimtests.framework.TestAnimationController;
import com.zigythebird.playeranimtests.framework.TestImages;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * The head turns by 20 degrees and every custom bone by 15 unless said otherwise, all about y, so the turns of a chain add up.
 */
public class CustomBonePoseTest {
    private static final String PIVOT = "{\"pivot\": [0, 28, 0]}";

    @Test
    @DisplayName("A bone without a model takes its vanilla parent's pose")
    public void pivotUnderVanilla() {
        TestAnimationController controller = play("{\"p\": %1$s}", "{\"p\": \"head\"}");
        assertYaw(controller, "p", 35);
    }

    @Test
    @DisplayName("A drawn bone leaves its vanilla parent's pose to the part")
    public void drawnUnderVanilla() {
        TestAnimationController controller = play("{\"d\": %2$s}", "{\"d\": \"head\"}");
        assertYaw(controller, "d", 15);
    }

    @Test
    @DisplayName("A drawn bone under a bone without a model takes that bone as it is posed itself")
    public void drawnUnderPivot() {
        TestAnimationController controller = play("{\"p\": %1$s, \"d\": %2$s}", "{\"p\": \"head\", \"d\": \"p\"}");
        assertYaw(controller, "p", 35);
        assertYaw(controller, "d", 30);
    }

    @Test
    @DisplayName("A bone without a model under a drawn bone takes the vanilla bone the drawn one leaves out")
    public void pivotUnderDrawn() {
        TestAnimationController controller = play("{\"t\": %2$s, \"c\": %1$s}", "{\"t\": \"head\", \"c\": \"t\"}");
        assertYaw(controller, "t", 15);
        assertYaw(controller, "c", 50);
    }

    @Test
    @DisplayName("A drawn bone under both kinds takes every custom bone up to the vanilla one")
    public void drawnUnderPivotUnderDrawn() {
        TestAnimationController controller = play("{\"t\": %2$s, \"c\": %1$s, \"d\": %2$s}", "{\"t\": \"head\", \"c\": \"t\", \"d\": \"c\"}");
        assertYaw(controller, "c", 50);
        assertYaw(controller, "d", 45);
    }

    @Test
    @DisplayName("Drawn bones of the same name in two layers are each posed by their own controller")
    public void sameNameInTwoLayers() {
        TestAnimationController lower = play("{\"d\": %2$s}", "{}", 15);
        TestAnimationController upper = play("{\"d\": %2$s}", "{}", 30);
        AnimationStack stack = new AnimationStack();
        stack.addAnimLayer(0, lower);
        stack.addAnimLayer(1, upper);

        Map<IAnimation, Float> yaws = new HashMap<>();
        stack.collectModels((owner, bone) -> yaws.put(owner, owner.get3DTransform(bone.getName()).rotation.y));
        Assertions.assertEquals(Math.toRadians(15), yaws.get(lower), 1e-4, "the lower layer's bone should keep its own pose");
        Assertions.assertEquals(Math.toRadians(30), yaws.get(upper), 1e-4, "the upper layer's bone should keep its own pose");
    }

    private static TestAnimationController play(String model, String parents) {
        return play(model, parents, 15);
    }

    /**
     * %1$s in the model is a bone with a pivot only, %2$s a drawn one.
     */
    private static TestAnimationController play(String model, String parents, float customTurn) {
        JsonObject modelJson = JsonParser.parseString(model.formatted(PIVOT, drawn())).getAsJsonObject();
        JsonObject bones = new JsonObject();
        bones.add("head", turn(20));
        for (String name : modelJson.keySet()) bones.add(name, turn(customTurn));

        JsonObject animation = new JsonObject();
        animation.addProperty("loop", true);
        animation.add("bones", bones);
        JsonObject animations = new JsonObject();
        animations.add("parents_test", animation);
        JsonObject json = new JsonObject();
        json.add("model", modelJson);
        json.add("parents", JsonParser.parseString(parents));
        json.add("animations", animations);
        Animation loaded = UniversalAnimLoader.loadAnimations(json).get("parents_test");

        TestAnimationController controller = TestAnimationController.looping(loaded);
        controller.setupAnim(new AnimationData(0, 0, false));
        return controller;
    }

    private static String drawn() {
        String texture = Base64.getEncoder().encodeToString(TestImages.png(1, 1, 0xFFFFFFFF));
        return "{\"pivot\": [0, 28, 0], \"texture\": \"" + texture + "\", \"elements\": [{\"from\": [0, 0, 0], \"to\": [1, 1, 1], \"faces\": {\"up\": {\"texture\": \"#texture\"}}}]}";
    }

    private static JsonObject turn(float degrees) {
        JsonArray rotation = new JsonArray();
        rotation.add(0);
        rotation.add(degrees);
        rotation.add(0);
        JsonObject keyframes = new JsonObject();
        keyframes.add("0.0", rotation);
        JsonObject bone = new JsonObject();
        bone.add("rotation", keyframes);
        return bone;
    }

    private static void assertYaw(TestAnimationController controller, String bone, float degrees) {
        Assertions.assertEquals(Math.toRadians(degrees), controller.get3DTransform(bone).rotation.y, 1e-4, bone + " should be turned by " + degrees + " degrees");
    }
}
