# Voxy + Oculus OpenGL program boundary repair

## User-visible failure

With Voxy, Oculus, and Sable rendering together, the OpenGL debug callback can
emit `No active program` thousands of times per second. The flood consumes CPU
in driver validation and debug-stack processing and can grow the client log by
multiple gigabytes per hour.

## Root cause

Voxy 0.2.13's `VoxyRenderSystem.renderOpaque` saves and restores framebuffer
and viewport state, but its normal cleanup ends with a direct
`GL30C.glUseProgram(0)`. Oculus `ExtendedShader` separately caches its last
applied Java shader object. When Voxy clears the real program without updating
that cache, a later shader application can skip rebinding and upload uniforms
while `GL_CURRENT_PROGRAM` is zero.

Sable exposes the defect at high volume because its sub-level section renderer
updates several uniforms for each layer and section. The first captured native
call site was an Oculus matrix uniform upload reached from Sable's sub-level
render dispatcher. Sable is therefore a multiplier and caller at the failure
site, while Voxy's incomplete render-state restoration is the boundary defect
fixed here.

## Repair boundary

`VoxyRenderSystemTextureBoundaryMixin` reads the actual
`GL_CURRENT_PROGRAM` at `renderOpaque` entry. It redirects Voxy's single final
`GL30C.glUseProgram(0)` call to restore that captured program instead. A no-op
redirect would be incorrect because it would leave Voxy's own program active.

The existing Sable/Oculus recovery remains useful as a guardrail for other
renderers, but it is not used as the primary repair. The patch does not filter
all OpenGL diagnostics, enable a no-error context, cancel rendering, or swallow
an exception.

## Flood circuit breaker

The deployed environment also contains renderers other than Voxy that can bind
program zero without synchronizing Minecraft and Oculus shader caches. Until
all of those independent boundaries have runtime coverage, a narrow circuit
breaker prevents another multi-gigabyte log incident:

- only error id 1282 with the exact message
  `GL_INVALID_OPERATION error generated. No active program.` is eligible;
- the first message in every 10-second window follows Minecraft's normal path;
- continued copies are counted and replaced by one summary at the next window;
- depth-format and every other OpenGL diagnostic remain unchanged;
- no uniform upload or rendering call is skipped by the guard itself.

This removes Minecraft's queue/log work and Voxy's per-message throwable and
stack walk for suppressed copies. The driver callback and invalid GL call still
exist, so any non-zero suppression count means the root repair remains
incomplete and must be investigated.

## Verification boundary

Static and build verification require:

- an entry capture of `GL_CURRENT_PROGRAM`;
- one redirect of Voxy's `GL30C.glUseProgram(int)` cleanup call;
- restoration of the captured value rather than a hard-coded program;
- successful unit tests, production build, and packaged Mixin audit.

Runtime acceptance still requires the target instance with Voxy rendering and
Oculus shaders enabled. Exercise Sable single-block and chunked sub-levels,
shader reload/toggle, world or dimension changes, and window resize. The log
must remain free of a sustained `No active program` flood.

`Depth formats do not match` is tracked separately. It occurs during a
framebuffer depth/stencil copy and has reproduced independently of the program
cache failure, so it must not be hidden or claimed fixed by this boundary.
