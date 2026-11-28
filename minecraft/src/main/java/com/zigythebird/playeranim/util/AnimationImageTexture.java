package com.zigythebird.playeranim.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.zigythebird.playeranimcore.animation.AnimationImage;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.function.Supplier;

/**
 * A texture uploaded from the memory of an animation image, a bone texture or an icon.
 * The memory stays the image's: closing the texture frees only the texture on the GPU.
 */
public class AnimationImageTexture extends AbstractTexture {
    public AnimationImageTexture(Supplier<String> label, AnimationImage image) {
        RenderSystem.assertOnRenderThread();
        GpuDevice device = RenderSystem.getDevice();
        this.texture = device.createTexture(label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, GpuFormat.RGBA8_UNORM, image.width(), image.height(), 1, 1);
        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        this.textureView = device.createTextureView(this.texture);
        device.createCommandEncoder().writeToTexture(this.texture, image.pixels(), 0, 0, 0, 0, image.width(), image.height());
    }
}
