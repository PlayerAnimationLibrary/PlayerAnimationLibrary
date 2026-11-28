package com.zigythebird.playeranimcore.animation;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;
import org.redlance.smalt.MediaException;
import org.redlance.smalt.MediaLimits;
import org.redlance.smalt.image.DecodedPixels;
import org.redlance.smalt.image.ImageDecoderOptions;
import org.redlance.smalt.image.ImageFormat;
import org.redlance.smalt.image.ImageInfo;
import org.redlance.smalt.image.PixelTarget;
import org.redlance.smalt.image.jxl.JxlDecoder;
import org.redlance.smalt.image.jxl.JxlEncoder;
import org.redlance.smalt.image.jxl.JxlEncoderOptions;
import org.redlance.smalt.image.png.PngDecoder;
import org.redlance.smalt.io.MediaSource;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

/**
 * An image of an animation, a bone texture or an icon, decoded once from a PNG or JPEG XL file.
 * The pixels live in direct memory as packed 8-bit RGBA, row after row, for as long as the image.
 */
public final class AnimationImage {
    private static final JxlEncoderOptions NETWORK_ENCODING = JxlEncoderOptions.lossless().toBuilder().effort(9).build();

    private final int width;
    private final int height;
    private final boolean alpha;
    private final ByteBuffer pixels;
    private final Encoded file;
    /**
     * Encoded on the first send for an image decoded from a file.
     */
    private volatile @Nullable Encoded network;

    /**
     * @param bytes the file; must not be modified
     * @param jxl whether it is JPEG XL rather than PNG
     */
    public record Encoded(byte[] bytes, boolean jxl) {
    }

    private AnimationImage(byte[] file, boolean jxl, long maxPixels, boolean received) {
        // A received file is one frame of the size of its image, so only a file that was not received needs jxlLimits
        ImageDecoderOptions options = ImageDecoderOptions.builder()
                .limits(jxl && !received ? jxlLimits(file, maxPixels) : limits(maxPixels).build())
                .exact(received)
                .boundedWork(received)
                .build();
        Allocation target = new Allocation();
        DecodedPixels decoded;
        try (MediaSource source = MediaSource.of(file)) {
            decoded = jxl ? JxlDecoder.decodeInto(source, options, target) : PngDecoder.decodeInto(source, options, target);
        }
        this.width = decoded.width();
        this.height = decoded.height();
        this.alpha = decoded.hasAlpha();
        this.pixels = target.pixels;
        this.file = new Encoded(file, jxl);
        this.network = received ? this.file : null;
    }

    private static MediaLimits.Builder limits(long maxPixels) {
        if (maxPixels < 1) {
            throw new MediaException.LimitExceeded("No pixels are left for this image");
        }
        return MediaLimits.builder().maxPixels(Math.min(maxPixels, MediaLimits.DEFAULT.maxPixels()));
    }

    /**
     * The limits of a JPEG XL file that was not received. A frame of such a file can be larger than its image, and
     * frames that are not displayed are decoded too: every frame is held to the size of the image, and their number
     * to what one displayed frame allows.
     */
    private static MediaLimits jxlLimits(byte[] file, long maxPixels) {
        ImageInfo info;
        try (MediaSource source = MediaSource.of(file)) {
            info = JxlDecoder.probe(source);
        }
        long pixels = (long) info.width() * info.height();
        if (pixels > maxPixels) {
            throw new MediaException.LimitExceeded("Image of " + info.width() + "x" + info.height() + " is over the " + maxPixels + " pixels allowed");
        }
        return limits(pixels).maxFrames(1).build();
    }

    /**
     * Reads an image that a packet carries in its next {@code length} bytes, decoding it on the calling thread.
     * The image keeps a copy of them, to be sent as they came, so they must hold nothing but the image, as
     * {@link ImageDecoderOptions#exact()} describes, and a JPEG XL image must be coded the way {@link #network()}
     * codes one, lossless, as {@link ImageDecoderOptions#boundedWork()} describes.
     *
     * @param maxPixels the most pixels the image may have
     * @throws MediaException.LimitExceeded if the image has more pixels
     * @throws MediaException.Unsupported if the file holds more than the image, or is coded otherwise
     * @throws MediaException if the file is not a valid image of its format
     */
    public static AnimationImage read(ByteBuf buf, int length, boolean jxl, long maxPixels) {
        byte[] file = new byte[length];
        buf.readBytes(file);
        return new AnimationImage(file, jxl, maxPixels, true);
    }

    /**
     * Decodes a PNG or JPEG XL file, told apart by its signature. The array is kept, not copied.
     *
     * @param maxPixels the most pixels the image may have
     * @throws IllegalArgumentException if the file is neither
     * @throws MediaException.LimitExceeded if the image has more pixels
     * @throws MediaException if the file is not a valid image of its format
     */
    public static AnimationImage decode(byte[] file, long maxPixels) {
        ImageFormat format = ImageFormat.detect(file, file.length).orElse(null);
        if (format != ImageFormat.PNG && format != ImageFormat.JXL) {
            throw new IllegalArgumentException("Not a PNG or JPEG XL image");
        }
        return new AnimationImage(file, format == ImageFormat.JXL, maxPixels, false);
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    /**
     * @return a read-only view of the pixels, valid while the image is reachable
     */
    public ByteBuffer pixels() {
        return this.pixels.asReadOnlyBuffer();
    }

    /**
     * @return the file the image was read from
     */
    public Encoded file() {
        return this.file;
    }

    /**
     * An image decoded from a file is sent as JPEG XL encoded from its pixels on the first call, whatever the file
     * held besides them; a received one is sent as it came.
     *
     * @return the file the image is sent as
     */
    public Encoded network() {
        Encoded network = this.network;
        if (network == null) {
            synchronized (this) {
                network = this.network;
                if (network == null) {
                    byte[] encoded = JxlEncoder.encode(MemorySegment.ofBuffer(this.pixels), this.width, this.height, this.alpha, NETWORK_ENCODING);
                    this.network = network = new Encoded(encoded, true);
                }
            }
        }
        return network;
    }

    /**
     * Images are equal when they have the same size and pixels, however they were read.
     */
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AnimationImage other && this.width == other.width && this.height == other.height
                && this.pixels.equals(other.pixels);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * this.width + this.height) + this.pixels.hashCode();
    }

    /**
     * Gives the decoder direct memory once the image size is known. The decoder's limits, at most
     * {@link MediaLimits#DEFAULT}'s pixels, keep its size within an int.
     */
    private static final class Allocation implements PixelTarget {
        private ByteBuffer pixels;

        @Override
        public MemorySegment pixels(int width, int height) {
            this.pixels = ByteBuffer.allocateDirect(width * height * 4);
            return MemorySegment.ofBuffer(this.pixels);
        }
    }
}
