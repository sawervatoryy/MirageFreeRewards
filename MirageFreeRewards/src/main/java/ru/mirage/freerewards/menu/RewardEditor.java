package ru.mirage.freerewards.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.mirage.freerewards.ChatInput;
import ru.mirage.freerewards.Colors;
import ru.mirage.freerewards.Items;
import ru.mirage.freerewards.MirageFreeRewards;
import ru.mirage.freerewards.Reward;
import ru.mirage.freerewards.RewardManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Редактор одной награды. Иконку и предметы можно задавать предметом на курсоре. */
public final class RewardEditor implements Menu {
    private final MirageFreeRewards plugin;
    private final Player player;
    private final Reward r;
    private Inventory inv;

    public RewardEditor(MirageFreeRewards plugin, Player player, Reward r) {
        this.plugin = plugin;
        this.player = player;
        this.r = r;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    @Override
    public boolean allowBottom() {
        return true;
    }

    public void open() {
        inv = Bukkit.createInventory(this, 45, Colors.component("{primary}&lНаграда {muted}• {secondary}" + r.id));
        render();
        player.openInventory(inv);
    }

    private static List<String> preview(List<String> src, String empty) {
        List<String> out = new ArrayList<>();
        if (src.isEmpty()) {
            out.add("{muted}" + empty);
            return out;
        }
        int n = Math.min(src.size(), 6);
        for (int i = 0; i < n; i++) out.add("{muted}" + (i + 1) + ". {text}" + src.get(i));
        if (src.size() > n) out.add("{muted}... ещё " + (src.size() - n));
        return out;
    }

    private static List<String> join(List<String> head, String... tail) {
        List<String> out = new ArrayList<>(head);
        for (String t : tail) out.add(t);
        return out;
    }

    private void render() {
        for (int i = 0; i < 45; i++) inv.setItem(i, Items.pane(Material.WHITE_STAINED_GLASS_PANE));

        inv.setItem(10, Items.make(r.icon, r.name, List.of(
                "{muted}Иконка награды", "",
                "{primary}Клик предметом с курсора {muted}- {text}сменить"), r.glow));
        inv.setItem(11, Items.make(Material.PAPER, "{primary}Название", List.of(
                "{text}Сейчас: &r" + r.name, "",
                "{primary}Клик {muted}- {text}изменить",
                "{muted}Поддерживаются &, &#HEX, <gradient>"), false));
        inv.setItem(12, Items.make(Material.BOOK, "{primary}Описание", join(preview(r.lore, "пусто"),
                "", "{primary}ЛКМ {muted}- {text}добавить строку", "{primary}ПКМ {muted}- {text}очистить"), false));

        StringBuilder types = new StringBuilder();
        for (Reward.Type t : Reward.Type.values()) {
            types.append(t == r.type ? "{secondary}» " : "{muted}  ").append(t.name()).append("\n");
        }
        List<String> typeLore = new ArrayList<>();
        for (String l : types.toString().split("\n")) typeLore.add(l);
        typeLore.add("");
        typeLore.add("{primary}Клик {muted}- {text}переключить");
        inv.setItem(13, Items.make(Material.COMPARATOR, "{primary}Тип награды", typeLore, false));

        inv.setItem(14, Items.make(Material.COMMAND_BLOCK, "{primary}Команды", join(preview(r.commands, "нет команд"),
                "", "{primary}ЛКМ {muted}- {text}добавить", "{primary}ПКМ {muted}- {text}удалить последнюю",
                "{muted}[console] [player] [message] [broadcast] [sound]"), false));

        List<String> itemLines = new ArrayList<>(r.items);
        for (ItemStack is : r.itemStacks) itemLines.add(is.getAmount() + "x " + is.getType().name());
        inv.setItem(15, Items.make(Material.CHEST, "{primary}Предметы", join(preview(itemLines, "нет предметов"),
                "", "{primary}ЛКМ с предметом на курсоре {muted}- {text}добавить",
                "{primary}ЛКМ без предмета {muted}- {text}ввести МАТЕРИАЛ:кол-во",
                "{primary}ПКМ {muted}- {text}удалить последний"), false));

        inv.setItem(16, Items.make(r.enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                r.enabled ? "{secondary}Включена" : "{error}Выключена",
                List.of("{primary}Клик {muted}- {text}переключить"), false));

        inv.setItem(19, Items.make(Material.CLOCK, "{primary}Кулдаун", List.of(
                "{text}Сейчас: {primary}" + RewardManager.cooldownText(r.cooldown), "",
                "{primary}Клик {muted}- {text}изменить",
                "{muted}Примеры: 1d 2h 30m, 3600, once, none"), false));
        inv.setItem(20, Items.make(Material.TRIPWIRE_HOOK, "{primary}Право", List.of(
                "{text}Сейчас: {primary}" + (r.permission.isBlank() ? "нет" : r.permission), "",
                "{primary}Клик {muted}- {text}изменить",
                "{muted}Напишите - чтобы убрать право"), false));
        inv.setItem(21, Items.make(Material.ITEM_FRAME, "{primary}Слот в меню", List.of(
                "{text}Сейчас: {primary}" + (r.slot < 0 ? "авто" : String.valueOf(r.slot)), "",
                "{primary}Клик {muted}- {text}выбрать слот"), false));
        inv.setItem(22, Items.make(Material.GLOWSTONE_DUST, r.glow ? "{secondary}Свечение: вкл" : "{muted}Свечение: выкл",
                List.of("{primary}Клик {muted}- {text}переключить"), false));
        inv.setItem(24, Items.make(Material.GOLD_NUGGET, "{accent}Выдать мне (тест)", List.of(
                "{text}Выдаёт содержимое награды", "{text}без учёта кулдауна"), false));
        inv.setItem(40, Items.make(Material.ARROW, "{primary}« Назад", List.of(), false));
    }

    private void saveRender() {
        plugin.rewards().save();
        render();
    }

    private void ask(String prompt, Consumer<String> handler) {
        ChatInput.ask(plugin, player, prompt, t -> {
            handler.accept(t);
            plugin.rewards().save();
        }, this::open);
    }

    @Override
    public void click(InventoryClickEvent e) {
        ItemStack cur = e.getCursor();
        boolean hasCursor = cur != null && !cur.getType().isAir();
        boolean right = e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT;

        switch (e.getSlot()) {
            case 10 -> {
                if (hasCursor) {
                    r.icon = cur.getType();
                    saveRender();
                } else {
                    plugin.send(player, "icon-hint");
                }
            }
            case 11 -> ask("Введите новое название награды", t -> r.name = t);
            case 12 -> {
                if (right) {
                    r.lore.clear();
                    saveRender();
                } else {
                    ask("Введите строку описания", t -> r.lore.add(t));
                }
            }
            case 13 -> {
                Reward.Type[] v = Reward.Type.values();
                r.type = v[(r.type.ordinal() + 1) % v.length];
                saveRender();
            }
            case 14 -> {
                if (right) {
                    if (!r.commands.isEmpty()) r.commands.remove(r.commands.size() - 1);
                    saveRender();
                } else {
                    ask("Введите команду (без /), например: [console] give {player} diamond 1", t -> r.commands.add(t));
                }
            }
            case 15 -> {
                if (right) {
                    if (!r.itemStacks.isEmpty()) r.itemStacks.remove(r.itemStacks.size() - 1);
                    else if (!r.items.isEmpty()) r.items.remove(r.items.size() - 1);
                    saveRender();
                } else if (hasCursor) {
                    r.itemStacks.add(cur.clone());
                    saveRender();
                } else {
                    ask("Введите предмет: МАТЕРИАЛ:кол-во[:Название]", t -> {
                        if (Items.parse(t) == null) plugin.send(player, "invalid-item");
                        else r.items.add(t);
                    });
                }
            }
            case 16 -> {
                r.enabled = !r.enabled;
                saveRender();
            }
            case 19 -> ask("Введите кулдаун (1d 2h 30m / секунды / once / none)", t -> {
                long v = RewardManager.parseDuration(t);
                if (v == RewardManager.INVALID) plugin.send(player, "invalid-duration");
                else r.cooldown = v;
            });
            case 20 -> ask("Введите право (или - чтобы убрать)", t -> r.permission = t.equals("-") ? "" : t);
            case 21 -> new SlotPicker(plugin, player, r).open();
            case 22 -> {
                r.glow = !r.glow;
                saveRender();
            }
            case 24 -> {
                plugin.rewards().deliver(player, r);
                plugin.send(player, "tested");
            }
            case 40 -> new EditorMenu(plugin, player).open();
            default -> {
            }
        }
    }
}
