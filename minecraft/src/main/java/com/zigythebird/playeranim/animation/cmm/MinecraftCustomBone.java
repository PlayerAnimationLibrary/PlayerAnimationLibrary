package com.zigythebird.playeranim.animation.cmm;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.bones.CustomBone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class MinecraftCustomBone extends CustomBone implements ModelBaker, ModelBaker.Interner, ModelDebugName, BlockStateModelPart, Closeable {
    private static final AtomicInteger TEXTURE_COUNTER = new AtomicInteger(0);

    private final TextureAtlasSprite sprite;
    private final RenderType renderType;
    private final QuadCollection geometry;

    public MinecraftCustomBone(String name, CustomModelBone data) throws IOException {
        super(name, data);

        this.sprite = loadAtlasSprite(Objects.requireNonNull(data.texture()).toPng());
        this.renderType = RenderTypes.entityTranslucent(this.sprite.atlasLocation());

        List<CuboidModelElement> blockElements = new ArrayList<>();
        for (JsonElement element : Objects.requireNonNull(data.elements())) {
            blockElements.add(CuboidModel.GSON.fromJson(element, CuboidModelElement.class));
        }
        this.geometry = UnbakedCuboidGeometry.bake(blockElements, TextureSlots.EMPTY, this, BlockModelRotation.IDENTITY, this);
    }

    public @NonNull RenderType getRenderType() {
        return this.renderType;
    }

    public @NonNull TextureAtlasSprite getSprite() {
        return this.sprite;
    }

    @Override
    public void close() {
        if (this.sprite != null) {
            this.sprite.close();
        }
    }

    @Override
    public @NonNull String debugName() {
        if (this.sprite == null) return PlayerAnimLibMod.MOD_ID + ":custom_bone";
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

    private static TextureAtlasSprite loadAtlasSprite(byte[] texture) throws IOException {
        RenderSystem.assertOnRenderThread();
        NativeImage image = NativeImage.read(texture);

        Identifier texId = PlayerAnimLibMod.id("dynamic/texture_" + TEXTURE_COUNTER.getAndIncrement());
        DynamicTexture dynTex = new DynamicTexture(texId::toString, image);
        Minecraft.getInstance().getTextureManager().register(texId, dynTex);

        int w = image.getWidth();
        int h = image.getHeight();
        SpriteContents contents = new SpriteContents(texId, new FrameSize(w, h), image);
        return new TextureAtlasSprite(texId, contents, w, h, 0, 0, 0);
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
}
