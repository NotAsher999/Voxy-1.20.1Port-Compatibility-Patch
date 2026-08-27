# Voxy + Flywheel instancing texture boundary repair

## User-visible failure

With Voxy enabled, Flywheel instancing consistently failed to render animated
block parts such as Create gears, bearing faces, and lever handles. Disabling
Voxy restored instancing, and disabling Flywheel instancing restored the parts.
The failure occurred with and without an Oculus shader pack.

## Source-level root cause

Voxy 0.2.13 runs its opaque LOD pipeline at the end of Embeddium's solid chunk
layer. Before returning, `VoxyRenderSystem.renderOpaque` intentionally clears
texture units 0 through 11. Flywheel's instancing path subsequently binds its
overlay, light, and instance-buffer inputs but inherits the diffuse block atlas
on texture unit 0 from the surrounding level renderer. The inherited input is
therefore missing only when Voxy runs first.

Flywheel's fragment pipeline samples `flw_diffuseTex` from texture unit 0 and
applies the material cutout predicate. A missing block atlas accounts for the
previously captured state: valid managers, instances, meshes, matrices, and draw
submission, but no surviving samples.

## Repair boundary

`VoxyRenderSystemTextureBoundaryMixin` runs after the complete Voxy opaque
pipeline returns. When Flywheel is loaded, it re-establishes only the Minecraft
block atlas on texture unit 0 and restores the previously active texture unit.
It does not alter Voxy geometry, depth composition, LOD selection, matrices, or
Flywheel instance data. The repair remains in the independent Voxy compatibility
patch and contains no Sable code or resources.

## Validation state

- Source and bytecode cause: confirmed against the deployed Voxy 0.2.13 jar and
  bundled Flywheel 1.0.5 jar.
- Contract tests: required before deployment.
- Runtime behavior: confirmed in PJ with Voxy enabled and Flywheel set to
  Instancing. The previously missing animated parts render normally again.
