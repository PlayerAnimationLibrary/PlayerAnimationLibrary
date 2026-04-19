package com.zigythebird.playeranimcore.bones;

public class AdvancedPlayerAnimBone extends ToggleablePlayerAnimBone {
    public Float scaleXTransitionLength = null;
    public Float scaleYTransitionLength = null;
    public Float scaleZTransitionLength = null;

    public Float positionXTransitionLength = null;
    public Float positionYTransitionLength = null;
    public Float positionZTransitionLength = null;

    public Float rotXTransitionLength = null;
    public Float rotYTransitionLength = null;
    public Float rotZTransitionLength = null;

    public Float bendTransitionLength = null;

    public AdvancedPlayerAnimBone(String name) {
        super(name);
    }

    public AdvancedPlayerAnimBone(PlayerAnimBone bone) {
        super(bone);
    }

    public void setEnabled(boolean enabled) {
        scaleXEnabled = enabled;
        scaleYEnabled = enabled;
        scaleZEnabled = enabled;

        positionXEnabled = enabled;
        positionYEnabled = enabled;
        positionZEnabled = enabled;

        rotXEnabled = enabled;
        rotYEnabled = enabled;
        rotZEnabled = enabled;

        bendEnabled = enabled;
    }

    public void setBendTransitionLength(Float bendTransitionLength) {
        this.bendTransitionLength = bendTransitionLength;
    }

    public void setRotZTransitionLength(Float rotZTransitionLength) {
        this.rotZTransitionLength = rotZTransitionLength;
    }

    public void setRotYTransitionLength(Float rotYTransitionLength) {
        this.rotYTransitionLength = rotYTransitionLength;
    }

    public void setRotXTransitionLength(Float rotXTransitionLength) {
        this.rotXTransitionLength = rotXTransitionLength;
    }

    public void setPositionZTransitionLength(Float positionZTransitionLength) {
        this.positionZTransitionLength = positionZTransitionLength;
    }

    public void setPositionYTransitionLength(Float positionYTransitionLength) {
        this.positionYTransitionLength = positionYTransitionLength;
    }

    public void setPositionXTransitionLength(Float positionXTransitionLength) {
        this.positionXTransitionLength = positionXTransitionLength;
    }

    public void setScaleZTransitionLength(Float scaleZTransitionLength) {
        this.scaleZTransitionLength = scaleZTransitionLength;
    }

    public void setScaleYTransitionLength(Float scaleYTransitionLength) {
        this.scaleYTransitionLength = scaleYTransitionLength;
    }

    public void setScaleXTransitionLength(Float scaleXTransitionLength) {
        this.scaleXTransitionLength = scaleXTransitionLength;
    }
}
