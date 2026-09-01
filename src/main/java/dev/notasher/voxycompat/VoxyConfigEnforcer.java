package dev.notasher.voxycompat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Keeps Voxy's two top-level activation settings enabled without bypassing
 * Voxy's own hardware and runtime availability checks.
 *
 * <p>The target fields are public in Voxy 0.2.13, but this project deliberately
 * has no compile-time Voxy dependency. Reflection keeps that boundary explicit
 * while the exact class and field contract is locked by the supported Voxy
 * version range and regression tests.</p>
 */
public final class VoxyConfigEnforcer {
    static final String CONFIG_CLASS = "me.cortex.voxy.client.config.VoxyConfig";
    static final String ENABLED_FIELD = "enabled";
    static final String RENDERING_FIELD = "enableRendering";
    static final String CONFIG_FIELD = "CONFIG";
    static final String SAVE_METHOD = "save";

    private VoxyConfigEnforcer() {
    }

    /** Corrects a config returned from Voxy's loader and persists the correction. */
    public static boolean enforceLoadedConfig(final Object config) {
        final boolean corrected = forceInMemory(config);
        if (corrected) {
            invokeSave(config);
        }
        return corrected;
    }

    /** Prevents any later Voxy save from persisting either activation value as false. */
    public static boolean enforceBeforeSave(final Object config) {
        return forceInMemory(config);
    }

    /**
     * Re-checks and persists the singleton after Voxy capability initialization.
     * The unconditional save also repairs a file that could not be written if
     * the config class was initialized before Voxy became available.
     */
    public static boolean enforceAfterVoxyInitialization() {
        try {
            final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            final ClassLoader loader = contextLoader != null
                    ? contextLoader
                    : VoxyConfigEnforcer.class.getClassLoader();
            final Class<?> configType = Class.forName(CONFIG_CLASS, true, loader);
            final Object config = configType.getField(CONFIG_FIELD).get(null);
            final boolean corrected = forceInMemory(config);
            invokeSave(config);
            return corrected;
        } catch (ReflectiveOperationException exception) {
            throw contractFailure("read Voxy's live client config", exception);
        }
    }

    /** Rejects a settings-screen request before Voxy can shut down the live instance. */
    public static void rejectDisableRequest(final Object config) {
        forceInMemory(config);
        invokeSave(config);
    }

    static boolean forceInMemory(final Object config) {
        Objects.requireNonNull(config, "Voxy config");
        try {
            final Class<?> type = config.getClass();
            final Field enabled = type.getField(ENABLED_FIELD);
            final Field rendering = type.getField(RENDERING_FIELD);
            final boolean changed = !enabled.getBoolean(config) || !rendering.getBoolean(config);
            enabled.setBoolean(config, true);
            rendering.setBoolean(config, true);
            return changed;
        } catch (ReflectiveOperationException exception) {
            throw contractFailure("update Voxy's activation fields", exception);
        }
    }

    private static void invokeSave(final Object config) {
        try {
            final Method save = config.getClass().getMethod(SAVE_METHOD);
            save.invoke(config);
        } catch (ReflectiveOperationException exception) {
            throw contractFailure("persist Voxy's client config", exception);
        }
    }

    private static IllegalStateException contractFailure(
            final String action,
            final ReflectiveOperationException exception) {
        final Throwable cause = exception instanceof InvocationTargetException invocation
                && invocation.getCause() != null
                ? invocation.getCause()
                : exception;
        return new IllegalStateException("Unable to " + action + "; the supported Voxy config contract changed", cause);
    }
}
