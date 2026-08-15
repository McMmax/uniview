package dev.terrorify.univue;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * What a button's action receives when clicked — the same shape whether the
 * click came from a Java inventory slot or a Bedrock form button, so actions
 * never branch on platform unless they want to.
 *
 * @param playerId the clicking player's UUID (Floodgate identity for Bedrock)
 * @param menu     the menu the click happened in
 * @param slot     the Java slot of the clicked button (also set for Bedrock,
 *                 resolved from the form response index)
 * @param bedrock  whether the click came from a Bedrock form
 * @param port     handle back into the platform renderer, for chaining
 *                 ({@code ctx.port().open(ctx.playerId(), nextMenu)}) or
 *                 closing
 */
public record ClickContext(
    @NotNull UUID playerId,
    @NotNull Menu menu,
    int slot,
    boolean bedrock,
    @NotNull MenuPort port
) {
}
