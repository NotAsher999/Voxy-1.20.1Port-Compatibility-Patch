package dev.notasher.voxycompat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Replays Voxy 0.2.13's real settings-page activation path after a client
 * world and Voxy session both exist.
 */
final class VoxyActivationBridge {
    private static volatile RuntimeAccess runtimeAccess;

    private VoxyActivationBridge() {
    }

    static Attempt attempt(final Object levelRenderer) {
        try {
            return attempt(levelRenderer, runtimeAccess());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return Attempt.fatal(exception);
        }
    }

    static Attempt attempt(final Object levelRenderer, final RuntimeAccess runtime) {
        Objects.requireNonNull(runtime, "Voxy runtime access");
        if (levelRenderer == null) {
            return Attempt.waiting();
        }

        try {
            if (!runtime.isAvailable() || !runtime.isInSession()) {
                return Attempt.waiting();
            }

            final Object config = Objects.requireNonNull(runtime.config(), "Voxy config");
            boolean changed = false;

            Object instance = runtime.instance();
            if (instance == null) {
                runtime.enableVoxy(config);
                changed = true;
                instance = runtime.instance();
                if (instance == null) {
                    runtime.save(config);
                    return Attempt.retry(null);
                }
            } else if (!runtime.enabled(config)) {
                runtime.setEnabled(config, true);
                changed = true;
            }

            Object renderer = runtime.renderer(levelRenderer);
            if (renderer == null) {
                runtime.enableRendering(config);
                changed = true;
                renderer = runtime.renderer(levelRenderer);
                if (renderer == null) {
                    runtime.save(config);
                    return Attempt.retry(null);
                }
            } else if (!runtime.renderingEnabled(config)) {
                runtime.setRenderingEnabled(config, true);
                changed = true;
            }

            if (!runtime.enabled(config)) {
                runtime.setEnabled(config, true);
                changed = true;
            }
            if (!runtime.renderingEnabled(config)) {
                runtime.setRenderingEnabled(config, true);
                changed = true;
            }
            if (changed) {
                runtime.save(config);
            }

            return Attempt.success(changed);
        } catch (VoxyInvocationException exception) {
            return Attempt.retry(exception.getCause());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Attempt.fatal(exception);
        }
    }

    private static RuntimeAccess runtimeAccess() throws ReflectiveOperationException {
        RuntimeAccess access = runtimeAccess;
        if (access == null) {
            synchronized (VoxyActivationBridge.class) {
                access = runtimeAccess;
                if (access == null) {
                    access = new ReflectiveRuntimeAccess();
                    runtimeAccess = access;
                }
            }
        }
        return access;
    }

    enum Status {
        WAITING,
        RETRY,
        SUCCESS,
        FATAL
    }

    record Attempt(Status status, boolean changed, Throwable failure) {
        static Attempt waiting() {
            return new Attempt(Status.WAITING, false, null);
        }

        static Attempt retry(final Throwable failure) {
            return new Attempt(Status.RETRY, false, failure);
        }

        static Attempt success(final boolean changed) {
            return new Attempt(Status.SUCCESS, changed, null);
        }

        static Attempt fatal(final Throwable failure) {
            return new Attempt(Status.FATAL, false, failure);
        }
    }

    interface RuntimeAccess {
        boolean isAvailable() throws ReflectiveOperationException;

        boolean isInSession() throws ReflectiveOperationException;

        Object config() throws ReflectiveOperationException;

        boolean enabled(Object config) throws ReflectiveOperationException;

        boolean renderingEnabled(Object config) throws ReflectiveOperationException;

        void setEnabled(Object config, boolean value) throws ReflectiveOperationException;

        void setRenderingEnabled(Object config, boolean value) throws ReflectiveOperationException;

        Object instance() throws ReflectiveOperationException;

        void enableVoxy(Object config) throws ReflectiveOperationException;

        Object renderer(Object levelRenderer) throws ReflectiveOperationException;

        void enableRendering(Object config) throws ReflectiveOperationException;

        void save(Object config) throws ReflectiveOperationException;
    }

    private static final class ReflectiveRuntimeAccess implements RuntimeAccess {
        private static final String CONFIG_CLASS = "me.cortex.voxy.client.config.VoxyConfig";
        private static final String COMMON_CLASS = "me.cortex.voxy.commonImpl.VoxyCommon";
        private static final String SESSION_CLASS = "me.cortex.voxy.client.ClientSessionEvents";
        private static final String SCREEN_CLASS = "me.cortex.voxy.client.config.VoxyConfigScreenPages";
        private static final String RENDER_ACCESS_CLASS = "me.cortex.voxy.client.core.IGetVoxyRenderSystem";

        private final Field config;
        private final Field enabled;
        private final Field renderingEnabled;
        private final Field inSession;
        private final Method isAvailable;
        private final Method getInstance;
        private final Method enableVoxy;
        private final Method getRenderer;
        private final Method enableRendering;
        private final Method save;
        private final Class<?> renderAccess;

        private ReflectiveRuntimeAccess() throws ReflectiveOperationException {
            final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            final ClassLoader loader = contextLoader != null
                    ? contextLoader
                    : VoxyActivationBridge.class.getClassLoader();
            final Class<?> configType = Class.forName(CONFIG_CLASS, true, loader);
            final Class<?> commonType = Class.forName(COMMON_CLASS, true, loader);
            final Class<?> sessionType = Class.forName(SESSION_CLASS, true, loader);
            final Class<?> screenType = Class.forName(SCREEN_CLASS, true, loader);
            this.renderAccess = Class.forName(RENDER_ACCESS_CLASS, true, loader);

            this.config = configType.getField("CONFIG");
            this.enabled = configType.getField("enabled");
            this.renderingEnabled = configType.getField("enableRendering");
            this.inSession = sessionType.getField("inSession");
            this.isAvailable = commonType.getMethod("isAvailable");
            this.getInstance = commonType.getMethod("getInstance");
            this.enableVoxy = screenType.getDeclaredMethod("lambda$page$0", configType, Boolean.class);
            this.enableVoxy.setAccessible(true);
            this.getRenderer = this.renderAccess.getMethod("getVoxyRenderSystem");
            this.enableRendering = screenType.getDeclaredMethod("lambda$page$10", configType, Boolean.class);
            this.enableRendering.setAccessible(true);
            this.save = configType.getMethod("save");
        }

        @Override
        public boolean isAvailable() throws ReflectiveOperationException {
            return (boolean) invoke(this.isAvailable, null);
        }

        @Override
        public boolean isInSession() throws IllegalAccessException {
            return this.inSession.getBoolean(null);
        }

        @Override
        public Object config() throws IllegalAccessException {
            return this.config.get(null);
        }

        @Override
        public boolean enabled(final Object config) throws IllegalAccessException {
            return this.enabled.getBoolean(config);
        }

        @Override
        public boolean renderingEnabled(final Object config) throws IllegalAccessException {
            return this.renderingEnabled.getBoolean(config);
        }

        @Override
        public void setEnabled(final Object config, final boolean value) throws IllegalAccessException {
            this.enabled.setBoolean(config, value);
        }

        @Override
        public void setRenderingEnabled(final Object config, final boolean value) throws IllegalAccessException {
            this.renderingEnabled.setBoolean(config, value);
        }

        @Override
        public Object instance() throws ReflectiveOperationException {
            return invoke(this.getInstance, null);
        }

        @Override
        public void enableVoxy(final Object config) throws ReflectiveOperationException {
            invoke(this.enableVoxy, null, config, Boolean.TRUE);
        }

        @Override
        public Object renderer(final Object levelRenderer) throws ReflectiveOperationException {
            if (!this.renderAccess.isInstance(levelRenderer)) {
                throw new IllegalStateException("Voxy's level-renderer access Mixin is not active");
            }
            return invoke(this.getRenderer, levelRenderer);
        }

        @Override
        public void enableRendering(final Object config) throws ReflectiveOperationException {
            invoke(this.enableRendering, null, config, Boolean.TRUE);
        }

        @Override
        public void save(final Object config) throws ReflectiveOperationException {
            invoke(this.save, config);
        }

        private static Object invoke(
                final Method method,
                final Object receiver,
                final Object... arguments) throws ReflectiveOperationException {
            try {
                return method.invoke(receiver, arguments);
            } catch (InvocationTargetException exception) {
                final Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
                throw new VoxyInvocationException(cause);
            }
        }
    }

    private static final class VoxyInvocationException extends RuntimeException {
        private VoxyInvocationException(final Throwable cause) {
            super(cause);
        }
    }
}
