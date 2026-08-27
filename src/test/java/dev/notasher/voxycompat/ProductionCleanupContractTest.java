package dev.notasher.voxycompat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductionCleanupContractTest {
    @Test
    void productionSourcesContainNoBlackGrassDiagnostics() throws IOException {
        final Path root = Path.of(System.getProperty("user.dir"));
        final String entrypoint = Files.readString(root.resolve(
                "src/main/java/dev/notasher/voxycompat/VoxyCompatibilityPatch.java"));
        final String mixinConfig = Files.readString(root.resolve(
                "src/main/resources/voxy-compatibility-patch.mixins.json"));

        assertFalse(entrypoint.contains("RegisterClientCommandsEvent"));
        assertFalse(entrypoint.contains("Commands.literal"));
        assertFalse(entrypoint.contains("\"voxycompat\""));
        assertFalse(mixinConfig.contains("VoxySkyFullbrightDiagnosticMixin"));
        assertFalse(mixinConfig.contains("ModelRenderTypeProbe"));
    }
}
