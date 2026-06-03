package com.zigythebird.playeranimtests.framework;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Streams every {@link Animation} on the test classpath under {@value #RESOURCE_PATH}
 * as a single-argument row. Plug into a parameterized test with
 * {@code @ArgumentsSource(AnimationsProvider.class)} and a {@code void foo(Animation a)}
 * signature; the display name for each animation is its own {@link Animation#getNameOrId()}.
 * <p>
 * The Minecraft module's resources are pulled into this module's test sourceSet by
 * {@code core/build.gradle}, so the animation JSONs are ordinary classpath entries.
 */
public final class AnimationsProvider implements ArgumentsProvider {
    public static final String RESOURCE_PATH = "/assets/player_animation_library/player_animations";

    @Override
    public @NonNull Stream<? extends Arguments> provideArguments(@NonNull ParameterDeclarations parameters, @NonNull ExtensionContext context) throws IOException, URISyntaxException {
        URL url = Objects.requireNonNull(AnimationsProvider.class.getResource(RESOURCE_PATH), "Animations not on test classpath: " + RESOURCE_PATH);

        List<Path> files;
        try (Stream<Path> walk = Files.list(Paths.get(url.toURI()))) {
            files = walk.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
        }
        return files.stream().flatMap(AnimationsProvider::loadEntries);
    }

    private static Stream<Arguments> loadEntries(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            return UniversalAnimLoader.loadAnimations(is).values().stream().map(Arguments::of);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + file, e);
        }
    }
}
