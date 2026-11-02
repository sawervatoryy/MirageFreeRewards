package ru.mirage.freerewards;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RewardManager {
    public enum State { READY, COOLDOWN, CLAIMED, LOCKED }

    /** Маркер неверного формата в parseDuration. */
    public static final long INVALID = Long.MIN_VALUE;

    private final MirageFreeRewards plugin;
    private final Map<String, Reward> rewards = new LinkedHashMap<>();
    private File rewardsFile;
    private File dataFile;
    private YamlConfiguration data = new YamlConfiguration();

    public RewardManager(MirageFreeRewards plugin) {
        this.plugin = plugin;
    }

    // ───────────── загрузка / сохранение ─────────────

    public void load() {
        rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) plugin.saveResource("rewards.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(rewardsFile);
        rewards.clear();
        ConfigurationSection root = y.getConfigurationSection("rewards");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) continue;
                Reward r = new Reward(id);
                r.enabled = s.getBoolean("enabled", true);
                r.name = s.getString("name", "{primary}" + id);
                r.lore = new ArrayList<>(s.getStringList("lore"));
                r.icon = Items.material(s.getString("icon", "CHEST"), Material.CHEST);
                r.slot = s.getInt("slot", -1);
                try {
                    r.type = Reward.Type.valueOf(s.getString("type", "COMMAND").toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    r.type = Reward.Type.COMMAND;
                }
                r.cooldown = s.getLong("cooldown", 86400);
                r.permission = s.getString("permission", "");
                r.glow = s.getBoolean("glow", false);
                r.commands = new ArrayList<>(s.getStringList("commands"));
                r.items = new ArrayList<>(s.getStringList("items"));
                List<?> stacks = s.getList("item-stacks");
                if (stacks != null) {
                    for (Object o : stacks) if (o instanceof ItemStack is) r.itemStacks.add(is);
                }
                rewards.put(id, r);
            }
        }
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Reward r : rewards.values()) {
            String p = "rewards." + r.id + ".";
            y.set(p + "enabled", r.enabled);
            y.set(p + "name", r.name);
            y.set(p + "icon", r.icon.name());
            y.set(p + "slot", r.slot);
            y.set(p + "type", r.type.name());
            y.set(p + "cooldown", r.cooldown);
            y.set(p + "permission", r.permission);
            y.set(p + "glow", r.glow);
            y.set(p + "lore", new ArrayList<>(r.lore));
            y.set(p + "commands", new ArrayList<>(r.commands));
            y.set(p + "items", new ArrayList<>(r.items));
            if (!r.itemStacks.isEmpty()) y.set(p + "item-stacks", new ArrayList<>(r.itemStacks));
        }
        try {
            y.save(rewardsFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Не удалось сохранить rewards.yml: " + ex.getMessage());
        }
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Не удалось сохранить data.yml: " + ex.getMessage());
        }
    }

    // ───────────── доступ ─────────────

    public List<Reward> all() {
        return new ArrayList<>(rewards.values());
    }

    public Reward get(String id) {
        return rewards.get(id);
    }

    public Reward create() {
        int n = 1;
        while (rewards.containsKey("reward_" + n)) n++;
        Reward r = new Reward("reward_" + n);
        r.name = "{primary}&lНовая награда";
        r.lore.add("{text}Описание награды");
        rewards.put(r.id, r);
        return r;
    }

    public void remove(String id) {
        rewards.remove(id);
    }

    // ───────────── состояние ─────────────

    private long lastClaim(UUID u, String id) {
        return data.getLong("players." + u + "." + id, 0L);
    }

    public State state(Player p, Reward r) {
        if (!r.permission.isBlank() && !p.hasPermission(r.permission)) return State.LOCKED;
        long last = lastClaim(p.getUniqueId(), r.id);
        if (last <= 0) return State.READY;
        if (r.cooldown < 0) return State.CLAIMED;
        if (r.cooldown == 0) return State.READY;
        return remaining(p, r) > 0 ? State.COOLDOWN : State.READY;
    }

    /** Секунды до следующего получения. */
    public long remaining(Player p, Reward r) {
        long last = lastClaim(p.getUniqueId(), r.id);
        if (last <= 0 || r.cooldown <= 0) return 0;
        long left = r.cooldown - (System.currentTimeMillis() - last) / 1000L;
        return Math.max(0, left);
    }

    public boolean claim(Player p, Reward r) {
        if (state(p, r) != State.READY) return false;
        data.set("players." + p.getUniqueId() + "." + r.id, System.currentTimeMillis());
        saveData();
        deliver(p, r);
        return true;
    }

    public void reset(UUID uuid, String id) {
        if (id == null) data.set("players." + uuid, null);
        else data.set("players." + uuid + "." + id, null);
        saveData();
    }

    public void resetAll(String id) {
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) return;
        for (String u : players.getKeys(false)) {
            if (id == null) data.set("players." + u, null);
            else data.set("players." + u + "." + id, null);
        }
        saveData();
    }

    // ───────────── выдача ─────────────

    /** Выдаёт содержимое награды без проверки кулдауна. */
    public void deliver(Player p, Reward r) {
        for (String spec : r.items) {
            ItemStack it = Items.parse(spec);
            if (it != null) give(p, it);
        }
        for (ItemStack is : r.itemStacks) give(p, is.clone());
        for (String c : r.commands) run(p, c);
    }

    private void give(Player p, ItemStack it) {
        Map<Integer, ItemStack> left = p.getInventory().addItem(it);
        for (ItemStack rest : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), rest);
    }

    private void run(Player p, String raw) {
        String line = raw.replace("{player}", p.getName()).replace("{uuid}", p.getUniqueId().toString());
        String low = line.toLowerCase(Locale.ROOT);
        if (low.startsWith("[player]")) {
            p.performCommand(cmd(line.substring(8)));
        } else if (low.startsWith("[console]")) {
            console(cmd(line.substring(9)));
        } else if (low.startsWith("[message]")) {
            p.sendMessage(Colors.component(line.substring(9).trim()));
        } else if (low.startsWith("[broadcast]")) {
            Bukkit.broadcast(Colors.component(line.substring(11).trim()));
        } else if (low.startsWith("[sound]")) {
            p.playSound(p.getLocation(), line.substring(7).trim(), 1f, 1f);
        } else {
            console(cmd(line));
        }
    }

    private static String cmd(String s) {
        s = s.trim();
        return s.startsWith("/") ? s.substring(1) : s;
    }

    private void console(String command) {
        if (!command.isEmpty()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    // ───────────── время ─────────────

    /** "1d 2h 30m", "3600", "once", "none" → секунды; INVALID при ошибке. */
    public static long parseDuration(String in) {
        String s = in.trim().toLowerCase(Locale.ROOT);
        if (s.equals("once") || s.equals("раз") || s.equals("один раз") || s.equals("-1")) return -1;
        if (s.equals("none") || s.equals("нет") || s.equals("0")) return 0;
        if (s.matches("\\d+")) return Long.parseLong(s);
        Matcher m = Pattern.compile("(\\d+)\\s*(d|h|m|s|д|ч|м|с)").matcher(s);
        long total = 0;
        boolean any = false;
        while (m.find()) {
            any = true;
            long n = Long.parseLong(m.group(1));
            long unit;
            switch (m.group(2)) {
                case "d", "д" -> unit = 86400L;
                case "h", "ч" -> unit = 3600L;
                case "m", "м" -> unit = 60L;
                default -> unit = 1L;
            }
            total += n * unit;
        }
        return any ? total : INVALID;
    }

    public static String formatDuration(long sec) {
        if (sec <= 0) return "0с";
        long d = sec / 86400, h = (sec % 86400) / 3600, m = (sec % 3600) / 60, s = sec % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("д ");
        if (h > 0) sb.append(h).append("ч ");
        if (m > 0) sb.append(m).append("м ");
        if (s > 0 && d == 0) sb.append(s).append("с");
        return sb.toString().trim();
    }

    public static String cooldownText(long cooldown) {
        if (cooldown < 0) return "один раз";
        if (cooldown == 0) return "без ограничений";
        return formatDuration(cooldown);
    }
}
