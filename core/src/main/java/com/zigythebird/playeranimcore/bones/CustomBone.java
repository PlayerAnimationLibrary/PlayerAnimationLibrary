package com.zigythebird.playeranimcore.bones;

import com.zigythebird.playeranimcore.animation.CustomModelBone;
import com.zigythebird.playeranimcore.math.Vec3f;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

public class CustomBone extends PlayerAnimBone implements Closeable {
    private final CustomModelBone data;
    private final @Nullable String baseParent;

    public CustomBone(String name, CustomModelBone data, @Nullable String baseParent) {
        super(name);
        this.data = data;
        this.baseParent = baseParent;
    }

    public Vec3f getPivot() {
        return this.data.pivot();
    }

    public CustomModelBone getData() {
        return this.data;
    }

    /**
     * The vanilla bone a drawn bone is drawn on, whose pose its own leaves out; null when the bone is not drawn or no vanilla bone is above it.
     */
    public @Nullable String getBaseParent() {
        return this.baseParent;
    }

    @Override
    public void close() {
        // no-op
    }
}
