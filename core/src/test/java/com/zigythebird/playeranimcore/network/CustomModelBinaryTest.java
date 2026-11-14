package com.zigythebird.playeranimcore.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimcore.math.Vec3f;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redlance.platformtools.webp.decoder.DecodedImage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class CustomModelBinaryTest {

    private static JsonArray vec3(float x, float y, float z) {
        JsonArray arr = new JsonArray(3);
        arr.add(x);
        arr.add(y);
        arr.add(z);
        return arr;
    }

    private static JsonArray uv(float minU, float minV, float maxU, float maxV) {
        JsonArray arr = new JsonArray(4);
        arr.add(minU);
        arr.add(minV);
        arr.add(maxU);
        arr.add(maxV);
        return arr;
    }

    private static JsonObject makeFace(String texture, String cullface, float[] uvs, int tintIndex, int rotation) {
        JsonObject face = new JsonObject();
        if (texture != null) face.addProperty("texture", texture);
        if (cullface != null) face.addProperty("cullface", cullface);
        if (uvs != null) face.add("uv", uv(uvs[0], uvs[1], uvs[2], uvs[3]));
        if (tintIndex != -1) face.addProperty("tintindex", tintIndex);
        if (rotation != 0) face.addProperty("rotation", rotation);
        return face;
    }

    private static void assertRoundTrip(CustomModelBone bone, int version) {
        ByteBuf buf = Unpooled.buffer();
        NetworkUtils.writeCustomBone(buf, bone, version);
        CustomModelBone read = NetworkUtils.readCustomBone(buf, version);

        Assertions.assertEquals(bone.pivot(), read.pivot(), "pivot mismatch");
        if (bone.texture() != null && read.texture() != null) {
            Assertions.assertArrayEquals(bone.texture().argb(), read.texture().argb(), "texture mismatch");
        }

        if (bone.elements() == null) {
            Assertions.assertNull(read.elements(), "expected null elements");
        } else {
            Assertions.assertNotNull(read.elements(), "expected non-null elements");
            Assertions.assertEquals(bone.elements().size(), read.elements().size(), "elements count mismatch");
            for (int i = 0; i < bone.elements().size(); i++) {
                Assertions.assertEquals(
                        bone.elements().get(i),
                        read.elements().get(i),
                        "element " + i + " mismatch"
                );
            }
        }

        Assertions.assertFalse(buf.isReadable(), "buffer has leftover bytes");
        buf.release();
    }

    // ── Tests ──

    @Test
    @DisplayName("Bone with only pivot (version < 7)")
    void pivotOnlyOldVersion() {
        CustomModelBone bone = new CustomModelBone(new Vec3f(1, 2, 3), null, null);
        assertRoundTrip(bone, 6);
    }

    @Test
    @DisplayName("Bone with only pivot (version 7)")
    void pivotOnly() {
        CustomModelBone bone = new CustomModelBone(new Vec3f(1, 2, 3), null, null);
        assertRoundTrip(bone, 7);
    }

    @Test
    @DisplayName("Bone with texture bytes")
    void boneWithTexture() {
        DecodedImage tex = DecodedImage.ofArgb(new int[] {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF}, 2, 2);
        CustomModelBone bone = new CustomModelBone(new Vec3f(0, 0, 0), tex, null);
        assertRoundTrip(bone, 7);
    }

    @Test
    @DisplayName("Full element: from, to, 6 faces with all fields")
    void fullElement() {
        JsonObject element = new JsonObject();
        element.add("from", vec3(0, 0, 0));
        element.add("to", vec3(16, 16, 16));

        JsonObject faces = new JsonObject();
        String[] dirs = {"down", "up", "north", "south", "west", "east"};
        for (String dir : dirs) {
            faces.add(dir, makeFace("#all", dir, new float[]{0, 0, 16, 16}, -1, 0));
        }
        element.add("faces", faces);

        JsonArray elements = new JsonArray();
        elements.add(element);

        CustomModelBone bone = new CustomModelBone(new Vec3f(8, 8, 8), null, elements);
        assertRoundTrip(bone, 7);
    }

    @Test
    @DisplayName("Element with shade=false and light_emission")
    void shadeAndLightEmission() {
        JsonObject element = new JsonObject();
        element.add("from", vec3(0, 0, 0));
        element.add("to", vec3(8, 8, 8));
        element.addProperty("shade", false);
        element.addProperty("light_emission", 15);

        JsonObject faces = new JsonObject();
        faces.add("up", makeFace("#top", null, null, -1, 0));
        element.add("faces", faces);

        JsonArray elements = new JsonArray();
        elements.add(element);

        CustomModelBone bone = new CustomModelBone(new Vec3f(0, 0, 0), null, elements);
        assertRoundTrip(bone, 7);
    }

    @Test
    @DisplayName("Face with tintindex, rotation, UV")
    void faceAllFields() {
        JsonObject element = new JsonObject();
        element.add("from", vec3(0, 0, 0));
        element.add("to", vec3(16, 16, 16));

        JsonObject faces = new JsonObject();
        faces.add("north", makeFace("#side", "north", new float[]{0, 0, 16, 16}, 0, 270));
        element.add("faces", faces);

        JsonArray elements = new JsonArray();
        elements.add(element);

        CustomModelBone bone = new CustomModelBone(new Vec3f(8, 8, 8), null, elements);
        assertRoundTrip(bone, 7);
    }

    @Test
    @DisplayName("Face without texture (minimal)")
    void faceNoTexture() {
        JsonObject element = new JsonObject();
        element.add("from", vec3(0, 0, 0));
        element.add("to", vec3(16, 16, 16));

        JsonObject faces = new JsonObject();
        JsonObject face = new JsonObject();
        face.add("uv", uv(0, 0, 16, 16));
        faces.add("up", face);
        element.add("faces", faces);

        JsonArray elements = new JsonArray();
        elements.add(element);

        CustomModelBone bone = new CustomModelBone(new Vec3f(0, 0, 0), null, elements);
        assertRoundTrip(bone, 7);
    }

    @Test
    @DisplayName("Single-axis rotation with rescale")
    void singleAxisRotation() {
        JsonObject element = new JsonObject();
        element.add("from", vec3(0, 0, 0));
        element.add("to", vec3(16, 16, 16));

        JsonObject rot = new JsonObject();
        rot.add("origin", vec3(8, 8, 8));
        rot.addProperty("axis", "y");
        rot.addProperty("angle", 22.5f);
        rot.addProperty("rescale", true);
        element.add("rotation", rot);

        JsonObject faces = new JsonObject();
        faces.add("north", makeFace("#side", null, null, -1, 0));
        element.add("faces", faces);

        JsonArray elements = new JsonArray();
        elements.add(element);

        CustomModelBone bone = new CustomModelBone(new Vec3f(8, 8, 8), null, elements);
        assertRoundTrip(bone, 7);
    }

    @Test
    @DisplayName("Euler XYZ rotation")
    void eulerRotation() {
        JsonObject element = new JsonObject();
        element.add("from", vec3(0, 0, 0));
        element.add("to", vec3(16, 16, 16));

        JsonObject rot = new JsonObject();
        rot.add("origin", vec3(8, 8, 8));
        rot.addProperty("x", 45f);
        rot.addProperty("y", 0f);
        rot.addProperty("z", -30f);
        element.add("rotation", rot);

        JsonObject faces = new JsonObject();
        faces.add("up", makeFace("#top", null, null, -1, 0));
        element.add("faces", faces);

        JsonArray elements = new JsonArray();
        elements.add(element);

        CustomModelBone bone = new CustomModelBone(new Vec3f(0, 0, 0), null, elements);
        assertRoundTrip(bone, 7);
    }

    @Test
    @DisplayName("Element without from/to (minimal)")
    void elementMinimal() {
        JsonObject element = new JsonObject();
        // no from, no to, no faces, no rotation

        JsonArray elements = new JsonArray();
        elements.add(element);

        CustomModelBone bone = new CustomModelBone(new Vec3f(0, 0, 0), null, elements);
        assertRoundTrip(bone, 7);
    }

    @Test
    @DisplayName("Multiple elements")
    void multipleElements() {
        JsonArray elements = new JsonArray();

        for (int i = 0; i < 3; i++) {
            JsonObject element = new JsonObject();
            element.add("from", vec3(i, i, i));
            element.add("to", vec3(i + 8, i + 8, i + 8));

            JsonObject faces = new JsonObject();
            faces.add("north", makeFace("#tex" + i, "north", new float[]{0, 0, 16, 16}, -1, 0));
            element.add("faces", faces);

            elements.add(element);
        }

        CustomModelBone bone = new CustomModelBone(new Vec3f(8, 8, 8), DecodedImage.ofArgb(new int[]{0xFFAA0000, 0xFF00BB00, 0xFF0000CC, 0xFFDDDDDD}, 2, 2), elements);
        assertRoundTrip(bone, 7);
    }

    @Test
    @DisplayName("All directions for cullface")
    void allCullfaceDirections() {
        String[] dirs = {"down", "up", "north", "south", "west", "east"};

        for (String dir : dirs) {
            JsonObject element = new JsonObject();
            element.add("from", vec3(0, 0, 0));
            element.add("to", vec3(16, 16, 16));

            JsonObject faces = new JsonObject();
            faces.add(dir, makeFace("#tex", dir, null, -1, 0));
            element.add("faces", faces);

            JsonArray elements = new JsonArray();
            elements.add(element);

            CustomModelBone bone = new CustomModelBone(new Vec3f(0, 0, 0), null, elements);
            assertRoundTrip(bone, 7);
        }
    }

    @Test
    @DisplayName("All axis for single-axis rotation")
    void allAxes() {
        String[] axes = {"x", "y", "z"};

        for (String axis : axes) {
            JsonObject element = new JsonObject();
            element.add("from", vec3(0, 0, 0));
            element.add("to", vec3(16, 16, 16));

            JsonObject rot = new JsonObject();
            rot.add("origin", vec3(8, 8, 8));
            rot.addProperty("axis", axis);
            rot.addProperty("angle", 45f);
            element.add("rotation", rot);

            JsonObject faces = new JsonObject();
            faces.add("up", makeFace("#top", null, null, -1, 0));
            element.add("faces", faces);

            JsonArray elements = new JsonArray();
            elements.add(element);

            CustomModelBone bone = new CustomModelBone(new Vec3f(0, 0, 0), null, elements);
            assertRoundTrip(bone, 7);
        }
    }

    @Test
    @DisplayName("Real parsed bone from cmm_bone_test.json")
    void realParsedBone() throws IOException {
        try (InputStream is = CustomModelBinaryTest.class.getResourceAsStream("/cmm_bone_test.json")) {
            Assertions.assertNotNull(is, "cmm_bone_test.json not found");
            Map<String, Animation> animations = UniversalAnimLoader.loadAnimations(is);

            Assertions.assertFalse(animations.isEmpty(), "no animations loaded");

            Animation animation = animations.values().iterator().next();
            Assertions.assertFalse(animation.bones().isEmpty(), "no bones in animation");

            for (Map.Entry<String, CustomModelBone> entry : animation.bones().entrySet()) {
                CustomModelBone bone = entry.getValue();

                Assertions.assertNotNull(bone.pivot(), "pivot is null for bone " + entry.getKey());
                Assertions.assertNotNull(bone.texture(), "texture is null for bone " + entry.getKey());
                Assertions.assertNotNull(bone.elements(), "elements is null for bone " + entry.getKey());
                Assertions.assertFalse(bone.elements().isEmpty(), "elements empty for bone " + entry.getKey());

                assertRoundTrip(bone, 7);
            }
        }
    }

    @Test
    @DisplayName("Buffer fully consumed after read")
    void bufferFullyConsumed() {
        JsonObject element = new JsonObject();
        element.add("from", vec3(1, 2, 3));
        element.add("to", vec3(4, 5, 6));
        element.addProperty("shade", false);
        element.addProperty("light_emission", 7);

        JsonObject rot = new JsonObject();
        rot.add("origin", vec3(0, 0, 0));
        rot.addProperty("x", 10f);
        rot.addProperty("y", 20f);
        rot.addProperty("z", 30f);
        rot.addProperty("rescale", true);
        element.add("rotation", rot);

        JsonObject faces = new JsonObject();
        faces.add("down", makeFace("#bottom", "down", new float[]{0, 0, 16, 16}, 2, 90));
        faces.add("up", makeFace("#top", "up", new float[]{0, 0, 16, 16}, 0, 180));
        faces.add("north", makeFace("#side", "north", new float[]{0, 0, 16, 16}, -1, 0));
        element.add("faces", faces);

        JsonArray elements = new JsonArray();
        elements.add(element);

        CustomModelBone bone = new CustomModelBone(new Vec3f(4, 5, 6), DecodedImage.ofArgb(new int[]{0xFF112233, 0xFF445566, 0xFF778899, 0xFFAABBCC}, 2, 2), elements);
        assertRoundTrip(bone, 7);
    }
}
