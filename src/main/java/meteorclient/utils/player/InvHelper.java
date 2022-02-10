package meteorclient.utils.player;

import net.minecraft.item.AirBlockItem;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;


import static meteorclient.UnderWare.mc;

public class InvHelper {

    public static Integer getEmptySlots() {
        int emptySlots = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) emptySlots++;
        }
        return emptySlots;
    }

    public static boolean isInventoryFull() {
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) return false;
        }
        return true;
    }

    public static void clickSlot(int slot, int button, SlotActionType action) {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, button, action, mc.player);
    }

    public static int invIndexToSlotId(int invIndex) {
        if (invIndex < 9 && invIndex != -1) return 44 - (8 - invIndex);
        return invIndex;
    }

    public static String getItemName(int slot) {
        return mc.player.getInventory().getStack(slot).getName().toString();
    }
}
