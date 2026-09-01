package dev.notasher.voxycompat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxyActivationBridgeTest {
    private static final Object LEVEL_RENDERER = new Object();

    @Test
    void waitsWithoutMutatingAnythingUntilVoxyIsAvailable() {
        final FakeRuntime runtime = new FakeRuntime();
        runtime.available = false;

        final VoxyActivationBridge.Attempt attempt =
                VoxyActivationBridge.attempt(LEVEL_RENDERER, runtime);

        assertEquals(VoxyActivationBridge.Status.WAITING, attempt.status());
        assertEquals(0, runtime.enableVoxyCalls);
        assertEquals(0, runtime.enableRenderingCalls);
        assertEquals(0, runtime.saveCalls);
    }

    @Test
    void waitsUntilTheClientWorldHasAnEstablishedVoxySession() {
        final FakeRuntime runtime = new FakeRuntime();
        runtime.inSession = false;

        final VoxyActivationBridge.Attempt attempt =
                VoxyActivationBridge.attempt(LEVEL_RENDERER, runtime);

        assertEquals(VoxyActivationBridge.Status.WAITING, attempt.status());
        assertEquals(0, runtime.enableVoxyCalls);
        assertEquals(0, runtime.enableRenderingCalls);
    }

    @Test
    void trueFlagsDoNotHideMissingRuntimeObjects() {
        final FakeRuntime runtime = new FakeRuntime();
        runtime.enabled = true;
        runtime.renderingEnabled = true;

        final VoxyActivationBridge.Attempt attempt =
                VoxyActivationBridge.attempt(LEVEL_RENDERER, runtime);

        assertEquals(VoxyActivationBridge.Status.SUCCESS, attempt.status());
        assertTrue(attempt.changed());
        assertEquals(1, runtime.enableVoxyCalls);
        assertEquals(1, runtime.enableRenderingCalls);
        assertNotNull(runtime.instance);
        assertNotNull(runtime.renderer);
        assertEquals(1, runtime.saveCalls);
    }

    @Test
    void enablesBothSettingsThroughTheirRuntimePaths() {
        final FakeRuntime runtime = new FakeRuntime();

        final VoxyActivationBridge.Attempt attempt =
                VoxyActivationBridge.attempt(LEVEL_RENDERER, runtime);

        assertEquals(VoxyActivationBridge.Status.SUCCESS, attempt.status());
        assertTrue(runtime.enabled);
        assertTrue(runtime.renderingEnabled);
        assertEquals(1, runtime.enableVoxyCalls);
        assertEquals(1, runtime.enableRenderingCalls);
        assertEquals(1, runtime.saveCalls);
    }

    @Test
    void stopsWithoutReplayingSettersWhenRuntimeIsAlreadyActive() {
        final FakeRuntime runtime = new FakeRuntime();
        runtime.enabled = true;
        runtime.renderingEnabled = true;
        runtime.instance = new Object();
        runtime.renderer = new Object();

        final VoxyActivationBridge.Attempt attempt =
                VoxyActivationBridge.attempt(LEVEL_RENDERER, runtime);

        assertEquals(VoxyActivationBridge.Status.SUCCESS, attempt.status());
        assertFalse(attempt.changed());
        assertEquals(0, runtime.enableVoxyCalls);
        assertEquals(0, runtime.enableRenderingCalls);
        assertEquals(0, runtime.saveCalls);
    }

    @Test
    void correctsFalseFlagsWithoutCreatingDuplicateRuntimeObjects() {
        final FakeRuntime runtime = new FakeRuntime();
        runtime.instance = new Object();
        runtime.renderer = new Object();

        final VoxyActivationBridge.Attempt attempt =
                VoxyActivationBridge.attempt(LEVEL_RENDERER, runtime);

        assertEquals(VoxyActivationBridge.Status.SUCCESS, attempt.status());
        assertTrue(runtime.enabled);
        assertTrue(runtime.renderingEnabled);
        assertEquals(0, runtime.enableVoxyCalls);
        assertEquals(0, runtime.enableRenderingCalls);
        assertEquals(1, runtime.saveCalls);
    }

    @Test
    void retriesWhenTheRealRenderingPathHasNotCreatedARendererYet() {
        final FakeRuntime runtime = new FakeRuntime();
        runtime.createRenderer = false;

        final VoxyActivationBridge.Attempt attempt =
                VoxyActivationBridge.attempt(LEVEL_RENDERER, runtime);

        assertEquals(VoxyActivationBridge.Status.RETRY, attempt.status());
        assertEquals(1, runtime.enableVoxyCalls);
        assertEquals(1, runtime.enableRenderingCalls);
        assertEquals(1, runtime.saveCalls);
    }

    private static final class FakeRuntime implements VoxyActivationBridge.RuntimeAccess {
        private final Object config = new Object();
        private boolean available = true;
        private boolean inSession = true;
        private boolean enabled;
        private boolean renderingEnabled;
        private boolean createInstance = true;
        private boolean createRenderer = true;
        private Object instance;
        private Object renderer;
        private int enableVoxyCalls;
        private int enableRenderingCalls;
        private int saveCalls;

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public boolean isInSession() {
            return inSession;
        }

        @Override
        public Object config() {
            return config;
        }

        @Override
        public boolean enabled(final Object ignored) {
            return enabled;
        }

        @Override
        public boolean renderingEnabled(final Object ignored) {
            return renderingEnabled;
        }

        @Override
        public void setEnabled(final Object ignored, final boolean value) {
            enabled = value;
        }

        @Override
        public void setRenderingEnabled(final Object ignored, final boolean value) {
            renderingEnabled = value;
        }

        @Override
        public Object instance() {
            return instance;
        }

        @Override
        public void enableVoxy(final Object ignored) {
            enableVoxyCalls++;
            enabled = true;
            if (createInstance) {
                instance = new Object();
            }
        }

        @Override
        public Object renderer(final Object ignored) {
            return renderer;
        }

        @Override
        public void enableRendering(final Object ignored) {
            enableRenderingCalls++;
            renderingEnabled = true;
            if (createRenderer) {
                renderer = new Object();
            }
        }

        @Override
        public void save(final Object ignored) {
            saveCalls++;
        }
    }
}
