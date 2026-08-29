package dev.notasher.voxycompat.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraftforge.fml.ModList;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores render state that Voxy's opaque renderer inherits from the
 * surrounding level render pass.
 *
 * <p>Voxy 0.2.13 binds program {@code 0} while cleaning up its opaque LOD
 * pipeline instead of restoring the program that was active on entry. Oculus
 * and vanilla cache their last applied shader, so leaving the real OpenGL
 * state at {@code 0} can make the next uniform upload run without a program.
 * The redirect below replaces only Voxy's final unbind with restoration of the
 * actual entry program.</p>
 *
 * <p>Voxy also deliberately clears texture units 0 through 11. Flywheel
 * rebinds overlay, light, and instance textures, but its instancing path
 * expects the block atlas to remain on texture unit 0. Re-establishing that
 * single input keeps both renderers' own pipelines unchanged.</p>
 */
@Pseudo
@Mixin(targets = "me.cortex.voxy.client.core.VoxyRenderSystem", priority = 900, remap = false)
public abstract class VoxyRenderSystemTextureBoundaryMixin {
    @Unique
    private int voxyCompat$entryProgram;

    @Inject(
            method = "renderOpaque(Lme/cortex/voxy/client/core/rendering/Viewport;)V",
            at = @At("HEAD")
    )
    private void voxyCompat$captureEntryProgram(final CallbackInfo ci) {
        this.voxyCompat$entryProgram = GL20C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
    }

    @Redirect(
            method = "renderOpaque(Lme/cortex/voxy/client/core/rendering/Viewport;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL30C;glUseProgram(I)V"
            )
    )
    private void voxyCompat$restoreEntryProgram(final int ignoredProgram) {
        GL30C.glUseProgram(this.voxyCompat$entryProgram);
    }

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
