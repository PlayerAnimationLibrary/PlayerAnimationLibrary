package com.zigythebird.playeranimtests.framework;

import com.zigythebird.playeranimcore.animation.AnimationImage;
import org.redlance.smalt.image.RgbaPicture;
import org.redlance.smalt.image.jxl.JxlEncoder;
import org.redlance.smalt.image.jxl.JxlEncoderOptions;
import org.redlance.smalt.image.png.PngEncoder;

/**
 * Test images given as packed ARGB pixels, row after row.
 */
public final class TestImages {
    private TestImages() {
    }

    public static RgbaPicture picture(int width, int height, int... argb) {
        byte[] rgba = new byte[width * height * 4];
        for (int i = 0; i < argb.length; i++) {
            rgba[i * 4] = (byte) (argb[i] >> 16);
            rgba[i * 4 + 1] = (byte) (argb[i] >> 8);
            rgba[i * 4 + 2] = (byte) argb[i];
            rgba[i * 4 + 3] = (byte) (argb[i] >>> 24);
        }
        return RgbaPicture.of(width, height, width * 4, rgba, true);
    }

    public static byte[] png(int width, int height, int... argb) {
        return PngEncoder.encode(picture(width, height, argb));
    }

    public static byte[] jxl(int width, int height, int... argb) {
        return JxlEncoder.encode(picture(width, height, argb), JxlEncoderOptions.lossless());
    }

    public static byte[] jxlInContainer(int width, int height, int... argb) {
        return JxlEncoder.encode(picture(width, height, argb), JxlEncoderOptions.lossless().toBuilder().container(true).build());
    }

    public static AnimationImage image(int width, int height, int... argb) {
        return AnimationImage.decode(png(width, height, argb), Long.MAX_VALUE);
    }
}
