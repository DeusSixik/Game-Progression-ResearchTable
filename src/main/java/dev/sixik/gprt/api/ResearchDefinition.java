package dev.sixik.gprt.api;

import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Public immutable research-definition API.
 * <p>
 * This is the addon-facing counterpart of the internal research definition model. It gives
 * external code a stable place to describe one research entry without depending directly on the
 * current classes under {@code impl}.
 * </p>
 *
 * <p><b>Typical usage:</b></p>
 * <pre>{@code
 * ResearchDefinition definition = Researches.register("gunpowder")
 *         .title("Gunpowder")
 *         .description("An unstable powder with violent potential.")
 *         .required("chemistry")
 *         .condition("powder_like_material")
 *         .rewardResearch("stage_gunpowder")
 *         .table()
 *         .build();
 * }</pre>
 *
 * <p>
 * The definition intentionally stores only declarative data: dependencies, conditions, rewards
 * and study mode. Rendering and runtime logic are expected to consume this data later.
 * </p>
 */
public final class ResearchDefinition {
    private final String key;
    private final String groupId;
    private final ResearchVisibilityMode visibilityMode;
    private final String title;
    private final String description;
    private final @Nullable Object iconTexture;
    private final ResearchStudyType studyType;
    private final ResearchRevealAnimationType revealAnimationType;
    private final long studyDurationMs;
    private final List<String> requiredResearches;
    private final List<ResearchCondition> conditions;
    private final List<ResearchReward> rewards;

    private ResearchDefinition(String key,
                               String groupId,
                               ResearchVisibilityMode visibilityMode,
                               String title,
                               String description,
                               @Nullable Object iconTexture,
                               ResearchStudyType studyType,
                               ResearchRevealAnimationType revealAnimationType,
                               long studyDurationMs,
                               List<String> requiredResearches,
                               List<ResearchCondition> conditions,
                               List<ResearchReward> rewards
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.groupId = groupId == null || groupId.isBlank() ? null : groupId;
        this.visibilityMode = visibilityMode == null ? ResearchVisibilityMode.ALWAYS_VISIBLE : visibilityMode;
        this.title = title == null || title.isBlank() ? key : title;
        this.description = description == null ? "" : description;
        this.iconTexture = iconTexture;
        this.studyType = studyType == null ? ResearchStudyType.INSTANT : studyType;
        this.revealAnimationType = revealAnimationType;
        this.studyDurationMs = Math.max(0L, studyDurationMs);
        this.requiredResearches = List.copyOf(requiredResearches == null ? List.of() : requiredResearches);
        this.conditions = List.copyOf(conditions == null ? List.of() : conditions);
        this.rewards = List.copyOf(rewards == null ? List.of() : rewards);
    }

    /**
     * Creates a new builder for one research definition identified by the given key.
     */
    public static Builder builder(String key) {
        return new Builder(key);
    }

    /**
     * Alias for script/addon-style usage.
     */
    public static Builder register(String key) {
        return builder(key);
    }

    public String getKey() {
        return key;
    }

    /**
     * Returns the optional logical group id assigned to this research.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Returns how this research should become visible relative to its parents.
     */
    public ResearchVisibilityMode getVisibilityMode() {
        return visibilityMode;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public @Nullable Object getIconTexture() {
        return iconTexture;
    }

    public ResearchStudyType getStudyType() {
        return studyType;
    }

    /**
     * Returns the optional per-research reveal animation override.
     * <p>
     * When {@code null}, screens may fall back to group presets or their own default reveal
     * behavior.
     * </p>
     */
    public ResearchRevealAnimationType getRevealAnimationType() {
        return revealAnimationType;
    }

    public long getStudyDurationMs() {
        return studyDurationMs;
    }

    public List<String> getRequiredResearches() {
        return requiredResearches;
    }

    public List<ResearchCondition> getConditions() {
        return conditions;
    }

    public List<ResearchReward> getRewards() {
        return rewards;
    }

    public boolean isInstant() {
        return studyType == ResearchStudyType.INSTANT;
    }

    public boolean isTimed() {
        return studyType == ResearchStudyType.TIMED;
    }

    public boolean isTable() {
        return studyType == ResearchStudyType.TABLE;
    }

    /**
     * Converts this public API definition into the current internal runtime definition type.
     * <p>
     * Extra declarative fields such as conditions/rewards are preserved only on this public API
     * object for now. The current internal runtime definition still consumes the base metadata.
     * </p>
     */
    public dev.sixik.gprt.impl.client.research_screen.research_tree.definition.ResearchDefinition toInternalDefinition() {
        return new dev.sixik.gprt.impl.client.research_screen.research_tree.definition.ResearchDefinition(
                key,
                title,
                description,
                iconTexture,
                studyType.toInternalType(),
                revealAnimationType == null ? null : revealAnimationType.toInternalType(),
                studyDurationMs
        );
    }

    /**
     * Fluent builder for addon-facing research definitions.
     * <p>
     * The builder is meant to be the public DSL used by integrations such as KubeJS and
     * CraftTweaker. It describes research data only; it does not depend on any screen/render code.
     * </p>
     */
    public static final class Builder {
        private final String key;
        private String groupId;
        private ResearchVisibilityMode visibilityMode = ResearchVisibilityMode.ALWAYS_VISIBLE;
        private String title;
        private String description;
        private @Nullable Object iconTexture;
        private ResearchStudyType studyType = ResearchStudyType.INSTANT;
        private ResearchRevealAnimationType revealAnimationType;
        private long studyDurationMs;
        private final List<String> requiredResearches = new ArrayList<>();
        private final List<ResearchCondition> conditions = new ArrayList<>();
        private final List<ResearchReward> rewards = new ArrayList<>();

        private Builder(String key) {
            this.key = Objects.requireNonNull(key, "key");
        }

        /**
         * Sets the display title shown in trees, tooltips and panels.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Assigns this research to a previously declared logical group id.
         */
        public Builder group(String groupId) {
            this.groupId = groupId == null || groupId.isBlank() ? null : groupId;
            return this;
        }

        /**
         * Assigns this research to the provided public group definition.
         */
        public Builder group(ResearchGroupDefinition groupDefinition) {
            return group(groupDefinition == null ? null : groupDefinition.getId());
        }

        /**
         * Controls when this research node becomes visible relative to its parents.
         */
        public Builder visibility(ResearchVisibilityMode visibilityMode) {
            if (visibilityMode != null) {
                this.visibilityMode = visibilityMode;
            }
            return this;
        }

        /**
         * Sets the descriptive text shown in information panels and other UI.
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Stores an icon payload that can later be consumed by `ResearchNode#setIconTexture(...)`.
         */
        public Builder iconTexture(@Nullable Object iconTexture) {
            this.iconTexture = iconTexture;
            return this;
        }

        /**
         * Friendly alias for {@link #iconTexture(Object)}.
         */
        public Builder icon(@Nullable Object iconTexture) {
            return iconTexture(iconTexture);
        }

        /**
         * Declares another research that this one depends on.
         */
        public Builder required(String researchKey) {
            if (researchKey != null && !researchKey.isBlank() && !requiredResearches.contains(researchKey)) {
                requiredResearches.add(researchKey);
            }
            return this;
        }

        /**
         * Declares several dependency keys at once.
         */
        public Builder required(String... researchKeys) {
            if (researchKeys == null) {
                return this;
            }
            for (String researchKey : researchKeys) {
                required(researchKey);
            }
            return this;
        }

        /**
         * Adds one prebuilt condition entry.
         */
        public Builder condition(ResearchCondition condition) {
            if (condition != null) {
                conditions.add(condition);
            }
            return this;
        }

        /**
         * Adds one generic condition key.
         */
        public Builder condition(String key) {
            return condition(ResearchCondition.custom(key));
        }

        /**
         * Adds one generic condition key/value pair.
         */
        public Builder condition(String key, String value) {
            return condition(ResearchCondition.custom(key, value));
        }

        /**
         * Adds one stage condition value.
         */
        public Builder stage(String stage) {
            return condition(ResearchCondition.stage(stage));
        }

        /**
         * Adds an item-backed condition.
         */
        public Builder condition(ItemStack stack) {
            return condition(ResearchCondition.item(stack));
        }

        /**
         * Adds an item-backed condition.
         */
        public Builder condition(ItemLike itemLike) {
            return condition(ResearchCondition.item(itemLike));
        }

        /**
         * Adds an ingredient-backed condition.
         */
        public Builder condition(Ingredient ingredient) {
            return condition(ResearchCondition.ingredient(ingredient));
        }

        /**
         * Adds one prebuilt reward entry.
         */
        public Builder reward(ResearchReward reward) {
            if (reward != null) {
                rewards.add(reward);
            }
            return this;
        }

        /**
         * Adds one generic reward key.
         */
        public Builder reward(String rewardKey) {
            return reward(ResearchReward.custom(rewardKey));
        }

        /**
         * Adds one generic reward key/value pair.
         */
        public Builder reward(String rewardKey, String rewardValue) {
            return reward(ResearchReward.custom(rewardKey, rewardValue));
        }

        /**
         * Adds a reward that points to another research/stage key.
         */
        public Builder rewardResearch(String researchKey) {
            return reward(ResearchReward.research(researchKey));
        }

        /**
         * Adds an item reward.
         */
        public Builder reward(ItemStack stack) {
            return reward(ResearchReward.item(stack));
        }

        /**
         * Adds an item reward.
         */
        public Builder reward(ItemLike itemLike) {
            return reward(ResearchReward.item(itemLike));
        }

        /**
         * Adds an ingredient reward.
         */
        public Builder reward(Ingredient ingredient) {
            return reward(ResearchReward.ingredient(ingredient));
        }

        /**
         * Sets the study mode explicitly.
         */
        public Builder studyType(ResearchStudyType studyType) {
            if (studyType != null) {
                this.studyType = studyType;
            }
            return this;
        }

        /**
         * Sets an optional reveal-animation override for this exact research.
         * <p>
         * This is useful when one specific node should appear differently from the rest of its
         * group. If omitted, group/theme defaults remain in control.
         * </p>
         */
        public Builder revealAnimation(ResearchRevealAnimationType revealAnimationType) {
            this.revealAnimationType = revealAnimationType;
            return this;
        }

        /**
         * Stores the duration payload in milliseconds.
         */
        public Builder studyDurationMs(long studyDurationMs) {
            this.studyDurationMs = Math.max(0L, studyDurationMs);
            return this;
        }

        /**
         * Convenience helper that marks the research as instant.
         */
        public Builder instant() {
            this.studyType = ResearchStudyType.INSTANT;
            return this;
        }

        /**
         * Convenience helper that marks the research as timed and assigns its duration.
         */
        public Builder timed(long studyDurationMs) {
            this.studyType = ResearchStudyType.TIMED;
            this.studyDurationMs = Math.max(0L, studyDurationMs);
            return this;
        }

        /**
         * Convenience helper that marks the research as table-based.
         */
        public Builder table() {
            this.studyType = ResearchStudyType.TABLE;
            return this;
        }

        /**
         * Builds the immutable public research definition.
         */
        public ResearchDefinition build() {
            return new ResearchDefinition(
                    key,
                    groupId,
                    visibilityMode,
                    title,
                    description,
                    iconTexture,
                    studyType,
                    revealAnimationType,
                    studyDurationMs,
                    requiredResearches,
                    conditions,
                    rewards
            );
        }
    }
}
