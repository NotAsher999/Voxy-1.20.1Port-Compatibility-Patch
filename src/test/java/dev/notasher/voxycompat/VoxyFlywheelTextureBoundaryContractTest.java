package dev.notasher.voxycompat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxyFlywheelTextureBoundaryContractTest {
    @Test
    void restoresOnlyFlywheelsMissingDiffuseAtlasAtTheVoxyBoundary() throws IOException {
        final Path root = Path.of(System.getProperty("user.dir"));
        final String source = Files.readString(root.resolve(
                "src/main/java/dev/notasher/voxycompat/mixin/VoxyRenderSystemTextureBoundaryMixin.java"));
        final String mixins = Files.readString(root.resolve(
                "src/main/resources/voxy-compatibility-patch.mixins.json"));

        assertTrue(mixins.contains("VoxyRenderSystemTextureBoundaryMixin"));
        assertTrue(source.contains("me.cortex.voxy.client.core.VoxyRenderSystem"));
        assertTrue(source.contains("renderOpaque(Lme/cortex/voxy/client/core/rendering/Viewport;)V"));
        assertTrue(source.contains("ModList.get().isLoaded(\"flywheel\")"));
        assertTrue(source.contains("TextureAtlas.LOCATION_BLOCKS"));
        assertTrue(source.contains("RenderSystem.setShaderTexture(0, blockAtlas)"));
        assertTrue(source.contains("RenderSystem.bindTexture(blockAtlas)"));
        assertTrue(source.contains("GlStateManager._activeTexture(previousActiveTexture)"));
        assertFalse(source.contains("Matrix4"));
        assertFalse(source.contains("glBindTextureUnit"));
        assertFalse(source.contains("dev.ryanhcode.sable"));
    }
}
