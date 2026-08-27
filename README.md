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

## Build

On Windows, double-click `build.cmd`. The finished jar is written to `dist`.

On other systems, run `./gradlew clean build`. The jar is written to `build/libs`.
