package com.zigythebird.playeranimcore.animation.layered;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.easing.EasingType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;


/**
 * Layer to easily swap animations, add modifiers or do other sorts of effects.
 * The AnimationController class already has a way to add modifiers, so you probably don't need this.
 * Modifiers <b>affect</b> each other. For example, if you put a fade modifier after a speed modifier, it will be affected by the modifier.
 */
public class ModifierLayer<T extends IAnimation> implements IAnimation {
    protected final List<AbstractModifier> modifiers = new ArrayList<>();

    @Nullable
    T animation;

    public ModifierLayer(@Nullable T animation, AbstractModifier... modifiers) {
        this.animation = animation;
        Collections.addAll(this.modifiers, modifiers);
    }

    public ModifierLayer() {
        this(null);
    }

    public @Nullable T getAnimation() {
        return animation;
    }

    @Override
    public void tick(AnimationData state) {
        for (int i = 0; i < modifiers.size(); i++) {
            if (modifiers.get(i).canRemove()) {
                removeModifier(i--);
            }
        }
        if (modifiers.size() > 0) {
            modifiers.get(0).tick(state);
        } else if (animation != null) animation.tick(state);
    }

    public void addModifier(@NotNull AbstractModifier modifier, int idx) {
        modifier.setHost(this);
        modifiers.add(idx, modifier);
        linkModifiers();
    }

    public void addModifierBefore(@NotNull AbstractModifier modifier) {
        this.addModifier(modifier, 0);
    }

    public void addModifierLast(@NotNull AbstractModifier modifier) {
        this.addModifier(modifier, modifiers.size());
    }

    public void removeModifier(int idx) {
        modifiers.remove(idx);
        linkModifiers();
    }

    public void removeAllModifiers() {
        modifiers.clear();
    }

    public int getModifierCount() {
        return modifiers.size();
    }

    public @Nullable AbstractModifier getModifier(int idx) {
        try {
            return modifiers.get(idx);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public boolean removeModifierIf(Predicate<? super AbstractModifier> predicate) {
        return modifiers.removeIf(predicate);
    }

    public void setAnimation(@Nullable T animation) {
        this.animation = animation;
        this.linkModifiers();
    }

    /**
     * Fade out from current animation into new animation.
     * Does not fade if there is currently no active animation
     * @param fadeModifier Fade modifier, use {@link AbstractFadeModifier#standardFadeIn(int, EasingType)} for simple fade.
     * @param newAnimation New animation can be null to fade into default state.
     */
    public void replaceAnimationWithFade(@NotNull AbstractFadeModifier fadeModifier, @Nullable T newAnimation) {
        replaceAnimationWithFade(fadeModifier, newAnimation, true);
    }

    /**
     * Fade out from current to a new animation
     * @param fadeModifier    Fade modifier, use {@link AbstractFadeModifier#standardFadeIn(int, EasingType)} for simple fade.
     * @param newAnimation    New animation, can be null to fade into default state.
     * @param fadeFromNothing Do fade even if we go from nothing. (for KeyframeAnimation, it can be false by default)
     */
    public void replaceAnimationWithFade(@NotNull AbstractFadeModifier fadeModifier, @Nullable T newAnimation, boolean fadeFromNothing) {
        if (fadeFromNothing || getAnimation() != null && getAnimation().isActive()) {
            fadeModifier.setTransitionAnimation(this.getAnimation());
            addModifierLast(fadeModifier);
        }
        this.setAnimation(newAnimation);
    }

    public int size() {
        return modifiers.size();
    }

    protected void linkModifiers() {
        Iterator<AbstractModifier> modifierIterator = modifiers.iterator();
        if (modifierIterator.hasNext()) {
            AbstractModifier tmp = modifierIterator.next();
            while (modifierIterator.hasNext()) {
                AbstractModifier tmp2 = modifierIterator.next();
                tmp.setAnim(tmp2);
                tmp = tmp2;
            }
            tmp.setAnim(this.animation);
        }
    }


    @Override
    public boolean isActive() {
        if (!modifiers.isEmpty()) {
            return modifiers.get(0).isActive();
        } else if (animation != null) return animation.isActive();
        return false;
    }

    @Override
    public void get3DTransform(@NotNull PlayerAnimBone bone) {
        if (!modifiers.isEmpty()) {
            modifiers.getFirst().get3DTransform(bone);
        } else if (animation != null) {
            animation.get3DTransform(bone);
        }
    }

    @Override
    public void setupAnim(AnimationData state) {
        if (!modifiers.isEmpty()) {
            modifiers.get(0).setupAnim(state);
        } else if (animation != null) animation.setupAnim(state);
    }

    @Override
    public @NotNull FirstPersonMode getFirstPersonMode() {
        if (!modifiers.isEmpty()) {
            return modifiers.get(0).getFirstPersonMode();
        } else if (animation != null) return animation.getFirstPersonMode();
        return IAnimation.super.getFirstPersonMode();
    }

    @Override
    public @NotNull FirstPersonConfiguration getFirstPersonConfiguration() {
        if (!modifiers.isEmpty()) {
            return modifiers.get(0).getFirstPersonConfiguration();
        } else if (animation != null) return animation.getFirstPersonConfiguration();
        return IAnimation.super.getFirstPersonConfiguration();
    }

    @Override
    public String toString() {
        return "ModifierLayer{" +
                "modifiers=" + modifiers +
                ", animation=" + animation +
                '}';
    }
}
