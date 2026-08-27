package dev.notasher.voxycompat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineBoundaryContractTest {
    @Test
    void refreshRequiresACompletedDestroyAndCannotRunPerFrame() throws IOException {
        final Path root = Path.of(System.getProperty("user.dir"));
        final String source = Files.readString(root.resolve(
                "src/main/java/dev/notasher/voxycompat/mixin/OculusPipelineManagerMixin.java"));
        final String mixins = Files.readString(root.resolve(
                "src/main/resources/voxy-compatibility-patch.mixins.json"));

        assertTrue(mixins.contains("OculusPipelineManagerMixin"));
        assertTrue(source.contains("method = \"destroyPipeline\""));
        assertTrue(source.contains("at = @At(\"TAIL\")"));
        assertTrue(source.contains("method = \"preparePipeline\""));
        assertTrue(source.contains("at = @At(\"RETURN\")"));
        assertTrue(source.contains("voxyCompat$refreshPending = true"));
        assertTrue(source.contains("voxyCompat$refreshPending = false"));
        assertTrue(source.contains("minecraft.levelRenderer.allChanged()"));
        assertFalse(source.contains("renderLevel"));
        assertFalse(source.contains("ci.cancel()"));
    }
}
