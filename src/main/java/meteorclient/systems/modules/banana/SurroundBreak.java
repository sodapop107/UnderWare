package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.entity.BEntityUtils;
import meteorclient.utils.player.BPlayerUtils;
import meteorclient.events.render.Render3DEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.renderer.ShapeMode;
import meteorclient.settings.*;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import meteorclient.utils.player.FindItemResult;
import meteorclient.utils.player.InvUtils;
import meteorclient.utils.render.color.SettingColor;
import meteorclient.utils.world.BlockUtils;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public class SurroundBreak extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSurBreak = settings.createGroup("Surround Break");

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder().name("place-range").description("The radius crystals can be placed.").defaultValue(4.5).sliderMin(0).sliderMax(10).build());
    private final Setting<Integer> delaySetting = sgGeneral.add(new IntSetting.Builder().name("place-delay").description("How many ticks between crystal placements.").defaultValue(1).sliderMin(0).sliderMax(10).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Sends rotation packets to the server when placing.").defaultValue(false).build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").description("Renders your swing client-side.").defaultValue(true).build());
    private final Setting<Boolean> checkEntity = sgGeneral.add(new BoolSetting.Builder().name("Check Entity").description("Check if placing intersects with entities.").defaultValue(true).build());
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder().name("Swap Back").description("Swaps back to your previous slot after placing.").defaultValue(true).build());
    private final Setting<Boolean> direct = sgSurBreak.add(new BoolSetting.Builder().name("Direct").description("Places a crystal right next to target's surround.").defaultValue(true).build());
    private final Setting<Boolean> diagonal = sgSurBreak.add(new BoolSetting.Builder().name("Diagonal").description("Places a crystal diagonal to target's surround.").defaultValue(true).build());
    private final Setting<Boolean> horizontal = sgSurBreak.add(new BoolSetting.Builder().name("Horizontal").description("Places a crystal horizontal to target's surround.").defaultValue(true).build());
    private final Setting<Boolean> below = sgSurBreak.add(new BoolSetting.Builder().name("1-Below").description("Places a crystal 1 block below to target's surround.").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders a block overlay where the crystal will be placed.").defaultValue(true).build());
    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder().name("render-time").description("How long to render for.").defaultValue(10).min(0).sliderMax(30).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Lines).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The color of the sides of the blocks being rendered.").defaultValue(new SettingColor(255, 255, 255, 255)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The color of the lines of the blocks being rendered.").defaultValue(new SettingColor(255, 255, 255, 255)).build());

    private PlayerEntity target;
    private List<BlockPos> placePositions = new ArrayList<>();
    private int delay;
    private int renderTimer;

    public SurroundBreak() {
        super(Categories.BANANAPLUS, "surround-break", "Automatically places a crystal next to CA target's surround.");
    }

    @Override
    public void onActivate() {
        target = null;
        if (!placePositions.isEmpty()) placePositions.clear();
        delay = 0;
        renderTimer = 0;
    }

    @EventHandler(priority = EventPriority.MEDIUM + 60)
    private void onTick(TickEvent.Pre event) {

        BananaBomber crystalAura = Modules.get().get(BananaBomber.class);
        if (crystalAura.isActive()) {
            target = crystalAura.getPlayerTarget();
        }
        if (target == null || !crystalAura.isActive()) return;

        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (!crystal.found()) return;

        if (!BEntityUtils.isSurrounded(target)) return;

        placePositions.clear();

        findPlacePos(target);

        if (delay >= delaySetting.get() && placePositions.size() > 0) {
            BlockPos blockPos = placePositions.get(placePositions.size() - 1);
            if (BPlayerUtils.distanceTo(blockPos) > placeRange.get()) return;

            if (BlockUtils.place(blockPos, crystal, rotate.get(), 50, swing.get(), checkEntity.get(), swapBack.get()))
                placePositions.remove(blockPos);

            delay = 0;
        } else delay++;

        if (renderTimer > 0) renderTimer--;

        renderTimer = renderTime.get();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (renderTimer > 0 && render.get()) {
            for (BlockPos pos : placePositions)
                event.renderer.box(pos.down(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    private void add(BlockPos blockPos) {
        double x = blockPos.up().getX();
        double y = blockPos.up().getY();
        double z = blockPos.up().getZ();
        if (!placePositions.contains(blockPos) && (mc.world.getOtherEntities(null, new Box(x, y, z, x + 1D, y + 2D, z + 1D)).isEmpty() && mc.world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ())).isAir() && (mc.world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ())).isOf(Blocks.BEDROCK) || mc.world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ())).isOf(Blocks.OBSIDIAN)))) {
            placePositions.add(blockPos);
        }
    }

    private boolean isBedRock(BlockPos pos) {
        return mc.world.getBlockState(pos).isOf(Blocks.BEDROCK);
    }

    private void findPlacePos(PlayerEntity target) {
        placePositions.clear();
        BlockPos targetPos = target.getBlockPos();

        if (direct.get()) {
            if (!isBedRock(targetPos.add(1, 0, 0))) add(targetPos.add(2, 0, 0));
            if (!isBedRock(targetPos.add(0, 0, 1))) add(targetPos.add(0, 0, 2));
            if (!isBedRock(targetPos.add(-1, 0, 0))) add(targetPos.add(-2, 0, 0));
            if (!isBedRock(targetPos.add(0, 0, -1))) add(targetPos.add(0, 0, -2));
        } else if (diagonal.get()) {
            if (!isBedRock(targetPos.add(1, 0, 0))) {
                add(targetPos.add(2, 0, 1));
                add(targetPos.add(2, 0, -1));
            }
            if (!isBedRock(targetPos.add(-1, 0, 0))) {
                add(targetPos.add(-2, 0, 1));
                add(targetPos.add(-2, 0, -1));
            }
            if (!isBedRock(targetPos.add(0, 0, 1))) {
                add(targetPos.add(1, 0, 2));
                add(targetPos.add(-1, 0, 2));
            }
            if (!isBedRock(targetPos.add(0, 0, -1))) {
                add(targetPos.add(1, 0, -2));
                add(targetPos.add(-1, 0, -2));
            }
        } else if (horizontal.get()) {
            if (!isBedRock(targetPos.add(1, 0, 0)) && !isBedRock(targetPos.add(0, 0, 1))) {
                add(targetPos.add(1, 0, 1));
            }
            if (!isBedRock(targetPos.add(-1, 0, 0)) && !isBedRock(targetPos.add(0, 0, 1))) {
                add(targetPos.add(-1, 0, 1));
            }
            if (!isBedRock(targetPos.add(-1, 0, 0)) && !isBedRock(targetPos.add(0, 0, -1))) {
                add(targetPos.add(-1, 0, -1));
            }
            if (!isBedRock(targetPos.add(1, 0, 0)) && !isBedRock(targetPos.add(0, 0, -1))) {
                add(targetPos.add(1, 0, -1));
            }
        } else if (below.get()) {
            if (!isBedRock(targetPos.add(1, 0, 0))) add(targetPos.add(2, -1, 0));
            if (!isBedRock(targetPos.add(0, 0, 1))) add(targetPos.add(0, -1, 2));
            if (!isBedRock(targetPos.add(-1, 0, 0))) add(targetPos.add(-2, -1, 0));
            if (!isBedRock(targetPos.add(0, 0, -1))) add(targetPos.add(0, -1, -2));

            if (!isBedRock(targetPos.add(1, 0, 0)) && !isBedRock(targetPos.add(0, 0, 1))) {
                add(targetPos.add(1, -1, 1));
            }
            if (!isBedRock(targetPos.add(-1, 0, 0)) && !isBedRock(targetPos.add(0, 0, 1))) {
                add(targetPos.add(-1, -1, 1));
            }
            if (!isBedRock(targetPos.add(-1, 0, 0)) && !isBedRock(targetPos.add(0, 0, -1))) {
                add(targetPos.add(-1, -1, -1));
            }
            if (!isBedRock(targetPos.add(1, 0, 0)) && !isBedRock(targetPos.add(0, 0, -1))) {
                add(targetPos.add(1, -1, -1));
            }

            if (!isBedRock(targetPos.add(1, 0, 0))) {
                add(targetPos.add(2, -1, 1));
                add(targetPos.add(2, -1, -1));
            }
            if (!isBedRock(targetPos.add(-1, 0, 0))) {
                add(targetPos.add(-2, -1, 1));
                add(targetPos.add(-2, -1, -1));
            }
            if (!isBedRock(targetPos.add(0, 0, 1))) {
                add(targetPos.add(1, -1, 2));
                add(targetPos.add(-1, -1, 2));
            }
            if (!isBedRock(targetPos.add(0, 0, -1))) {
                add(targetPos.add(1, -1, -2));
                add(targetPos.add(-1, -1, -2));
            }
        }
    }
}
