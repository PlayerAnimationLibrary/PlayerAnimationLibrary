package com.zigythebird.playeranimcore.bones;

import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.math.Vec3f;

public abstract class CustomBone extends PlayerAnimBone implements AutoCloseable {
    private final CustomModelBone data;

    protected CustomBone(String name, CustomModelBone data) {
        super(name);
        this.data = data;
    }

    public Vec3f getPivot() {
        return this.data.pivot();
    }

    public CustomModelBone getData() {
        return this.data;
    }

    public boolean hasModelData() {
        return this.data.texture() != null && this.data.elements() != null && !this.data.elements().isEmpty();
    }

    @Override
    public abstract void close();
}
