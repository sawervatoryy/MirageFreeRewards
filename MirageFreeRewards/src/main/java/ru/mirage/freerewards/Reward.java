package ru.mirage.freerewards;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class Reward {
    public enum Type { COMMAND, ITEM, DONATE }

    public final String id;
    public boolean enabled = true;
    public String name;
    public List<String> lore = new ArrayList<>();
    public Material icon = Material.CHEST;
    public int slot = -1;
    public Type type = Type.COMMAND;
    /** секунды; -1 = один раз; 0 = без ограничений */
    public long cooldown = 86400;
    public String permission = "";
    public boolean glow = false;
    public List<String> commands = new ArrayList<>();
    public List<String> items = new ArrayList<>();
    public List<ItemStack> itemStacks = new ArrayList<>();

    public Reward(String id) {
        this.id = id;
        this.name = "{primary}" + id;
    }
}
