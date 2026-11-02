package ru.mirage.freerewards.menu;

import org.bukkit.configuration.file.FileConfiguration;

final class Layout {
    private Layout() {}

    static int rows(FileConfiguration c) {
        return Math.max(3, Math.min(6, c.getInt("menu.rows", 5)));
    }

    /** Слот инфо-предмета или -1, если выключен. */
    static int info(FileConfiguration c, int size) {
        if (!c.getBoolean("menu.info.enabled", true)) return -1;
        int s = c.getInt("menu.info.slot", 4);
        return (s >= 0 && s < size) ? s : 4;
    }

    static int close(FileConfiguration c, int size, int rows) {
        int s = c.getInt("menu.close.slot", -1);
        return (s >= 0 && s < size) ? s : (rows - 1) * 9 + 4;
    }
}
