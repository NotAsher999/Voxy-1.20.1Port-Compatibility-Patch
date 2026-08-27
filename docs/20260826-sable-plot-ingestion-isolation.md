# Voxy + Sable hidden-plot ingestion isolation

Date: 2026-08-26

## Reported behavior

- Voxy alone renders grass normally.
- With Sable, Veil, and Simulated present, Voxy LOD grass can turn black regardless of whether a shader pack is enabled.
- Some black LOD grass appears in front of, or overlays, moving physical structures.

## Evidence and root-cause boundary

CPU-side tint diagnostics showed valid green grass colors, so a grass color-provider failure was not supported by the logs.

Sable stores physical sub-level chunks in a reserved plot grid inside the parent `ClientLevel`. The observed plot chunks were around chunk coordinate `1280064`, corresponding to Sable's distant internal storage area. Voxy listens to ordinary `ClientLevel` chunk and block changes and, without a compatibility boundary, submits those plot chunks to the same LOD ingestion service used for ordinary world terrain.

This is invalid cross-mod ownership: Sable's plot chunks are implementation storage that must be transformed and rendered as physical structures, not persisted as normal world LOD terrain. Ingesting them can introduce their storage-space geometry and lighting into Voxy's world database and explains both the black LOD symptom and geometry appearing independently of the physical body's transform.

## Implemented compatibility boundary

The standalone patch intercepts only these Voxy ingestion boundaries:

- `VoxelIngestService.enqueueIngest(...)` for whole chunks.
- The private `VoxelIngestService.rawIngest0(WorldEngine, ...)` queue boundary
  shared by both public `rawIngest(...)` overloads, including Embeddium section
  upload updates.

Before ingestion, it reflectively calls Sable's public `SubLevelContainer.getContainer(ClientLevel)` and `SubLevelContainer.inBounds(int, int)`. Only positions Sable itself identifies as its reserved plot grid return `false` from Voxy ingestion. Ordinary world chunks retain Voxy's original behavior.

The patch does not:

- hard-code Sable's distant coordinates;
- alter Voxy colors, shaders, depth, draw order, cache format, or renderer logic;
- alter Sable or Simulated source;
- copy or package any Sable class, resource, or implementation;
- require Sable or Oculus to be installed.

Diagnostics are capped at 16 entries by default. The cap can be changed with the JVM property `-DvoxyCompat.plotFilterLogLimit=<count>`.

## Build verification

- Gradle `clean test build`: passed.
- Contract tests: 2 passed, 0 failed.
- Packaged Sable class/resource paths: none.
- Deployed jar SHA-256: `D5F1F9ADCE2229653164294778EE03A37CB40C2B4AE8760DA515C9C4929E5AD8`.

## Runtime verification status

Status: corrected boundary compiled and deployed; runtime verification pending.

The first deployed isolation build did not resolve the symptom because it
intercepted only `rawIngest(WorldIdentifier, ...)`. Voxy 0.2.13's Embeddium
`MixinRenderSectionManager` calls `rawIngest(WorldEngine, ...)` directly, and
that overload delegates to `rawIngest0(...)`. Consequently the earlier clean
cache test did not exercise the intended filter at all. The absence of bounded
filter messages was evidence of this missed overload, not evidence that plot
ingestion was unrelated.

The previously populated cache was moved, not deleted, to:

`J:\YZ\PJ\.minecraft\versions\1.20.1-Forge\saves\新的世界 (5)\voxy.backup-before-sable-plot-filter-20260826`

Voxy will create a clean `voxy` directory on the next load. The decisive log evidence is one or more bounded `Filtered Sable plot ... ingestion` entries while physical structures are present, together with normal distant terrain and no black/overlay grass.

The cache for the corrected boundary test was moved, not deleted, to:

`J:\YZ\PJ\.minecraft\versions\1.20.1-Forge\saves\新的世界 (5)\voxy.backup-before-raw-ingest0-20260826-1452`

CPU biome tint results remained valid green values. A separate GPU
rendering/composition defect must not be asserted unless the corrected
ingestion boundary is first exercised and disproven at runtime.

An attempted bounded GPU probe was removed after it caused an
`IllegalClassLoadError`: helper records were emitted inside the Mixin-owned
package, which Mixin 0.8.5 forbids target classes from referencing. The crash
occurred before any OpenGL query ran. This diagnostic implementation is not
part of the deployed stable patch and must not be restored in that form.

## 2026-08-26 15:04 correction and bounded CPU probe

The corrected `rawIngest0` boundary produced no filter records during another
clean-data test, while the black LOD remained under both Iris and normal Voxy
pipelines. Hidden-plot ingestion is therefore not established as the black
grass cause; the earlier assumption that Sable's dedicated renderer used
Voxy's ordinary section manager was unsupported.

A read-only CPU diagnostic now records the block/sky light ranges at grass
positions entering `rawIngest0`. It logs at most 16 matching sections and
examines at most 512 candidates by default. It performs no GL/GPU access and
does not change ingestion. The log limit is configurable with
`-DvoxyCompat.lightProbeLogLimit=<count>`.

Diagnostic build SHA-256:
`3351B96D80ED78B490BEB8F2CD94B8EDBCBD0C5518A5C2392D05DB8A78ECF403`.

## 2026-08-26 15:18 lighting result and post-light refresh candidate

The bounded CPU probe produced 16 grass-bearing ordinary-world sections. In
every record the sky `DataLayer` existed and was non-empty, but every sampled
grass position had `skyRange=[0,0]`. This is decisive evidence that the black
value already exists when Voxy ingests the section; it is not introduced by
the grass tint provider, shader-pack selection, or final framebuffer
composition.

Minecraft 1.20.1 installs the chunk body in
`ClientPacketListener#handleLevelChunkWithLight` and queues the matching
`applyLightData(...)` work for later execution. Voxy's Embeddium upload hook can
therefore ingest a newly uploaded section while the client light storage still
contains zero skylight. The compatibility patch now records only chunks where
`rawIngest0` actually observes a grass block with zero skylight. When Minecraft
later finishes `applyLightData(...)` for that same chunk, the patch removes the
pending marker and calls Voxy's public `tryAutoIngestChunk(LevelChunk)` entry
point with the live chunk and live light storage.

The candidate does not manufacture a light value, run the light engine,
replace Voxy conversion, or alter cache/render formats. It also does not
re-ingest every loaded chunk: chunks that were not observed with the bad input
take no additional Voxy work. Refresh diagnostics default to 16 entries and
are configurable with `-DvoxyCompat.lightRefreshLogLimit=<count>`; zero disables
only those log messages.

Build and packaging verification:

- `clean test build`: passed;
- tests: 5 passed, 0 failed;
- generated Forge refmap maps the Minecraft injection method;
- packaged Sable/Voxy implementation or resource paths: none;
- deployed SHA-256: `2C6F89F28775702B8EB85C4855A786336EB21F800F1AC2B1F79F1CC8F2CADE61`.

The erroneous cache was moved, not deleted, to:

`J:\YZ\PJ\.minecraft\versions\1.20.1-Forge\saves\新的世界 (5)\voxy.backup-before-post-light-refresh-20260826-1528`

Runtime status: deployed and pending user verification. A successful run must
show bounded `Post-light Voxy chunk refresh ... accepted=true` records and
normal distant grass. If no refresh records occur while the probe still sees
zero skylight, the assumed ordering edge is disproven and this candidate must
not be declared fixed.

## 2026-08-26 final cleanup after visual success

The user reported that distant grass rendered normally in the next clean-data
run. However, `latest.log` contained no `Post-light Voxy chunk refresh` record,
while the bounded input probe still recorded zero skylight. The post-light
candidate therefore did not execute during the successful run and cannot be
credited as the fix.

In accordance with the engineering rules, the unexercised candidate was
removed completely: its Minecraft-target Mixin, reflective Voxy bridge,
pending-state collection, generated refmap requirement, and tests are no
longer in the stable patch. This also removes a possible session-length pending
state and avoids unnecessary compatibility surface.

The successful run followed creation of a new Voxy data directory. The visual
issue is accepted as currently absent, but causal attribution remains open
because earlier clean-data attempts had reproduced the black terrain. A future
recurrence must be investigated from fresh logs rather than restoring the
discarded timing candidate without evidence.

The grass-lighting probe remains available but now defaults to disabled
(`voxyCompat.lightProbeLogLimit=0`). It can be re-enabled with a positive log
limit if the symptom returns. Stable cleanup build:

- `clean test build`: passed;
- tests: 3 passed, 0 failed;
- packaged Sable/Voxy implementation or resource paths: none;
- removed candidate classes in jar: none present;
- two consecutive one-click builds produced the same SHA-256;
- deployed SHA-256: `6D0AC0295485567B721FFB6FEE7111886C2BC0E6E68386018C28F9E0FC101328`.

## 2026-08-26 recurrence and observed-input deferred retry

After deploying the cleaned stable jar without the unexercised packet-order
candidate, the user re-entered the same world and reported that the distant
grass was black again. The cache-only explanation is therefore disproven and
the issue remains an intermittent ingestion race.

The replacement candidate does not depend on predicting whether Minecraft's
light packet callback runs before or after Embeddium upload. At `rawIngest0`, it
registers only chunks where Voxy is actually about to ingest a grass block with
zero skylight. A bounded client-tick queue then reads the live client light
storage. Once it observes real non-zero skylight on grass, it invokes Voxy's
public `tryAutoIngestChunk(LevelChunk)` path. It never manufactures light,
runs the light engine, or changes Voxy conversion/rendering/cache formats.

Runtime bounds default to 1024 pending chunks, 64 examined entries per tick,
and 40 attempts per chunk. World changes clear the queue, unloaded chunks are
removed, and success/expiry logs are capped at 16. All bounds are configurable
with the `voxyCompat.lightRefresh*` JVM properties documented in the README.

Verification and deployment:

- two consecutive one-click builds produced the same jar;
- tests: 4 passed, 0 failed;
- packaged Sable/Voxy implementation or resource paths: none;
- candidate SHA-256: `41CAB65CBB4982996CD600BC89FB48A74614562FDCC78562BE3EEC2342FD4217`;
- the regressed cache was moved, not deleted, to
  `J:\YZ\PJ\.minecraft\versions\1.20.1-Forge\saves\新的世界 (5)\voxy.backup-before-deferred-light-refresh-20260826-1622`.

Status: deployed, runtime verification pending. A successful causal test must
contain bounded `Refreshed Voxy chunk ... after skylight became ready` entries
and normal distant grass. Visual success without those entries does not prove
this candidate.

## 2026-08-26 model-lighting boundary

Later bounded input records showed surface grass with sky light 0 at the solid
voxel itself and sky light 14-15 immediately above it in the same `DataLayer`.
That is valid Minecraft opaque-block lighting, so the raw light layer and its
delivery timing are not the defect.

Voxy's `Mipper` retains the selected non-air voxel's own light byte. Its final
face lighting therefore depends on the baked model metadata choosing the
opaque/adjacent-light path. The remaining compatibility question is why the
combined environment changes that classification when Voxy alone is normal.

The explicit `/voxycompat inspect` diagnostic reads the already-baked grass
model metadata and per-face lighting flags. It is read-only, uses no model
factory Mixin, performs no rebake or re-ingestion, and catches reflection
failures without affecting renderer state.

The runtime result was `fullyOpaque=false`, `translucent=true`,
`canonicalSolid=false`, with self-lighting enabled on all six faces. Inspection
of Connector's Fabric block-render-layer bridge then showed that its legacy
first-layer lookup bypasses the live `ChunkRenderTypeSet` iterator and indexes
Forge's frozen startup array. Aeronautics' late Veil layer reindexing had made
those two views disagree. The corrective candidate therefore belongs to
Simulated/Aeronautics: its legacy single-layer compatibility now derives the
first layer from the live Forge set. No Voxy lighting or model behavior is
changed by that candidate.

## 2026-08-26 final attribution and production cleanup

The user confirmed that distant grass now renders normally and that gears
remain visible. The immediately preceding apparent recurrence was only a
missed `/voxycompat inspect` command, not a visual regression. Because the
command was not run in the successful session, no final metadata output is
claimed beyond the visual acceptance result.

The long investigation is attributed to an earlier overfit compatibility fix.
Aeronautics' live `ChunkRenderTypeSet#iterator()` overwrite had been accepted
after it restored one gear-rendering case. That validation covered an old bit
0 set and one iterator consumer, but did not map multiple cached set
generations or Connector's direct lookup through the startup-frozen chunk
layer array. Forge's shared default `SOLID` set retained old bit 3, so a single
current-ID interpretation could not preserve both cases. Treating the first
successful visual sample as a complete render-layer contract violated the
project rules for dependency closure, consumer mapping, and upstream semantic
recovery.

The accepted Simulated fix restores the source priority of the legacy
single-layer API: vanilla registrations remain vanilla, ordinary blocks with
no explicit Forge registration return canonical solid, explicit mod layer
sets use the live iterator, and leaves/moving blocks retain their existing
special handling. It contains no Voxy, grass, or lighting special case.

All black-grass investigation facilities have now been removed from this
independent patch: the `/voxycompat` commands, forced/full/live skylight modes,
loaded-chunk refresh bridge, model inspector, grass light summaries, and the
diagnostic ingestion Mixin. Production contracts prevent these facilities
from returning. The patch keeps only the Oculus pipeline lifecycle repair and
Sable plot-ingestion isolation; the latter retains a bounded configurable
filter log but performs no grass scan or light read.

Verification:

- full `clean test build`: passed;
- production implementation classes: 4;
- diagnostic classes and command registration in jar: none;
- packaged Sable/Voxy classes or assets/data: none;
- cleaned patch SHA-256:
  `AF1BAF120736890C5D560AE23C7A1DC8A3FD57902000E303241D41C7F47E8319`;
- deployed backup:
  `J:\Voxy-Compatibility-Patch-1.20.1\artifacts\pj-deploy-backups\20260826-203804`.

Historical diagnostics remain in this document as audit evidence, not in the
runtime jar.
