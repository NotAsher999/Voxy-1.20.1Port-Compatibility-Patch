package dev.notasher.voxycompat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxyProgramBoundaryContractTest {
    @Test
    void restoresTheActualEntryProgramAtVoxysFinalUnbind() throws IOException {
        final Path root = Path.of(System.getProperty("user.dir"));
        final String source = Files.readString(root.resolve(
                "src/main/java/dev/notasher/voxycompat/mixin/VoxyRenderSystemTextureBoundaryMixin.java"));

        assertTrue(source.contains("at = @At(\"HEAD\")"));
        assertTrue(source.contains("GL20C.GL_CURRENT_PROGRAM"));
        assertTrue(source.contains("@Redirect"));
        assertTrue(source.contains("Lorg/lwjgl/opengl/GL30C;glUseProgram(I)V"));
        assertTrue(source.contains("GL30C.glUseProgram(this.voxyCompat$entryProgram)"));
        assertFalse(source.contains("GL30C.glUseProgram(0)"));
    }
}
