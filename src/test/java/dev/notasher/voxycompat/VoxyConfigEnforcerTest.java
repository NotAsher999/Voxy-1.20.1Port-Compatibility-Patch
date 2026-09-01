package dev.notasher.voxycompat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxyConfigEnforcerTest {
    @Test
    void loadedDisabledConfigIsCorrectedAndPersisted() {
        final FakeVoxyConfig config = new FakeVoxyConfig(false, false);

        VoxyConfigEnforcer.enforceLoadedConfig(config);

        assertTrue(config.enabled);
        assertTrue(config.enableRendering);
        assertEquals(1, config.saveCount);
    }

    @Test
    void alreadyEnabledLoadDoesNotPerformASecondWrite() {
        final FakeVoxyConfig config = new FakeVoxyConfig(true, true);

        VoxyConfigEnforcer.enforceLoadedConfig(config);

        assertEquals(0, config.saveCount);
    }

    @Test
    void saveBoundaryCorrectsBothFieldsWithoutRecursing() {
        final FakeVoxyConfig config = new FakeVoxyConfig(false, true);

        VoxyConfigEnforcer.enforceBeforeSave(config);

        assertTrue(config.enabled);
        assertTrue(config.enableRendering);
        assertEquals(0, config.saveCount);
    }

    @Test
    void rejectedTogglePersistsTrueWithoutRunningVoxysShutdownSetter() {
        final FakeVoxyConfig config = new FakeVoxyConfig(true, false);

        VoxyConfigEnforcer.rejectDisableRequest(config);

        assertTrue(config.enabled);
        assertTrue(config.enableRendering);
        assertEquals(1, config.saveCount);
    }

    @Test
    void forceInMemoryReportsWhetherAChangeWasRequired() {
        final FakeVoxyConfig config = new FakeVoxyConfig(false, true);

        assertTrue(VoxyConfigEnforcer.forceInMemory(config));
        assertFalse(VoxyConfigEnforcer.forceInMemory(config));
    }

    public static final class FakeVoxyConfig {
        public boolean enabled;
        public boolean enableRendering;
        int saveCount;

        FakeVoxyConfig(final boolean enabled, final boolean enableRendering) {
            this.enabled = enabled;
            this.enableRendering = enableRendering;
        }

        public void save() {
            this.saveCount++;
        }
    }
}
