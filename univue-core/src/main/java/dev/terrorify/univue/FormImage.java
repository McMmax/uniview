package dev.terrorify.univue;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An image on a Bedrock form button — the Cumulus/Bedrock
 * {@code {"type":"path"|"url","data":...}} pair.
 *
 * <p>{@code PATH} references a texture inside the client's resource packs
 * ({@code "textures/items/diamond"}), which is also what tile-style resource
 * packs key their art from. {@code URL} loads over HTTP and renders slower on
 * first display; prefer PATH for anything shipped in our packs.
 */
public record FormImage(@NotNull Type type, @NotNull String data) {

    public enum Type {
        PATH("path"),
        URL("url");

        private final String json;

        Type(String json) {
            this.json = json;
        }

        /** The lowercase wire name Bedrock expects. */
        public @NotNull String jsonName() {
            return json;
        }
    }

    public FormImage {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(data, "data");
    }

    public static @NotNull FormImage path(@NotNull String texturePath) {
        return new FormImage(Type.PATH, texturePath);
    }

    public static @NotNull FormImage url(@NotNull String url) {
        return new FormImage(Type.URL, url);
    }
}
