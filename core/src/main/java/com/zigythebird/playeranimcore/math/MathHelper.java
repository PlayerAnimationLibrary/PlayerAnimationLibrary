package com.zigythebird.playeranimcore.math;

import org.redlance.mocha.runtime.standard.MochaMath;

/**
 * Some casts to {@link float} to make my life easier.
 */
public class MathHelper {
    public static float cos(float a) {
        return (float) Math.cos(a);
    }

    public static float cosFromSin(float sin, float angle) {
        float cos = MochaMath.sqrt(1.0F - sin * sin);
        float a = angle + (MochaMath.PI / 2F);
        float b = a - (float)((int)(a / (MochaMath.PI * 2F))) * (MochaMath.PI * 2F);
        if ((double)b < (double)0.0F) {
            b += (MochaMath.PI * 2F);
        }

        return b >= MochaMath.PI ? -cos : cos;
    }

    public static boolean absEqualsOne(float r) {
        return (Float.floatToRawIntBits(r) & Integer.MAX_VALUE) == 1065353216;
    }

    public static float safeAsin(float r) {
        return r <= -1.0F ? (-MochaMath.PI / 2F) : (float) (r >= 1.0F ? (MochaMath.PI / 2F) : Math.asin(r));
    }
    
    public static float length(float x, float y, float z, float w) {
        return MochaMath.sqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))));
    }
}
