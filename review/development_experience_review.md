# Development Experience Review

Date: 2026-06-05
Project: GameProgressionResearchTable
Scope: current research-tree / research-table client architecture, content authoring API, debug tooling, and UI iteration ergonomics.

## Executive Summary

Current development experience is already solid for the main developer and good for fast UI experimentation, but it is not yet fully ergonomic for a new contributor or for external content authors.

High-level scorecard:
- Core architecture: 7.5/10
- UI experimentation speed: 8/10
- Content authoring ergonomics: 5.5/10
- Discoverability for a new contributor: 6/10
- Long-term maintainability trajectory: 8/10

The biggest positive is that the project has moved from "screen-specific hardcoded logic" toward layered systems:
- screen shell and progression orchestration
- reusable info-panel content model
- node widget factory / theme / wrapper separation
- builder-based public research API
- dedicated debug/demo datasets

The main bottleneck now is no longer rendering capability. The main bottleneck is developer ergonomics: many things are possible, but not always obvious or compact to configure.

## What Already Feels Good

### 1. Strong reusable screen shell
Relevant files:
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/research_tree/ResearchTreeScreenMainScreen.java`
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/research_table/ResearchTableScreen.java`
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/demo/ResearchTreeScreenDebug.java`

Why this is good:
- `ResearchTreeScreenMainScreen` now acts as a reusable runtime shell instead of forcing every feature into one screen class.
- Common features already live in one place: panel open/close flow, realtime refresh, unlock animation sequencing, progress synchronization, node refresh wiring.
- Specialized screens (`ResearchTableScreen`, `ResearchTreeScreenDebug`) can override only the parts they actually care about.

DX value:
- Reduces repeated boilerplate.
- Makes experimentation cheaper because the "engine" layer is already present.
- Makes future screen variants realistic.

### 2. Info panel moved to data-first design
Relevant files:
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/research_tree/info/ResearchInfoContent.java`
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/research_tree/info/ResearchInfoContentFactory.java`
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/research_tree/info/ResearchInfoContentPresets.java`
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/research_tree/info/ResearchInfoPanel.java`
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/research_table/info/TableInfoPanelWidget.java`
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/demo/DebugCustomInfoPanel.java`

Why this is good:
- Panel widgets consume a snapshot model instead of rebuilding gameplay logic during render.
- Different panel skins can reuse the same content.
- Builder/preset flow is already flexible enough for standard content plus custom sections.

DX value:
- New UI ideas can be tested by changing content composition instead of rewriting panel internals.
- Safer to add new sections without destabilizing unrelated UI.

### 3. Node rendering is becoming system-based instead of hardcoded
Relevant files:
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/research_tree/node_widgets/DefaultResearchNodeWidgetFactory.java`
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/research_table/widget_factory/ResearchTableNodeWidgetFactory.java`
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/demo/DebugResearchNodeWidgetShowcase.java`
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/research_tree/node_widgets/ResearchNodeTheme.java`
- `src/main/java/dev/sixik/gprt/impl/client/research_screen/research_tree/node_widgets/ResearchNodeGroupThemeResolver.java`

Why this is good:
- Widget creation, theming, and wrapper/layout influence have been separated enough to support multiple visual styles.
- Debug showcase gives concrete examples instead of only abstract APIs.

DX value:
- Changing visual style no longer implies rewriting the entire node implementation.
- Makes style iteration much faster.

### 4. Public research API exists and is already meaningful
Relevant files:
- `src/main/java/dev/sixik/gprt/api/ResearchDefinition.java`
- `src/main/java/dev/sixik/gprt/api/ResearchGroupDefinition.java`

Why this is good:
- There is a real addon-facing builder layer instead of forcing users into `impl` classes.
- Conditions, rewards, visibility, study type, reveal animation, icons, groups are already representable.

DX value:
- This is the foundation required for future CraftTweaker/KubeJS integration.
- It gives the project a stable authoring surface.

### 5. Test/demo data is useful instead of trivial
Relevant files:
- `src/main/java/dev/sixik/gprt/test/GprtTests.java`

Why this is good:
- You already have separate datasets for:
  - reveal animation showcase
  - larger debug progression trees
- This dramatically improves UI iteration speed.

DX value:
- Makes regressions easier to notice.
- Lets you validate unlock sequences and style changes against realistic trees.

## What Still Feels Uncomfortable

### 1. Too much configuration still lives in screen overrides
Examples:
- `ResearchTableScreen.buildInfoContent(...)`
- `ResearchTreeScreenDebug.buildInfoContent(...)`
- style/demo-specific behavior still leaks into screen classes

Why it hurts:
- A lot of "what should this panel/node look like" is still controlled by imperative overrides.
- It works, but it scales poorly as more special cases appear.

Developer pain:
- A new contributor must inspect the screen class to understand behavior.
- Reuse is possible, but often manual.

### 2. UI customization is powerful, but discoverability is only moderate
Examples:
- timed progress style now exists through `TimedProgressVisualStyle`
- info-panel customization exists through `ResearchInfoContent`
- custom panel skins exist through custom `ResearchInfoPanelWidget`

Why it hurts:
- The capability is there, but the shortest path is not always obvious.
- You often need prior project knowledge to know whether to modify:
  - content builder
  - content preset
  - panel widget
  - screen override
  - theme resolver
  - widget factory

Developer pain:
- "Where should I make this change?" is still a common question.

### 3. Content-authoring and UI-authoring layers are not fully separated yet
Examples:
- public API is content-focused, which is good
- but many UI decisions still need Java-side screen overrides

Why it hurts:
- For modpack-style authoring, people should ideally be able to declare data and maybe pick presets, without needing client code changes.
- Right now that part is only partially ready.

Developer pain:
- Data authors still depend too much on screen implementations for advanced presentation.

### 4. Demo/debug UX is strong, but not yet unified into one "developer toolbox"
Examples:
- `ResearchTreeScreenDebug`
- `DebugResearchNodeWidgetShowcase`
- `GprtTests`

Why it hurts:
- These parts are useful, but they are distributed across multiple classes and patterns.
- There is not yet one central place that says:
  - test node styles here
  - test progress styles here
  - test reveal animations here
  - test content presets here

Developer pain:
- Tooling exists, but the workflow is not fully centralized.

### 5. Some APIs are still "mechanically correct" instead of "pleasant"
Examples:
- timed progress styling works, but is configured via content builder and helper knowledge
- some UI variations are still easiest to do by custom overriding rather than selecting a preset

Why it hurts:
- The system is technically flexible, but not always ergonomic.
- A good DX API should make the common path obvious and the advanced path possible.

## Where the Project Stands Right Now

The project is past the chaotic stage.
That matters a lot.

It is no longer in the state of:
- one huge screen class
- lots of hidden one-off hacks
- impossible-to-reuse UI code

It is now in the stage of:
- systems exist
- extension points exist
- but ergonomics and author-facing polish still need work

This is a very healthy stage.
The hardest architectural transition is already behind you.

## Three Refactorings With the Highest DX Payoff

### Priority 1: Create a declarative info-panel style preset layer
Goal:
- Stop treating every panel variation as a custom widget override or a one-off screen edit.

What to add:
- A dedicated preset/config layer for panel visuals, separate from content.
- Example concept:
  - title style preset
  - meta style preset
  - timed block style preset
  - section card style preset
  - reward/condition row style preset
  - footer/button preset

Possible shape:
- `ResearchInfoPanelTheme`
- `ResearchInfoPanelThemeResolver`
- `ResearchInfoPanelThemePresets`

Why it has the biggest payoff:
- Most future UI iteration seems to be panel-focused.
- You already have content/data separation; this is the natural next layer.
- It would reduce the need for subclassing just to change look-and-feel.

Expected DX improvement:
- Main developer: high
- New contributor: very high
- Content author: medium-high (if preset picking becomes externally configurable later)

### Priority 2: Centralize all debug/showcase flows into one structured developer playground
Goal:
- Turn the current debug ecosystem into a real internal toolkit.

What to unify:
- node style showcase
- reveal animation showcase
- timed progress style showcase
- panel content showcase
- test data presets
- unlock flow playback

Possible shape:
- `ResearchDevSandboxScreen`
- or a better-structured expansion of `ResearchTreeScreenDebug`

Why it has high payoff:
- Faster iteration means faster decisions and less breakage.
- UI-heavy systems benefit massively from a stable sandbox.

Expected DX improvement:
- Main developer: very high
- New contributor: high

### Priority 3: Add a small author-facing preset API on top of the public research builder
Goal:
- Make common customization paths declarative for content authors.

Examples:
- choose a reveal animation preset directly in builder (already done)
- choose a panel presentation preset
- choose a node widget visual preset id
- choose a timed progress style preset id
- choose whether unlock/reward sections are auto-generated or hidden

Possible shape:
- `presentationPreset("industrial")`
- `timedProgressStyle("dynamic_text")`
- `panelPreset("table_research")`
- `nodeVisualPreset("tech_card")`

Why it has high payoff:
- It closes the gap between powerful runtime systems and usable author workflows.
- It is especially valuable once external scripting integration begins.

Expected DX improvement:
- Main developer: medium
- Content author: very high
- Future addon ecosystem: very high

## Suggested Near-Term Roadmap

### Phase A - ergonomics
1. Finish panel visual preset layer.
2. Normalize timed-progress customization through that layer.
3. Reduce ad-hoc panel styling in screen classes.

### Phase B - tooling
1. Expand debug screen into a unified dev sandbox.
2. Add quick toggles for preset/data/style combinations.
3. Add one-click sample states for timed/table/unlock scenarios.

### Phase C - author-facing API
1. Expose common presentation choices as declarative preset ids.
2. Keep advanced Java overrides possible, but make them optional.
3. Document recommended usage paths clearly.

## Final Assessment

If the question is "are we at the point where development is comfortable?" then the answer is yes for the core developer and mostly yes for advanced contributors.

If the question is "are we at the point where the system is pleasant and obvious for outsiders?" then not yet.

Current state in one sentence:
- the technical foundation is already good, and the next major gains come from ergonomics, presets, and tooling rather than from raw rendering capability.

## Short Practical Verdict

What is already convenient:
- building and testing tree logic
- experimenting with node visuals
- adding panel sections
- adding reveal behaviors
- wiring timed/table/instant studies

What is still inconvenient:
- knowing the best extension point for a change
- making visual changes without touching screen logic
- exposing these systems cleanly to future content authors

Recommended next focus:
- prioritize a panel-theme/preset layer first, because it gives the largest development-experience win for the least conceptual risk.
