package dev.terrorify.univue.minestom;

import dev.terrorify.univue.ClickContext;
import dev.terrorify.univue.Menu;
import dev.terrorify.univue.MenuButton;
import dev.terrorify.univue.MenuPort;
import dev.terrorify.univue.floodgate.FloodgateFormCodec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * The Minestom (Stomzy) renderer for univue menus: one {@link Menu} definition
 * opens as a native chest inventory for Java players and as a native Bedrock
 * form (Floodgate {@code floodgate:form} plugin channel) for Bedrock players.
 *
 * <p>Call {@link #init()} once at server bootstrap, then
 * {@link #open(Player, Menu)} anywhere. Also usable through {@link MenuPort}
 * from inside button actions, for chaining menus.
 *
 * <p>Bedrock players are recognised by their Floodgate identity: internode
 * injects UUIDs of the form {@code Uuid::from_u64_pair(0, xuid)} (high 64 bits
 * zero), which no Java account can have (Mojang UUIDs are version 4).
 *
 * <p>The Bedrock leg requires the proxy to broker {@code floodgate:form}
 * between this backend and the embedded GeyserLite — until that broker exists,
 * Bedrock opens log a warning and no form appears; Java is fully functional.
 */
public final class Univue implements MenuPort {

    private static final Univue INSTANCE = new Univue();

    /** One open Java menu per player: inventory identity → menu. */
    private final Map<UUID, OpenJavaMenu> javaMenus = new ConcurrentHashMap<>();
    /** One in-flight Bedrock form per player: formId → menu. */
    private final Map<UUID, PendingForm> bedrockForms = new ConcurrentHashMap<>();
    private final AtomicInteger nextFormId = new AtomicInteger(1);

    private volatile boolean initialized;

    private record OpenJavaMenu(@NotNull Inventory inventory, @NotNull Menu menu) {
    }

    private record PendingForm(short formId, @NotNull Menu menu) {
    }

    private Univue() {
    }

    public static @NotNull Univue get() {
        return INSTANCE;
    }

    /** Register the global event listeners. Idempotent. */
    public static void init() {
        Univue self = INSTANCE;
        synchronized (self) {
            if (self.initialized) {
                return;
            }
            self.initialized = true;
        }
        var handler = MinecraftServer.getGlobalEventHandler();
        handler.addListener(InventoryPreClickEvent.class, self::onInventoryPreClick);
        handler.addListener(PlayerPluginMessageEvent.class, self::onPluginMessage);
        handler.addListener(PlayerDisconnectEvent.class,
            event -> self.forget(event.getPlayer().getUuid()));
    }

    /** Floodgate identity = {@code UUID(0, xuid)}; no Java account has msb 0. */
    public static boolean isBedrock(@NotNull Player player) {
        return player.getUuid().getMostSignificantBits() == 0;
    }

    // ── opening ─────────────────────────────────────────────────────────────

    public static void open(@NotNull Player player, @NotNull Menu menu) {
        if (isBedrock(player)) {
            INSTANCE.openBedrock(player, menu);
        } else {
            INSTANCE.openJava(player, menu);
        }
    }

    @Override
    public void open(@NotNull UUID playerId, @NotNull Menu menu) {
        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerId);
        if (player != null) {
            open(player, menu);
        }
    }

    @Override
    public void close(@NotNull UUID playerId) {
        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerId);
        if (player == null) {
            forget(playerId);
            return;
        }
        if (javaMenus.remove(playerId) != null) {
            player.closeInventory();
        }
        if (bedrockForms.remove(playerId) != null) {
            player.sendPluginMessage(FloodgateFormCodec.CHANNEL,
                FloodgateFormCodec.encodeCloseForms());
        }
    }

    private void openJava(Player player, Menu menu) {
        InventoryType type = switch (menu.rows()) {
            case 1 -> InventoryType.CHEST_1_ROW;
            case 2 -> InventoryType.CHEST_2_ROW;
            case 3 -> InventoryType.CHEST_3_ROW;
            case 4 -> InventoryType.CHEST_4_ROW;
            case 5 -> InventoryType.CHEST_5_ROW;
            default -> InventoryType.CHEST_6_ROW;
        };
        Inventory inventory = new Inventory(type, Component.text(menu.title()));
        for (Map.Entry<Integer, MenuButton> entry : menu.buttons().entrySet()) {
            inventory.setItemStack(entry.getKey(), itemFor(entry.getValue()));
        }
        javaMenus.put(player.getUuid(), new OpenJavaMenu(inventory, menu));
        player.openInventory(inventory);
    }

    private void openBedrock(Player player, Menu menu) {
        // 15-bit id space, always positive: the top bit is Floodgate's
        // proxy-vs-server discriminator (see FormChannel.createFormData).
        short formId = (short) (nextFormId.getAndIncrement() & 0x7FFF);
        bedrockForms.put(player.getUuid(), new PendingForm(formId, menu));
        player.sendPluginMessage(FloodgateFormCodec.CHANNEL,
            FloodgateFormCodec.encodeSimpleForm(menu, formId));
    }

    private static ItemStack itemFor(MenuButton button) {
        Material material = Material.fromKey(button.icon());
        if (material == null) {
            MinecraftServer.LOGGER.warn(
                "univue: unknown icon '{}', rendering as stone", button.icon());
            material = Material.STONE;
        }
        // Vanilla italicizes custom names/lore; menu labels want the plain style.
        ItemStack stack = ItemStack.of(material)
            .withCustomName(Component.text(button.name())
                .decoration(TextDecoration.ITALIC, false));
        if (!button.lore().isEmpty()) {
            stack = stack.withLore(button.lore().stream()
                .map(line -> (Component) Component.text(line)
                    .decoration(TextDecoration.ITALIC, false))
                .toList());
        }
        return stack;
    }

    // ── event plumbing ──────────────────────────────────────────────────────

    private void onInventoryPreClick(InventoryPreClickEvent event) {
        Player player = event.getPlayer();
        OpenJavaMenu open = javaMenus.get(player.getUuid());
        if (open == null || event.getInventory() != open.inventory()) {
            return;
        }
        // univue menus are UI, never item containers: swallow every click,
        // then dispatch if it landed on a button.
        event.setCancelled(true);
        MenuButton button = open.menu().buttons().get(event.getSlot());
        if (button == null || button.action() == null) {
            return;
        }
        dispatch(button.action(), new ClickContext(
            player.getUuid(), open.menu(), event.getSlot(), false, this));
    }

    private void onPluginMessage(PlayerPluginMessageEvent event) {
        if (!FloodgateFormCodec.CHANNEL.equals(event.getIdentifier())) {
            return;
        }
        Player player = event.getPlayer();
        FloodgateFormCodec.FormResponse response =
            FloodgateFormCodec.decodeResponse(event.getMessage());
        if (response == null) {
            return;
        }
        PendingForm pending = bedrockForms.get(player.getUuid());
        if (pending == null || pending.formId() != response.formId()) {
            // Stale response (player already got a newer menu) — drop it.
            return;
        }
        bedrockForms.remove(player.getUuid());
        if (response.dismissed()) {
            return;
        }
        List<MenuButton> buttons = pending.menu().bedrockButtons();
        if (response.buttonIndex() >= buttons.size()) {
            return; // hostile/garbled index — client input is untrusted
        }
        MenuButton button = buttons.get(response.buttonIndex());
        Consumer<ClickContext> action = button.action();
        if (action == null) {
            return;
        }
        dispatch(action, new ClickContext(
            player.getUuid(), pending.menu(), slotOf(pending.menu(), button), true, this));
    }

    /** Recover the Java slot for a Bedrock button so actions see one shape. */
    private static int slotOf(Menu menu, MenuButton button) {
        for (Map.Entry<Integer, MenuButton> entry : menu.buttons().entrySet()) {
            if (entry.getValue() == button) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private void dispatch(Consumer<ClickContext> action, ClickContext ctx) {
        try {
            action.accept(ctx);
        } catch (Exception e) {
            MinecraftServer.LOGGER.error(
                "univue: menu action threw for {} in '{}'", ctx.playerId(), ctx.menu().title(), e);
        }
    }

    private void forget(@Nullable UUID playerId) {
        if (playerId != null) {
            javaMenus.remove(playerId);
            bedrockForms.remove(playerId);
        }
    }
}
