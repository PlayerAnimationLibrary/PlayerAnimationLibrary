package com.zigythebird.playeranimcore.animation.layered;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Player animation stack, can contain multiple active or passive layers, will always be evaluated from the lowest index.
 * Highest index = it can override everything else
 */
public class AnimationStack implements IAnimation {
    protected final List<Pair<Integer, IAnimation>> layers = new ArrayList<>();

    public List<Pair<Integer, IAnimation>> getLayers() {
        return this.layers;
    }

    @Override
    public boolean isActive() {
        for (Pair<Integer, IAnimation> layer : layers) {
            if (layer.right().isActive()) return true;
        }
        return false;
    }

    @Override
    public void tick(AnimationData state) {
        for (Pair<Integer, IAnimation> layer : layers) {
            layer.right().tick(state);
        }
    }

    @Override
    public void get3DTransform(@NotNull PlayerAnimBone bone) {
        for (Pair<Integer, IAnimation> layer : layers) {
            if (layer.right().isActive() /*
            Not sure if this is necessary, hard to implement rn
            && (!FirstPersonMode.isFirstPersonPass() || layer.right().getFirstPersonMode().isEnabled())
            */) {
                layer.right().get3DTransform(bone);
            }
        }
    }

    @Override
    public void setupAnim(AnimationData state) {
        for (Pair<Integer, IAnimation> layer : layers) {
            if (layer.right().isActive())
                layer.right().setupAnim(state);
        }
    }

    /**
     * Add an animation layer.
     * If there are multiple layers with the same priority, the one added first will have higher priority
     * @param priority priority
     * @param layer    animation layer
     */
    public void addAnimLayer(int priority, IAnimation layer) {
        int search = 0;
        //Insert the layer into the correct slot
        while (layers.size() > search && layers.get(search).left() < priority) {
            search++;
        }
        layers.add(search, Pair.of(priority, layer));
    }

    /**
     * Remove an animation layer
     * @param layer needle
     * @return true if any elements were removed.
     */
    public boolean removeLayer(IAnimation layer) {
        return layers.removeIf(integerIAnimationPair -> integerIAnimationPair.right() == layer);
    }

    /**
     * Remove EVERY layer with priority
     * @param layerLevel search and destroy
     * @return true if any elements were removed.
     */
    public boolean removeLayer(int layerLevel) {
        return layers.removeIf(integerIAnimationPair -> integerIAnimationPair.left() == layerLevel);
    }

    @Override
    public @NotNull FirstPersonMode getFirstPersonMode() {
        for (int i = layers.size(); i > 0;) {
            Pair<Integer, IAnimation> layer = layers.get(--i);
            if (layer.right().isActive()) { // layer.right.requestFirstPersonMode(tickDelta).takeIf{ it != NONE }?.let{ return@requestFirstPersonMode it }
                FirstPersonMode mode = layer.right().getFirstPersonMode();
                if (mode != FirstPersonMode.NONE) return mode;
            }
        }
        return FirstPersonMode.NONE;
    }

    @Override
    public @NotNull FirstPersonConfiguration getFirstPersonConfiguration() {
        for (int i = layers.size(); i > 0;) {
            Pair<Integer, IAnimation> layer = layers.get(--i);
            if (layer.right().isActive()) { // layer.right.requestFirstPersonMode(tickDelta).takeIf{ it != NONE }?.let{ return@requestFirstPersonMode it }
                FirstPersonMode mode = layer.right().getFirstPersonMode();
                if (mode != FirstPersonMode.NONE) return layer.right().getFirstPersonConfiguration();
            }
        }
        return IAnimation.super.getFirstPersonConfiguration();
    }

    public int getPriority() {
        int priority = 0;
        for (int i=layers.size()-1; i>=0; i--) {
            Pair<Integer, IAnimation> layer = layers.get(i);
            if (layer.right().isActive()) {
                priority = layer.left();
                break;
            }
        }
        return priority;
    }

    @Override
    public String toString() {
        return "AnimationStack{" +
                "layers=" + layers +
                '}';
    }
}
