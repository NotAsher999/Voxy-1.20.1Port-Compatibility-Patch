package dev.notasher.voxycompat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reflective boundary adapter for Sable's public sub-level API.
 *
 * <p>The compatibility patch deliberately has no compile-time Sable dependency
 * and does not reproduce Sable's plot-coordinate rules. Sable remains the sole
 * owner of those rules through {@code SubLevelContainer.inBounds(int, int)}.</p>
 */
public final class SablePlotBoundary {
    private static final String CONTAINER_CLASS_NAME =
            "dev.ryanhcode.sable.api.sublevel.SubLevelContainer";

    private static volatile boolean initialized;
    private static volatile boolean available;
    private static Method getClientContainer;
    private static Method inBounds;

    private SablePlotBoundary() {
    }

    public static boolean containsChunk(final Level level, final int chunkX, final int chunkZ) {
        if (!(level instanceof final ClientLevel clientLevel)) {
            return false;
        }

        initialize();
        if (!available) {
            return false;
        }

        try {
            final Object container = getClientContainer.invoke(null, clientLevel);
            return container != null && Boolean.TRUE.equals(inBounds.invoke(container, chunkX, chunkZ));
        } catch (final IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            disableAfterFailure(exception);
            return false;
        }
    }

    private static void initialize() {
        if (initialized) {
            return;
        }

        synchronized (SablePlotBoundary.class) {
            if (initialized) {
                return;
            }

            try {
                final ClassLoader loader = SablePlotBoundary.class.getClassLoader();
                final Class<?> containerClass = Class.forName(CONTAINER_CLASS_NAME, false, loader);
                getClientContainer = containerClass.getMethod("getContainer", ClientLevel.class);
                inBounds = containerClass.getMethod("inBounds", int.class, int.class);
                available = true;
            } catch (final ClassNotFoundException | NoSuchMethodException | LinkageError exception) {
                available = false;
            } finally {
                initialized = true;
            }
        }
    }

    private static void disableAfterFailure(final Throwable exception) {
        if (available) {
            available = false;
            VoxyCompatibilityPatch.LOGGER.warn(
                    "Disabling Sable plot isolation because its public boundary API failed", exception);
        }
    }
}
