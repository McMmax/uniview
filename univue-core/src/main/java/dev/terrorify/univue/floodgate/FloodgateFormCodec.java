package dev.terrorify.univue.floodgate;

import dev.terrorify.univue.FormImage;
import dev.terrorify.univue.Menu;
import dev.terrorify.univue.MenuButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Encodes univue menus as Floodgate {@code floodgate:form} plugin messages and
 * decodes the responses. Pure bytes and strings — no platform types — so it is
 * unit-testable and reusable by any future platform module.
 *
 * <p>Wire contract, taken from Floodgate's
 * {@code core/.../pluginmessage/channel/FormChannel.java} (createFormData /
 * handleServerCall) — the proxy-side broker must speak exactly this:
 *
 * <pre>
 * server → proxy : [1 byte FormType ordinal][2 bytes formId BE][UTF-8 form JSON]
 * server → proxy : []                                         (close all forms)
 * proxy  → server: [2 bytes formId BE][UTF-8 response body]
 * </pre>
 *
 * <p>Cumulus {@code FormType} ordinals: SIMPLE_FORM=0, MODAL_FORM=1,
 * CUSTOM_FORM=2. univue menus map to SIMPLE_FORM (title + content + button
 * list). The form JSON is the raw Bedrock shape Cumulus emits:
 * {@code {"type":"form","title":...,"content":...,"buttons":[{"text":...}]}}.
 *
 * <p>A SimpleForm response body is the selected button index as decimal text
 * (possibly newline-terminated); a dismissed form is the literal
 * {@code "null"} (or an empty body when force-closed).
 */
public final class FloodgateFormCodec {

    /** Plugin message channel both directions use. */
    public static final String CHANNEL = "floodgate:form";

    private static final int SIMPLE_FORM_ORDINAL = 0;

    private FloodgateFormCodec() {
    }

    // ── encode ──────────────────────────────────────────────────────────────

    /** Frame a menu as a floodgate:form payload with the given form id. */
    public static byte @NotNull [] encodeSimpleForm(@NotNull Menu menu, short formId) {
        byte[] json = simpleFormJson(menu).getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[json.length + 3];
        data[0] = (byte) SIMPLE_FORM_ORDINAL;
        data[1] = (byte) (formId >> 8 & 0xFF);
        data[2] = (byte) (formId & 0xFF);
        System.arraycopy(json, 0, data, 3, json.length);
        return data;
    }

    /** An empty payload closes every open form for the player. */
    public static byte @NotNull [] encodeCloseForms() {
        return new byte[0];
    }

    /** The raw Bedrock SimpleForm JSON for a menu. Visible for tests. */
    public static @NotNull String simpleFormJson(@NotNull Menu menu) {
        StringBuilder out = new StringBuilder(256);
        out.append("{\"type\":\"form\",\"title\":\"").append(escape(menu.bedrockTitle()))
            .append("\",\"content\":\"")
            .append(escape(menu.bedrockContent() != null ? menu.bedrockContent() : ""))
            .append("\",\"buttons\":[");
        List<MenuButton> buttons = menu.bedrockButtons();
        for (int i = 0; i < buttons.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            MenuButton button = buttons.get(i);
            out.append("{\"text\":\"").append(escape(button.bedrockLabel())).append('"');
            FormImage image = button.bedrockImage();
            if (image != null) {
                out.append(",\"image\":{\"type\":\"").append(image.type().jsonName())
                    .append("\",\"data\":\"").append(escape(image.data())).append("\"}");
            }
            out.append('}');
        }
        return out.append("]}").toString();
    }

    // ── decode ──────────────────────────────────────────────────────────────

    /**
     * A decoded proxy → server response frame.
     *
     * @param formId      id from the matching {@link #encodeSimpleForm} call
     * @param buttonIndex index into {@link Menu#bedrockButtons()}, or -1 if the
     *                    player dismissed the form (or it was closed)
     */
    public record FormResponse(short formId, int buttonIndex) {
        public boolean dismissed() {
            return buttonIndex < 0;
        }
    }

    /**
     * Decode a proxy → server frame. Returns null for frames too short to
     * carry a form id (nothing actionable in them).
     */
    public static @Nullable FormResponse decodeResponse(byte @NotNull [] data) {
        if (data.length < 2) {
            return null;
        }
        short formId = (short) ((data[0] & 0xFF) << 8 | data[1] & 0xFF);
        String body = new String(data, 2, data.length - 2, StandardCharsets.UTF_8).trim();
        // SimpleForm: decimal button index; "null"/empty = dismissed. Anything
        // unparseable is treated as dismissed rather than thrown — the client
        // is untrusted input.
        int index = -1;
        if (!body.isEmpty() && !"null".equals(body)) {
            try {
                index = Integer.parseInt(body);
            } catch (NumberFormatException ignored) {
                index = -1;
            }
            if (index < 0) {
                index = -1;
            }
        }
        return new FormResponse(formId, index);
    }

    // ── json string escaping (no dependency; menus carry short UI strings) ──

    private static @NotNull String escape(@NotNull String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
