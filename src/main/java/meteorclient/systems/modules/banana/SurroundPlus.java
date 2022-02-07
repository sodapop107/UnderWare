package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.misc.Timer;
import meteorclient.utils.world.BWorldUtils;
import meteorclient.events.packets.PacketEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.settings.*;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.modules.movement.Step;
import meteorclient.systems.modules.movement.speed.Speed;
import meteorclient.systems.modules.movement.Blink;
import meteorclient.utils.misc.Keybind;
import meteorclient.utils.player.InvUtils;
import meteorclient.utils.player.PlayerUtils;
import meteorclient.utils.world.Dimension;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SurroundPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgToggle = settings.createGroup("Different Toggle Modes");
    private final SettingGroup sgModules = settings.createGroup("Other Module Toggles");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("Mode").description("The mode at which Surround operates in.").defaultValue(Mode.Normal).build());
    private final Setting<Keybind> wideKeybind = sgGeneral.add(new KeybindSetting.Builder().name("force-russian-keybind").description("Turns on Russian surround when held").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Keybind> widePlusKeybind = sgGeneral.add(new KeybindSetting.Builder().name("force-russian+-keybind").description("Turns on russian+ when held").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Boolean> doubleHeight = sgGeneral.add(new BoolSetting.Builder().name("double-height").description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.").defaultValue(false).build());
    private final Setting<Keybind> doubleHeightKeybind = sgGeneral.add(new KeybindSetting.Builder().name("double-height-keybind").description("Turns on double height.").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Primary> primary = sgGeneral.add(new EnumSetting.Builder<Primary>().name("Primary block").description("Primary block to use.").defaultValue(Primary.Obsidian).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("Delay").description("Delay in ticks between placing blocks.").defaultValue(0).sliderMin(0).sliderMax(10).build());
    private final Setting<Boolean> stayOn = sgGeneral.add(new BoolSetting.Builder().name("Blinkers").description("Surround stays on when you are in blink").defaultValue(false).build());
    private final Setting<Boolean> snap = sgGeneral.add(new BoolSetting.Builder().name("Center").description("Will align you at the center of your hole when you turn this on.").defaultValue(true).build());
    private final Setting<Integer> centerDelay = sgGeneral.add(new IntSetting.Builder().name("Center Delay").description("Delay in ticks before you get centered.").visible(snap::get).defaultValue(0).sliderMin(0).sliderMax(10).build());
    private final Setting<Boolean> placeOnCrystal = sgGeneral.add(new BoolSetting.Builder().name("Ignore entities").description("Will try to place even if there is an entity in its way.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("Rotate").description("Whether to rotate or not.").defaultValue(false).build());
    private final Setting<Boolean> air = sgGeneral.add(new BoolSetting.Builder().name("Air-Place").description("Whether to place blocks midair or not.").defaultValue(true).build());
    private final Setting<Boolean> allBlocks = sgGeneral.add(new BoolSetting.Builder().name("Any-blastproof").description("Will allow any blast proof block to be used.").defaultValue(true).build());
    private final Setting<Boolean> notifyBreak = sgGeneral.add(new BoolSetting.Builder().name("notify-break").description("Notifies you about who is breaking your surround.").defaultValue(false).build());
    private final Setting<Boolean> antiSurroundBreak = sgGeneral.add(new BoolSetting.Builder().name("anti-surround-break").description("Activates Russian+ mode when someone tries to city you.").defaultValue(false).build());
    public final Setting<Boolean> ignoreOpenable = sgGeneral.add(new BoolSetting.Builder().name("Ignore-openable-blocks").description("Ignores openable blocks when placing surround.").defaultValue(false).build());
    private final Setting<Boolean> onlyGround = sgToggle.add(new BoolSetting.Builder().name("Only On Ground").description("Will not attempt to place while you are not standing on ground.").defaultValue(false).build());
    private final Setting<Boolean> disableOnYChange = sgToggle.add(new BoolSetting.Builder().name("disable-on-y-change").description("Automatically disables when your y level (step, jumping, atc).").defaultValue(false).build());
    private final Setting<Boolean> disableOnLeaveHole = sgToggle.add(new BoolSetting.Builder().name("disable-on-hole-leave").description("Automatically disables when you leave your hole (normal)").defaultValue(true).build());
    private final Setting<Boolean> surroundUp = sgToggle.add(new BoolSetting.Builder().name("Surround-Up").description("helps you surround up").defaultValue(false).visible(disableOnLeaveHole::get).build());
    public final Setting<Boolean> pauseAntiClick = sgModules.add(new BoolSetting.Builder().name("Pause-Anti-Click").description("Pauses anti click when surround is enabled.").defaultValue(false).build());
    private final Setting<Boolean> toggleStep = sgModules.add(new BoolSetting.Builder().name("toggle-step").description("Toggles off step when activating surround.").defaultValue(false).build());
    private final Setting<Boolean> toggleSpeed = sgModules.add(new BoolSetting.Builder().name("toggle-speed").description("Toggles off speed when activating surround.").defaultValue(false).build());
    private final Setting<Boolean> toggleStrafe = sgModules.add(new BoolSetting.Builder().name("toggle-strafe+").description("Toggles off strafe+ when activating surround.").defaultValue(false).build());
    private final Setting<Boolean> toggleBack = sgModules.add(new BoolSetting.Builder().name("toggle-back").description("Toggles on speed or surround when turning off surround.").defaultValue(false).build());

    public static SurroundPlus INSTANCE;

    private BlockPos lastPos = new BlockPos(0, -100, 0);
    private int ticks = 0;

    private boolean hasCentered = false, shouldExtra;
    private Timer onGroundCenter = new Timer();
    private BlockPos prevBreakPos;

    private static final Timer surroundInstanceDelay = new Timer();
    int timeToStart = 0;
    public static void setSurroundWait(int timeToStart) {
        INSTANCE.timeToStart = timeToStart;
    }

    boolean doSnap = true;
    public static void toggleCenter(boolean doSnap) {
        INSTANCE.doSnap = doSnap;
    }

    Modules modules = Modules.get();

    public SurroundPlus() {
        super(Categories.BANANAPLUS, "surround+", "Surrounds you in blocks to prevent you from taking lots of damage.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if(onGroundCenter.passedTicks(centerDelay.get()) && snap.get() && doSnap && !hasCentered && mc.player.isOnGround()) {
            BWorldUtils.snapPlayer(lastPos);
            hasCentered = true;
        }

        if(!hasCentered && !mc.player.isOnGround()) {
            onGroundCenter.reset();
        }

        BlockPos roundedPos = BWorldUtils.roundBlockPos(mc.player.getPos());
        if(onlyGround.get() && !mc.player.isOnGround() && roundedPos.getY() <= lastPos.getY()) {
            lastPos = BWorldUtils.roundBlockPos(mc.player.getPos());
        }

        if(surroundInstanceDelay.passedMillis(timeToStart) && (mc.player.isOnGround() || !onlyGround.get())) {
            if(delay.get() != 0 && ticks++ % delay.get() != 0) return;

            if (!modules.get(Blink.class).isActive() || !stayOn.get())
            {
                AbstractClientPlayerEntity loc = mc.player;
                BlockPos locRounded = BWorldUtils.roundBlockPos(loc.getPos());
                if(!lastPos.equals(loc.isOnGround() ? locRounded : loc.getBlockPos())) {
                    if((disableOnYChange.get() && mc.player.prevY < mc.player.getY()) ||
                            onlyGround.get() || disableOnLeaveHole.get() && (!(loc.getPos().y <= lastPos.getY() + 1.5)
                            || ((Math.floor(loc.getPos().x) != lastPos.getX() || Math.floor(loc.getPos().z) != lastPos.getZ()) && !(loc.getPos().y <= lastPos.getY() + 0.75))
                            || (!mc.world.getBlockState(lastPos).getMaterial().isReplaceable() && loc.getBlockPos() != lastPos)
                            || (surroundUp.get() && ((mc.options.keyJump.isPressed() || mc.player.input.jumping) && (mc.options.keyLeft.isPressed() || mc.player.input.pressingLeft || mc.options.keyRight.isPressed() || mc.player.input.pressingRight || mc.options.keyForward.isPressed() || mc.player.input.pressingForward || mc.options.keyBack.isPressed() || mc.player.input.pressingBack))))) {
                        doToggle();
                        return;
                    }
                    if(!onlyGround.get() && locRounded.getY() <= lastPos.getY() || !disableOnLeaveHole.get() || (surroundUp.get() && !(mc.options.keyLeft.isPressed() && mc.player.input.pressingLeft && mc.options.keyRight.isPressed() && mc.player.input.pressingRight && mc.options.keyForward.isPressed() && mc.player.input.pressingForward && mc.options.keyBack.isPressed() && mc.player.input.pressingBack))) {
                        lastPos = locRounded;
                    }
                }
            }

            int obbyIndex = findBlock();
            if(obbyIndex == -1) return;
            int prevSlot = mc.player.getInventory().selectedSlot;

            if(needsToPlace()) {
                for(BlockPos pos : getPositions()) {
                    if(mc.world.getBlockState(pos).getMaterial().isReplaceable())

                        mc.player.getInventory().selectedSlot = obbyIndex;
                    if(BWorldUtils.placeBlockMainHand(pos, rotate.get(), air.get(), placeOnCrystal.get())) {

                        if(delay.get() != 0) {
                            mc.player.getInventory().selectedSlot = prevSlot;
                            return;
                        }
                    }
                }

                mc.player.getInventory().selectedSlot = prevSlot;
            }
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        shouldExtra = false;
    }

    private void doToggle() {
        if (toggleBack.get()) {
            if (toggleStep.get() && !modules.isActive(Step.class)) modules.get(Step.class).toggle();
            if (toggleSpeed.get() && !modules.isActive(Speed.class)) modules.get(Speed.class).toggle();
            if (toggleStrafe.get() && !modules.isActive(StrafePlus.class)) modules.get(StrafePlus.class).toggle();
        }
        toggle();
    }

    private boolean needsToPlace() {
        return anyAir(lastPos.down(), lastPos.north(), lastPos.east(), lastPos.south(), lastPos.west(),
                lastPos.north().up(), lastPos.east().up(), lastPos.south().up(), lastPos.west().up(),
                lastPos.north(2), lastPos.east(2), lastPos.south(2), lastPos.west(2),
                lastPos.north().east(), lastPos.east().south(), lastPos.south().west(), lastPos.west().north());
    }

    private List<BlockPos> getPositions() {
        List<BlockPos> positions = new ArrayList<>();
        if(!onlyGround.get()) add(positions, lastPos.down());
        add(positions, lastPos.north());
        add(positions, lastPos.east());
        add(positions, lastPos.south());
        add(positions, lastPos.west());

        if (doubleHeight.get() || doubleHeightKeybind.get().isPressed())
        {
            add(positions, lastPos.north().up());
            add(positions, lastPos.east().up());
            add(positions, lastPos.south().up());
            add(positions, lastPos.west().up());
        }

        if(mode.get() != Mode.Normal || wideKeybind.get().isPressed() || widePlusKeybind.get().isPressed() || shouldExtra) {
            if(mc.world.getBlockState(lastPos.north()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.north(2));
            if(mc.world.getBlockState(lastPos.east()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.east(2));
            if(mc.world.getBlockState(lastPos.south()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.south(2));
            if(mc.world.getBlockState(lastPos.west()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.west(2));
        }

        if(mode.get() == Mode.RussianPlus || widePlusKeybind.get().isPressed() || shouldExtra) {
            if(mc.world.getBlockState(lastPos.north()).getBlock() != Blocks.BEDROCK || mc.world.getBlockState(lastPos.east()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.north().east());
            if(mc.world.getBlockState(lastPos.east()).getBlock() != Blocks.BEDROCK || mc.world.getBlockState(lastPos.south()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.east().south());
            if(mc.world.getBlockState(lastPos.south()).getBlock() != Blocks.BEDROCK || mc.world.getBlockState(lastPos.west()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.south().west());
            if(mc.world.getBlockState(lastPos.west()).getBlock() != Blocks.BEDROCK || mc.world.getBlockState(lastPos.north()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.west().north());

        }
        return positions;
    }

    private void add(List<BlockPos> list, BlockPos pos) {
        if(mc.world.getBlockState(pos).isAir() && allAir(pos.north(), pos.east(), pos.south(), pos.west(), pos.up(), pos.down()) && !air.get()) list.add(pos.down());
        list.add(pos);
    }

    private boolean allAir(BlockPos... pos) {
        return Arrays.stream(pos).allMatch(blockPos -> mc.world.getBlockState(blockPos).isAir());
    }

    private boolean anyAir(BlockPos... pos) {
        return Arrays.stream(pos).anyMatch(blockPos -> mc.world.getBlockState(blockPos).isAir());
    }

    private Block primaryBlock(){
        Block index = null;
        if (primary.get() == Primary.Obsidian) {index = Blocks.OBSIDIAN;}
        else if (primary.get() == Primary.EnderChest) {index = Blocks.ENDER_CHEST;}
        else if (primary.get() == Primary.CryingObsidian) {index = Blocks.CRYING_OBSIDIAN;}
        else if (primary.get() == Primary.NetheriteBlock) {index = Blocks.NETHERITE_BLOCK;}
        else if (primary.get() == Primary.AncientDebris) {index = Blocks.ANCIENT_DEBRIS;}
        else if (primary.get() == Primary.RespawnAnchor) {index = Blocks.RESPAWN_ANCHOR;}
        else if (primary.get() == Primary.Anvil) {index = Blocks.ANVIL;}
        return index;
    }

    private int findBlock() {
        int index = InvUtils.findInHotbar(primaryBlock().asItem()).slot();
        if (index == -1 && allBlocks.get()) {
            for(int i = 0; i < 9; ++i) {
                Item item = mc.player.getInventory().getStack(i).getItem();
                if (item instanceof BlockItem) {
                    if (item == Items.ANCIENT_DEBRIS || item == Items.OBSIDIAN || item == Items.CRYING_OBSIDIAN || item == Items.ANVIL || item == Items.CHIPPED_ANVIL || item == Items.DAMAGED_ANVIL || item == Items.ENCHANTING_TABLE || item == Items.ENDER_CHEST  || item == Items.NETHERITE_BLOCK || (PlayerUtils.getDimension() != Dimension.Nether && item == Items.RESPAWN_ANCHOR)) {
                        return i;
                    }
                }
            }
        }
        return index;
    }

    @Override
    public void onActivate() {
        lastPos = mc.player.isOnGround() ? BWorldUtils.roundBlockPos(mc.player.getPos()) : mc.player.getBlockPos();
        if (toggleStep.get() && modules.isActive(Step.class)) {
            (modules.get(Step.class)).toggle();
        }

        if (toggleSpeed.get() && modules.isActive(Speed.class)) {
            (modules.get(Speed.class)).toggle();
        }

        if (toggleStrafe.get() && modules.isActive(StrafePlus.class)) {
            (modules.get(StrafePlus.class)).toggle();
        }
    }

    @Override
    public void onDeactivate() {
        ticks = 0;
        doSnap = true;
        timeToStart = 0;
        hasCentered = false;
    }

    PlayerEntity prevBreakingPlayer = null;

    @EventHandler
    public void onBreakPacket(PacketEvent.Receive event) {
        if(!(event.packet instanceof BlockBreakingProgressS2CPacket)) return;
        BlockBreakingProgressS2CPacket bbpp = (BlockBreakingProgressS2CPacket) event.packet;
        BlockPos bbp = bbpp.getPos();
        if(antiSurroundBreak.get()) {
            shouldExtra = true;
        }
        if (notifyBreak.get()) {
            PlayerEntity breakingPlayer = (PlayerEntity) mc.world.getEntityById(bbpp.getEntityId());
            BlockPos playerBlockPos = mc.player.getBlockPos();

            if (bbpp.getProgress() > 0 ) return;
            if (bbp.equals(prevBreakPos)) return;
            if (breakingPlayer == prevBreakingPlayer) return;
            if (breakingPlayer.equals(mc.player)) return;

            if (bbp.equals(playerBlockPos.north())) {
                notifySurroundBreak(Direction.NORTH, breakingPlayer);
            } else if (bbp.equals(playerBlockPos.east())) {
                notifySurroundBreak(Direction.EAST, breakingPlayer);
            } else if (bbp.equals(playerBlockPos.south())) {
                notifySurroundBreak(Direction.SOUTH, breakingPlayer);
            } else if (bbp.equals(playerBlockPos.west())) {
                notifySurroundBreak(Direction.WEST, breakingPlayer);
            }

            prevBreakingPlayer = breakingPlayer;

            prevBreakPos = bbp;
        }
    }

    private void notifySurroundBreak(Direction direction, PlayerEntity player) {
        switch (direction) {
            case NORTH: {
                warning("Your north surround block is being broken by " + player.getEntityName());
                break;
            }
            case EAST: {
                warning("Your east surround block is being broken by " + player.getEntityName());
                break;
            }
            case SOUTH: {
                warning("Your south surround block is being broken by " + player.getEntityName());
                break;
            }
            case WEST: {
                warning("Your west surround block is being broken by " + player.getEntityName());
                break;
            }
        }
    }

    public enum Mode {
        Normal, Russian, RussianPlus
    }

    public enum Primary {
        Obsidian, EnderChest, CryingObsidian, NetheriteBlock, AncientDebris, RespawnAnchor, Anvil
    }
}
