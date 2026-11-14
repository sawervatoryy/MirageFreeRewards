package ru.mirage.freerewards;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Раскрытие плейсхолдеров. PlaceholderAPI подключается мягко (через reflection),
 * поэтому плагин работает и без неё. Для %statistic_hours_played% есть встроенный
 * запасной вариант на статистике Minecraft.
 */
public final class Placeholders {
    private static Method papiMethod;
    private static boolean papiChecked;

    private Placeholders() {}

    public static String resolve(Player p, String text) {
        if (text == null) return "";
        String out = text;
        Method m = papi();
        if (m != null) {
            try {
                Object r = m.invoke(null, p, out);
                if (r instanceof String s) out = s;
            } catch (Throwable ignored) {
                // оставляем текст как есть
            }
        }
        if (out.contains(Conditions.PLAYTIME)) {
            out = out.replace(Conditions.PLAYTIME, String.valueOf(playHours(p)));
        }
        return out;
    }

    private static Method papi() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return null;
        if (!papiChecked) {
            papiChecked = true;
            try {
                Class<?> c = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                papiMethod = c.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            } catch (Throwable t) {
                papiMethod = null;
            }
        }
        return papiMethod;
    }

    /** Полных часов на сервере (по статистике Minecraft). */
    public static long playHours(Player p) {
        return p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 72000L;
    }
}
