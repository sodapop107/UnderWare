package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.entity.BEntityUtils;
import meteorclient.events.game.OpenScreenEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.settings.BoolSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.systems.modules.Module;
import meteorclient.utils.player.FindItemResult;
import meteorclient.utils.player.InvUtils;
import meteorclient.utils.world.BlockUtils;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.util.math.BlockPos;

public class SelfAnvilPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotates towards where you will be placing the anvil.").defaultValue(false).build());
    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder().name("only-in-hole").description("Only functions when you are standing in a hole.").defaultValue(false).build());
    private final Setting<Boolean> placeTop = sgGeneral.add(new BoolSetting.Builder().name("place-anvil-top").description("Places anvil above you.").defaultValue(true).build());
    private final Setting<Boolean> placeTop2 = sgGeneral.add(new BoolSetting.Builder().name("place-anvil-2-top").description("Places anvil 2 blocks above you.").defaultValue(false).build());
    private final Setting<Boolean> placeTop3 = sgGeneral.add(new BoolSetting.Builder().name("place-anvil-3-top").description("Places anvil 3 blocks above you.").defaultValue(false).build());

    public SelfAnvilPlus() {
        super(Categories.BANANAPLUS, "self-anvil+", "Automatically places an anvil on you to prevent other players from going into your hole.");
    }

    private boolean sentMessage;
    private boolean placed1;
    private boolean placed2;
    private boolean placed3;

    @Override
    public void onActivate() {
        sentMessage = false;
        placed1 = false;
        placed2 = false;
        placed3 = false;
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof AnvilScreen) event.cancel();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (onlyInHole.get() && !BEntityUtils.isInHole(true)) {
            if (!sentMessage) {
                warning("Not in a hole! Waiting for toggle / move in hole");
                sentMessage = true;
            }
        } else {
            FindItemResult anvil = InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()) instanceof AnvilBlock);

            BlockPos head = mc.player != null ? mc.player.getBlockPos().up() : null;

            if (mc.world.getBlockState(head.up()).isAir() && placeTop.get() && !placed1) {
                BlockUtils.place(mc.player.getBlockPos().add(0, 2, 0), anvil, rotate.get(), 0);
                placed1 = true;
            }

            if (mc.world.getBlockState(head.up(2)).isAir() && placeTop2.get() && !placed2) {
                BlockUtils.place(mc.player.getBlockPos().add(0, 3, 0), anvil, rotate.get(), 0);
                placed2 = true;
            }

            if (mc.world.getBlockState(head.up(3)).isAir() && placeTop3.get() && !placed3) {
                BlockUtils.place(mc.player.getBlockPos().add(0, 4, 0), anvil, rotate.get(), 0);
                placed3 = true;
            }

            toggle();
        }
    }
}
