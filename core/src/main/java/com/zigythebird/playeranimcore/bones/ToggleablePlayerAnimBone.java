package com.zigythebird.playeranimcore.bones;

public class ToggleablePlayerAnimBone extends PlayerAnimBone {
    public boolean scaleXEnabled = true;
    public boolean scaleYEnabled = true;
    public boolean scaleZEnabled = true;

    public boolean positionXEnabled = true;
    public boolean positionYEnabled = true;
    public boolean positionZEnabled = true;

    public boolean rotXEnabled = true;
    public boolean rotYEnabled = true;
    public boolean rotZEnabled = true;

    public boolean bendEnabled = true;

    public ToggleablePlayerAnimBone(String name) {
        super(name);
    }

    public ToggleablePlayerAnimBone(PlayerAnimBone bone) {
        super(bone);

        if (bone instanceof ToggleablePlayerAnimBone boneEnabled) {
            scaleXEnabled = boneEnabled.isScaleXEnabled();
            scaleYEnabled = boneEnabled.isScaleYEnabled();
            scaleZEnabled = boneEnabled.isScaleZEnabled();

            positionXEnabled = boneEnabled.isPositionXEnabled();
            positionYEnabled = boneEnabled.isPositionYEnabled();
            positionZEnabled = boneEnabled.isPositionZEnabled();

            rotXEnabled = boneEnabled.isRotXEnabled();
            rotYEnabled = boneEnabled.isRotYEnabled();
            rotZEnabled = boneEnabled.isRotZEnabled();

            bendEnabled = boneEnabled.isBendEnabled();
        }
    }

    public void setPositionEnabled(boolean enabled) {
        this.positionXEnabled = enabled;
        this.positionYEnabled = enabled;
        this.positionZEnabled = enabled;
    }

    public void setRotEnabled(boolean enabled) {
        this.rotXEnabled = enabled;
        this.rotYEnabled = enabled;
        this.rotZEnabled = enabled;
    }

    public void setScaleEnabled(boolean enabled) {
        this.scaleXEnabled = enabled;
        this.scaleYEnabled = enabled;
        this.scaleZEnabled = enabled;
    }

    public boolean isScaleXEnabled() {
        return scaleXEnabled;
    }

    public boolean isScaleYEnabled() {
        return scaleYEnabled;
    }

    public boolean isScaleZEnabled() {
        return scaleZEnabled;
    }

    public boolean isPositionXEnabled() {
        return positionXEnabled;
    }

    public boolean isPositionYEnabled() {
        return positionYEnabled;
    }

    public boolean isPositionZEnabled() {
        return positionZEnabled;
    }

    public boolean isRotXEnabled() {
        return rotXEnabled;
    }

    public boolean isRotYEnabled() {
        return rotYEnabled;
    }

    public boolean isRotZEnabled() {
        return rotZEnabled;
    }

    public boolean isBendEnabled() {
        return bendEnabled;
    }
}
