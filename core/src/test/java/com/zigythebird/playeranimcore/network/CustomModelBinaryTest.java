package com.zigythebird.playeranimcore.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationImage;
import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimtests.framework.TestImages;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redlance.mocha.parser.util.network.ProtocolUtils;
import org.redlance.mocha.parser.util.network.VarIntUtils;
import org.redlance.smalt.MediaException;
import org.redlance.smalt.image.jxl.JxlAnimationEncoder;
import org.redlance.smalt.image.jxl.JxlDecoder;
import org.redlance.smalt.image.jxl.JxlEncoder;
import org.redlance.smalt.image.jxl.JxlEncoderOptions;
import org.redlance.smalt.io.MediaSource;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
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
        CustomModelBone read = NetworkUtils.readCustomBone(buf, version, NetworkUtils.MAX_TEXTURE_PIXELS);

        Assertions.assertEquals(bone.pivot(), read.pivot(), "pivot mismatch");
        assertSameTexture(bone.texture(), read.texture());

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

    private static void assertSameTexture(AnimationImage expected, AnimationImage actual) {
        Assertions.assertEquals(expected != null, actual != null, "texture presence mismatch");
        if (expected != null) {
            Assertions.assertEquals(expected.width(), actual.width(), "texture width mismatch");
            Assertions.assertEquals(expected.height(), actual.height(), "texture height mismatch");
            Assertions.assertEquals(expected.pixels(), actual.pixels(), "texture mismatch");
        }
    }

    private static JsonArray singleElement(JsonObject element) {
        JsonArray elements = new JsonArray();
        elements.add(element);
        return elements;
    }

    private static JsonObject cube() {
        JsonObject element = new JsonObject();
        element.add("from", vec3(0, 0, 0));
        element.add("to", vec3(16, 16, 16));
        JsonObject faces = new JsonObject();
        faces.add("up", makeFace("#top", null, null, -1, 0));
        element.add("faces", faces);
        return element;
    }

    private static void assertWriterRejects(JsonObject element) {
        CustomModelBone bone = new CustomModelBone(new Vec3f(0, 0, 0), null, singleElement(element));
        ByteBuf buf = Unpooled.buffer();
        Assertions.assertThrows(JsonParseException.class, () -> NetworkUtils.writeCustomBone(buf, bone, 7));
        buf.release();
    }

    private static String base64Png(int width, int height, int... argb) {
        return Base64.getEncoder().encodeToString(TestImages.png(width, height, argb));
    }

    /**
     * A bone of only a texture, as a packet carries it.
     */
    private static ByteBuf receivedTexture(byte[] file, boolean jxl) {
        ByteBuf buf = Unpooled.buffer();
        NetworkUtils.writeVec3f(buf, new Vec3f(0, 0, 0));
        VarIntUtils.writeVarInt(buf, jxl ? 3 : 1);
        VarIntUtils.writeVarInt(buf, file.length);
        buf.writeBytes(file);
        return buf;
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
        AnimationImage tex = TestImages.image(2, 2, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF);
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
    @DisplayName("Element with shade_direction_override and light_emission")
    void shadeAndLightEmission() {
        JsonObject element = new JsonObject();
        element.add("from", vec3(0, 0, 0));
        element.add("to", vec3(8, 8, 8));
        element.addProperty("shade_direction_override", "up");
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

        CustomModelBone bone = new CustomModelBone(new Vec3f(8, 8, 8), TestImages.image(2, 2, 0xFFAA0000, 0xFF00BB00, 0xFF0000CC, 0xFFDDDDDD), elements);
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
        element.addProperty("shade_direction_override", "west");
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

        CustomModelBone bone = new CustomModelBone(new Vec3f(4, 5, 6), TestImages.image(2, 2, 0xFF112233, 0xFF445566, 0xFF778899, 0xFFAABBCC), elements);
        assertRoundTrip(bone, 7);
    }

    @Test
    @DisplayName("A rotation axis is carried the way Minecraft reads it")
    void rotationAxisName() {
        JsonObject element = cube();
        JsonObject rotation = new JsonObject();
        rotation.add("origin", vec3(8, 8, 8));
        rotation.addProperty("axis", "Y");
        rotation.addProperty("angle", 45f);
        element.add("rotation", rotation);

        ByteBuf buf = Unpooled.buffer();
        NetworkUtils.writeCustomBone(buf, new CustomModelBone(new Vec3f(0, 0, 0), null, singleElement(element)), 7);
        JsonObject read = NetworkUtils.readCustomBone(buf, 7, NetworkUtils.MAX_TEXTURE_PIXELS).elements().get(0).getAsJsonObject();
        Assertions.assertEquals("y", read.getAsJsonObject("rotation").get("axis").getAsString(), "Minecraft lower-cases the axis");
        buf.release();

        rotation.addProperty("axis", "w");
        assertWriterRejects(element);
    }

    @Test
    @DisplayName("An empty face texture comes back as an empty string")
    void emptyFaceTexture() {
        JsonObject element = cube();
        element.getAsJsonObject("faces").add("down", makeFace("", null, null, -1, 0));
        assertRoundTrip(new CustomModelBone(new Vec3f(0, 0, 0), null, singleElement(element)), 7);
    }

    @Test
    @DisplayName("The writer rejects what it cannot carry the way Minecraft reads it")
    void writerRejects() {
        JsonObject lightEmission = cube();
        lightEmission.addProperty("light_emission", 16);
        assertWriterRejects(lightEmission);

        JsonObject shadeDirection = cube();
        shadeDirection.addProperty("shade_direction_override", "sideways");
        assertWriterRejects(shadeDirection);

        JsonObject rotationWithoutValue = cube();
        JsonObject rotation = new JsonObject();
        rotation.add("origin", vec3(8, 8, 8));
        rotationWithoutValue.add("rotation", rotation);
        assertWriterRejects(rotationWithoutValue);

        JsonObject unknownFacing = cube();
        unknownFacing.getAsJsonObject("faces").add("front", makeFace("#top", null, null, -1, 0));
        assertWriterRejects(unknownFacing);

        JsonObject shortUv = cube();
        JsonArray uv = new JsonArray();
        uv.add(0);
        uv.add(0);
        uv.add(16);
        shortUv.getAsJsonObject("faces").getAsJsonObject("up").add("uv", uv);
        assertWriterRejects(shortUv);

        JsonObject stringCoordinate = cube();
        JsonArray from = new JsonArray();
        from.add("0");
        from.add(0);
        from.add(0);
        stringCoordinate.add("from", from);
        assertWriterRejects(stringCoordinate);
    }

    @Test
    @DisplayName("A count larger than the rest of the packet is refused before anything is allocated for it")
    void craftedCounts() {
        ByteBuf texture = Unpooled.buffer();
        NetworkUtils.writeVec3f(texture, new Vec3f(0, 0, 0));
        VarIntUtils.writeVarInt(texture, 1); // a PNG texture
        VarIntUtils.writeVarInt(texture, Integer.MAX_VALUE);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> NetworkUtils.readCustomBone(texture, 7, NetworkUtils.MAX_TEXTURE_PIXELS));
        texture.release();

        ByteBuf keyframes = Unpooled.buffer();
        VarIntUtils.writeVarInt(keyframes, Integer.MAX_VALUE);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> AnimationBinary.readKeyframeList(keyframes, false, false));
        keyframes.release();
    }

    @Test
    @DisplayName("Textures past the pixels an animation may hold are refused")
    void textureBudget() {
        ByteBuf small = Unpooled.buffer();
        NetworkUtils.writeCustomBone(small, new CustomModelBone(new Vec3f(0, 0, 0), TestImages.image(2, 2, new int[4]), null), 7);
        Assertions.assertThrows(MediaException.LimitExceeded.class, () -> NetworkUtils.readCustomBone(small, 7, 3));
        small.release();

        byte[] large = TestImages.png(64, 64, new int[64 * 64]);
        byte[] largeJxl = TestImages.jxl(64, 64, new int[64 * 64]);
        for (ByteBuf received : new ByteBuf[]{receivedTexture(large, false), receivedTexture(largeJxl, true)}) {
            Assertions.assertThrows(MediaException.LimitExceeded.class, () -> NetworkUtils.readCustomBone(received, 7, 64 * 64 - 1));
            received.release();
        }

        Map<String, CustomModelBone> bones = new LinkedHashMap<>();
        AnimationImage half = TestImages.image(2048, 1024, new int[2048 * 1024]);
        bones.put("first", new CustomModelBone(new Vec3f(0, 0, 0), half, null));
        bones.put("second", new CustomModelBone(new Vec3f(0, 0, 0), half, null));
        ByteBuf full = Unpooled.buffer();
        NetworkUtils.writeMap(full, bones, ProtocolUtils::writeString, (byteBuf, bone) -> NetworkUtils.writeCustomBone(byteBuf, bone, 7));
        Assertions.assertEquals(2, NetworkUtils.readCustomBones(full, 7).size(), "textures that fill the budget exactly are read");
        full.release();

        bones.put("third", new CustomModelBone(new Vec3f(0, 0, 0), TestImages.image(1, 1, 0), null));
        ByteBuf over = Unpooled.buffer();
        NetworkUtils.writeMap(over, bones, ProtocolUtils::writeString, (byteBuf, bone) -> NetworkUtils.writeCustomBone(byteBuf, bone, 7));
        Assertions.assertThrows(MediaException.LimitExceeded.class, () -> NetworkUtils.readCustomBones(over, 7));
        over.release();
    }

    @Test
    @DisplayName("A model is refused at load when its textures hold more pixels than an animation can be sent with")
    void loadTextureBudget() {
        JsonObject bone = new JsonObject();
        bone.add("pivot", vec3(0, 0, 0));
        bone.addProperty("texture", base64Png(2049, 2048, new int[2049 * 2048]));
        bone.add("elements", singleElement(cube()));
        JsonObject model = new JsonObject();
        model.add("prop", bone);
        JsonObject json = new JsonObject();
        json.add("model", model);
        json.add("animations", new JsonObject());

        Assertions.assertThrows(JsonParseException.class, () -> UniversalAnimLoader.loadAnimations(json));
    }

    @Test
    @DisplayName("A loaded model equals and hashes as its copy read from the network")
    void loadedModelEqualsReceived() {
        // Every element value here is one the codec carries in another form or drops
        String json = """
                {
                  "model": {
                    "prop": {
                      "pivot": [0, 12, 0],
                      "texture": "%s",
                      "elements": [{
                        "name": "cube",
                        "from": [0, 0, 0],
                        "to": [8, 8, 8],
                        "shade": false,
                        "neoforge_data": {"color": -1},
                        "rotation": {"origin": [4, 4, 4], "axis": "Y", "angle": 22.5},
                        "faces": {
                          "north": {"texture": "", "uv": [0.1, 0, 8, 8], "tintindex": 1},
                          "up": {"texture": "#0"}
                        }
                      }]
                    }
                  },
                  "animations": {"model_test": {"loop": true}}
                }
                """.formatted(base64Png(2, 2, 0xFF112233, 0x00445566, 0x80778899, 0xFFAABBCC));
        Animation loaded = UniversalAnimLoader.loadAnimations(JsonParser.parseString(json).getAsJsonObject()).get("model_test");

        ByteBuf buf = Unpooled.buffer();
        AnimationBinary.write(buf, loaded);
        Animation received = AnimationBinary.read(buf);
        buf.release();

        CustomModelBone loadedBone = loaded.bones().get("prop");
        CustomModelBone receivedBone = received.bones().get("prop");
        Assertions.assertEquals(loadedBone, receivedBone, "the bone should equal its copy");
        Assertions.assertEquals(loadedBone.hashCode(), receivedBone.hashCode(), "the bone should hash as its copy");
        assertSameTexture(loadedBone.texture(), receivedBone.texture());
    }

    @Test
    @DisplayName("A received texture is forwarded as the file it came in, PNG or JPEG XL")
    void forwardsReceivedFile() {
        int[] pixels = {0xFF112233, 0x00445566, 0x80778899, 0xFFAABBCC};
        byte[] png = TestImages.png(2, 2, pixels);
        byte[] jxl = TestImages.jxl(2, 2, pixels);
        for (ByteBuf received : new ByteBuf[]{receivedTexture(png, false), receivedTexture(jxl, true)}) {
            ByteBuf packet = received.copy();
            CustomModelBone bone = NetworkUtils.readCustomBone(received, 7, NetworkUtils.MAX_TEXTURE_PIXELS);
            received.release();

            ByteBuf forwarded = Unpooled.buffer();
            NetworkUtils.writeCustomBone(forwarded, bone, 7);
            Assertions.assertEquals(packet, forwarded, "the bone should be forwarded byte for byte");
            packet.release();
            forwarded.release();
        }
    }

    @Test
    @DisplayName("A received texture that holds more than its image is refused, since it would be forwarded as it came")
    void refusesReceivedExtras() {
        int[] pixels = {0xFF112233, 0x00445566, 0x80778899, 0xFFAABBCC};
        byte[] png = TestImages.png(2, 2, pixels);
        byte[] jxl = TestImages.jxl(2, 2, pixels);
        byte[] boxed = TestImages.jxlInContainer(2, 2, pixels);
        ByteBuf[] packets = {receivedTexture(Arrays.copyOf(png, png.length + 1), false),
                receivedTexture(Arrays.copyOf(jxl, jxl.length + 1), true), receivedTexture(boxed, true)};
        for (ByteBuf received : packets) {
            Assertions.assertThrows(MediaException.Unsupported.class, () -> NetworkUtils.readCustomBone(received, 7, NetworkUtils.MAX_TEXTURE_PIXELS));
            received.release();
        }
    }

    @Test
    @DisplayName("A received JPEG XL texture coded otherwise than the mod codes its own is refused, though it loads from a file")
    void refusesReceivedOtherCoding() {
        int[] pixels = new int[32 * 32];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0xFF000000 | (i * 7 & 0xFF) << 16 | (i / 32 * 8 & 0xFF) << 8 | (i % 32 * 8 & 0xFF);
        }
        // A lossy file is a bare codestream of one frame, so only its coding sets it apart
        byte[] lossy = JxlEncoder.encode(TestImages.picture(32, 32, pixels), JxlEncoderOptions.ofDistance(1.0f));
        ByteBuf received = receivedTexture(lossy, true);
        Assertions.assertThrows(MediaException.Unsupported.class, () -> NetworkUtils.readCustomBone(received, 7, NetworkUtils.MAX_TEXTURE_PIXELS));
        received.release();

        AnimationImage loaded = AnimationImage.decode(lossy, Long.MAX_VALUE);
        ByteBuf buf = Unpooled.buffer();
        NetworkUtils.writeCustomBone(buf, new CustomModelBone(new Vec3f(0, 0, 0), loaded, null), 7);
        assertSameTexture(loaded, NetworkUtils.readCustomBone(buf, 7, NetworkUtils.MAX_TEXTURE_PIXELS).texture());
        buf.release();
    }

    @Test
    @DisplayName("A received JPEG XL texture is refused from its first header, without its other frame headers being read")
    void refusesReceivedBeforeFrames() {
        JxlAnimationEncoder animation = new JxlAnimationEncoder(2, 2, JxlEncoderOptions.lossless());
        animation.addFrame(TestImages.picture(2, 2, 0xFF112233, 0xFF445566, 0xFF778899, 0xFFAABBCC), Duration.ofMillis(100));
        animation.addFrame(TestImages.picture(2, 2, 0xFFCCBBAA, 0xFF998877, 0xFF665544, 0xFF332211), Duration.ofMillis(100));
        byte[] file = animation.finish();

        // The longest start of the file whose frame headers cannot be read: it ends inside the header of the second frame
        int cut = file.length - 1;
        while (cut > 0 && !isMalformed(Arrays.copyOf(file, cut))) {
            cut--;
        }
        Assertions.assertTrue(cut > 0, "no cut ends inside the second frame header");
        ByteBuf received = receivedTexture(Arrays.copyOf(file, cut), true);
        Assertions.assertThrows(MediaException.Unsupported.class, () -> NetworkUtils.readCustomBone(received, 7, NetworkUtils.MAX_TEXTURE_PIXELS));
        received.release();
    }

    private static boolean isMalformed(byte[] file) {
        try (MediaSource source = MediaSource.of(file)) {
            JxlDecoder.probe(source);
            return false;
        } catch (MediaException.Malformed e) {
            return true;
        }
    }

    @Test
    @DisplayName("A texture loaded from a file, PNG or JPEG XL, is sent as JPEG XL encoded once from its pixels")
    void sendsLoadedTextureAsJxl() {
        int[] pixels = {0xFF112233, 0x00445566, 0x80778899, 0xFFAABBCC};
        byte[] boxed = TestImages.jxlInContainer(2, 2, pixels);
        for (AnimationImage texture : new AnimationImage[]{TestImages.image(2, 2, pixels), AnimationImage.decode(boxed, Long.MAX_VALUE)}) {
            Assertions.assertTrue(texture.network().jxl());
            Assertions.assertSame(texture.network().bytes(), texture.network().bytes(), "encoded once");
            Assertions.assertNotSame(texture.file().bytes(), texture.network().bytes(), "encoded from the pixels");

            ByteBuf buf = Unpooled.buffer();
            NetworkUtils.writeCustomBone(buf, new CustomModelBone(new Vec3f(0, 0, 0), texture, null), 7);
            CustomModelBone read = NetworkUtils.readCustomBone(buf, 7, NetworkUtils.MAX_TEXTURE_PIXELS);
            Assertions.assertTrue(read.texture().file().jxl());
            assertSameTexture(texture, read.texture());
            buf.release();
        }
    }
}
