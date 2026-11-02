package ru.mirage.freerewards.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import ru.mirage.freerewards.ChatInput;
import ru.mirage.freerewards.Colors;
import ru.mirage.freerewards.Items;
import ru.mirage.freerewards.MirageFreeRewards;

import java.util.List;

/** Настройки меню, анимаций и цветовой темы прямо в игре. */
public final class SettingsEditor implements Menu {
    private static final String[] STYLES = {"SPIRAL", "CENTER_OUT", "ROWS"};

    private final MirageFreeRewards plugin;
    private final Player player;
    private Inventory inv;

    public SettingsEditor(MirageFreeRewards plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void open() {
        inv = Bukkit.createInventory(this, 27, Colors.component("{primary}&lНастройки {muted}• {secondary}MirageFreeRewards"));
        render();
        player.openInventory(inv);
    }

    private void render() {
        FileConfiguration c = plugin.getConfig();
        for (int i = 0; i < 27; i++) inv.setItem(i, Items.pane(Material.WHITE_STAINED_GLASS_PANE));

        inv.setItem(10, Items.make(Material.NAME_TAG, "{primary}Заголовок меню", List.of(
                "{text}Сейчас: &r" + c.getString("menu.title", ""), "", "{primary}Клик {muted}- {text}изменить"), false));
        inv.setItem(11, Items.make(Material.LADDER, "{primary}Рядов: {secondary}" + c.getInt("menu.rows", 5), List.of(
                "{primary}ЛКМ {muted}- {text}+1", "{primary}ПКМ {muted}- {text}-1", "{muted}от 3 до 6"), false));
        inv.setItem(12, toggle("Анимация открытия", c.getBoolean("animation.open.enabled", true)));
        inv.setItem(13, Items.make(Material.ARROW, "{primary}Стиль анимации: {secondary}" + c.getString("animation.open.style", "SPIRAL"),
                List.of("{muted}SPIRAL / CENTER_OUT / ROWS", "", "{primary}Клик {muted}- {text}переключить"), false));
        inv.setItem(14, toggle("Мерцание рамки", c.getBoolean("animation.idle.enabled", true)));
        inv.setItem(15, toggle("Закрывать после получения", c.getBoolean("menu.close-on-claim", false)));

        inv.setItem(19, color(Material.WHITE_DYE, "Основной цвет", "primary", "{primary}"));
        inv.setItem(20, color(Material.RED_DYE, "Дополнительный цвет", "secondary", "{secondary}"));
        inv.setItem(21, color(Material.PINK_DYE, "Акцентный цвет", "accent", "{accent}"));
        inv.setItem(22, color(Material.LIGHT_GRAY_DYE, "Цвет текста", "text", "{text}"));
        inv.setItem(26, Items.make(Material.ARROW, "{primary}« Назад", List.of(), false));
    }

    private org.bukkit.inventory.ItemStack toggle(String name, boolean on) {
        return Items.make(on ? Material.LIME_DYE : Material.GRAY_DYE,
                (on ? "{secondary}" : "{muted}") + name + ": " + (on ? "вкл" : "выкл"),
                List.of("{primary}Клик {muted}- {text}переключить"), false);
    }

    private org.bukkit.inventory.ItemStack color(Material m, String name, String key, String sample) {
        return Items.make(m, "{primary}" + name, List.of(
                "{text}Значение: {primary}" + plugin.getConfig().getString("theme." + key, ""),
                "{text}Образец: " + sample + "████████", "",
                "{primary}Клик {muted}- {text}изменить",
                "{muted}Формат: &f, &#RRGGBB или #RRGGBB"), false);
    }

    private void set(String path, Object value) {
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
        Colors.setTheme(plugin.getConfig().getConfigurationSection("theme"));
    }

    private void flip(String path, boolean def) {
        set(path, !plugin.getConfig().getBoolean(path, def));
        render();
    }

    private void askColor(String key, String label) {
        ChatInput.ask(plugin, player, "Введите цвет: " + label + " (&f, &#RRGGBB или #RRGGBB)",
                t -> set("theme." + key, t), this::open);
    }

    @Override
    public void click(InventoryClickEvent e) {
        FileConfiguration c = plugin.getConfig();
        switch (e.getSlot()) {
            case 10 -> ChatInput.ask(plugin, player, "Введите заголовок меню", t -> set("menu.title", t), this::open);
            case 11 -> {
                int rows = c.getInt("menu.rows", 5);
                boolean right = e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT;
                rows = Math.max(3, Math.min(6, rows + (right ? -1 : 1)));
                set("menu.rows", rows);
                render();
            }
            case 12 -> flip("animation.open.enabled", true);
            case 13 -> {
                String cur = c.getString("animation.open.style", "SPIRAL");
                int idx = 0;
                for (int i = 0; i < STYLES.length; i++) if (STYLES[i].equalsIgnoreCase(cur)) idx = i;
                set("animation.open.style", STYLES[(idx + 1) % STYLES.length]);
                render();
            }
            case 14 -> flip("animation.idle.enabled", true);
            case 15 -> flip("menu.close-on-claim", false);
            case 19 -> askColor("primary", "основной");
            case 20 -> askColor("secondary", "дополнительный");
            case 21 -> askColor("accent", "акцентный");
            case 22 -> askColor("text", "текст");
            case 26 -> new EditorMenu(plugin, player).open();
            default -> {
            }
        }
    }
}
