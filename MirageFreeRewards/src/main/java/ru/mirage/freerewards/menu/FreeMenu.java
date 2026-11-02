package ru.mirage.freerewards.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.mirage.freerewards.Colors;
import ru.mirage.freerewards.Items;
import ru.mirage.freerewards.MirageFreeRewards;
import ru.mirage.freerewards.Reward;
import ru.mirage.freerewards.RewardManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Меню /free с анимированным открытием. */
public final class FreeMenu implements Menu {
    private final MirageFreeRewards plugin;
    private final Player player;
    private Inventory inv;
    private int rows, size, infoSlot, closeSlot;
    private boolean ready, borderOn, fillerOn;
    private Material borderMat, fillerMat;
    private BukkitTask task;

    private final Map<Integer, Reward> slotMap = new TreeMap<>();
    private final List<Integer> ring = new ArrayList<>();
    private final Set<Integer> deco = new LinkedHashSet<>();

    public FreeMenu(MirageFreeRewards plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    // ───────────── открытие ─────────────

    public void open() {
        FileConfiguration c = plugin.getConfig();
        rows = Layout.rows(c);
        size = rows * 9;
        infoSlot = Layout.info(c, size);
        closeSlot = Layout.close(c, size, rows);
        borderOn = c.getBoolean("menu.border.enabled", true);
        fillerOn = c.getBoolean("menu.filler.enabled", false);
        borderMat = Items.material(c.getString("menu.border.material"), Material.WHITE_STAINED_GLASS_PANE);
        fillerMat = Items.material(c.getString("menu.filler.material"), Material.WHITE_STAINED_GLASS_PANE);

        inv = Bukkit.createInventory(this, size, Colors.component(c.getString("menu.title", "{primary}Free")));
        layout();
        player.openInventory(inv);
        plugin.sound(player, "open");

        if (c.getBoolean("animation.open.enabled", true)) {
            animate();
        } else {
            renderStatic();
            for (Map.Entry<Integer, Reward> e : slotMap.entrySet()) inv.setItem(e.getKey(), rewardItem(e.getValue()));
            ready = true;
            startIdle();
        }
    }

    private boolean isRing(int s) {
        int r = s / 9, col = s % 9;
        return r == 0 || r == rows - 1 || col == 0 || col == 8;
    }

    private void layout() {
        slotMap.clear();
        deco.clear();
        ring.clear();
        for (int col = 0; col < 9; col++) ring.add(col);
        for (int r = 1; r < rows; r++) ring.add(r * 9 + 8);
        for (int col = 7; col >= 0; col--) ring.add((rows - 1) * 9 + col);
        for (int r = rows - 2; r >= 1; r--) ring.add(r * 9);

        Set<Integer> reserved = new HashSet<>();
        reserved.add(infoSlot);
        reserved.add(closeSlot);

        List<Reward> auto = new ArrayList<>();
        for (Reward r : plugin.rewards().all()) {
            if (!r.enabled) continue;
            if (r.slot >= 0 && r.slot < size && !reserved.contains(r.slot) && !slotMap.containsKey(r.slot)) {
                slotMap.put(r.slot, r);
            } else {
                auto.add(r);
            }
        }
        for (int row = 1; row < rows - 1 && !auto.isEmpty(); row++) {
            for (int col = 1; col <= 7 && !auto.isEmpty(); col++) {
                int s = row * 9 + col;
                if (!slotMap.containsKey(s) && !reserved.contains(s)) slotMap.put(s, auto.remove(0));
            }
        }
        for (int s = 0; s < size; s++) {
            if (reserved.contains(s) || slotMap.containsKey(s)) continue;
            if (isRing(s) ? borderOn : fillerOn) deco.add(s);
        }
    }

    private Material decoMaterial(int s) {
        return isRing(s) ? borderMat : fillerMat;
    }

    private void renderStatic() {
        for (int s : deco) inv.setItem(s, Items.pane(decoMaterial(s)));
        renderButtons();
    }

    private void renderButtons() {
        FileConfiguration c = plugin.getConfig();
        if (infoSlot >= 0) {
            List<String> lore = new ArrayList<>();
            for (String l : c.getStringList("menu.info.lore")) lore.add(l.replace("{player}", player.getName()));
            inv.setItem(infoSlot, Items.make(
                    Items.material(c.getString("menu.info.material"), Material.NETHER_STAR),
                    c.getString("menu.info.name", ""), lore, true));
        }
        inv.setItem(closeSlot, Items.make(
                Items.material(c.getString("menu.close.material"), Material.BARRIER),
                c.getString("menu.close.name", "{error}Закрыть"),
                c.getStringList("menu.close.lore"), false));
    }

    // ───────────── анимации ─────────────

    private boolean valid() {
        return player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() == this;
    }

    private void animate() {
        FileConfiguration c = plugin.getConfig();
        String style = c.getString("animation.open.style", "SPIRAL").toUpperCase(Locale.ROOT);
        int stepDelay = Math.max(1, c.getInt("animation.open.step-delay", 2));
        int revealDelay = Math.max(1, c.getInt("animation.open.reveal-delay", 2));
        Material head = Items.material(c.getString("animation.open.head-material"), Material.RED_STAINED_GLASS_PANE);

        Map<Integer, Integer> step = new HashMap<>();
        int maxStep = 0;
        for (int s : deco) {
            int st;
            if (style.equals("ROWS")) {
                st = s / 9;
            } else if (style.equals("CENTER_OUT")) {
                st = (int) Math.round(Math.abs(s % 9 - 4) + Math.abs(s / 9 - (rows - 1) / 2.0));
            } else {
                int idx = ring.indexOf(s);
                st = idx >= 0 ? idx / 2 : ring.size() / 2;
            }
            step.put(s, st);
            maxStep = Math.max(maxStep, st);
        }
        final int last = maxStep;
        final List<Integer> order = new ArrayList<>(slotMap.keySet());

        task = new BukkitRunnable() {
            int t = 0;
            int ri = 0;
            int wait = 0;
            boolean buttons = false;

            @Override
            public void run() {
                if (!valid()) {
                    cancel();
                    return;
                }
                if (t <= last + 1) {
                    for (Map.Entry<Integer, Integer> en : step.entrySet()) {
                        int v = en.getValue();
                        if (v == t) inv.setItem(en.getKey(), Items.pane(head));
                        else if (v == t - 1) inv.setItem(en.getKey(), Items.pane(decoMaterial(en.getKey())));
                    }
                    plugin.sound(player, "tick");
                    t++;
                    return;
                }
                if (!buttons) {
                    renderButtons();
                    buttons = true;
                    return;
                }
                if (wait > 0) {
                    wait--;
                    return;
                }
                if (ri < order.size()) {
                    int s = order.get(ri++);
                    inv.setItem(s, rewardItem(slotMap.get(s)));
                    plugin.sound(player, "reveal");
                    wait = revealDelay - 1;
                    return;
                }
                ready = true;
                cancel();
                startIdle();
            }
        }.runTaskTimer(plugin, 1L, stepDelay);
    }

    private void startIdle() {
        FileConfiguration c = plugin.getConfig();
        final boolean idleOn = c.getBoolean("animation.idle.enabled", true) && borderOn;
        final int interval = Math.max(1, c.getInt("animation.idle.interval", 4));
        final List<Material> cycle = new ArrayList<>();
        for (String name : c.getStringList("animation.idle.cycle")) {
            Material m = Items.material(name, null);
            if (m != null) cycle.add(m);
        }
        task = new BukkitRunnable() {
            int tick = 0;
            int acc = 0;

            @Override
            public void run() {
                if (!valid()) {
                    cancel();
                    return;
                }
                tick++;
                if (idleOn && cycle.size() > 1) {
                    for (int s : deco) {
                        if (!isRing(s)) continue;
                        int idx = ring.indexOf(s);
                        if (idx < 0) continue;
                        inv.setItem(s, Items.pane(cycle.get((idx + tick) % cycle.size())));
                    }
                }
                acc += interval;
                if (acc >= 20) {
                    acc = 0;
                    refreshRewards();
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void refreshRewards() {
        for (Map.Entry<Integer, Reward> e : slotMap.entrySet()) inv.setItem(e.getKey(), rewardItem(e.getValue()));
    }

    // ───────────── предметы ─────────────

    private ItemStack rewardItem(Reward r) {
        FileConfiguration c = plugin.getConfig();
        RewardManager.State st = plugin.rewards().state(player, r);
        ConfigurationSection s = c.getConfigurationSection("menu.states." + st.name().toLowerCase(Locale.ROOT));

        Material m = r.icon;
        boolean glow = r.glow;
        List<String> lore = new ArrayList<>(r.lore);
        if (s != null) {
            Material override = Items.material(s.getString("material", ""), null);
            if (override != null) m = override;
            glow = glow || s.getBoolean("glow", false);
            lore.addAll(s.getStringList("lore"));
        }
        if (r.type == Reward.Type.DONATE && st == RewardManager.State.LOCKED) {
            lore.addAll(c.getStringList("menu.donate-lock-lore"));
        }
        String remaining = RewardManager.formatDuration(plugin.rewards().remaining(player, r));
        List<String> out = new ArrayList<>();
        for (String l : lore) {
            out.add(l.replace("{remaining}", remaining).replace("{player}", player.getName()).replace("{reward}", r.name));
        }
        return Items.make(m, r.name, out, glow);
    }

    // ───────────── клики ─────────────

    @Override
    public void click(InventoryClickEvent e) {
        if (!ready) return;
        int slot = e.getSlot();
        if (slot == closeSlot) {
            plugin.sound(player, "click");
            player.closeInventory();
            return;
        }
        Reward r = slotMap.get(slot);
        if (r == null) return;

        if (player.hasPermission("mirage.free.admin") && e.getClick() == ClickType.SHIFT_RIGHT) {
            new RewardEditor(plugin, player, r).open();
            return;
        }

        switch (plugin.rewards().state(player, r)) {
            case READY -> {
                if (plugin.rewards().claim(player, r)) {
                    plugin.sound(player, "claim");
                    plugin.send(player, "claimed", "{reward}", r.name);
                    inv.setItem(slot, rewardItem(r));
                    if (plugin.getConfig().getBoolean("menu.close-on-claim", false)) player.closeInventory();
                }
            }
            case COOLDOWN -> {
                plugin.sound(player, "error");
                plugin.send(player, "cooldown", "{reward}", r.name,
                        "{remaining}", RewardManager.formatDuration(plugin.rewards().remaining(player, r)));
            }
            case CLAIMED -> {
                plugin.sound(player, "error");
                plugin.send(player, "claimed-once", "{reward}", r.name);
            }
            case LOCKED -> {
                plugin.sound(player, "error");
                plugin.send(player, r.type == Reward.Type.DONATE ? "donate-locked" : "locked");
            }
        }
    }

    @Override
    public void onClose() {
        if (task != null) task.cancel();
    }
}
