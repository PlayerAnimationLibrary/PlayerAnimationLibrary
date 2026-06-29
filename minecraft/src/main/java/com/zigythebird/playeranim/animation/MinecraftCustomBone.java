package com.zigythebird.playeranim.animation;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranimcore.PlayerAnimLib;
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
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MinecraftCustomBone extends CustomBone implements MaterialBaker, ModelBaker, ModelBaker.Interner, ModelDebugName {
    private static final AtomicInteger TEXTURE_COUNTER = new AtomicInteger(0);

    @Nullable
    private TextureAtlasSprite sprite;
    @Nullable
    private RenderType renderType;
    @Nullable
    private QuadCollection geometry;

    public MinecraftCustomBone(String name, CustomModelBone data) {
        super(name, data);
        if (hasModelData()) {
            try {
                this.sprite = loadAtlasSprite(data.texture().toPng());
                this.renderType = RenderTypes.entityTranslucent(this.sprite.atlasLocation());

                List<CuboidModelElement> blockElements = new ArrayList<>();
                for (JsonElement element : data.elements()) {
                    blockElements.add(CuboidModel.GSON.fromJson(element, CuboidModelElement.class));
                }
                this.geometry = UnbakedCuboidGeometry.bake(blockElements, TextureSlots.EMPTY, this, BlockModelRotation.IDENTITY, this);
            } catch (Exception e) {
                PlayerAnimLib.LOGGER.error("Failed to load custom model for bone: {}", name, e);
            }
        }
    }

    public boolean hasModel() {
        return this.geometry != null;
    }

    public @Nullable QuadCollection getGeometry() {
        return this.geometry;
    }

    public @Nullable RenderType getRenderType() {
        return this.renderType;
    }

    @Override
    public void close() {
        if (this.sprite != null) {
            this.sprite.close();
            this.sprite = null;
        }
        this.renderType = null;
        this.geometry = null;
    }

    @Override
    public @NonNull String debugName() {
        if (this.sprite == null) return PlayerAnimLibMod.MOD_ID + ":custom_bone";
        return this.sprite.atlasLocation().toString();
    }

    @Override
    public Material.@NonNull Baked get(Material material, @NonNull ModelDebugName name) {
        return new Material.Baked(this.sprite, material.forceTranslucent());
    }

    @Override
    public Material.@NonNull Baked reportMissingReference(@NonNull String reference, @NonNull ModelDebugName name) {
        return new Material.Baked(this.sprite, false);
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
    public @NonNull MaterialBaker materials() {
        return this;
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
    public <T> T compute(SharedOperationKey<T> key) {
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
}
