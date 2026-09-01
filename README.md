# Voxy Compatibility Patch for Minecraft 1.20.1

A narrowly scoped client-side compatibility patch for:

- Minecraft 1.20.1
- Forge 47.x
- Voxy 0.2.13-alpha 1.20.1 Port
- Oculus 1.8.x (optional)
- Sable 2.0.5 1.20.1 Port (optional)

## Fixed issue

When Oculus destroys and recreates its shader pipeline while changing singleplayer worlds, Voxy 0.2.13-alpha can retain sampler suppliers backed by the destroyed render targets. Rendering the second world then crashes with `Tried to use destroyed RenderTargets`.

The patch observes the actual Oculus `destroyPipeline` event and requests exactly one standard Minecraft level-renderer rebuild after the replacement pipeline has been prepared. Ordinary per-frame calls to `preparePipeline` do not trigger a rebuild.

This project does not modify Voxy's source code, cancel rendering, suppress exceptions, or disable any feature.

Voxy 0.2.13 also finishes its opaque LOD pass with `glUseProgram(0)` instead of
restoring the OpenGL program that was active when it entered. Oculus caches the
last shader object it applied, so that raw unbind can leave the Java cache and
the real OpenGL state inconsistent. The next uniform upload can then produce a
high-volume `No active program` error flood. The patch captures the actual
entry program once per Voxy pass and restores it at Voxy's existing cleanup
call. It does not suppress OpenGL diagnostics or skip uniform uploads.

As a circuit breaker, the patch also rate-limits only the exact NVIDIA
`No active program` debug text after preserving the first message in each
10-second window. It emits a suppression count for a continuing flood. Other
OpenGL errors, including framebuffer depth-format errors that share error id
1282, remain untouched. This guard protects the log and render thread if a
second renderer violates the same state boundary; it is not presented as a
substitute for the program restoration above.

Voxy 0.2.13 also clears texture units 0 through 11 after its opaque LOD pass.
Flywheel instancing expects the surrounding level renderer to leave the block
atlas bound on texture unit 0, so animated Create parts could disappear when
Voxy rendered first. The patch restores only that atlas at the Voxy render
boundary when Flywheel is present, without changing either renderer's geometry,
matrices, depth behavior, or feature selection.

When Sable stores moving physical structures in its reserved plot area inside
the client level, Voxy can otherwise ingest those hidden chunks as ordinary
world LOD data. The patch asks Sable's public `SubLevelContainer.inBounds`
boundary before Voxy ingestion and excludes only chunks owned by that reserved
area. This prevents hidden physical-storage geometry and its lighting from
polluting the normal Voxy world database.

The Sable integration is reflective and optional. No Sable source, compiled
class, resource, coordinate rule, or other implementation is copied into this
project or its jar.

## Per-world Voxy activation

Voxy 0.2.13 can occasionally load with both `enabled` and `enableRendering`
stored as `true` while its world instance or renderer was never created. Merely
rewriting those booleans does not replay Voxy's runtime lifecycle and therefore
does not make LOD rendering start.

While a real client world is open, this patch checks at a low frequency (once
every 40 client ticks) whether Voxy is available, its client session exists,
and both the world instance and renderer are active. If either runtime object is
missing, it invokes Voxy's own two settings-page activation paths in their
normal order. It then verifies the actual instance and renderer, saves the two
settings, shows an in-game confirmation, and stops polling for that world.

No activation polling runs on the title screen. Leaving or switching worlds
resets the one-shot state for the next world. The patch does not bypass Voxy's
availability check or replace its renderer creation logic.

## Build

On Windows, double-click `build.cmd`. The finished jar is written to `dist`.

On other systems, run `./gradlew clean build`. The jar is written to `build/libs`.
