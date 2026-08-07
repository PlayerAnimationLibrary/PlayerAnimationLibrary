package com.zigythebird.playeranimcore;

import com.zigythebird.playeranimcore.animation.*;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import io.github.kosmx.emotes.testing.common.EmoteDataHashingTest;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AnimationPerformanceTest {
    //Simulates the animation running at 60 FPS and measures the performance
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @BenchmarkMode(Mode.AverageTime)
    public void measureAnimationPerformance() throws IOException {
        AnimationController controller = new HumanoidAnimationController((_, _, _) -> PlayState.STOP, MolangLoader::createNewEngine);

        controller.triggerAnimation(RawAnimation.begin().then(EmoteDataHashingTest.loadAnimation(), Animation.LoopType.PLAY_ONCE));

        int framesSinceLastTick = 2;
        while (controller.isActive()) {
            if (framesSinceLastTick >= 2) {
                framesSinceLastTick = 0;
                controller.tick(new AnimationData(0, 0, false));
            }
            else {
                framesSinceLastTick += 1;
                controller.setupAnim(new AnimationData(0, (float) (0.4 * framesSinceLastTick + 0.1), false));
            }
        }
    }
}
