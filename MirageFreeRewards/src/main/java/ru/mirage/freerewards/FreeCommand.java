package ru.mirage.freerewards;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.mirage.freerewards.menu.EditorMenu;
import ru.mirage.freerewards.menu.FreeMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FreeCommand implements CommandExecutor, TabCompleter {
    private final MirageFreeRewards plugin;

    public FreeCommand(MirageFreeRewards plugin) {
        this.plugin = plugin;
    }

    private boolean admin(CommandSender s) {
        if (s.hasPermission("mirage.free.admin")) return true;
        plugin.send(s, "no-permission");
        return false;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) {
            if (!(s instanceof Player pl)) {
                plugin.send(s, "players-only");
                return true;
            }
            if (!pl.hasPermission("mirage.free.use")) {
                plugin.send(s, "no-permission");
                return true;
            }
            new FreeMenu(plugin, pl).open();
            return true;
        }

        String sub = a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "about", "info" -> about(s);
            case "edit", "editor", "gui" -> {
                if (!admin(s)) return true;
                if (!(s instanceof Player pl)) {
                    plugin.send(s, "players-only");
                    return true;
                }
                new EditorMenu(plugin, pl).open();
            }
            case "reload" -> {
                if (!admin(s)) return true;
                plugin.reloadAll();
                plugin.send(s, "reloaded");
            }
            case "reset" -> {
                if (!admin(s)) return true;
                if (a.length < 2) {
                    help(s);
                    return true;
                }
                String id = a.length > 2 ? a[2] : null;
                if (id != null && plugin.rewards().get(id) == null) {
                    plugin.send(s, "unknown-reward", "{id}", id);
                    return true;
                }
                if (a[1].equals("*")) {
                    plugin.rewards().resetAll(id);
                } else {
                    OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(a[1]);
                    if (op == null) {
                        plugin.send(s, "unknown-player", "{player}", a[1]);
                        return true;
                    }
                    plugin.rewards().reset(op.getUniqueId(), id);
                }
                plugin.send(s, "reset-done");
            }
            case "give" -> {
                if (!admin(s)) return true;
                if (a.length < 3) {
                    help(s);
                    return true;
                }
                Player target = Bukkit.getPlayerExact(a[1]);
                if (target == null) {
                    plugin.send(s, "unknown-player", "{player}", a[1]);
                    return true;
                }
                Reward r = plugin.rewards().get(a[2]);
                if (r == null) {
                    plugin.send(s, "unknown-reward", "{id}", a[2]);
                    return true;
                }
                plugin.rewards().deliver(target, r);
                plugin.send(s, "give-done", "{id}", r.id, "{player}", target.getName());
            }
            default -> help(s);
        }
        return true;
    }

    private void about(CommandSender s) {
        s.sendMessage(Colors.component("<gradient:#FFFFFF:#EE3F3F>&lMirageFreeRewards</gradient> {muted}v" + plugin.getPluginMeta().getVersion()));
        s.sendMessage(Colors.component("{text}Создано студией {secondary}" + MirageFreeRewards.STUDIO));
        s.sendMessage(Colors.component("{text}Автор: {accent}" + MirageFreeRewards.AUTHOR));
        s.sendMessage(Colors.component("{text}Discord: {primary}" + MirageFreeRewards.DISCORD));
    }

    private void help(CommandSender s) {
        s.sendMessage(Colors.component("{primary}&l/free {muted}» {text}открыть меню наград"));
        if (!s.hasPermission("mirage.free.admin")) return;
        s.sendMessage(Colors.component("{primary}/free edit {muted}» {text}игровой редактор"));
        s.sendMessage(Colors.component("{primary}/free reload {muted}» {text}перезагрузить"));
        s.sendMessage(Colors.component("{primary}/free reset <игрок|*> [награда] {muted}» {text}сбросить кулдауны"));
        s.sendMessage(Colors.component("{primary}/free give <игрок> <награда> {muted}» {text}выдать награду"));
        s.sendMessage(Colors.component("{primary}/free about {muted}» {text}о плагине"));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        List<String> out = new ArrayList<>();
        boolean admin = s.hasPermission("mirage.free.admin");
        if (a.length == 1) {
            out.add("about");
            if (admin) out.addAll(List.of("edit", "reload", "reset", "give"));
        } else if (admin && (a[0].equalsIgnoreCase("reset") || a[0].equalsIgnoreCase("give"))) {
            if (a.length == 2) {
                for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
                if (a[0].equalsIgnoreCase("reset")) out.add("*");
            } else if (a.length == 3) {
                for (Reward r : plugin.rewards().all()) out.add(r.id);
            }
        }
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
