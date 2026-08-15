package dev.terrorify.univue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A declarative, immutable menu. Write it once; the platform module renders it
 * as a native Java chest inventory or a native Bedrock form.
 *
 * <p>Slots are Java chest slots ({@code row * 9 + column}). On Bedrock the
 * grid collapses to a form: interactive buttons in ascending slot order become
 * form buttons, so slot order is also the Bedrock reading order — lay out the
 * important actions first.
 */
public final class Menu {

    private final String title;
    private final int rows;
    private final SortedMap<Integer, MenuButton> buttons;
    private final @Nullable String bedrockContent;
    private final @Nullable String bedrockTitle;

    private Menu(Builder builder) {
        this.title = builder.title;
        this.rows = builder.rows;
        this.buttons = Collections.unmodifiableSortedMap(new TreeMap<>(builder.buttons));
        this.bedrockContent = builder.bedrockContent;
        this.bedrockTitle = builder.bedrockTitle;
    }

    public @NotNull String title() {
        return title;
    }

    /**
     * The title used on the Bedrock form; falls back to {@link #title()}.
     * Tile-style resource packs (ui/server_form.json overrides) intercept
     * forms BY TITLE, so this is effectively the Bedrock style selector — give
     * a menu a stable, pack-known bedrock title to render it as a tile panel,
     * without changing anything about the wire format or the Java rendering.
     */
    public @NotNull String bedrockTitle() {
        return bedrockTitle != null ? bedrockTitle : title;
    }

    /** Chest rows, 1-6. */
    public int rows() {
        return rows;
    }

    public int size() {
        return rows * 9;
    }

    /** Buttons by slot, ascending. */
    public @NotNull SortedMap<Integer, MenuButton> buttons() {
        return buttons;
    }

    /** Optional body text shown above the buttons on the Bedrock form. */
    public @Nullable String bedrockContent() {
        return bedrockContent;
    }

    /**
     * The buttons a Bedrock player sees, in slot order. Index into this list is
     * the form response index — both renderer and response handler must use
     * this exact method so they can never disagree.
     */
    public @NotNull List<MenuButton> bedrockButtons() {
        List<MenuButton> visible = new ArrayList<>();
        for (MenuButton button : buttons.values()) {
            if (button.bedrockVisible()) {
                visible.add(button);
            }
        }
        return visible;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title = "";
        private int rows = 3;
        private final Map<Integer, MenuButton> buttons = new LinkedHashMap<>();
        private @Nullable String bedrockContent;
        private @Nullable String bedrockTitle;

        private Builder() {
        }

        public @NotNull Builder title(@NotNull String title) {
            this.title = Objects.requireNonNull(title, "title");
            return this;
        }

        public @NotNull Builder rows(int rows) {
            if (rows < 1 || rows > 6) {
                throw new IllegalArgumentException("rows must be 1-6, got " + rows);
            }
            this.rows = rows;
            return this;
        }

        public @NotNull Builder button(int slot, @NotNull MenuButton button) {
            if (slot < 0) {
                throw new IllegalArgumentException("slot must be >= 0, got " + slot);
            }
            buttons.put(slot, Objects.requireNonNull(button, "button"));
            return this;
        }

        /** Optional body text for the Bedrock form. */
        public @NotNull Builder bedrockContent(@Nullable String bedrockContent) {
            this.bedrockContent = bedrockContent;
            return this;
        }

        /** Bedrock-only title override — see {@link Menu#bedrockTitle()}. */
        public @NotNull Builder bedrockTitle(@Nullable String bedrockTitle) {
            this.bedrockTitle = bedrockTitle;
            return this;
        }

        public @NotNull Menu build() {
            int size = rows * 9;
            for (int slot : buttons.keySet()) {
                if (slot >= size) {
                    throw new IllegalArgumentException(
                        "slot " + slot + " does not fit in " + rows + " rows (" + size + " slots)");
                }
            }
            return new Menu(this);
        }
    }
}
