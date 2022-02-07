package meteorclient.systems.modules.banana;

import meteorclient.utils.world.BWorldUtils;
import meteorclient.utils.misc.Timer;
import meteorclient.events.world.TickEvent;
import meteorclient.settings.*;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;
import meteorclient.utils.entity.EntityUtils;
import meteorclient.utils.entity.SortPriority;
import meteorclient.utils.entity.TargetUtils;
import meteorclient.utils.player.InvUtils;
import meteorclient.utils.player.PlayerUtils;
import meteorclient.utils.world.Dimension;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoTrapPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder().name("target-range").description("The range players can be targeted.").defaultValue(4).build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority").description("How to select the player to target.").defaultValue(SortPriority.LowestHealth).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("Delay").description("Delay in ticks between placing blocks.").defaultValue(0).sliderMin(0).sliderMax(10).build());
    private final Setting<TopMode> topPlacement = sgGeneral.add(new EnumSetting.Builder<TopMode>().name("top-blocks").description("Which blocks to place on the top half of the target.").defaultValue(TopMode.Full).build());
    private final Setting<BottomMode> bottomPlacement = sgGeneral.add(new EnumSetting.Builder<BottomMode>().name("bottom-blocks").description("Which blocks to place on the bottom half of the target.").defaultValue(BottomMode.Platform).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotates towards blocks when placing.").defaultValue(true).build());
    private final Setting<Primary> primary = sgGeneral.add(new EnumSetting.Builder<Primary>().name("Primary block").description("Primary block to use.").defaultValue(Primary.Obsidian).build());
    private final Setting<Boolean> onlyGround = sgGeneral.add(new BoolSetting.Builder().name("Only On Ground").description("Will not attempt to place while the target are not standing on ground.").defaultValue(false).build());
    private final Setting<Boolean> placeOnCrystal = sgGeneral.add(new BoolSetting.Builder().name("Ignore entities").description("Will try to place even if there is an entity in its way.").defaultValue(true).build());
    private final Setting<Boolean> air = sgGeneral.add(new BoolSetting.Builder().name("Air-Place").description("Whether to place blocks midair or not.").defaultValue(true).build());
    private final Setting<Boolean> allBlocks = sgGeneral.add(new BoolSetting.Builder().name("Any-blastproof").description("Will allow any blast proof block to be used.").defaultValue(true).build());
    private BlockPos lastPos = new BlockPos(0, -100, 0);
    private int ticks = 0;

    private static final Timer surroundInstanceDelay = new Timer();
    int timeToStart = 0;

    public AutoTrapPlus() {
        super(Categories.Combat, "auto-trap+", "Traps people in an obsidian box to prevent them from moving.");
    }

    private boolean needsToPlace() {
        return anyAir(lastPos.up(1), lastPos.north().up(), lastPos.east().up(), lastPos.west().up(), lastPos.south().up(), lastPos.down(), lastPos.north().down(), lastPos.east().down(), lastPos.south().down(), lastPos.west().down(), lastPos.north(), lastPos.east(), lastPos.south(), lastPos.west());
    }

    private List<BlockPos> getPositions() {
        List<BlockPos> positions = new ArrayList<>();

        if (topPlacement.get() == TopMode.Top || topPlacement.get() == TopMode.Face || topPlacement.get() == TopMode.Full) {
            add(positions, lastPos.up(2));
        }

        if (topPlacement.get() == TopMode.Face || topPlacement.get() == TopMode.Full) {
            add(positions, lastPos.north().up());
            add(positions, lastPos.east().up());
            add(positions, lastPos.west().up());
        }

        if (topPlacement.get() == TopMode.Full) {
            add(positions, lastPos.south().up());
        }

        if (bottomPlacement.get() == BottomMode.Single || bottomPlacement.get() == BottomMode.Platform || bottomPlacement.get() == BottomMode.Full) {
            add(positions, lastPos.down());
        }

        if (bottomPlacement.get() == BottomMode.Platform) {
            add(positions, lastPos.north().down());
            add(positions, lastPos.east().down());
            add(positions, lastPos.south().down());
            add(positions, lastPos.west().down());
        }

        if (bottomPlacement.get() == BottomMode.Full) {
            add(positions, lastPos.north());
            add(positions, lastPos.east());
            add(positions, lastPos.south());
            add(positions, lastPos.west());
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
        }
        return index;
    }

    private int findBlock() {
        int index = InvUtils.findInHotbar(primaryBlock().asItem()).slot();
        if (index == -1 && allBlocks.get()) {
            for (int i = 0; i < 9; ++i) {
                Item item = mc.player.getInventory().getStack(i).getItem();
                if (item instanceof BlockItem) {
                    if (item == Items.ANCIENT_DEBRIS) {
                        return i;
                    }
                    if (item == Items.OBSIDIAN || item == Items.CRYING_OBSIDIAN || item == Items.ENDER_CHEST || item == Items.NETHERITE_BLOCK || (PlayerUtils.getDimension() != Dimension.Nether && item == Items.RESPAWN_ANCHOR)) {
                        return i;
                    }
                }
            }
        }
        return index;
    }

    private final List<BlockPos> placePositions = new ArrayList<>();
    private PlayerEntity target;

    @Override
    public void onActivate() {
        target = null;
        placePositions.clear();
        lastPos = target.isOnGround() ? BWorldUtils.roundBlockPos(target.getPos()) : target.getBlockPos();

    }

    @Override
    public void onDeactivate() {
        placePositions.clear();
        ticks = 0;
        timeToStart = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        if (TargetUtils.isBadTarget(target, range.get())) target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (TargetUtils.isBadTarget(target, range.get())) return;

        BlockPos roundedPos = BWorldUtils.roundBlockPos(target.getPos());
        if (onlyGround.get() && !target.isOnGround() && roundedPos.getY() <= lastPos.getY()) {
            lastPos = BWorldUtils.roundBlockPos(target.getPos());
        }

        if (surroundInstanceDelay.passedMillis(timeToStart) && (target.isOnGround() || !onlyGround.get())) {
            if (delay.get() != 0 && ticks++ % delay.get() != 0) return;

            PlayerEntity loc = target;
            BlockPos locRounded = BWorldUtils.roundBlockPos(loc.getPos());
            if (!lastPos.equals(loc.isOnGround() ? locRounded : loc.getBlockPos())) {
                if (onlyGround.get() || !(loc.getPos().y <= lastPos.getY() + 1.5) || ((Math.floor(loc.getPos().x) != lastPos.getX() || Math.floor(loc.getPos().z) != lastPos.getZ()) && !(loc.getPos().y <= lastPos.getY() + 0.75)) || (!mc.world.getBlockState(lastPos).getMaterial().isReplaceable() && loc.getBlockPos() != lastPos)) {
                    return;
                }

                if (!onlyGround.get() && locRounded.getY() <= lastPos.getY()) {
                    lastPos = locRounded;
                }
            }

            int obbyIndex = findBlock();
            if (obbyIndex == -1) return;
            int prevSlot = mc.player.getInventory().selectedSlot;

            if (needsToPlace()) {
                for (BlockPos pos : getPositions()) {
                    if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) mc.player.getInventory().selectedSlot = obbyIndex;
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

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }

    public enum TopMode {
        Full, Top, Face, None
    }

    public enum BottomMode {
        Single, Platform, Full, None
    }

    public enum Primary {
        Obsidian, EnderChest, CryingObsidian, NetheriteBlock, AncientDebris, RespawnAnchor
    }
}
