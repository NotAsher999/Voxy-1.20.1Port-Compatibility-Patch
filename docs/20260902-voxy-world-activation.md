# Voxy per-world activation repair

## Rejected approach

The experimental commit `435e64187994d0b505d61c2ba061a782aad62034`
forced Voxy's `enabled` and `enableRendering` configuration fields to remain
`true` during configuration load and save. It was kept on a separate branch and
was never merged into `main`.

That rejected binary used version `1.0.2`. The replacement uses `1.0.3` so the
two behaviors cannot be mistaken for the same artifact.

Runtime testing rejected that approach: both values could be `true` while Voxy
still had no world instance or renderer. A value lock does not replay Voxy's
runtime activation lifecycle.

## Upstream behavior used by the repair

The deployed Voxy 0.2.13-alpha jar exposes the runtime state through
`VoxyCommon.getInstance()` and the level-renderer
`IGetVoxyRenderSystem.getVoxyRenderSystem()` bridge. Its configuration page
handles the two switches through `VoxyConfigScreenPages.lambda$page$0` and
`lambda$page$10`. Those paths create the common world instance and the client
renderer respectively, and perform Voxy's normal shader reload integration.

The target Voxy jar audited for this contract has SHA-256:

`E71E54DBBB034601AE6FAD694502928764815F6EA06B6CB5BD4B46D31BCE3828`

## Current repair

The compatibility patch listens to the Forge client tick event but does no work
on the title screen. Forty ticks after a real `ClientLevel` appears, and every
forty ticks thereafter, it checks Voxy availability and waits for Voxy's client
session. It then:

1. verifies the common Voxy instance and invokes the original global-setting
   path if it is missing;
2. verifies the level renderer and invokes the original rendering-setting path
   if it is missing;
3. ensures the two persisted fields agree with the active runtime state and
   saves the configuration;
4. displays a client message and permanently stops polling for that world only
   after both runtime objects are present.

Leaving or changing worlds resets the state. Temporary readiness failures keep
the low-frequency poll active. A missing or incompatible Voxy reflection
contract is reported once and stops the poll rather than repeatedly failing on
the render thread.

## Validation boundary

Automated tests cover the false-positive state where both booleans are already
`true` but the instance and renderer are missing, readiness waits, retry
behavior, idempotence after success, per-world reset, and absence of the
rejected configuration-lock Mixins.

The final acceptance gate remains a production-client run: enter a world with
Voxy initially inactive, wait for the translated confirmation, and verify that
LOD rendering actually begins. Build success alone is not runtime acceptance.

## Candidate checkpoint

- Version: `1.0.3`
- Production jar SHA-256:
  `110C229D9A66A3B1C05F2291152EDFC98357C8465F5F8AA39DA75A67F67E6443`
- Automated validation: 8 suites / 15 tests, with no failures, errors, or
  skipped tests
- Packaging validation: Java 17 bytecode, translated messages, activation
  bridge, world service, and the original four compatibility Mixins present;
  rejected configuration-lock classes absent
- Deployment: installed as the only active compatibility-patch jar in the
  DeceasedCraft client; rejected `1.0.2` preserved in the repository-local
  deployment backup
- Runtime status: pending user world-entry and visible-LOD acceptance
