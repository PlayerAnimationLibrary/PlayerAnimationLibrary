package com.zigythebird.playeranimcore.bones;

import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.math.Vec3f;

import java.io.Closeable;

public class CustomBone extends PlayerAnimBone implements Closeable {
    private final CustomModelBone data;

    public CustomBone(String name, CustomModelBone data) {
        super(name);
        this.data = data;
    }

    public Vec3f getPivot() {
        return this.data.pivot();
    }

    public CustomModelBone getData() {
        return this.data;
    }

    @Override
    public void close() {
        // no-op
    }
}
