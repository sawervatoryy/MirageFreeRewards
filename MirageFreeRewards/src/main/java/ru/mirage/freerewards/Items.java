package ru.mirage.freerewards;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class Items {
    private Items() {}

    public static ItemStack make(Material m, String name, List<String> lore, boolean glow) {
        if (m == null || !m.isItem() || m.isAir()) m = Material.STONE;
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (name != null) meta.displayName(Colors.component(name));
        if (lore != null && !lore.isEmpty()) meta.lore(Colors.components(lore));
        if (glow) meta.setEnchantmentGlintOverride(true);
        meta.addItemFlags(ItemFlag.values());
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack pane(Material m) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(" "));
        meta.setHideTooltip(true);
        it.setItemMeta(meta);
        return it;
    }

    public static Material material(String name, Material def) {
        if (name == null || name.isBlank()) return def;
        Material m = Material.matchMaterial(name.trim());
        return (m != null && m.isItem() && !m.isAir()) ? m : def;
    }

    /** МАТЕРИАЛ:кол-во[:Название] — null, если формат неверный. */
    public static ItemStack parse(String spec) {
        if (spec == null || spec.isBlank()) return null;
        String[] a = spec.split(":", 3);
        Material m = Material.matchMaterial(a[0].trim());
        if (m == null || !m.isItem() || m.isAir()) return null;
        int amount = 1;
        if (a.length > 1) {
            try {
                amount = Math.max(1, Math.min(2304, Integer.parseInt(a[1].trim())));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        ItemStack it = new ItemStack(m, amount);
        if (a.length > 2 && !a[2].isBlank()) {
            ItemMeta meta = it.getItemMeta();
            meta.displayName(Colors.component(a[2]));
            it.setItemMeta(meta);
        }
        return it;
    }
}
