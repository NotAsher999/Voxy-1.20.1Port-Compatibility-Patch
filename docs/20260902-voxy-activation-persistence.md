# Voxy activation persistence

## Observed behavior

The DeceasedCraft client repeatedly started with Voxy's global activation or
rendering setting disabled. The affected runs logged `Not creating renderer due
to disabled` or `Not creating renderer due to disabled rendering`. Opening the
video settings and enabling both options immediately allowed
`Creating Voxy render system` in the same process.

The Voxy 0.2.13 client config stores these values as `enabled` and
`enable_rendering` in `config/voxy-config.json`.

## Upstream write boundaries

The deployed Voxy 0.2.13 bytecode has a deliberately small write surface:

- `VoxyConfig.loadOrCreate()` constructs or deserializes the two public fields;
- `VoxyConfig.save()` serializes the current fields;
- `VoxyConfigScreenPages.lambda$page$0` writes `enabled` and can shut down the
  complete Voxy instance;
- `VoxyConfigScreenPages.lambda$page$10` writes `enableRendering` and can shut
  down the Voxy renderer.

No other Voxy 0.2.13 production class writes either field. Readers still check
`VoxyCommon.isAvailable()` before creating or using the renderer.

## Compatibility repair

The patch restores both fields to `true` at config-load return and immediately
before every config save. It rechecks and saves the singleton once more at the
return of `VoxyClient.initVoxyClient()`, after Voxy has established its real
hardware/runtime availability. The two settings-page setters are cancelled
only when their requested value is `false`, preventing them from shutting down
the live instance before the corrected save occurs.

The repair intentionally does not replace or return a constant from
`VoxyConfig.isRenderingEnabled()`, change `VoxyCommon.isAvailable()`, create a
renderer or world instance itself, or alter Voxy capability flags. Unsupported
hardware and failed exclusive-lock acquisition therefore retain Voxy's normal
safe-disabled behavior even though the user's persisted preference remains on.

## Validation contract

Automated tests cover in-memory correction, persistence calls, non-recursive
save enforcement, both exact settings-page setter descriptors, the Voxy client
initialization boundary, production Mixin registration, and the absence of an
availability/renderer bypass.

Runtime acceptance remains:

1. start with both JSON values set to `false`;
2. confirm the patch rewrites both to `true` and Voxy creates its renderer;
3. restart several times and confirm both values remain `true`;
4. confirm the settings page cannot shut either locked value off;
5. confirm a genuine Voxy capability failure still reports unavailable rather
   than attempting renderer creation.

## Candidate checkpoint

The `1.0.2` candidate was built against Forge 47.4.0 and statically checked
against the deployed Voxy 0.2.13-alpha JAR with SHA-256
`E71E54DBBB034601AE6FAD694502928764815F6EA06B6CB5BD4B46D31BCE3828`.
Eight JUnit suites ran 13 tests with zero failures, errors, or skips. The final
candidate JAR is 17,119 bytes with SHA-256
`59895A9B9C6DBAA42112A04F1432759171B736CC999679489B121CBD24EE4503`.
It is deployed to the DeceasedCraft client for the runtime acceptance sequence
above; this checkpoint does not claim that multi-restart validation has already
passed.
