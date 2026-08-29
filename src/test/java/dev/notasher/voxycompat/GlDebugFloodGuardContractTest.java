package dev.notasher.voxycompat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlDebugFloodGuardContractTest {
    @Test
    void limitsOnlyTheConfirmedMessageAndKeepsVisibleEvidence() throws IOException {
        final Path root = Path.of(System.getProperty("user.dir"));
        final String source = Files.readString(root.resolve(
                "src/main/java/dev/notasher/voxycompat/mixin/GlDebugFloodGuardMixin.java"));
        final String mixins = Files.readString(root.resolve(
                "src/main/resources/voxy-compatibility-patch.mixins.json"));

        assertTrue(mixins.contains("GlDebugFloodGuardMixin"));
        assertTrue(source.contains("GL_INVALID_OPERATION error generated. No active program."));
        assertTrue(source.contains("if (id != 1282)"));
        assertTrue(source.contains("VOXY_COMPAT$NO_ACTIVE_PROGRAM.equals(message)"));
        assertTrue(source.contains("voxyCompat$passedInWindow < VOXY_COMPAT$PASSTHROUGH_LIMIT"));
        assertTrue(source.contains("Suppressed {} repeated 'No active program'"));
        assertTrue(source.contains("ci.cancel()"));
        assertFalse(source.contains("Depth formats do not match"));
        assertFalse(source.contains("glDebugMessageControl"));
    }
}
