package dev.notasher.voxycompat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SablePlotIsolationContractTest {
    @Test
    void isolationUsesSableOwnedBoundaryAtVoxyIngestOnly() throws IOException {
        final Path root = Path.of(System.getProperty("user.dir"));
        final String boundary = Files.readString(root.resolve(
                "src/main/java/dev/notasher/voxycompat/SablePlotBoundary.java"));
        final String mixin = Files.readString(root.resolve(
                "src/main/java/dev/notasher/voxycompat/mixin/VoxyIngestServiceMixin.java"));

        assertTrue(boundary.contains("SubLevelContainer"));
        assertTrue(boundary.contains("getMethod(\"inBounds\", int.class, int.class)"));
        assertFalse(boundary.contains("import dev.ryanhcode.sable"));
        assertFalse(boundary.contains("20_480_000"));
        assertFalse(boundary.contains("20_000_000"));

        assertTrue(mixin.contains("VoxelIngestService"));
        assertTrue(mixin.contains("method = \"enqueueIngest\""));
        assertTrue(mixin.contains("method = \"rawIngest0("));
        assertTrue(mixin.contains("me/cortex/voxy/common/world/WorldEngine"));
        assertFalse(mixin.contains("me/cortex/voxy/commonImpl/WorldIdentifier"));
        assertTrue(mixin.contains("cir.setReturnValue(false)"));
        assertFalse(mixin.contains("RenderSystem"));
        assertFalse(mixin.contains("drawChunkLayer"));
        assertFalse(mixin.contains("glDepth"));
        assertFalse(mixin.contains("GRASS_BLOCK"));
        assertFalse(mixin.contains("LIGHT-PROBE"));
        assertFalse(mixin.contains("SkyFullbrightDiagnostic"));
        assertFalse(mixin.contains("blockLight.get"));
        assertFalse(mixin.contains("skyLight.get"));
    }

}
