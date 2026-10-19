package com.zigythebird.playeranim.animation.cmm;

import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.model.sprite.MaterialBaker;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class CustomMaterialBaker extends MaterialBaker {
    public CustomMaterialBaker() {
        SpriteLoader.Preparations dummyAtlas = new SpriteLoader.Preparations(0, 0, 0, null, Collections.emptyMap(), CompletableFuture.completedFuture(null));
        super(dummyAtlas, dummyAtlas);
    }
}
