package dev.terrorify.univue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * One interactive (or decorative) element of a {@link Menu}.
 *
 * <p>The model is platform-agnostic on purpose: {@code icon} is a Minecraft
 * item identifier string ({@code "minecraft:diamond"}), never a platform item
 * type, so the same menu renders as a Java inventory slot and as a Bedrock
 * form button without the author doing anything.
 *
 * <p>Bedrock adaptation rules:
 * <ul>
 *   <li>{@code action == null} (decorative filler) → omitted from the form.</li>
 *   <li>{@link #bedrockText()} overrides the button label on Bedrock only —
 *       Java keeps {@link #name()}. Lore does not exist on forms; put anything
 *       essential in the name or the override.</li>
 *   <li>{@link #bedrockHidden()} keeps a button Java-only (e.g. a close button:
 *       Bedrock forms have native dismissal).</li>
 * </ul>
 */
public final class MenuButton {

    private final String icon;
    private final String name;
    private final List<String> lore;
    private final @Nullable Consumer<ClickContext> action;
    private final @Nullable String bedrockText;
    private final @Nullable FormImage bedrockImage;
    private final boolean bedrockHidden;

    private MenuButton(Builder builder) {
        this.icon = builder.icon;
        this.name = builder.name;
        this.lore = List.copyOf(builder.lore);
        this.action = builder.action;
        this.bedrockText = builder.bedrockText;
        this.bedrockImage = builder.bedrockImage;
        this.bedrockHidden = builder.bedrockHidden;
    }

    public @NotNull String icon() {
        return icon;
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull List<String> lore() {
        return lore;
    }

    public @Nullable Consumer<ClickContext> action() {
        return action;
    }

    /** Label used on the Bedrock form; falls back to {@link #name()}. */
    public @NotNull String bedrockLabel() {
        return bedrockText != null ? bedrockText : name;
    }

    public @Nullable String bedrockText() {
        return bedrockText;
    }

    /** Optional image shown on the Bedrock form button (and used by tile packs). */
    public @Nullable FormImage bedrockImage() {
        return bedrockImage;
    }

    public boolean bedrockHidden() {
        return bedrockHidden;
    }

    /** Interactive buttons appear on Bedrock forms; decorative filler does not. */
    public boolean bedrockVisible() {
        return action != null && !bedrockHidden;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String icon = "minecraft:stone";
        private String name = "";
        private List<String> lore = Collections.emptyList();
        private @Nullable Consumer<ClickContext> action;
        private @Nullable String bedrockText;
        private @Nullable FormImage bedrockImage;
        private boolean bedrockHidden;

        private Builder() {
        }

        /** Item identifier, e.g. {@code "minecraft:diamond"}. */
        public @NotNull Builder icon(@NotNull String icon) {
            this.icon = Objects.requireNonNull(icon, "icon");
            return this;
        }

        public @NotNull Builder name(@NotNull String name) {
            this.name = Objects.requireNonNull(name, "name");
            return this;
        }

        public @NotNull Builder lore(@NotNull List<String> lore) {
            this.lore = Objects.requireNonNull(lore, "lore");
            return this;
        }

        public @NotNull Builder lore(@NotNull String... lines) {
            this.lore = List.of(lines);
            return this;
        }

        /** Click handler. Absent = decorative filler (Java-only, non-clickable). */
        public @NotNull Builder action(@Nullable Consumer<ClickContext> action) {
            this.action = action;
            return this;
        }

        /** Bedrock-only label override; Java keeps {@code name}. */
        public @NotNull Builder bedrockText(@Nullable String bedrockText) {
            this.bedrockText = bedrockText;
            return this;
        }

        /** Image on the Bedrock form button — {@link FormImage#path} or {@link FormImage#url}. */
        public @NotNull Builder bedrockImage(@Nullable FormImage bedrockImage) {
            this.bedrockImage = bedrockImage;
            return this;
        }

        /** Keep this button off the Bedrock form entirely. */
        public @NotNull Builder bedrockHidden(boolean bedrockHidden) {
            this.bedrockHidden = bedrockHidden;
            return this;
        }

        public @NotNull MenuButton build() {
            return new MenuButton(this);
        }
    }
}
