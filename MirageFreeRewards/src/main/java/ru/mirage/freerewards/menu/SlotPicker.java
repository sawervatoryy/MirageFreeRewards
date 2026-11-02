package ru.mirage.freerewards.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import ru.mirage.freerewards.Colors;
import ru.mirage.freerewards.Items;
import ru.mirage.freerewards.MirageFreeRewards;
import ru.mirage.freerewards.Reward;

import java.util.List;

/** Выбор слота награды прямо в макете меню. */
public final class SlotPicker implements Menu {
    private final MirageFreeRewards plugin;
    private final Player player;
    private final Reward r;
    private Inventory inv;
    private int rows, size, infoSlot, closeSlot;

    public SlotPicker(MirageFreeRewards plugin, Player player, Reward r) {
        this.plugin = plugin;
        this.player = player;
        this.r = r;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void open() {
        FileConfiguration c = plugin.getConfig();
        rows = Layout.rows(c);
        size = rows * 9;
        infoSlot = Layout.info(c, size);
        closeSlot = Layout.close(c, size, rows);
        inv = Bukkit.createInventory(this, size, Colors.component("{primary}&lСлот {muted}• {secondary}" + r.id));

        for (int s = 0; s < size; s++) {
            inv.setItem(s, Items.make(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "{text}Слот " + s,
                    List.of("{primary}Клик {muted}- {text}поставить сюда"), false));
        }
        for (Reward o : plugin.rewards().all()) {
            if (o != r && o.slot >= 0 && o.slot < size) {
                inv.setItem(o.slot, Items.make(o.icon, o.name, List.of("{muted}Занято: " + o.id), false));
            }
        }
        if (r.slot >= 0 && r.slot < size) {
            inv.setItem(r.slot, Items.make(r.icon, r.name, List.of("{secondary}Текущий слот"), true));
        }
        if (infoSlot >= 0) {
            inv.setItem(infoSlot, Items.make(Material.NETHER_STAR, "{primary}Подсказка", List.of(
                    "{text}Клик по слоту - поставить награду",
                    "{text}Shift+клик - авто-размещение",
                    "{muted}Закройте окно, чтобы отменить"), false));
        }
        inv.setItem(closeSlot, Items.make(Material.BARRIER, "{error}Зарезервировано", List.of("{muted}Кнопка закрытия меню"), false));
        player.openInventory(inv);
    }

    @Override
    public void click(InventoryClickEvent e) {
        int s = e.getSlot();
        if (s == infoSlot || s == closeSlot) return;
        r.slot = e.isShiftClick() ? -1 : s;
        plugin.rewards().save();
        new RewardEditor(plugin, player, r).open();
    }
}
