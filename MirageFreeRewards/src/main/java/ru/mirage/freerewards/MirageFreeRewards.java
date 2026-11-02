package ru.mirage.freerewards;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mirage.freerewards.menu.Menu;

import java.util.Objects;

/**
 * MirageFreeRewards — бесплатные награды /free.
 * Создано студией MirageStudio. Автор: sawervatoryy.
 */
public final class MirageFreeRewards extends JavaPlugin {
    public static final String STUDIO = "MirageStudio";
    public static final String AUTHOR = "sawervatoryy";
    public static final String DISCORD = "https://discord.gg/dWPenZegK";

    private RewardManager rewards;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        rewards = new RewardManager(this);
        reloadAll();

        FreeCommand command = new FreeCommand(this);
        Objects.requireNonNull(getCommand("free")).setExecutor(command);
        Objects.requireNonNull(getCommand("free")).setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new ChatInput(this), this);

        getLogger().info("MirageFreeRewards " + getPluginMeta().getVersion() + " включён.");
        getLogger().info("Создано студией " + STUDIO + " | Автор: " + AUTHOR + " | Discord: " + DISCORD);
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Menu) p.closeInventory();
        }
    }

    public void reloadAll() {
        reloadConfig();
        Colors.setTheme(getConfig().getConfigurationSection("theme"));
        rewards.load();
    }

    public RewardManager rewards() {
        return rewards;
    }

    /** Сообщение из messages.<key> с префиксом; пары {плейсхолдер}, значение. */
    public void send(CommandSender to, String key, String... kv) {
        String raw = getConfig().getString("messages." + key, key);
        for (int i = 0; i + 1 < kv.length; i += 2) raw = raw.replace(kv[i], kv[i + 1]);
        to.sendMessage(Colors.component(getConfig().getString("messages.prefix", "") + raw));
    }

    /** Звук из sounds.<key>: "ключ,громкость,высота". */
    public void sound(Player p, String key) {
        String v = getConfig().getString("sounds." + key, "");
        if (v == null || v.isBlank()) return;
        String[] a = v.split(",");
        float vol = a.length > 1 ? parse(a[1], 1f) : 1f;
        float pitch = a.length > 2 ? parse(a[2], 1f) : 1f;
        p.playSound(p.getLocation(), a[0].trim(), vol, pitch);
    }

    private static float parse(String s, float def) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
