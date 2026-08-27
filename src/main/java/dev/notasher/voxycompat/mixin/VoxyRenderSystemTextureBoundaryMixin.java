package dev.notasher.voxycompat.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraftforge.fml.ModList;
import org.lwjgl.opengl.GL13C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores the block atlas dependency that Flywheel's instancing renderer
 * inherits from the surrounding level render pass.
 *
 * <p>Voxy 0.2.13 deliberately clears texture units 0 through 11 after its
 * opaque LOD pipeline. Flywheel rebinds overlay, light, and instance textures,
 * but its instancing path expects the block atlas to remain on texture unit 0.
 * Re-establishing that single input at the Voxy boundary keeps both renderers'
 * own pipelines unchanged.</p>
 */
@Pseudo
@Mixin(targets = "me.cortex.voxy.client.core.VoxyRenderSystem", priority = 900, remap = false)
public abstract class VoxyRenderSystemTextureBoundaryMixin {
    @Inject(
            method = "renderOpaque(Lme/cortex/voxy/client/core/rendering/Viewport;)V",
            at = @At("RETURN")
    )
    private void voxyCompat$restoreFlywheelDiffuseAtlas(final CallbackInfo ci) {
        if (!ModList.get().isLoaded("flywheel")) {
            return;
        }

        final int previousActiveTexture = GlStateManager._getActiveTexture();
        final int blockAtlas = Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS)
                .getId();

        RenderSystem.setShaderTexture(0, blockAtlas);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
        RenderSystem.bindTexture(blockAtlas);
        GlStateManager._activeTexture(previousActiveTexture);
    }
}
