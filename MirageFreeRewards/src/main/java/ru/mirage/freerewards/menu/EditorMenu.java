package ru.mirage.freerewards.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import ru.mirage.freerewards.Colors;
import ru.mirage.freerewards.Items;
import ru.mirage.freerewards.MirageFreeRewards;
import ru.mirage.freerewards.Reward;
import ru.mirage.freerewards.RewardManager;

import java.util.List;

/** Главное окно игрового редактора: список наград. */
public final class EditorMenu implements Menu {
    private final MirageFreeRewards plugin;
    private final Player player;
    private Inventory inv;
    private int page;

    public EditorMenu(MirageFreeRewards plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void open() {
        inv = Bukkit.createInventory(this, 54, Colors.component("{primary}&lРедактор {muted}• {secondary}MirageFreeRewards"));
        render();
        player.openInventory(inv);
    }

    private void render() {
        inv.clear();
        List<Reward> list = plugin.rewards().all();
        int pages = Math.max(1, (list.size() + 44) / 45);
        if (page >= pages) page = pages - 1;

        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i;
            if (idx >= list.size()) break;
            Reward r = list.get(idx);
            inv.setItem(i, Items.make(r.icon, r.name, List.of(
                    "{muted}ID: {text}" + r.id,
                    "{muted}Тип: {text}" + r.type.name(),
                    "{muted}Кулдаун: {text}" + RewardManager.cooldownText(r.cooldown),
                    "{muted}Слот: {text}" + (r.slot < 0 ? "авто" : String.valueOf(r.slot)),
                    "{muted}Статус: " + (r.enabled ? "{secondary}включена" : "{error}выключена"),
                    "",
                    "{primary}ЛКМ {muted}- {text}редактировать",
                    "{primary}Shift+ПКМ {muted}- {error}удалить"), r.enabled && r.glow));
        }
        for (int i = 45; i < 54; i++) inv.setItem(i, Items.pane(Material.WHITE_STAINED_GLASS_PANE));
        if (page > 0) inv.setItem(45, Items.make(Material.ARROW, "{primary}« Назад", List.of("{text}Страница " + page), false));
        if (page < pages - 1) inv.setItem(53, Items.make(Material.ARROW, "{primary}Вперёд »", List.of("{text}Страница " + (page + 2)), false));
        inv.setItem(46, Items.make(Material.EMERALD, "{secondary}Добавить награду", List.of("{text}Создать новую награду"), false));
        inv.setItem(47, Items.make(Material.COMPARATOR, "{primary}Настройки", List.of("{text}Меню, анимации, цвета"), false));
        inv.setItem(48, Items.make(Material.SUNFLOWER, "{accent}Перезагрузить", List.of("{text}Перечитать config.yml и rewards.yml"), false));
        inv.setItem(49, Items.make(Material.ENDER_EYE, "{primary}Предпросмотр", List.of("{text}Открыть меню /free"), false));
        inv.setItem(51, Items.make(Material.BOOK, "{primary}MirageFreeRewards", List.of(
                "{muted}Создано студией {secondary}" + MirageFreeRewards.STUDIO,
                "{muted}Автор: {accent}" + MirageFreeRewards.AUTHOR,
                "{muted}" + MirageFreeRewards.DISCORD), false));
        inv.setItem(52, Items.make(Material.BARRIER, "{error}Закрыть", List.of(), false));
    }

    @Override
    public void click(InventoryClickEvent e) {
        int s = e.getSlot();
        if (s < 45) {
            List<Reward> list = plugin.rewards().all();
            int idx = page * 45 + s;
            if (idx >= list.size()) return;
            Reward r = list.get(idx);
            if (e.getClick() == ClickType.SHIFT_RIGHT) {
                plugin.rewards().remove(r.id);
                plugin.rewards().save();
                render();
            } else {
                new RewardEditor(plugin, player, r).open();
            }
            return;
        }
        switch (s) {
            case 45 -> {
                if (page > 0) {
                    page--;
                    render();
                }
            }
            case 53 -> {
                page++;
                render();
            }
            case 46 -> {
                Reward r = plugin.rewards().create();
                plugin.rewards().save();
                new RewardEditor(plugin, player, r).open();
            }
            case 47 -> new SettingsEditor(plugin, player).open();
            case 48 -> {
                plugin.reloadAll();
                plugin.send(player, "reloaded");
                render();
            }
            case 49 -> new FreeMenu(plugin, player).open();
            case 52 -> player.closeInventory();
            default -> {
            }
        }
    }
}
