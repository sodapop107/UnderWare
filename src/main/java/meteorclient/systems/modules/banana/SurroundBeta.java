package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.world.BWorldUtils;
import meteorclient.utils.misc.Timer;
import meteorclient.events.packets.PacketEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.settings.*;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.modules.movement.Blink;
import meteorclient.systems.modules.movement.Step;
import meteorclient.systems.modules.movement.speed.Speed;
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


public class SurroundBeta extends Module {
    private final SettingGroup sgMode = settings.createGroup("Surround Mode");
    private final SettingGroup sgKeyBinds = settings.createGroup("KeyBinds");
    private final SettingGroup sgBlocks = settings.createGroup("Blocks To Use");
    private final SettingGroup sgSettings = settings.createGroup("Settings");
    private final SettingGroup sgToggle = settings.createGroup("Different Toggle Modes");
    private final SettingGroup sgModules = settings.createGroup("Other Module Toggles");
    private final SettingGroup sgWIP = settings.createGroup("Anti Surround Break");
    private final SettingGroup sgD = settings.createGroup("Debug");

    private final Setting<Mode> mode = sgMode.add(new EnumSetting.Builder<Mode>().name("Mode").description("The mode at which Surround operates in.").defaultValue(Mode.Normal).build());
    public final Setting<Boolean> fourHole = sgMode.add(new BoolSetting.Builder().name("2x2").description("2x2 surround").defaultValue(false).build());
    public final Setting<Boolean> twoHole = sgMode.add(new BoolSetting.Builder().name("1x2").description("1x2 surround").defaultValue(false).build());
    private final Setting<Boolean> doubleHeight = sgMode.add(new BoolSetting.Builder().name("double-height").description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.").defaultValue(false).build());
    private final Setting<Boolean> doubleHeight2X2 = sgMode.add(new BoolSetting.Builder().name("double-height-2x2").description("Double height for 2x2 surround.").defaultValue(false).visible(fourHole::get).build());
    private final Setting<Boolean> doubleHeight1X2 = sgMode.add(new BoolSetting.Builder().name("double-height-1x2").description("double height for 1x2 surround").defaultValue(false).visible(twoHole::get).build());
    private final Setting<Boolean> keyBindMode = sgKeyBinds.add(new BoolSetting.Builder().name("Hold or Toggle").description("If the keybinds should be hold or toggle. Default: hold").defaultValue(false).build());
    private final Setting<Keybind> doubleHeightKeybind = sgKeyBinds.add(new KeybindSetting.Builder().name("double-height-keybind").description("Turns on double height.").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Keybind> wideKeybind = sgKeyBinds.add(new KeybindSetting.Builder().name("force-russian-keybind").description("Turns on Russian surround when held").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Keybind> widePlusKeybind = sgKeyBinds.add(new KeybindSetting.Builder().name("force-russian+-keybind").description("Turns on russian+ when held").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Primary> primary = sgBlocks.add(new EnumSetting.Builder<Primary>().name("Primary block").description("Primary block to use.").defaultValue(Primary.Obsidian).build());
    private final Setting<Boolean> allBlocks = sgBlocks.add(new BoolSetting.Builder().name("Any-blastproof").description("Will allow any blast proof block to be used.").defaultValue(true).build());
    private final Setting<Integer> delay = sgSettings.add(new IntSetting.Builder().name("Delay").description("Delay in ticks between placing blocks.").defaultValue(0).sliderMin(0).sliderMax(10).build());
    private final Setting<Boolean> stayOn = sgSettings.add(new BoolSetting.Builder().name("Blinkers").description("Surround stays on when you are in blink").defaultValue(false).build());
    private final Setting<Boolean> snap = sgSettings.add(new BoolSetting.Builder().name("Center").description("Will align you at the center of your hole when you turn this on.").defaultValue(true).build());
    private final Setting<Integer> centerDelay = sgSettings.add(new IntSetting.Builder().name("Center Delay").description("Delay in ticks before you get centered.").visible(snap::get).defaultValue(0).sliderMin(0).sliderMax(10).build());
    private final Setting<Boolean> placeOnCrystal = sgSettings.add(new BoolSetting.Builder().name("Ignore entities").description("Will try to place even if there is an entity in its way.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgSettings.add(new BoolSetting.Builder().name("Rotate").description("Whether to rotate or not.").defaultValue(false).build());
    private final Setting<Boolean> air = sgSettings.add(new BoolSetting.Builder().name("Air-Place").description("Whether to place blocks midair or not.").defaultValue(true).build());
    public final Setting<Boolean> ignoreOpenable = sgSettings.add(new BoolSetting.Builder().name("Ignore-openable-blocks").description("Ignores openable blocks when placing surround.").defaultValue(false).build());
    private final Setting<Boolean> notifyBreak = sgSettings.add(new BoolSetting.Builder().name("notify-break").description("Notifies you about who is breaking your surround.").defaultValue(false).build());
    private final Setting<Boolean> onlyGround = sgToggle.add(new BoolSetting.Builder().name("Only On Ground").description("Will not attempt to place while you are not standing on ground.").defaultValue(false).build());
    private final Setting<Boolean> disableOnYChange = sgToggle.add(new BoolSetting.Builder().name("disable-on-y-change").description("Automatically disables when your y level (step, jumping, atc). (recommended)").defaultValue(false).build());
    private final Setting<Boolean> disableOnLeaveHole = sgToggle.add(new BoolSetting.Builder().name("disable-on-hole-leave").description("Automatically disables when you leave your hole.").defaultValue(true).build());
    private final Setting<Boolean> surroundUp = sgToggle.add(new BoolSetting.Builder().name("Surround-Up").description("helps you surround up").defaultValue(false).visible(disableOnLeaveHole::get).build());
    private final Setting<Boolean> toggleStep = sgModules.add(new BoolSetting.Builder().name("toggle-step").description("Toggles off step when activating surround.").defaultValue(false).build());
    private final Setting<Boolean> toggleSpeed = sgModules.add(new BoolSetting.Builder().name("toggle-speed").description("Toggles off speed when activating surround.").defaultValue(false).build());
    private final Setting<Boolean> toggleStrafe = sgModules.add(new BoolSetting.Builder().name("toggle-strafe+").description("Toggles off strafe+ when activating surround.").defaultValue(false).build());
    private final Setting<Boolean> toggleBack = sgModules.add(new BoolSetting.Builder().name("toggle-back").description("Toggles on speed or surround when turning off surround.").defaultValue(false).build());
    private final Setting<Boolean> moveInHole = sgModules.add(new BoolSetting.Builder().name("Stay on").description("Don't toggle between 2x2, 1x2 and 1x1 holes").defaultValue(false).build());
    private final Setting<Boolean> antiSurroundBreak = sgWIP.add(new BoolSetting.Builder().name("anti-surround-break").description("Activates a wider surround mode when someone tries to city you.").defaultValue(false).build());
    private final Setting<AntiBreakMode> antiMode = sgWIP.add(new EnumSetting.Builder<AntiBreakMode>().name("anti-Mode").description("The mode at which anti surround break operates in.").defaultValue(AntiBreakMode.RussianPlus).visible(antiSurroundBreak::get).build());
    private final Setting<Integer> breakProgress = sgWIP.add(new IntSetting.Builder().name("break-progress").description("at wish percent to activate anti-surround-break").defaultValue(0).sliderMin(0).sliderMax(100).max(100).visible(antiSurroundBreak::get).build());
    private final Setting<Boolean> smartAnti = sgWIP.add(new BoolSetting.Builder().name("smart-anti").description("Activates a wider surround mode on the side that is being broken.").defaultValue(false).visible(antiSurroundBreak::get).build());
    private final Setting<Boolean> block = sgD.add(new BoolSetting.Builder().name("DeBug").description("info about module").defaultValue(false).build());

    private BlockPos lastPos = new BlockPos(0, -100, 0);
    private BlockPos lastPosNotS = new BlockPos(0, -100, 0);
    BlockPos locNotSingle = new BlockPos(0, -100, 0);

    public static SurroundBeta INSTANCE;

    boolean pressedDoub = false, canPressDoub = true;

    int nS = 0, wE = 0;
    double nSPos = 0, wEPos = 0;

    private int ticks = 0;

    private boolean hasCentered = false, shouldExtra, should1x2NS = false, should1x2WE = false, should2x2 = false;
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

    public SurroundBeta() {
        super(Categories.BANANAPLUS, "Beta Surround+", "Surrounds you in blocks to prevent you from taking lots of damage.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        if (!doubleHeightKeybind.get().isPressed()) canPressDoub = true;
        if (doubleHeightKeybind.get().isPressed() && pressedDoub && canPressDoub) {
            pressedDoub = false;
            canPressDoub = false;
        }

        if (onGroundCenter.passedTicks(centerDelay.get()) && snap.get() && doSnap && !hasCentered && mc.player.isOnGround()) {
            BWorldUtils.snapPlayer(lastPos);
            hasCentered = true;
        }

        if (!hasCentered && !mc.player.isOnGround()) {
            onGroundCenter.reset();
        }

        BlockPos roundedPos = BWorldUtils.roundBlockPos(mc.player.getPos());
        if (onlyGround.get() && !mc.player.isOnGround() && roundedPos.getY() <= lastPos.getY()) {
            lastPos = BWorldUtils.roundBlockPos(mc.player.getPos());
        }

        if (surroundInstanceDelay.passedMillis(timeToStart) && (mc.player.isOnGround() || !onlyGround.get())) {
            if (delay.get() != 0 && ticks++ % delay.get() != 0) return;

            if (!modules.get(Blink.class).isActive() || !stayOn.get()) {
                AbstractClientPlayerEntity loc = mc.player;
                BlockPos locRounded = BWorldUtils.roundBlockPos(loc.getPos());
                locNotSingle = new BlockPos(wEPos - wE, mc.player.getBlockPos().getY(), nSPos - nS);

                if (block.get()) info("blockPos: " + String.valueOf(locNotSingle));

                if (twoHole.get() || fourHole.get()) {
                    if ((disableOnYChange.get() && mc.player.prevY < mc.player.getY())
                            || onlyGround.get() || (disableOnLeaveHole.get() && !should1x2NS && !should1x2WE && (((Math.floor(loc.getPos().x) != lastPos.getX() || Math.floor(loc.getPos().z) != lastPos.getZ()))))
                            || disableOnLeaveHole.get() && ((surroundUp.get() && ((mc.options.keyJump.isPressed() || mc.player.input.jumping) && (mc.options.keyLeft.isPressed() || mc.player.input.pressingLeft || mc.options.keyRight.isPressed() || mc.player.input.pressingRight || mc.options.keyForward.isPressed() || mc.player.input.pressingForward || mc.options.keyBack.isPressed() || mc.player.input.pressingBack))))) {
                        doToggle();
                        return;
                    }

                    if (!(((Math.abs(mc.player.getPos().x) - Math.floor(Math.abs(mc.player.getPos().x))) > 0.3) && ((Math.abs(mc.player.getPos().x) - Math.floor(Math.abs(mc.player.getPos().x))) < 0.7))
                            && (!onlyGround.get() && locRounded.getY() <= lastPos.getY() || !disableOnLeaveHole.get() || (surroundUp.get() && !(mc.options.keyLeft.isPressed() && mc.player.input.pressingLeft && mc.options.keyRight.isPressed() && mc.player.input.pressingRight && mc.options.keyForward.isPressed() && mc.player.input.pressingForward && mc.options.keyBack.isPressed() && mc.player.input.pressingBack)))) {
                        wE = 1;
                        wEPos = Math.round(mc.player.getPos().x);
                        should1x2WE = true;
                    } else {
                        wEPos = mc.player.getBlockPos().getX();
                        wE = 0;
                        should1x2WE = false;
                    }

                    if (!(((Math.abs(mc.player.getPos().z) - Math.floor(Math.abs(mc.player.getPos().z))) > 0.3) && ((Math.abs(mc.player.getPos().z) - Math.floor(Math.abs(mc.player.getPos().z))) < 0.7))
                            && (!onlyGround.get() && locRounded.getY() <= lastPos.getY() || !disableOnLeaveHole.get() || (surroundUp.get() && !(mc.options.keyLeft.isPressed() && mc.player.input.pressingLeft && mc.options.keyRight.isPressed() && mc.player.input.pressingRight && mc.options.keyForward.isPressed() && mc.player.input.pressingForward && mc.options.keyBack.isPressed() && mc.player.input.pressingBack)))) {
                        nSPos = Math.round(mc.player.getPos().z);
                        nS = 1;
                        should1x2NS = true;
                    } else {
                        nSPos = mc.player.getBlockPos().getZ();
                        nS = 0;
                        should1x2NS = false;
                    }

                    if (should1x2NS && should1x2WE) should2x2 = true;
                    else should2x2 = false;

                } else {
                    should2x2 = false;
                    should1x2WE = false;
                    should1x2NS = false;
                }

                if (!(lastPos.equals(loc.isOnGround() ? locRounded : loc.getBlockPos())) && ((!should1x2NS && !should1x2WE && !should2x2) || (!twoHole.get() && !fourHole.get()))) {

                    if ((disableOnYChange.get() && mc.player.prevY < mc.player.getY()) || onlyGround.get() || disableOnLeaveHole.get() && ((surroundUp.get() && ((mc.options.keyJump.isPressed() || mc.player.input.jumping) && (mc.options.keyLeft.isPressed() || mc.player.input.pressingLeft || mc.options.keyRight.isPressed() || mc.player.input.pressingRight || mc.options.keyForward.isPressed() || mc.player.input.pressingForward || mc.options.keyBack.isPressed() || mc.player.input.pressingBack)))) || ((!should1x2NS && !should1x2WE && !should2x2) && (!(loc.getPos().y <= lastPos.getY() + 1.5) || ((Math.floor(loc.getPos().x) != lastPos.getX() || Math.floor(loc.getPos().z) != lastPos.getZ()) && !(loc.getPos().y <= lastPos.getY() + 0.75)) || (!mc.world.getBlockState(lastPos).getMaterial().isReplaceable() && loc.getBlockPos() != lastPos)))) {
                        doToggle();
                        return;
                    }

                    if (!onlyGround.get() && locRounded.getY() <= lastPos.getY() || !disableOnLeaveHole.get() || (surroundUp.get() && !(mc.options.keyLeft.isPressed() && mc.player.input.pressingLeft && mc.options.keyRight.isPressed() && mc.player.input.pressingRight && mc.options.keyForward.isPressed() && mc.player.input.pressingForward && mc.options.keyBack.isPressed() && mc.player.input.pressingBack))) {
                        lastPos = locRounded;
                    }
                }
            }

            int obbyIndex = findBlock();
            if (obbyIndex == -1) return;
            int prevSlot = mc.player.getInventory().selectedSlot;

            if (needsToPlace()) {
                for (BlockPos pos : getPositions()) {
                    if (mc.world.getBlockState(pos).getMaterial().isReplaceable())
                        mc.player.getInventory().selectedSlot = obbyIndex;

                    if (BWorldUtils.placeBlockMainHand(pos, rotate.get(), air.get(), placeOnCrystal.get())) {

                        if (delay.get() != 0) {
                            mc.player.getInventory().selectedSlot = prevSlot;
                            return;
                        }
                    }
                }

                mc.player.getInventory().selectedSlot = prevSlot;
            }
        }
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
                lastPos.north().east(), lastPos.east().south(), lastPos.south().west(), lastPos.west().north(),
                locNotSingle.down(), locNotSingle.down().south(), locNotSingle.down().east(),
                locNotSingle.down().east().south(), locNotSingle.north().east(), locNotSingle.east(2).south(),
                locNotSingle.south(2).east(), locNotSingle.east(), locNotSingle.east().south(),
                locNotSingle.south(2), locNotSingle.west().south(), locNotSingle.north(),
                locNotSingle.east(2), locNotSingle.east().north(), locNotSingle.south(),
                locNotSingle.south().east(), locNotSingle.west());
    }

    private List<BlockPos> getPositions() {
        List<BlockPos> positions = new ArrayList<>();
        if ((!should1x2NS && !should1x2WE && !should2x2) || (!twoHole.get() && !fourHole.get())) {
            if (!onlyGround.get()) add(positions, lastPos.down());

            add(positions, lastPos.north());
            add(positions, lastPos.east());
            add(positions, lastPos.south());
            add(positions, lastPos.west());
        } else if (fourHole.get() && should2x2) {
            if (!onlyGround.get() || mc.player.isOnGround()) {
                add(positions, locNotSingle.down());
                add(positions, locNotSingle.down().south());
                add(positions, locNotSingle.down().east());
                add(positions, locNotSingle.down().east().south());
            }

            add(positions, locNotSingle.north());
            add(positions, locNotSingle.north().east());
            add(positions, locNotSingle.east(2));
            add(positions, locNotSingle.east(2).south());
            add(positions, locNotSingle.south(2).east());
            add(positions, locNotSingle.south(2));
            add(positions, locNotSingle.west().south());
            add(positions, locNotSingle.west());
        } else if (twoHole.get() && should1x2NS) {
            if (!onlyGround.get() || mc.player.isOnGround()) {
                add(positions, locNotSingle.down());
                add(positions, locNotSingle.down().south());
            }

            add(positions, locNotSingle.north());
            add(positions, locNotSingle.east());
            add(positions, locNotSingle.east().south());
            add(positions, locNotSingle.south(2));
            add(positions, locNotSingle.west().south());
            add(positions, locNotSingle.west());
        } else if (twoHole.get() && should1x2WE) {
            if (!onlyGround.get() || mc.player.isOnGround()) {
                add(positions, locNotSingle.down());
                add(positions, locNotSingle.down().east());
            }

            add(positions, locNotSingle.north());
            add(positions, locNotSingle.east(2));
            add(positions, locNotSingle.east().north());
            add(positions, locNotSingle.south());
            add(positions, locNotSingle.south().east());
            add(positions, locNotSingle.west());
        }

        if ((doubleHeight.get() || doubleHeightKeybind.get().isPressed() || pressedDoub)) {
            if (keyBindMode.get() && !pressedDoub && canPressDoub) {
                canPressDoub = false;
                pressedDoub = true;
            }

            if ((fourHole.get() && doubleHeight2X2.get() && should2x2)) {
                add(positions, locNotSingle.north().up());
                add(positions, locNotSingle.north().east().up());
                add(positions, locNotSingle.east(2).up());
                add(positions, locNotSingle.east(2).south().up());
                add(positions, locNotSingle.south(2).east().up());
                add(positions, locNotSingle.south(2).up());
                add(positions, locNotSingle.west().south().up());
                add(positions, locNotSingle.west().up());
            } else if (twoHole.get() && doubleHeight1X2.get() && should1x2NS) {
                add(positions, locNotSingle.north().up());
                add(positions, locNotSingle.east().up());
                add(positions, locNotSingle.east().south().up());
                add(positions, locNotSingle.south(2).up());
                add(positions, locNotSingle.west().south().up());
                add(positions, locNotSingle.west().up());
            } else if (twoHole.get() && doubleHeight1X2.get() && should1x2WE) {
                add(positions, locNotSingle.north().up());
                add(positions, locNotSingle.east(2).up());
                add(positions, locNotSingle.east().north().up());
                add(positions, locNotSingle.south().up());
                add(positions, locNotSingle.south().east().up());
                add(positions, locNotSingle.west().up());
            } else {
                add(positions, lastPos.north().up());
                add(positions, lastPos.east().up());
                add(positions, lastPos.south().up());
                add(positions, lastPos.west().up());
            }

        }

        if (((mode.get() != Mode.Normal || wideKeybind.get().isPressed() || widePlusKeybind.get().isPressed() || shouldExtra)) && ((!should1x2NS && !should1x2WE && !should2x2) || (!twoHole.get() && !fourHole.get()))) {
            if (mc.world.getBlockState(lastPos.north()).getBlock() != Blocks.BEDROCK && ((north && shouldExtra) || (shouldExtra && !smartAnti.get()) || (mode.get() != Mode.Normal || wideKeybind.get().isPressed() || widePlusKeybind.get().isPressed()))) add(positions, lastPos.north(2));
            if (mc.world.getBlockState(lastPos.east()).getBlock() != Blocks.BEDROCK && ((east && shouldExtra) || (shouldExtra && !smartAnti.get()) || (mode.get() != Mode.Normal || wideKeybind.get().isPressed() || widePlusKeybind.get().isPressed()))) add(positions, lastPos.east(2));
            if (mc.world.getBlockState(lastPos.south()).getBlock() != Blocks.BEDROCK && ((south && shouldExtra) || (shouldExtra && !smartAnti.get()) || (mode.get() != Mode.Normal || wideKeybind.get().isPressed() || widePlusKeybind.get().isPressed()))) add(positions, lastPos.south(2));
            if (mc.world.getBlockState(lastPos.west()).getBlock() != Blocks.BEDROCK && ((west && shouldExtra) || (shouldExtra && !smartAnti.get()) || (mode.get() != Mode.Normal || wideKeybind.get().isPressed() || widePlusKeybind.get().isPressed()))) add(positions, lastPos.west(2));
        }

        if (((mode.get() == Mode.RussianPlus || widePlusKeybind.get().isPressed() || (shouldExtra && antiMode.get() == AntiBreakMode.RussianPlus))) && ((!should1x2NS && !should1x2WE && !should2x2) || (!twoHole.get() && !fourHole.get()))) {
            if ((mc.world.getBlockState(lastPos.north()).getBlock() != Blocks.BEDROCK || mc.world.getBlockState(lastPos.east()).getBlock() != Blocks.BEDROCK) && (((east || north) && shouldExtra) || (shouldExtra && !smartAnti.get()) || (mode.get() != Mode.Normal || wideKeybind.get().isPressed() || widePlusKeybind.get().isPressed()))) add(positions, lastPos.north().east());
            if ((mc.world.getBlockState(lastPos.east()).getBlock() != Blocks.BEDROCK || mc.world.getBlockState(lastPos.south()).getBlock() != Blocks.BEDROCK) && (((south || east) && shouldExtra) || (shouldExtra && !smartAnti.get()) || (mode.get() != Mode.Normal || wideKeybind.get().isPressed() || widePlusKeybind.get().isPressed()))) add(positions, lastPos.east().south());
            if ((mc.world.getBlockState(lastPos.south()).getBlock() != Blocks.BEDROCK || mc.world.getBlockState(lastPos.west()).getBlock() != Blocks.BEDROCK) && ((west || south) && shouldExtra) || (shouldExtra && !smartAnti.get()) || (mode.get() != Mode.Normal || wideKeybind.get().isPressed() || widePlusKeybind.get().isPressed())) add(positions, lastPos.south().west());
            if ((mc.world.getBlockState(lastPos.west()).getBlock() != Blocks.BEDROCK || mc.world.getBlockState(lastPos.north()).getBlock() != Blocks.BEDROCK) && (((north || west) && shouldExtra) || (shouldExtra && !smartAnti.get()) || (mode.get() != Mode.Normal || wideKeybind.get().isPressed() || widePlusKeybind.get().isPressed()))) add(positions, lastPos.west().north());
        }

        return positions;
    }

    private void add(List<BlockPos> list, BlockPos pos) {
        if (mc.world.getBlockState(pos).isAir() && allAir(pos.north(), pos.east(), pos.south(), pos.west(), pos.up(), pos.down()) && !air.get()) list.add(pos.down());
        list.add(pos);
    }

    private boolean allAir(BlockPos... pos) {
        return Arrays.stream(pos).allMatch(blockPos -> mc.world.getBlockState(blockPos).isAir());
    }

    private boolean anyAir(BlockPos... pos) {
        return Arrays.stream(pos).anyMatch(blockPos -> mc.world.getBlockState(blockPos).isAir());
    }

    private Block primaryBlock() {
        Block index = null;

        if (primary.get() == Primary.Obsidian) {
            index = Blocks.OBSIDIAN;
        } else if (primary.get() == Primary.EnderChest) {
            index = Blocks.ENDER_CHEST;
        } else if (primary.get() == Primary.CryingObsidian) {
            index = Blocks.CRYING_OBSIDIAN;
        } else if (primary.get() == Primary.NetheriteBlock) {
            index = Blocks.NETHERITE_BLOCK;
        } else if (primary.get() == Primary.AncientDebris) {
            index = Blocks.ANCIENT_DEBRIS;
        } else if (primary.get() == Primary.RespawnAnchor) {
            index = Blocks.RESPAWN_ANCHOR;
        } else if (primary.get() == Primary.Anvil) {
            index = Blocks.ANVIL;
        }

        return index;
    }

    private int findBlock() {
        int index = InvUtils.findInHotbar(primaryBlock().asItem()).slot();

        if (index == -1 && allBlocks.get()) {
            for (int i = 0; i < 9; ++i) {
                Item item = mc.player.getInventory().getStack(i).getItem();

                if (item instanceof BlockItem) {
                    if (item == Items.ANCIENT_DEBRIS || item == Items.OBSIDIAN || item == Items.CRYING_OBSIDIAN || item == Items.ANVIL || item == Items.CHIPPED_ANVIL || item == Items.DAMAGED_ANVIL || item == Items.ENCHANTING_TABLE || item == Items.ENDER_CHEST || item == Items.NETHERITE_BLOCK || (PlayerUtils.getDimension() != Dimension.Nether && item == Items.RESPAWN_ANCHOR)) {
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
        shouldExtra = false;
        should2x2 = false;
        should1x2WE = false;
        should1x2NS = false;
        west = false;
        east = false;
        north = false;
        south = false;
    }

    PlayerEntity prevBreakingPlayer = null;

    boolean west, east, north, south;

    @EventHandler
    public void onBreakPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof BlockBreakingProgressS2CPacket)) return;

        BlockBreakingProgressS2CPacket bbpp = (BlockBreakingProgressS2CPacket) event.packet;
        BlockPos bbp = bbpp.getPos();

        if (notifyBreak.get() || antiSurroundBreak.get()) {
            PlayerEntity breakingPlayer = (PlayerEntity) mc.world.getEntityById(bbpp.getEntityId());
            BlockPos playerBlockPos = mc.player.getBlockPos();

            if (block.get()) String.valueOf(bbpp.getProgress() / 10 * 100);

            if (bbpp.getProgress() / 10 * 100 > breakProgress.get() && !breakingPlayer.equals(mc.player)) {
                if (antiSurroundBreak.get()) {
                    shouldExtra = true;
                }

                if (bbp.equals(playerBlockPos.north())) {
                    if (antiSurroundBreak.get() && smartAnti.get()) north = true;
                    if (notifyBreak.get()) notifySurroundBreak(Direction.NORTH, breakingPlayer);
                }
                if (bbp.equals(playerBlockPos.east())) {
                    if (antiSurroundBreak.get() && smartAnti.get()) east = true;
                    if (notifyBreak.get()) notifySurroundBreak(Direction.EAST, breakingPlayer);
                }
                if (bbp.equals(playerBlockPos.south())) {
                    if (antiSurroundBreak.get() && smartAnti.get()) south = true;
                    if (notifyBreak.get()) notifySurroundBreak(Direction.SOUTH, breakingPlayer);
                }
                if (bbp.equals(playerBlockPos.west())) {
                    if (antiSurroundBreak.get() && smartAnti.get()) west = true;
                    if (notifyBreak.get()) notifySurroundBreak(Direction.WEST, breakingPlayer);
                }
            }


            prevBreakingPlayer = breakingPlayer;

            prevBreakPos = bbp;
        }
    }

    private void notifySurroundBreak(Direction direction, PlayerEntity player) {
        switch (direction) {
            case NORTH -> {
                warning("Your north surround block is being broken by " + player.getEntityName());
                break;
            }
            case EAST -> {
                warning("Your east surround block is being broken by " + player.getEntityName());
                break;
            }
            case SOUTH -> {
                warning("Your south surround block is being broken by " + player.getEntityName());
                break;
            }
            case WEST -> {
                warning("Your west surround block is being broken by " + player.getEntityName());
                break;
            }
        }
    }

    public enum Mode {
        Normal, Russian, RussianPlus
    }

    public enum AntiBreakMode {
        Russian, RussianPlus
    }

    public enum Primary {
        Obsidian, EnderChest, CryingObsidian, NetheriteBlock, AncientDebris, RespawnAnchor, Anvil
    }
}
