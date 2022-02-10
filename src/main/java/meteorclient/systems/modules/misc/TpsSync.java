package meteorclient.systems.modules.misc;

import meteorclient.events.entity.player.BreakBlockEvent;
import meteorclient.events.entity.player.InteractBlockEvent;
import meteorclient.settings.BoolSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;

public class TpsSync extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> breakBlockDeSync = sgGeneral.add(new BoolSetting.Builder()
        .name("break")
        .description("Stop de-syncing for block breaking.")
        .defaultValue(true)
        .build()
    );


    private final Setting<Boolean> placeBlockDeSync = sgGeneral.add(new BoolSetting.Builder()
        .name("place")
        .description("Stop de-syncing for block placing.")
        .defaultValue(true)
        .build()
    );

    public TpsSync() {
        super(Categories.Misc, "tps-sync", "Prevent ghost blocks from forming.");
    }


    @EventHandler
    private void onBlockPlace(InteractBlockEvent event) {
        if (placeBlockDeSync.get()) {
            if (event.result.getBlockPos() != null) {
                assert mc.player != null;
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, event.result.getBlockPos(), Direction.UP));
            }
        }
    }

    @EventHandler
    private void onBlockBreak(BreakBlockEvent event) {
        if (breakBlockDeSync.get()) {
            if (event.blockPos != null) {
                assert mc.player != null;
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, event.blockPos, Direction.UP));
            }
        }
    }
}
