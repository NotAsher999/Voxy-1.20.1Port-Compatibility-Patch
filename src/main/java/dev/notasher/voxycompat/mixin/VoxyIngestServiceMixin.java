package dev.notasher.voxycompat.mixin;

import dev.notasher.voxycompat.SablePlotBoundary;
import dev.notasher.voxycompat.VoxyCompatibilityPatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Prevents Sable's hidden plot-storage chunks from entering Voxy's ordinary
 * world LOD database.
 */
@Pseudo
@Mixin(targets = "me.cortex.voxy.common.world.service.VoxelIngestService", priority = 900, remap = false)
public abstract class VoxyIngestServiceMixin {
    private static final int VOXY_COMPAT$LOG_LIMIT = Math.max(0,
            Integer.getInteger("voxyCompat.plotFilterLogLimit", 16));
    private static final AtomicLong VOXY_COMPAT$FILTERED = new AtomicLong();

    @Inject(method = "enqueueIngest", at = @At("HEAD"), cancellable = true,
            remap = false, require = 1)
    private void voxyCompat$excludeSablePlotChunk(
            @Coerce final Object worldEngine,
            final LevelChunk chunk,
            final CallbackInfoReturnable<Boolean> cir) {
        final ChunkPos position = chunk.getPos();
        if (SablePlotBoundary.containsChunk(chunk.getLevel(), position.x, position.z)) {
            voxyCompat$recordFiltered("chunk", position.x, position.z);
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "rawIngest0(Lme/cortex/voxy/common/world/WorldEngine;"
                    + "Lnet/minecraft/world/level/chunk/LevelChunkSection;III"
                    + "Lnet/minecraft/world/level/chunk/DataLayer;"
                    + "Lnet/minecraft/world/level/chunk/DataLayer;)Z",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void voxyCompat$excludeSablePlotSection(
            @Coerce final Object worldEngine,
            final LevelChunkSection section,
            final int sectionX,
            final int sectionY,
            final int sectionZ,
            final DataLayer blockLight,
            final DataLayer skyLight,
            final CallbackInfoReturnable<Boolean> cir) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level != null && SablePlotBoundary.containsChunk(level, sectionX, sectionZ)) {
            voxyCompat$recordFiltered("section", sectionX, sectionZ);
            cir.setReturnValue(false);
        }
    }

    private static void voxyCompat$recordFiltered(final String source, final int chunkX, final int chunkZ) {
        final long count = VOXY_COMPAT$FILTERED.incrementAndGet();
        if (count <= VOXY_COMPAT$LOG_LIMIT) {
            VoxyCompatibilityPatch.LOGGER.info(
                    "Filtered Sable plot {} ingestion at chunk ({}, {}), count={}",
                    source, chunkX, chunkZ, count);
        }
    }
}
