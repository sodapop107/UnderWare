package meteorclient.systems.modules.banana;

import meteorclient.events.world.TickEvent;
import meteorclient.settings.BoolSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;
import meteorclient.utils.player.FindItemResult;
import meteorclient.utils.player.InvUtils;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoSnowball extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoToggle = sgGeneral.add(new BoolSetting.Builder().name("auto-toggle").description("Toggles off when no snowball is found.").defaultValue(true).build());

    public AutoSnowball() {
        super(Categories.BANANAPLUS, "auto-snowball", "Automatically throws snowballs in your hotbar (very scary).");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        FindItemResult snowball = InvUtils.findInHotbar(Items.SNOWBALL);

        if (!snowball.found() && autoToggle.get()) {
            error("No snowballs found... disabling");
            toggle();
            return;
        } else if (snowball.found()) throwSnowball(snowball);
    }

    private void throwSnowball(FindItemResult snowball) {
        if (snowball.isOffhand()) {
            mc.interactionManager.interactItem(mc.player, mc.world, Hand.OFF_HAND);
        } else {
            InvUtils.swap(snowball.slot(), true);
            mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
            InvUtils.swapBack();
        }
    }
}
