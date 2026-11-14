package ru.mirage.freerewards;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Условие получения награды: плейсхолдер, оператор и значение. */
public final class Requirement {
    private static final String DEF_PLAYTIME = "{text}Нужно наиграть {primary}{value} ч {text}(у вас {primary}{current} ч{text})";
    private static final String DEF_GENERIC = "{text}Условие: {primary}{placeholder} {operator} {value} {text}(сейчас {primary}{current}{text})";
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    public final String placeholder;
    public final String operator;
    public final String value;
    /** Своё сообщение при невыполнении; пусто = стандартное. */
    public final String message;

    public Requirement(String placeholder, String operator, String value, String message) {
        this.placeholder = placeholder;
        this.operator = Conditions.validOperator(operator) ? operator : ">=";
        this.value = value;
        this.message = message == null ? "" : message;
    }

    /** "%player_level% >= 10" или просто "5" (часы игры). null — неверный формат. */
    public static Requirement parse(String text) {
        String[] a = Conditions.parse(text);
        return a == null ? null : new Requirement(a[0], a[1], a[2], "");
    }

    /** Требование «наиграть N часов». */
    public boolean isPlaytime() {
        return placeholder.equalsIgnoreCase(Conditions.PLAYTIME) && operator.equals(">=");
    }

    public String current(Player p) {
        return Placeholders.resolve(p, placeholder).trim();
    }

    public boolean test(Player p) {
        String cur = current(p);
        if (Conditions.unresolved(cur)) {
            if (WARNED.add(placeholder)) {
                JavaPlugin.getPlugin(MirageFreeRewards.class).getLogger().warning(
                        "Плейсхолдер " + placeholder + " не раскрывается: установите PlaceholderAPI и нужное расширение "
                                + "(/papi ecloud download ...). Награды с этим условием недоступны.");
            }
            return false;
        }
        return Conditions.compare(cur, operator, Placeholders.resolve(p, value).trim());
    }

    /** Текст для игрока: своё сообщение или стандартное. Токены: {value} {current} {placeholder} {operator}. */
    public String text(MirageFreeRewards plugin, Player p) {
        String raw;
        if (!message.isBlank()) {
            raw = message;
        } else if (isPlaytime()) {
            raw = plugin.getConfig().getString("messages.requirement-playtime", DEF_PLAYTIME);
        } else {
            raw = plugin.getConfig().getString("messages.requirement-generic", DEF_GENERIC);
        }
        return raw.replace("{value}", value)
                .replace("{current}", current(p))
                .replace("{placeholder}", placeholder)
                .replace("{operator}", operator);
    }

    public String describe() {
        return placeholder + " " + operator + " " + value;
    }
}
