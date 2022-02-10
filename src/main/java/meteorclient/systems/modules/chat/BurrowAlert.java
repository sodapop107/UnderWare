package meteorclient.systems.modules.chat;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.misc.Wrapper;
import meteorclient.utils.player.AutomationUtils;
import meteorclient.events.world.TickEvent;
import meteorclient.settings.IntSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.systems.modules.Module;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class BurrowAlert extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder().name("range").description("How far away from you to check for burrowed players.").defaultValue(2).min(0).sliderMax(10).build());

    public BurrowAlert() {
        super(Categories.Chat, "burrow-alert", "Alerts you when players are burrowed.");
    }

    private int burrowMsgWait;
    public static List<PlayerEntity> burrowedPlayers = new ArrayList<>();

    @Override
    public void onActivate() {
        burrowMsgWait = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (isValid(player)) {
                burrowedPlayers.add(player);
                warning(player.getEntityName() + " is burrowed!");
            }
            if (burrowedPlayers.contains(player) && !AutomationUtils.isBurrowed(player, true)) {
                burrowedPlayers.remove(player);
                warning(player.getEntityName() + " is no longer burrowed.");
            }
        }
    }

    private boolean isValid(PlayerEntity p) {
        if (p == mc.player) return false;
        return mc.player.distanceTo(p) <= range.get() && !burrowedPlayers.contains(p) && AutomationUtils.isBurrowed(p, true) && !Wrapper.isPlayerMoving(p);
    }



}
