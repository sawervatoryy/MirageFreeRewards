package ru.mirage.freerewards;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Ввод текста из чата для игрового редактора. */
public final class ChatInput implements Listener {
    private record Pending(Consumer<String> handler, Runnable back) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();
    private final MirageFreeRewards plugin;

    public ChatInput(MirageFreeRewards plugin) {
        this.plugin = plugin;
    }

    public static void ask(MirageFreeRewards plugin, Player p, String prompt, Consumer<String> handler, Runnable back) {
        PENDING.put(p.getUniqueId(), new Pending(handler, back));
        Bukkit.getScheduler().runTask(plugin, () -> {
            p.closeInventory();
            plugin.send(p, "input-prompt", "{prompt}", prompt);
            plugin.send(p, "input-hint");
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Pending pd = PENDING.remove(e.getPlayer().getUniqueId());
        if (pd == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("отмена") || text.equalsIgnoreCase("cancel")) {
                plugin.send(p, "input-cancelled");
            } else {
                pd.handler().accept(text);
            }
            pd.back().run();
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        PENDING.remove(e.getPlayer().getUniqueId());
    }
}
