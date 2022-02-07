package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.entity.BEntityUtils;
import meteorclient.utils.player.BPlayerUtils;
import meteorclient.events.world.TickEvent;
import meteorclient.settings.BoolSetting;
import meteorclient.settings.DoubleSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.systems.modules.Module;
import meteorclient.utils.entity.EntityUtils;
import meteorclient.utils.entity.SortPriority;
import meteorclient.utils.entity.TargetUtils;
import meteorclient.utils.player.FindItemResult;
import meteorclient.utils.player.InvUtils;
import meteorclient.utils.player.Rotations;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BurrowMiner extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").description("The radius in which players get targeted.").defaultValue(5).min(0).sliderMax(6).build());
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("auto-switch").description("Auto switches to a pickaxe when AutoCity is enabled.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").description("Renders your swing client-side.").defaultValue(false).build());
    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder().name("self-toggle").description("Automatically toggles off after activation.").defaultValue(true).build());
    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder().name("chat-info").description("Sends a message when it is trying to burrow mine someone.").defaultValue(true).build());

    private PlayerEntity target;
    private BlockPos blockPosTarget;
    private boolean sentMessage;

    public BurrowMiner() {
        super(Categories.BANANAPLUS, "burrow-miner", "Automatically mines target's burrow.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            PlayerEntity search = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
            if (search != target) sentMessage = false;
            target = search;
        }

        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            target = null;
            blockPosTarget = null;
            if (selfToggle.get()) toggle();
            return;
        }

        if (BEntityUtils.isBurrowed(target)) {
            blockPosTarget = target.getBlockPos();
        } else if (blockPosTarget == null) {
            if (selfToggle.get()) {
                error("No burrow block found... disabling.");
                toggle();
            }
            target = null;
            return;
        }

        if (BPlayerUtils.distanceTo(blockPosTarget) > mc.interactionManager.getReachDistance() && selfToggle.get()) {
            error("Burrow block out of reach... disabling.");
            toggle();
            return;
        }

        if (!sentMessage && chatInfo.get()) {
            info("Attempting to burrow mine %s.", target.getEntityName());
            sentMessage = true;
        }

        FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);

        if (!pickaxe.isHotbar()) {
            if (selfToggle.get()) {
                error("No pickaxe found... disabling.");
                toggle();
            }
            return;
        }

        if (autoSwitch.get()) InvUtils.swap(pickaxe.slot(), false);

        if (rotate.get())
            Rotations.rotate(Rotations.getYaw(blockPosTarget), Rotations.getPitch(blockPosTarget), () -> mine(blockPosTarget));
        else mine(blockPosTarget);

        if (selfToggle.get()) toggle();
    }

    private void mine(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        else mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
