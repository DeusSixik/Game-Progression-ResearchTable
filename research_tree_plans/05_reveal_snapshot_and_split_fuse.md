# Plan 05. Reveal Snapshot And Split Fuse

## Goal

Prepare a future rendering pipeline for advanced research reveal effects:

- under-node glow during reveal
- snapshot-based reveal rendering
- split/fuse animation driven by UV slicing
- optional shader seam and FBO-based post effects later

This file is a design plan only.
Actual implementation should start after the glow effect is finished and validated.

---

## Why this plan exists

Current reveal animation is based mostly on:

- camera motion
- whole-widget transform
- small accent transforms inside the node widget

That is enough for simple reveal styles, but it is not a good base for:

- a light source under the node
- a "node assembles from two halves" effect
- shader-driven seam/fuse transitions
- future texture-based reveal variants

For those effects, it is better to treat reveal as a separate render layer instead of trying to animate live UI children.

---

## Important design decision

For advanced reveal effects, the node should be considered visually frozen during the reveal.

That means:

- the reveal animation happens only once
- camera pan/zoom does not matter for the snapshot content itself
- the snapshot is rendered in node-local space
- world placement is applied only when drawing the reveal effect

So for this feature, a texture snapshot is a valid and desirable approach.

---

## Main idea

Use a dedicated reveal rendering path:

1. a node becomes available
2. a visual snapshot of that node is created once
3. the normal live widget is hidden or ignored during reveal
4. the reveal effect renders from the snapshot texture
5. after the animation ends, the normal widget becomes visible again

This separates:

- gameplay state
- final UI widget
- cinematic reveal presentation

---

## What should happen first

Before snapshot-based split/fuse work starts, implement and validate:

### Phase 0: Under Glow

Add a proper glow layer under the reveal node.

Reason:

- it is useful on its own
- it prepares the reveal pipeline for extra effects
- it helps verify render ordering around animated reveal nodes

Only after glow is stable should snapshot rendering be started.

---

## High-level architecture

### 1. Reveal Snapshot

One object stores a rendered texture for a node:

- texture handle / render target
- snapshot width and height
- node key or node id
- optional style/version metadata

Suggested class:

- `ResearchNodeRevealSnapshot`

### 2. Snapshot Manager

Responsible for:

- creating snapshot textures
- caching them
- invalidating them when needed
- disposing them when no longer needed

Suggested class:

- `ResearchNodeRevealSnapshotManager`

### 3. Reveal Renderer

Dedicated interface for advanced reveal presentation.

Suggested interface:

- `ResearchNodeRevealRenderer`

Responsibilities:

- render a reveal from snapshot data
- work in graph world coordinates
- accept progress `0..1`

### 4. Reveal Renderer Resolver

Maps a node or reveal style to a reveal renderer.

Suggested class/interface:

- `ResearchNodeRevealRendererResolver`

This makes it possible to support:

- default reveal
- glow reveal
- split/fuse reveal
- future shader-based reveal

---

## Snapshot strategy

### Preferred strategy

Create a snapshot on demand only for the node that is currently being revealed.

Why this is preferred:

- less memory usage
- less setup work on screen open
- no need to build a large atlas up front
- snapshot always matches the current final node look

### Optional future strategy

Pre-bake all revealable nodes into one atlas on screen initialization.

This is possible, but should be treated as an optimization experiment only if needed later.

Risks:

- more bookkeeping
- UV packing complexity
- harder invalidation rules
- less flexible when styles change

Current recommendation:

- do not start with atlas packing
- start with one snapshot per currently animated node

---

## Which UI draw method to use

Important note:

- `drawContents(...)` is not enough for a full node snapshot

Why:

- it renders contents and children
- but it does not include the full background/overlay pass by itself

For a true "what the player sees" snapshot, use a fuller rendering path closer to:

- `drawInBackgroundInternal(...)`

Or, if needed later, build a dedicated node card painter that can render both:

- live widget visuals
- offscreen snapshot visuals

Current recommendation:

- do not base snapshot generation on `drawContents(...)` alone

---

## Render flow target

Desired future reveal flow:

1. node unlock starts
2. reveal snapshot is requested
3. snapshot is rendered once
4. normal node widget is hidden for reveal duration
5. reveal renderer draws:
   - under glow
   - split halves from UVs
   - seam/fuse effect
   - optional flash
6. reveal ends
7. live widget becomes visible again
8. snapshot is released or cached

---

## Split Fuse effect plan

### Visual target

The node appears as if it is assembled from two parts:

- left/right split, or
- top/bottom split later if desired

Base version:

- the snapshot texture is sliced into two UV regions
- the two halves move toward the center
- a seam glow appears where they meet
- final flash finishes the assembly

### First implementation target

Do not start with shader-heavy logic.

Start with:

- UV slicing
- two textured quads
- simple seam glow
- optional alpha/brightness pulse

This gives most of the intended visual result with much lower complexity.

### Later enhancement

After the basic split/fuse works:

- add a custom shader
- distort seam edges
- add dissolve, heat, or electric fusion look

---

## Suggested classes

Potential future files:

- `research_tree/reveal/ResearchNodeRevealSnapshot.java`
- `research_tree/reveal/ResearchNodeRevealSnapshotManager.java`
- `research_tree/reveal/ResearchNodeRevealRenderer.java`
- `research_tree/reveal/ResearchNodeRevealRendererResolver.java`
- `research_tree/reveal/SplitFuseRevealRenderer.java`
- `research_tree/reveal/UnderGlowRevealRenderer.java`

Optional later helper:

- `research_tree/node_widgets/ResearchNodeCardPainter.java`

The painter helper is useful if snapshot generation needs a dedicated stable drawing path.

---

## Integration points

### In `ResearchTreeScreen`

This class should remain the owner of:

- unlock timing
- active reveal node
- camera lock during animation
- reveal progress

It should also become the place that triggers:

- glow rendering
- reveal renderer invocation
- reveal widget hide/show lifecycle

### In `ResearchTreeScreenMainScreen`

This layer can own:

- reveal renderer resolver hooks
- node-theme-aware reveal selection
- project-specific reveal presentation rules

---

## Required hooks

Future useful hooks:

- `onRevealSnapshotCreated(...)`
- `onRevealSnapshotReleased(...)`
- `resolveRevealRenderer(...)`
- `shouldHideLiveWidgetDuringReveal(...)`
- `onRevealEffectStart(...)`
- `onRevealEffectEnd(...)`

These hooks are not required yet, but planning them now will make the implementation cleaner.

---

## Risks and unknowns

### 1. Offscreen rendering details

Need to verify:

- how cleanly a node widget can be rendered to an offscreen target
- whether LDLib visual layers or opacity affect snapshot generation
- whether temporary scissor/pose state needs manual setup

### 2. GUI scale changes

Need a rule for snapshot invalidation if:

- GUI scale changes
- node theme changes
- debug widget style changes

### 3. Resource lifecycle

Need safe cleanup for:

- closed screen
- interrupted reveal
- node removed while reveal is active

---

## Recommended implementation order

### Stage 1

Implement only under-node glow.

### Stage 2

Add reveal renderer API and reveal-layer plumbing.

### Stage 3

Add on-demand snapshot generation for the currently animated node.

### Stage 4

Implement basic split/fuse from snapshot UV slicing.

### Stage 5

Experiment with shader seam and optional FBO/post processing improvements.

---

## Definition of done for the future Split Fuse task

The feature can be considered complete when:

- reveal node can be rendered from snapshot instead of live widget
- live widget can be hidden during reveal and restored after
- split/fuse animation works without content snapping
- seam glow is visible and stable
- effect respects graph camera transforms
- resources are properly cleaned up

---

## Current decision

For now:

- this is a postponed follow-up task
- next actual implementation target is the under-node glow reveal effect

Only after glow is finished should snapshot-based split/fuse work begin.
