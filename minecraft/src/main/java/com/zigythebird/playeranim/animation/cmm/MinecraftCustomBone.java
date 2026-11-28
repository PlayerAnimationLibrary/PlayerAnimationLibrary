package com.zigythebird.playeranim.animation.cmm;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranim.util.AnimationImageTexture;
import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.animation.AnimationImage;
import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.bones.CustomBone;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.math.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.*;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.lang.ref.Cleaner;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class MinecraftCustomBone extends CustomBone implements ModelBaker, ModelBaker.Interner, ModelDebugName, BlockStateModelPart, JsonDeserializationContext, Closeable {
    private static final AtomicInteger TEXTURE_COUNTER = new AtomicInteger(0);
    /**
     * Render types are cached by texture name for the whole session, so names are reused.
     */
    private static final Queue<Identifier> RELEASED_TEXTURES = new ArrayDeque<>();
    /**
     * For bones dropped without close().
     */
    private static final Cleaner CLEANER = Cleaner.create();
    private static final CuboidModelElement.Deserializer ELEMENT_DESERIALIZER = new CuboidModelElement.Deserializer();
    private static final Map<String, Function<HumanoidModel<?>, ModelPart>> PARTS = Map.of(
            "body", HumanoidModel::root,
            "head", model -> model.head,
            "torso", model -> model.body,
            "right_arm", model -> model.rightArm,
            "left_arm", model -> model.leftArm,
            "right_leg", model -> model.rightLeg,
            "left_leg", model -> model.leftLeg
    );
    /**
     * The part each layer draws on.
     */
    private static final Map<String, String> LAYER_BONES = Map.of(
            "right_item", "right_arm",
            "left_item", "left_arm",
            "cape", "torso",
            "elytra", "torso"
    );

    private final TextureAtlasSprite sprite;
    private final Cleaner.Cleanable textureRelease;
    private final RenderType renderType;
    private final QuadCollection geometry;
    private final PlayerAnimBone pose;
    private final @Nullable Function<HumanoidModel<?>, ModelPart> basePart;
    private final @Nullable LayerBone layerBone;

    public MinecraftCustomBone(String name, CustomModelBone data, @Nullable String baseParent, Function<String, Vec3f> bonePositions) {
        super(name, data, baseParent);
        this.pose = new PlayerAnimBone(name);
        this.basePart = baseParent == null ? null : PARTS.get(LAYER_BONES.getOrDefault(baseParent, baseParent));
        // A part already moves by its bone's pose
        this.layerBone = baseParent == null || PARTS.containsKey(baseParent) ? null : new LayerBone(new PlayerAnimBone(baseParent), bonePositions.apply(baseParent));

        List<CuboidModelElement> blockElements = new ArrayList<>();
        for (JsonElement element : Objects.requireNonNull(data.elements())) {
            blockElements.add(ELEMENT_DESERIALIZER.deserialize(element, CuboidModelElement.class, this));
        }

        this.sprite = loadAtlasSprite(Objects.requireNonNull(data.texture()));
        this.textureRelease = CLEANER.register(this, releaseTexture(this.sprite.atlasLocation()));
        this.renderType = RenderTypes.entityTranslucent(this.sprite.atlasLocation());
        this.geometry = UnbakedCuboidGeometry.bake(blockElements, TextureSlots.EMPTY, this, BlockModelRotation.IDENTITY, this);
    }

    public @NonNull RenderType getRenderType() {
        return this.renderType;
    }

    public @NonNull TextureAtlasSprite getSprite() {
        return this.sprite;
    }

    /**
     * @param stack poses the layer the bone is drawn on (item, cape, elytra), as the layer draws it
     * @param owner poses the bone
     */
    public void translateToBone(PoseStack poseStack, HumanoidModel<?> model, IAnimation stack, IAnimation owner) {
        // As translateToHand
        ModelPart root = model.root();
        root.translateAndRotate(poseStack);
        if (this.basePart != null) {
            ModelPart part = this.basePart.apply(model);
            if (part != root) part.translateAndRotate(poseStack);
            // The elements are placed for the part at rest
            PartPose initialPose = part.getInitialPose();
            poseStack.translate(-initialPose.x() / 16, -initialPose.y() / 16, -initialPose.z() / 16);
        }

        poseStack.translate(0, -EntityModel.MODEL_Y_OFFSET, 0);
        poseStack.scale(-1.0F, -1.0F, -1.0F);

        if (this.layerBone != null) {
            Vec3f pivot = this.layerBone.pivot();
            // The z of the player's pivots is mirrored in this frame
            applyPose(poseStack, stack, this.layerBone.pose(), pivot.x(), pivot.y(), -pivot.z());
        }
        Vec3f pivot = getPivot();
        applyPose(poseStack, owner, this.pose, pivot.x(), pivot.y(), pivot.z());
        poseStack.translate(-0.5f, 0, -0.5f);
    }

    private static void applyPose(PoseStack poseStack, IAnimation animation, PlayerAnimBone bone, float pivotX, float pivotY, float pivotZ) {
        bone.setToInitialPose();
        animation.get3DTransform(bone);
        poseStack.translate((pivotX - bone.position.x()) / 16, (pivotY + bone.position.y()) / 16, (pivotZ - bone.position.z()) / 16);
        RenderUtil.applyBoneRotationScale(poseStack, bone);
        poseStack.translate(-pivotX / 16, -pivotY / 16, -pivotZ / 16);
    }

    @Override
    public void close() {
        this.textureRelease.clean();
    }

    @Override
    public <T> T deserialize(JsonElement json, Type typeOfT) throws JsonParseException {
        return CuboidModel.GSON.fromJson(json, typeOfT);
    }

    @Override
    public @NonNull String debugName() {
        return this.sprite.atlasLocation().toString();
    }

    @Override
    public @NonNull ResolvedModel getModel(@NonNull Identifier location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NonNull BlockStateModelPart missingBlockModelPart() {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("all")
    public @NonNull MaterialBaker materials() {
        return null;
    }

    @Override
    public @NonNull Interner interner() {
        return this;
    }

    @Override
    public @NonNull Vector3fc vector(@NonNull Vector3fc vector) {
        return vector;
    }

    @Override
    public BakedQuad.@NonNull MaterialInfo materialInfo(BakedQuad.@NonNull MaterialInfo material) {
        return material;
    }

    @Override
    public <T> @NonNull T compute(SharedOperationKey<T> key) {
        return key.compute(this);
    }

    private static TextureAtlasSprite loadAtlasSprite(AnimationImage texture) {
        RenderSystem.assertOnRenderThread();
        Identifier texId = Objects.requireNonNullElseGet(RELEASED_TEXTURES.poll(), () -> PlayerAnimLibMod.id("cmb/texture_" + TEXTURE_COUNTER.getAndIncrement()));
        Minecraft.getInstance().getTextureManager().register(texId, new AnimationImageTexture(texId::toString, texture));

        int w = texture.width();
        int h = texture.height();
        // Over the texture's own memory, which closing would free: it is never closed
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, false, MemoryUtil.memAddress(texture.pixels()));
        SpriteContents contents = new SpriteContents(texId, new FrameSize(w, h), image);
        return new TextureAtlasSprite(texId, contents, w, h, 0, 0, 0);
    }

    private static Runnable releaseTexture(Identifier texture) {
        return () -> Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().getTextureManager().release(texture);
            RELEASED_TEXTURES.add(texture);
        });
    }

    @Override
    public @NonNull List<BakedQuad> getQuads(@Nullable Direction direction) {
        return this.geometry.getQuads(direction);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    @SuppressWarnings("all")
    public Material.@NonNull Baked particleMaterial() {
        return null;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return this.geometry.materialFlags();
    }

    private record LayerBone(PlayerAnimBone pose, Vec3f pivot) {
    }
}
