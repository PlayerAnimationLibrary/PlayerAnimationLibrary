package com.zigythebird.playeranimcore.benchmark;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Measures the three paths that cost real time in the library: parsing an animation file,
 * playing one from trigger to natural end, and the per-render-frame bone transform lookup
 * the Minecraft module performs for every animated player.
 * <p>
 * Run with {@code ./gradlew :core:jmh}; results land in {@code core/build/results/jmh/results.json}.
 * Iteration counts live in {@code core/build.gradle} so a local run and a CI run share one configuration.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class AnimationBenchmark {
    private static final String RESOURCE_PATH = "/assets/player_animation_library/player_animations/";

    /**
     * Render frames per game tick: the library is driven at 60 FPS against a 20 TPS tick rate.
     */
    private static final int FRAMES_PER_TICK = 3;

    /**
     * The animation under test, loaded once per trial. Named {@code file} or {@code file#animation}
     * for files that hold more than one.
     */
    @State(Scope.Benchmark)
    public static class Source {
        /**
         * Both supported formats across a spread of sizes and features, so a change that only
         * affects one of them still shows up somewhere.
         */
        @Param({
                "wave",                             // geckolib, 20 ticks, a single bone
                "running",                          // kosmx v3, 13 ticks, looping
                "molang_tests#molang_test2",        // a MoLang expression re-evaluated every frame
                "test_more_complex_emote_bezier",   // geckolib, bezier interpolation, full skeleton
                "club_penguin_dance"                // kosmx v3, 152 ticks, the largest shipped animation
        })
        public String animation;

        /**
         * The raw file, so {@link AnimationBenchmark#parse} measures parsing rather than disk I/O.
         */
        public byte[] json;
        public Animation loaded;

        /**
         * Render frames one playthrough is worth, fixed up front so {@link AnimationBenchmark#playThrough}
         * measures the same amount of work on both sides of a comparison.
         */
        public int frames;

        @Setup
        public void load() throws IOException {
            int separator = this.animation.indexOf('#');
            String file = separator < 0 ? this.animation : this.animation.substring(0, separator);

            try (InputStream stream = AnimationBenchmark.class.getResourceAsStream(RESOURCE_PATH + file + ".json")) {
                this.json = Objects.requireNonNull(stream, "No such benchmark animation: " + file).readAllBytes();
            }

            Map<String, Animation> animations = UniversalAnimLoader.loadAnimations(new ByteArrayInputStream(this.json));
            if (separator < 0 && animations.size() > 1) {
                // The loader returns a hash map, so "the first one" isn't a stable choice.
                throw new IllegalStateException(file + " holds " + animations.size() + " animations, name one as " + file + "#<animation>");
            }
            this.loaded = separator < 0 ? animations.values().iterator().next() : animations.get(this.animation.substring(separator + 1));

            Objects.requireNonNull(this.loaded, "No such animation in " + file + ": " + this.animation);
            if (this.loaded.length() >= Integer.MAX_VALUE) {
                // AnimationLoader.calculateAnimationLength returns Float.MAX_VALUE for animations whose
                // keyframes aren't distributed over time - there is no playthrough to measure.
                throw new IllegalStateException("Benchmark animation never ends: " + this.animation);
            }
            this.frames = (int) Math.ceil(this.loaded.length()) * FRAMES_PER_TICK;
        }
    }

    /**
     * A controller mid-playback, restarted every iteration so a single frame can be measured
     * without paying for controller construction on every invocation.
     */
    @State(Scope.Thread)
    public static class Frame {
        public BenchmarkController controller;
        public List<PlayerAnimBone> bones;
        public final AnimationData data = new AnimationData(0, 0, false);
        public int frame;

        /**
         * Looped, so an iteration longer than the animation keeps measuring playback
         * instead of a stopped controller.
         */
        @Setup(Level.Iteration)
        public void start(Source source) {
            this.controller = new BenchmarkController();
            this.controller.triggerAnimation(RawAnimation.begin().thenLoop(source.loaded));
            this.bones = this.controller.newBoneSet();
            this.frame = 0;
        }
    }

    /**
     * Deserializing an animation file, as {@code PlayerAnimResources} does for every animation
     * on every resource reload. This is the whole file - the {@code #animation} half of the
     * parameter selects nothing here, since files are parsed as a unit.
     */
    @Benchmark
    public Map<String, Animation> parse(Source source) throws IOException {
        return UniversalAnimLoader.loadAnimations(new ByteArrayInputStream(source.json));
    }

    /**
     * A single render frame: tick the controller when a game tick is due, then pull the transform
     * for every registered bone - the exact sequence {@code AvatarAnimManager#handleAnimations}
     * drives, where a full tick is followed by, not swapped for, the frame's own setup.
     */
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void renderFrame(Frame frame, Blackhole blackhole) {
        if (frame.frame == 0) frame.controller.tick(frame.data);

        frame.data.setPartialTick((float) frame.frame / FRAMES_PER_TICK);
        frame.frame = (frame.frame + 1) % FRAMES_PER_TICK;

        frame.controller.setupAnim(frame.data);

        for (PlayerAnimBone bone : frame.bones) {
            bone.setToInitialPose();
            frame.controller.get3DTransform(bone);
            blackhole.consume(bone);
        }
    }

    /**
     * Triggering an animation and running it to its natural end. Unlike {@link #renderFrame} this
     * includes controller construction - which builds a MoLang engine - and the one-off bone setup
     * a newly started animation goes through.
     */
    @Benchmark
    public void playThrough(Source source, Blackhole blackhole) {
        BenchmarkController controller = new BenchmarkController();
        // PLAY_ONCE rather than thenPlay: the latter defers to the loop type in the animation
        // json, which would leave the shipped looping animations running forever.
        controller.triggerAnimation(RawAnimation.begin().then(source.loaded, Animation.LoopType.PLAY_ONCE));

        List<PlayerAnimBone> bones = controller.newBoneSet();
        AnimationData data = new AnimationData(0, 0, false);

        // A fixed frame count rather than looping until isActive() goes false: the score is essentially
        // frames × per-frame cost, so a change to when playback ends would otherwise show up as a speed
        // difference. Deriving it from the animation keeps the work per op tied to the input alone.
        for (int frame = 0; frame < source.frames; frame++) {
            if (frame % FRAMES_PER_TICK == 0) controller.tick(data);

            data.setPartialTick((float) (frame % FRAMES_PER_TICK) / FRAMES_PER_TICK);
            controller.setupAnim(data);

            for (PlayerAnimBone bone : bones) {
                bone.setToInitialPose();
                controller.get3DTransform(bone);
                blackhole.consume(bone);
            }
        }
    }
}
