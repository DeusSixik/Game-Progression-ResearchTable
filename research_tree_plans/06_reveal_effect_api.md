# Reveal Effect API Plan

## Goal

Build a reusable reveal-animation pipeline for research nodes so unlock visuals are no longer hardcoded inside `ResearchTreeScreen` or tied to a single `SPLIT_FUSE` implementation.

The new API should let us plug in different reveal styles later, for example:
- split/fuse assembly,
- pixel materialization,
- dissolve/glitch,
- hologram scanlines,
- shard-based assembly from fragments.

## Core Design

### 1. Snapshot Layer

A reveal animation should work from a captured node snapshot instead of re-reading live widget state every frame.

Planned classes:
- `RevealSnapshot`
- `RevealSnapshotCaptureService`

Responsibilities:
- capture widget visuals into a reusable texture-backed snapshot,
- keep texture ownership/lifecycle in one place,
- support both retained GPU target and texture-manager fallback,
- expose width/height/texture id to all effects.

### 2. Effect API

Animations should be defined as effect instances with a simple lifecycle.

Planned interfaces:
- `ResearchRevealEffect`
- `RevealEffectInstance`

Lifecycle:
- `createInstance(initContext)` creates per-animation state,
- `render(renderContext)` draws the current frame,
- `dispose()` frees resources owned by the effect instance.

This lets one effect precompute tiles/fragments/seeds at start and reuse them across frames.

### 3. Context Objects

Effects should not reach back into `ResearchTreeScreen` directly.

Planned context objects:
- `RevealInitContext`
- `RevealRenderContext`

They should expose:
- node,
- visual definition,
- snapshot,
- current progress,
- target render bounds,
- backend helpers for textured draws / offscreen targets.

### 4. Backend / Render Utilities

Complex effects will need shared helpers for offscreen rendering and target reuse.

Planned classes:
- `RevealRenderBackend`
- `RevealTargetPool` or equivalent managed target reuse

Responsibilities:
- acquire/release temporary render targets,
- draw textured quads,
- render into offscreen targets,
- keep low-level GL/FBO code out of individual effects as much as possible.

## Migration Steps

### Phase 1 - Foundation
- add plan file,
- add base interfaces and data classes,
- keep implementation small and focused,
- do not change external screen behavior yet.

### Phase 2 - First Effect Migration
- adapt current `SplitFuseRevealRenderer` into the first `ResearchRevealEffect` implementation,
- keep the currently working legacy draw path as the actual visual behavior,
- preserve current snapshot capture behavior.

### Phase 3 - Screen Integration
- replace direct `splitFuseRevealRenderer` orchestration in `ResearchTreeScreen` with a generic active reveal instance pipeline,
- let the screen ask a resolver/factory which effect to use for the node,
- keep existing `ResearchRevealAnimationStyle` as the high-level style id.

### Phase 4 - Future Extensions
After migration we can add:
- `PixelMaterializeRevealEffect`,
- `ShardAssemblyRevealEffect`,
- shader-based composite passes,
- debug switching between reveal effects in real time.

## Constraints

- Do not regress the currently working reveal animation.
- Keep cleanup explicit and safe.
- Preserve snapshot fallback behavior.
- Keep the first pass simple; avoid overengineering until `SPLIT_FUSE` is successfully migrated.
