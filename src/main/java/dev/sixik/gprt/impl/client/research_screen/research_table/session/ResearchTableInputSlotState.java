package dev.sixik.gprt.impl.client.research_screen.research_table.session;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Immutable client snapshot of one logical slot used by the research table.
 * <p>
 * A logical table slot is not required to map 1:1 to a real inventory slot. It can represent
 * concepts such as "main clue", "tool", "reactive ingredient" or any other named input channel.
 * </p>
 */
public final class ResearchTableInputSlotState {
    private final String key;
    private final String label;
    private final ItemStack stack;
    private final boolean required;
    private final boolean locked;
    private final boolean highlighted;
    private final String hint;

    public ResearchTableInputSlotState(String key,
                                       String label,
                                       ItemStack stack,
                                       boolean required,
                                       boolean locked,
                                       boolean highlighted,
                                       String hint
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.label = label == null || label.isBlank() ? key : label;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.required = required;
        this.locked = locked;
        this.highlighted = highlighted;
        this.hint = hint == null ? "" : hint;
    }

    public static Builder builder(String key) {
        return new Builder(key);
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public ItemStack getStack() {
        return stack.copy();
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public String getHint() {
        return hint;
    }

    public Builder toBuilder() {
        return new Builder(key)
                .label(label)
                .stack(stack)
                .required(required)
                .locked(locked)
                .highlighted(highlighted)
                .hint(hint);
    }

    /**
     * Fluent builder for one immutable table-slot snapshot.
     */
    public static final class Builder {
        private final String key;
        private String label;
        private ItemStack stack = ItemStack.EMPTY;
        private boolean required;
        private boolean locked;
        private boolean highlighted;
        private String hint = "";

        private Builder(String key) {
            this.key = Objects.requireNonNull(key, "key");
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder stack(ItemStack stack) {
            this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder locked(boolean locked) {
            this.locked = locked;
            return this;
        }

        public Builder highlighted(boolean highlighted) {
            this.highlighted = highlighted;
            return this;
        }

        public Builder hint(String hint) {
            this.hint = hint;
            return this;
        }

        public ResearchTableInputSlotState build() {
            return new ResearchTableInputSlotState(key, label, stack, required, locked, highlighted, hint);
        }
    }
}
