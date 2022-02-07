package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.world.BWorldUtils;
import meteorclient.utils.misc.Timer;
import meteorclient.events.entity.player.FinishUsingItemEvent;
import meteorclient.events.packets.PacketEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.settings.*;
import meteorclient.systems.modules.Module;
import meteorclient.utils.player.InvUtils;
import meteorclient.utils.player.PlayerUtils;
import meteorclient.utils.world.Dimension;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelfTrapPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgToggle = settings.createGroup("Different Toggle Modes");

    private final Setting<TopMode> topPlacement = sgGeneral.add(new EnumSetting.Builder<TopMode>().name("Self Trap Mode").description("The mode at which SelfTrap+ operates in.").defaultValue(TopMode.Full).build());
    private final Setting<Primary> primary = sgGeneral.add(new EnumSetting.Builder<Primary>().name("Primary block").description("Primary block to use.").defaultValue(Primary.Obsidian).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("Delay").description("Delay in ticks between placing blocks.").defaultValue(0).sliderMin(0).sliderMax(10).build());
    private final Setting<Boolean> onlyGround = sgGeneral.add(new BoolSetting.Builder().name("Only On Ground").description("Will not attempt to place while you are not standing on ground.").defaultValue(false).build());
    private final Setting<Boolean> snap = sgGeneral.add(new BoolSetting.Builder().name("Center").description("Will align you at the center of your hole when you turn this on.").defaultValue(true).build());
    private final Setting<Integer> centerDelay = sgGeneral.add(new IntSetting.Builder().name("Center Delay").description("Delay in ticks before you get centered.").visible(snap::get).defaultValue(0).sliderMin(0).sliderMax(10).build());
    private final Setting<Boolean> placeOnCrystal = sgGeneral.add(new BoolSetting.Builder().name("Ignore entities").description("Will try to place even if there is an entity in its way.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("Rotate").description("Whether to rotate or not.").defaultValue(false).build());
    private final Setting<Boolean> air = sgGeneral.add(new BoolSetting.Builder().name("Air-Place").description("Whether to place blocks midair or not.").defaultValue(true).build());
    private final Setting<Boolean> allBlocks = sgGeneral.add(new BoolSetting.Builder().name("Any-blastproof").description("Will allow any blast proof block to be used.").defaultValue(true).build());
    private final Setting<Boolean> disableOnYChange = sgToggle.add(new BoolSetting.Builder().name("disable-on-y-change").description("Automatically disables when your y level (step, jumping, atc).").defaultValue(false).build());
    private final Setting<Boolean> onEat = sgToggle.add(new BoolSetting.Builder().name("disable-on-chorus/pearl").description("Automatically disables when you eat chorus or throw a pearl (pearl dont work if u use middle click extra)").defaultValue(false).build());

    public static SurroundPlus INSTANCE;

    private BlockPos lastPos = new BlockPos(0, -100, 0);
    private int ticks = 0;

    private boolean hasCentered = false;
    private Timer onGroundCenter = new Timer();

    private static final Timer surroundInstanceDelay = new Timer();
    int timeToStart = 0;

    public SelfTrapPlus() {
        super(Categories.BANANAPLUS, "self-trap+", "Surrounds your top part in blocks to prevent you from taking lots of damage");
    }

    public static void setSurroundWait(int timeToStart) {
        INSTANCE.timeToStart = timeToStart;
    }

    boolean doSnap = true;

    public static void toggleCenter(boolean doSnap) {
        INSTANCE.doSnap = doSnap;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
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
            AbstractClientPlayerEntity loc = mc.player;
            BlockPos locRounded = BWorldUtils.roundBlockPos(loc.getPos());
            if (!lastPos.equals(loc.isOnGround() ? locRounded : loc.getBlockPos())) {
                if ((disableOnYChange.get() && mc.player.prevY < mc.player.getY()) ||
                        onlyGround.get() || !(loc.getPos().y <= lastPos.getY() + 1.5)
                        || ((Math.floor(loc.getPos().x) != lastPos.getX() || Math.floor(loc.getPos().z) != lastPos.getZ()) && !(loc.getPos().y <= lastPos.getY() + 0.75))
                        || (!mc.world.getBlockState(lastPos).getMaterial().isReplaceable() && loc.getBlockPos() != lastPos)
                ) {
                    toggle();
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

    private boolean needsToPlace() {
        return anyAir(lastPos.up(2), lastPos.north().up(), lastPos.east().up(), lastPos.west().up(), lastPos.south().up());
    }

    private List<BlockPos> getPositions() {
        List<BlockPos> positions = new ArrayList<>();

        if (topPlacement.get() == TopMode.Top || topPlacement.get() == TopMode.Full) {
            add(positions, lastPos.up(2));
        }

        if (topPlacement.get() == TopMode.Full || topPlacement.get() == TopMode.Side) {
            add(positions, lastPos.north().up());
            add(positions, lastPos.east().up());
            add(positions, lastPos.west().up());
            add(positions, lastPos.south().up());
        }
        return positions;
    }

    private void add(List<BlockPos> list, BlockPos pos) {
        if (
                mc.world.getBlockState(pos).isAir() &&
                        allAir(pos.north(), pos.east(), pos.south(), pos.west(), pos.up(), pos.down()) &&
                        !air.get()
        ) list.add(pos.down());
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

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerInteractItemC2SPacket && mc.player.getOffHandStack().getItem() instanceof EnderPearlItem && onEat.get()) {

            toggle();
        }
    }

    @EventHandler
    private void onFinishUsingItem(FinishUsingItemEvent event) {
        if (event.itemStack.getItem() instanceof ChorusFruitItem && onEat.get()) {
            toggle();
        }
    }

    @Override
    public void onActivate() {
        lastPos = mc.player.isOnGround() ? BWorldUtils.roundBlockPos(mc.player.getPos()) : mc.player.getBlockPos();
    }

    @Override
    public void onDeactivate() {
        ticks = 0;
        doSnap = true;
        timeToStart = 0;
        hasCentered = false;
    }

    public enum TopMode {
        Full, Top, Side
    }

    public enum Primary {
        Obsidian, EnderChest, CryingObsidian, NetheriteBlock, AncientDebris, RespawnAnchor, Anvil
    }
}

