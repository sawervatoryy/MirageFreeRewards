package ru.mirage.freerewards.menu;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public interface Menu extends InventoryHolder {
    /** Клик по верхнему инвентарю (событие уже отменено). */
    void click(InventoryClickEvent e);

    default void onClose() {}

    /** Разрешать ли клики по инвентарю игрока (нужно редактору для предмета на курсоре). */
    default boolean allowBottom() {
        return false;
    }
}
