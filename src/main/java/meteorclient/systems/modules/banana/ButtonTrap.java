package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.entity.BEntityUtils;
import meteorclient.utils.player.BPlayerUtils;
import meteorclient.events.render.Render3DEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.renderer.ShapeMode;
import meteorclient.settings.*;
import meteorclient.systems.modules.Module;
import meteorclient.utils.entity.EntityUtils;
import meteorclient.utils.entity.SortPriority;
import meteorclient.utils.entity.TargetUtils;
import meteorclient.utils.player.FindItemResult;
import meteorclient.utils.player.InvUtils;
import meteorclient.utils.render.color.SettingColor;
import meteorclient.utils.world.BlockUtils;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ButtonTrap extends Module {


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    public enum When{
        Always,
        TopTrapped,
        FaceTrapped,
        BothTrapped,
        AnyTrapped
    }

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius players can be in to be targeted.")
            .defaultValue(5)
            .sliderMin(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<When> when = sgGeneral.add(new EnumSetting.Builder<When>()
            .name("when")
            .description("When to start button trapping.")
            .defaultValue(When.Always)
            .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The radius buttons can be placed.")
            .defaultValue(5)
            .sliderMin(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Integer> delaySetting = sgGeneral.add(new IntSetting.Builder()
            .name("place-delay")
            .description("How many ticks between block placements.")
            .defaultValue(1)
            .sliderMin(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Sends rotation packets to the server when placing.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("swing")
            .description("Renders your swing client-side.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> checkEntity = sgGeneral.add(new BoolSetting.Builder()
            .name("Check Entity")
            .description("Check if placing intersects with entities.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
            .name("Swap Back")
            .description("Swaps back to your previous slot after placing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("self-toggle")
            .description("Automatically toggles off if no target is found.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay where the button will be placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
    );

    private PlayerEntity target;
    private List<BlockPos> placePositions = new ArrayList<>();
    private int delay;

    public ButtonTrap() {
        super(Categories.BANANAPLUS, "button-trap", "Anti Surround.");
    }

    @Override
    public void onActivate() {
        target = null;
        if (!placePositions.isEmpty()) placePositions.clear();
        delay = 0;
    }

    @EventHandler(priority = EventPriority.MEDIUM + 60)
    private void onTick(TickEvent.Pre event) {

        target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);

        if (target == null || Objects.requireNonNull(mc.player).distanceTo(target) > targetRange.get()) {
            if (selfToggle.get()) {
                error("No target found, disabling...");
                toggle();
            }
            return;
        } else {
            if (when.get() == When.BothTrapped && !(BEntityUtils.isTopTrapped(target) && BEntityUtils.isFaceSurrounded(target))) return;
            else if (when.get() == When.AnyTrapped && !(BEntityUtils.isTopTrapped(target) || BEntityUtils.isFaceSurrounded(target))) return;
            else if (when.get() == When.TopTrapped && !BEntityUtils.isTopTrapped(target)) return;
            else if (when.get() == When.FaceTrapped && !BEntityUtils.isFaceSurrounded(target)) return;
        }

        FindItemResult button = InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()) instanceof AbstractButtonBlock);
        if (!button.found()) button = InvUtils.findInHotbar(Items.STRING);
        if (!button.found()) button = InvUtils.findInHotbar(Items.REDSTONE);
        if (!button.found()) {
            error("No button/string/redstone found in hotbar");
            toggle();
            return;
        }

            placePositions.clear();

            findPlacePos(target);

            if (delay >= delaySetting.get() && placePositions.size() > 0) {
                BlockPos blockPos = placePositions.get(placePositions.size() - 1);
                if (BPlayerUtils.distanceTo(blockPos) > placeRange.get()) return;

                if (BlockUtils.place(blockPos, button, rotate.get(), 50, swing.get(), checkEntity.get(), swapBack.get()))
                    placePositions.remove(blockPos);

                delay = 0;
            } else delay++;
        }


    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || placePositions.isEmpty()) return;
        for (BlockPos pos : placePositions)
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }


    private void add(BlockPos blockPos) {
        if (!placePositions.contains(blockPos)
                && mc.world.getBlockState(blockPos).getMaterial().isReplaceable()
                && mc.world.canPlace(Blocks.STONE_BUTTON.getDefaultState(), blockPos, ShapeContext.absent())
                && (mc.world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ())).isFullCube(mc.world, new BlockPos(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ()))
                || mc.world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ())).isFullCube(mc.world, new BlockPos(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ()))
                || mc.world.getBlockState(new BlockPos(blockPos.getX() + 1, blockPos.getY(), blockPos.getZ())).isFullCube(mc.world, new BlockPos(blockPos.getX() + 1, blockPos.getY(), blockPos.getZ()))
                || mc.world.getBlockState(new BlockPos(blockPos.getX() - 1, blockPos.getY(), blockPos.getZ())).isFullCube(mc.world, new BlockPos(blockPos.getX() - 1, blockPos.getY(), blockPos.getZ()))
                || mc.world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ() + 1)).isFullCube(mc.world, new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ() + 1))
                || mc.world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ() - 1)).isFullCube(mc.world, new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ() - 1))
        )
        ) {
            placePositions.add(blockPos);
        }
    }
    private void findPlacePos(PlayerEntity target) {
        placePositions.clear();
        BlockPos targetPos = target.getBlockPos();
        add(targetPos.add(1, 0, 0));
        add(targetPos.add(0, 0, 1));
        add(targetPos.add(-1, 0, 0));
        add(targetPos.add(0, 0, -1));
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
