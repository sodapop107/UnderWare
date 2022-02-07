package meteorclient.systems.modules.banana;

import meteorclient.events.world.TickEvent;
import meteorclient.settings.DoubleSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Glide extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder().name("fall-speed").description("Fall Speed").defaultValue(0.125).min(0.005).sliderMax(0.25).build());
    private final Setting<Double> moveSpeed = sgGeneral.add(new DoubleSetting.Builder().name("move-speed").description("Horizontal movement factor.").defaultValue(1.2).min(0.75).sliderMax(5).build());
    private final Setting<Double> minHeight = sgGeneral.add(new DoubleSetting.Builder().name("min-height").description("Won't glide when you are too close to the ground.").defaultValue(0).min(0).sliderMax(2).build());

    public Glide() {
        super(Categories.BANANAPLUS, "glide", "Makes you glide down slowly when falling.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        Vec3d v = mc.player.getVelocity();

        if (mc.player.isOnGround() || mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isClimbing() || v.y >= 0) return;

        if (minHeight.get() > 0) {
            Box box = mc.player.getBoundingBox();
            box = box.union(box.offset(0, -minHeight.get(), 0));
            if (!mc.world.isSpaceEmpty(box))
                return;

            BlockPos min = new BlockPos(new Vec3d(box.minX, box.minY, box.minZ));
            BlockPos max = new BlockPos(new Vec3d(box.maxX, box.maxY, box.maxZ));
            Stream<BlockPos> stream = StreamSupport.stream(getAllInBox(min, max).spliterator(), true);

            if (stream.map(this::getState).map(BlockState::getMaterial).anyMatch(Material::isLiquid)) return;
        }

        mc.player.setVelocity(v.x, Math.max(v.y, -fallSpeed.get()), v.z);
        mc.player.airStrafingSpeed *= moveSpeed.get();
    }

    public static ArrayList<BlockPos> getAllInBox(BlockPos from, BlockPos to) {
        ArrayList<BlockPos> blocks = new ArrayList<>();

        BlockPos min = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
        BlockPos max = new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    blocks.add(new BlockPos(x, y, z));
                }
            }
        }

        return blocks;
    }

    public BlockState getState(BlockPos pos) {
        return mc.world.getBlockState(pos);
    }
}
