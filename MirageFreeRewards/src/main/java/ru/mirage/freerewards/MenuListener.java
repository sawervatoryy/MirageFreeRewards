package ru.mirage.freerewards;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import ru.mirage.freerewards.menu.Menu;

public final class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof Menu menu)) return;

        int raw = e.getRawSlot();
        if (raw >= 0 && raw < top.getSize()) {
            e.setCancelled(true);
            menu.click(e);
            return;
        }
        boolean bad = !menu.allowBottom()
                || raw < 0
                || e.isShiftClick()
                || e.getAction() == InventoryAction.COLLECT_TO_CURSOR
                || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY;
        if (bad) e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof Menu)) return;
        int size = top.getSize();
        for (int s : e.getRawSlots()) {
            if (s < size) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Menu menu) menu.onClose();
    }
}
