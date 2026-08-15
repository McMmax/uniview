package dev.terrorify.univue.floodgate;

import dev.terrorify.univue.FormImage;
import dev.terrorify.univue.Menu;
import dev.terrorify.univue.MenuButton;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloodgateFormCodecTest {

    private static Menu sampleMenu() {
        return Menu.builder()
            .title("Kit \"Selector\"")
            .rows(3)
            .bedrockContent("Pick a kit")
            // slot order deliberately non-insertion order: 13 added first
            .button(13, MenuButton.builder().name("PvP").action(ctx -> { }).build())
            .button(4, MenuButton.builder().name("Archer").action(ctx -> { }).build())
            // decorative filler: no action → must NOT appear on the form
            .button(0, MenuButton.builder().name("frame").build())
            // bedrock-hidden interactive button → must NOT appear either
            .button(22, MenuButton.builder().name("Close").action(ctx -> { })
                .bedrockHidden(true).build())
            // bedrock label override
            .button(15, MenuButton.builder().name("§6Builder").bedrockText("Builder")
                .action(ctx -> { }).build())
            .build();
    }

    @Test
    void bedrockButtonsFollowSlotOrderAndSkipFillerAndHidden() {
        Menu menu = sampleMenu();
        var buttons = menu.bedrockButtons();
        assertEquals(3, buttons.size());
        assertEquals("Archer", buttons.get(0).bedrockLabel());   // slot 4
        assertEquals("PvP", buttons.get(1).bedrockLabel());      // slot 13
        assertEquals("Builder", buttons.get(2).bedrockLabel());  // slot 15, override
    }

    @Test
    void simpleFormJsonShapeAndEscaping() {
        String json = FloodgateFormCodec.simpleFormJson(sampleMenu());
        assertEquals(
            "{\"type\":\"form\",\"title\":\"Kit \\\"Selector\\\"\","
                + "\"content\":\"Pick a kit\",\"buttons\":["
                + "{\"text\":\"Archer\"},{\"text\":\"PvP\"},{\"text\":\"Builder\"}]}",
            json);
    }

    @Test
    void imagesAndBedrockTitleOverride() {
        Menu menu = Menu.builder()
            .title("§6Warps")            // Java title, with formatting codes
            .bedrockTitle("tile:warps")  // stable pack-known title = tile style selector
            .rows(1)
            .button(0, MenuButton.builder().name("Spawn")
                .bedrockImage(FormImage.path("textures/items/compass_item"))
                .action(ctx -> { }).build())
            .button(1, MenuButton.builder().name("Shop")
                .bedrockImage(FormImage.url("https://vault.terrorify.dev/i/shop.png"))
                .action(ctx -> { }).build())
            .button(2, MenuButton.builder().name("Plain").action(ctx -> { }).build())
            .build();
        assertEquals(
            "{\"type\":\"form\",\"title\":\"tile:warps\",\"content\":\"\",\"buttons\":["
                + "{\"text\":\"Spawn\",\"image\":{\"type\":\"path\","
                + "\"data\":\"textures/items/compass_item\"}},"
                + "{\"text\":\"Shop\",\"image\":{\"type\":\"url\","
                + "\"data\":\"https://vault.terrorify.dev/i/shop.png\"}},"
                + "{\"text\":\"Plain\"}]}",
            FloodgateFormCodec.simpleFormJson(menu));
        // no override → Java title is used on Bedrock too
        assertEquals("§6Warps",
            Menu.builder().title("§6Warps").build().bedrockTitle());
    }

    @Test
    void framingMatchesFloodgateFormChannel() {
        Menu menu = sampleMenu();
        short formId = (short) 0x1234;
        byte[] data = FloodgateFormCodec.encodeSimpleForm(menu, formId);
        assertEquals(0, data[0]);          // SIMPLE_FORM ordinal
        assertEquals(0x12, data[1]);       // formId high byte
        assertEquals(0x34, data[2]);       // formId low byte
        String json = new String(data, 3, data.length - 3, StandardCharsets.UTF_8);
        assertEquals(FloodgateFormCodec.simpleFormJson(menu), json);
    }

    @Test
    void responseDecoding() {
        // button 2 selected, newline-terminated the way Bedrock sends it
        byte[] picked = frame((short) 7, "2\n");
        var response = FloodgateFormCodec.decodeResponse(picked);
        assertEquals(7, response.formId());
        assertEquals(2, response.buttonIndex());
        assertFalse(response.dismissed());

        assertTrue(FloodgateFormCodec.decodeResponse(frame((short) 7, "null")).dismissed());
        assertTrue(FloodgateFormCodec.decodeResponse(frame((short) 7, "")).dismissed());
        // hostile input: not a number, negative — dismissed, never a throw
        assertTrue(FloodgateFormCodec.decodeResponse(frame((short) 7, "yes")).dismissed());
        assertTrue(FloodgateFormCodec.decodeResponse(frame((short) 7, "-3")).dismissed());

        // frames too short to carry a form id are null
        assertNull(FloodgateFormCodec.decodeResponse(new byte[0]));
        assertNull(FloodgateFormCodec.decodeResponse(new byte[] {1}));
    }

    private static byte[] frame(short formId, String body) {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[bodyBytes.length + 2];
        data[0] = (byte) (formId >> 8 & 0xFF);
        data[1] = (byte) (formId & 0xFF);
        System.arraycopy(bodyBytes, 0, data, 2, bodyBytes.length);
        return data;
    }
}
