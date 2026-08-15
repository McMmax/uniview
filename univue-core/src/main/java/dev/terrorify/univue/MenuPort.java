package dev.terrorify.univue;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * The platform renderer as seen from inside a button action. Implemented by
 * the platform module (univue-minestom); core code and menu definitions only
 * ever see this interface, which is what keeps menus portable.
 */
public interface MenuPort {

    /** Open (or replace) a menu for the player, on whichever platform they use. */
    void open(@NotNull UUID playerId, @NotNull Menu menu);

    /** Close whatever univue menu the player currently has open, if any. */
    void close(@NotNull UUID playerId);
}
