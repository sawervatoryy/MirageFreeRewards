package ru.mirage.freerewards;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Цвета: &f, &#RRGGBB, <gradient:#a:#b>текст</gradient>, {theme}-плейсхолдеры. */
public final class Colors {
    private static final char SECTION = '\u00A7';
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern BARE_HEX = Pattern.compile("^#([A-Fa-f0-9]{6})$");
    private static final Pattern GRADIENT = Pattern.compile("<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.*?)</gradient>");
    private static final Map<String, String> THEME = new LinkedHashMap<>();
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    private Colors() {}

    public static void setTheme(ConfigurationSection sec) {
        THEME.clear();
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            String v = sec.getString(key, "");
            Matcher m = BARE_HEX.matcher(v.trim());
            if (m.matches()) v = "&#" + m.group(1);
            THEME.put(key, v);
        }
    }

    public static String color(String input) {
        if (input == null) return "";
        String s = input;
        for (Map.Entry<String, String> e : THEME.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue());
        }
        Matcher gm = GRADIENT.matcher(s);
        StringBuilder g = new StringBuilder();
        while (gm.find()) {
            gm.appendReplacement(g, Matcher.quoteReplacement(gradient(gm.group(1), gm.group(2), gm.group(3))));
        }
        gm.appendTail(g);
        s = g.toString();

        Matcher hm = HEX.matcher(s);
        StringBuilder out = new StringBuilder();
        while (hm.find()) {
            StringBuilder rep = new StringBuilder().append(SECTION).append('x');
            for (char ch : hm.group(1).toCharArray()) rep.append(SECTION).append(ch);
            hm.appendReplacement(out, Matcher.quoteReplacement(rep.toString()));
        }
        hm.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }

    private static String gradient(String a, String b, String text) {
        StringBuilder fmt = new StringBuilder();
        StringBuilder visible = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length() && "klmnoKLMNO".indexOf(text.charAt(i + 1)) >= 0) {
                fmt.append('&').append(text.charAt(i + 1));
                i++;
            } else {
                visible.append(c);
            }
        }
        int n = visible.length();
        if (n == 0) return "";
        int r1 = Integer.parseInt(a.substring(0, 2), 16), g1 = Integer.parseInt(a.substring(2, 4), 16), b1 = Integer.parseInt(a.substring(4, 6), 16);
        int r2 = Integer.parseInt(b.substring(0, 2), 16), g2 = Integer.parseInt(b.substring(2, 4), 16), b2 = Integer.parseInt(b.substring(4, 6), 16);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0 : (double) i / (n - 1);
            int r = (int) Math.round(r1 + (r2 - r1) * t);
            int g = (int) Math.round(g1 + (g2 - g1) * t);
            int bl = (int) Math.round(b1 + (b2 - b1) * t);
            out.append(String.format("&#%02X%02X%02X", r, g, bl)).append(fmt).append(visible.charAt(i));
        }
        return out.toString();
    }

    public static Component component(String s) {
        return SERIALIZER.deserialize(color(s)).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static List<Component> components(List<String> lines) {
        List<Component> out = new ArrayList<>();
        for (String l : lines) out.add(component(l));
        return out;
    }
}
