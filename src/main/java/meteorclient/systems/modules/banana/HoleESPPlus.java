package meteorclient.systems.modules.banana;

import meteorclient.events.render.Render3DEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.mixin.AbstractBlockAccessor;
import meteorclient.renderer.Renderer3D;
import meteorclient.renderer.ShapeMode;
import meteorclient.settings.*;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import meteorclient.utils.misc.Pool;
import meteorclient.utils.render.color.Color;
import meteorclient.utils.render.color.SettingColor;
import meteorclient.utils.world.BlockIterator;
import meteorclient.utils.world.Dir;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class HoleESPPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder().name("horizontal-radius").description("Horizontal radius in which to search for holes.").defaultValue(10).min(0).sliderMax(32).build());
    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder().name("vertical-radius").description("Vertical radius in which to search for holes.").defaultValue(5).min(0).sliderMax(32).build());
    private final Setting<Integer> holeHeight = sgGeneral.add(new IntSetting.Builder().name("min-height").description("Minimum hole height required to be rendered.").defaultValue(2).min(1).build());
    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder().name("doubles").description("Highlights double holes that can be stood across.").defaultValue(true).build());
    private final Setting<Boolean> ignoreOwn = sgGeneral.add(new BoolSetting.Builder().name("ignore-own").description("Ignores rendering the hole you are currently standing in.").defaultValue(false).build());
    private final Setting<Boolean> webs = sgGeneral.add(new BoolSetting.Builder().name("webs").description("Whether to show holes that have webs inside of them.").defaultValue(false).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Lines).build());
    private final Setting<Double> height = sgRender.add(new DoubleSetting.Builder().name("height").description("The height of rendering.").defaultValue(0.25).min(0).build());
    private final Setting<Boolean> topQuad = sgRender.add(new BoolSetting.Builder().name("top-quad").description("Whether to render a quad at the top of the hole.").defaultValue(false).build());
    private final Setting<Boolean> bottomQuad = sgRender.add(new BoolSetting.Builder().name("bottom-quad").description("Whether to render a quad at the bottom of the hole.").defaultValue(false).build());
    private final Setting<SettingColor> bedrockColorTop = sgRender.add(new ColorSetting.Builder().name("bedrock-top").description("The top color for holes that are completely bedrock.").defaultValue(new SettingColor(100, 255, 0, 0)).build());
    private final Setting<SettingColor> bedrockColorBottom = sgRender.add(new ColorSetting.Builder().name("bedrock-bottom").description("The bottom color for holes that are completely bedrock.").defaultValue(new SettingColor(100, 255, 0, 200)).build());
    private final Setting<SettingColor> obsidianColorTop = sgRender.add(new ColorSetting.Builder().name("obsidian-top").description("The top color for holes that are completely obsidian.").defaultValue(new SettingColor(255, 0, 0, 0)).build());
    private final Setting<SettingColor> obsidianColorBottom = sgRender.add(new ColorSetting.Builder().name("obsidian-bottom").description("The bottom color for holes that are completely obsidian.").defaultValue(new SettingColor(255, 0, 0, 200)).build());
    private final Setting<SettingColor> mixedColorTop = sgRender.add(new ColorSetting.Builder().name("mixed-top").description("The top color for holes that have mixed bedrock and obsidian.").defaultValue(new SettingColor(255, 127, 0, 0)).build());
    private final Setting<SettingColor> mixedColorBottom = sgRender.add(new ColorSetting.Builder().name("mixed-bottom").description("The bottom color for holes that have mixed bedrock and obsidian.").defaultValue(new SettingColor(255, 127, 0, 200)).build());

    private final Pool<Hole> holePool = new Pool<>(Hole::new);
    private final List<Hole> holes = new ArrayList<>();

    private final byte NULL = 0;

    public HoleESPPlus() {
        super(Categories.BANANAPLUS, "hole-esp+", "Displays more  holes that you will take less damage in.");
    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (Hole hole : holes) holePool.free(hole);
        holes.clear();

        BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
            if (!validHole(blockPos)) return;

            int bedrock = 0, obsidian = 0, crying_obi = 0, netherite_block = 0, respawn_anchor = 0, ender_chest = 0, ancient_debris = 0, enchanting_table = 0, anvil = 0, anvilC = 0, anvilD = 0;

            Direction air = null;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP) continue;

                BlockState state = mc.world.getBlockState(blockPos.offset(direction));

                if (state.getBlock() == Blocks.BEDROCK) bedrock++;
                else if (state.getBlock() == Blocks.OBSIDIAN) obsidian++;
                else if (state.getBlock() == Blocks.RESPAWN_ANCHOR) respawn_anchor++;
                else if (state.getBlock() == Blocks.NETHERITE_BLOCK) netherite_block++;
                else if (state.getBlock() == Blocks.CRYING_OBSIDIAN) crying_obi++;
                else if (state.getBlock() == Blocks.ENDER_CHEST) ender_chest++;
                else if (state.getBlock() == Blocks.ANCIENT_DEBRIS) ancient_debris++;
                else if (state.getBlock() == Blocks.ENCHANTING_TABLE) enchanting_table++;
                else if (state.getBlock() == Blocks.ANVIL) anvil++;
                else if (state.getBlock() == Blocks.CHIPPED_ANVIL) anvilC++;
                else if (state.getBlock() == Blocks.DAMAGED_ANVIL) anvilD++;
                else if (direction == Direction.DOWN) return;
                else if (validHole(blockPos.offset(direction)) && air == null) {
                    for (Direction dir : Direction.values()) {
                        if (dir == direction.getOpposite() || dir == Direction.UP) continue;

                        BlockState blockState1 = mc.world.getBlockState(blockPos.offset(direction).offset(dir));

                        if (blockState1.getBlock() == Blocks.BEDROCK) bedrock++;
                        else if (blockState1.getBlock() == Blocks.OBSIDIAN) obsidian++;
                        else if (blockState1.getBlock() == Blocks.RESPAWN_ANCHOR) respawn_anchor++;
                        else if (blockState1.getBlock() == Blocks.NETHERITE_BLOCK) netherite_block++;
                        else if (blockState1.getBlock() == Blocks.CRYING_OBSIDIAN) crying_obi++;
                        else if (blockState1.getBlock() == Blocks.ENDER_CHEST) ender_chest++;
                        else if (blockState1.getBlock() == Blocks.ANCIENT_DEBRIS) ancient_debris++;
                        else if (blockState1.getBlock() == Blocks.ENCHANTING_TABLE) enchanting_table++;
                        else if (blockState1.getBlock() == Blocks.ANVIL) anvil++;
                        else if (blockState1.getBlock() == Blocks.CHIPPED_ANVIL) anvilC++;
                        else if (blockState1.getBlock() == Blocks.DAMAGED_ANVIL) anvilD++;

                        else return;
                    }

                    air = direction;
                }
            }

            if (obsidian + respawn_anchor + netherite_block + crying_obi + ender_chest + ancient_debris + enchanting_table + anvil == 5 && air == null) {
                holes.add(holePool.get().set(blockPos, obsidian == 5 ? Hole.Type.Obsidian : respawn_anchor == 5 ? Hole.Type.Respawn_anchor : netherite_block == 5 ? Hole.Type.Netherite_block : crying_obi == 5 ? Hole.Type.Crying_obi : ender_chest == 5 ? Hole.Type.Ender_chest : ancient_debris == 5 ? Hole.Type.Ancient_debris : enchanting_table == 5 ? Hole.Type.Enchanting_table : anvil == 5 ? Hole.Type.Anvil : anvilC == 5 ? Hole.Type.Chipped_anvil : anvilD == 5 ? Hole.Type.Damaged_anvil : Hole.Type.MixedNoBed, NULL));
            } else if (obsidian + bedrock + respawn_anchor + netherite_block + crying_obi + ender_chest + ancient_debris + enchanting_table + anvil == 5 && air == null) {
                holes.add(holePool.get().set(blockPos, obsidian == 5 ? Hole.Type.Obsidian : respawn_anchor == 5 ? Hole.Type.Respawn_anchor : netherite_block == 5 ? Hole.Type.Netherite_block : crying_obi == 5 ? Hole.Type.Crying_obi : ender_chest == 5 ? Hole.Type.Ender_chest : ancient_debris == 5 ? Hole.Type.Ancient_debris : enchanting_table == 5 ? Hole.Type.Enchanting_table : anvil == 5 ? Hole.Type.Anvil : anvilC == 5 ? Hole.Type.Chipped_anvil : anvilD == 5 ? Hole.Type.Damaged_anvil : (bedrock == 5 ? Hole.Type.Bedrock : Hole.Type.Mixed), NULL));
            } else if (obsidian + bedrock + respawn_anchor + netherite_block + crying_obi + ender_chest + ancient_debris + enchanting_table + anvil == 8 && doubles.get() && air != null) {
                holes.add(holePool.get().set(blockPos, obsidian == 8 ? Hole.Type.Obsidian : respawn_anchor == 8 ? Hole.Type.Respawn_anchor : netherite_block == 8 ? Hole.Type.Netherite_block : crying_obi == 8 ? Hole.Type.Crying_obi : ender_chest == 8 ? Hole.Type.Ender_chest : ancient_debris == 8 ? Hole.Type.Ancient_debris : enchanting_table == 8 ? Hole.Type.Enchanting_table : anvil == 8 ? Hole.Type.Anvil : anvilC == 5 ? Hole.Type.Chipped_anvil : anvilD == 5 ? Hole.Type.Damaged_anvil : (bedrock == 8 ? Hole.Type.Bedrock : bedrock == 1 ? Hole.Type.Mixed : Hole.Type.Mixed), Dir.get(air)));
            }
        });
    }

    private boolean validHole(BlockPos pos) {
        if ((ignoreOwn.get() && (mc.player.getBlockPos().equals(pos)))) return false;

        if (!webs.get() && mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) return false;

        if (((AbstractBlockAccessor) mc.world.getBlockState(pos).getBlock()).isCollidable()) return false;

        for (int i = 0; i < holeHeight.get(); i++) {
            if (((AbstractBlockAccessor) mc.world.getBlockState(pos.up(i)).getBlock()).isCollidable()) return false;
        }

        return true;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (HoleESPPlus.Hole hole : holes)
            hole.render(event.renderer, shapeMode.get(), height.get(), topQuad.get(), bottomQuad.get());
    }

    private static class Hole {
        public BlockPos.Mutable blockPos = new BlockPos.Mutable();
        public byte exclude;
        public Type type;

        public Hole set(BlockPos blockPos, Type type, byte exclude) {
            this.blockPos.set(blockPos);
            this.exclude = exclude;
            this.type = type;

            return this;
        }

        public Color getTopColor() {
            return switch (this.type) {
                case Obsidian, Crying_obi, Netherite_block, Respawn_anchor, Ender_chest, Ancient_debris, Enchanting_table, Anvil, Chipped_anvil, Damaged_anvil, MixedNoBed -> Modules.get().get(HoleESPPlus.class).obsidianColorTop.get();
                case Bedrock -> Modules.get().get(HoleESPPlus.class).bedrockColorTop.get();
                default -> Modules.get().get(HoleESPPlus.class).mixedColorTop.get();
            };
        }

        public Color getBottomColor() {
            return switch (this.type) {
                case Obsidian, Crying_obi, Netherite_block, Respawn_anchor, Ender_chest, Ancient_debris, Enchanting_table, Anvil, Chipped_anvil, Damaged_anvil, MixedNoBed -> Modules.get().get(HoleESPPlus.class).obsidianColorBottom.get();
                case Bedrock -> Modules.get().get(HoleESPPlus.class).bedrockColorBottom.get();
                default -> Modules.get().get(HoleESPPlus.class).mixedColorBottom.get();
            };
        }

        public void render(Renderer3D renderer, ShapeMode mode, double height, boolean topQuad, boolean bottomQuad) {
            int x = blockPos.getX();
            int y = blockPos.getY();
            int z = blockPos.getZ();

            Color top = getTopColor();
            Color bottom = getBottomColor();

            int originalTopA = top.a;
            int originalBottompA = bottom.a;

            if (mode.lines()) {
                if (Dir.isNot(exclude, Dir.WEST) && Dir.isNot(exclude, Dir.NORTH))
                    renderer.line(x, y, z, x, y + height, z, bottom, top);
                if (Dir.isNot(exclude, Dir.WEST) && Dir.isNot(exclude, Dir.SOUTH))
                    renderer.line(x, y, z + 1, x, y + height, z + 1, bottom, top);
                if (Dir.isNot(exclude, Dir.EAST) && Dir.isNot(exclude, Dir.NORTH))
                    renderer.line(x + 1, y, z, x + 1, y + height, z, bottom, top);
                if (Dir.isNot(exclude, Dir.EAST) && Dir.isNot(exclude, Dir.SOUTH))
                    renderer.line(x + 1, y, z + 1, x + 1, y + height, z + 1, bottom, top);

                if (Dir.isNot(exclude, Dir.NORTH)) renderer.line(x, y, z, x + 1, y, z, bottom);
                if (Dir.isNot(exclude, Dir.NORTH)) renderer.line(x, y + height, z, x + 1, y + height, z, top);
                if (Dir.isNot(exclude, Dir.SOUTH)) renderer.line(x, y, z + 1, x + 1, y, z + 1, bottom);
                if (Dir.isNot(exclude, Dir.SOUTH)) renderer.line(x, y + height, z + 1, x + 1, y + height, z + 1, top);

                if (Dir.isNot(exclude, Dir.WEST)) renderer.line(x, y, z, x, y, z + 1, bottom);
                if (Dir.isNot(exclude, Dir.WEST)) renderer.line(x, y + height, z, x, y + height, z + 1, top);
                if (Dir.isNot(exclude, Dir.EAST)) renderer.line(x + 1, y, z, x + 1, y, z + 1, bottom);
                if (Dir.isNot(exclude, Dir.EAST)) renderer.line(x + 1, y + height, z, x + 1, y + height, z + 1, top);
            }

            if (mode.sides()) {
                top.a = originalTopA / 2;
                bottom.a = originalBottompA / 2;

                if (Dir.isNot(exclude, Dir.UP) && topQuad)
                    renderer.quad(x, y + height, z, x, y + height, z + 1, x + 1, y + height, z + 1, x + 1, y + height, z, top);
                if (Dir.isNot(exclude, Dir.DOWN) && bottomQuad)
                    renderer.quad(x, y, z, x, y, z + 1, x + 1, y, z + 1, x + 1, y, z, bottom);

                if (Dir.isNot(exclude, Dir.NORTH))
                    renderer.gradientQuadVertical(x, y, z, x + 1, y + height, z, top, bottom);
                if (Dir.isNot(exclude, Dir.SOUTH))
                    renderer.gradientQuadVertical(x, y, z + 1, x + 1, y + height, z + 1, top, bottom);

                if (Dir.isNot(exclude, Dir.WEST))
                    renderer.gradientQuadVertical(x, y, z, x, y + height, z + 1, top, bottom);
                if (Dir.isNot(exclude, Dir.EAST))
                    renderer.gradientQuadVertical(x + 1, y, z, x + 1, y + height, z + 1, top, bottom);

                top.a = originalTopA;
                bottom.a = originalBottompA;
            }
        }

        public enum Type {
            Bedrock, Obsidian, Crying_obi, Netherite_block, Respawn_anchor, Ender_chest, Ancient_debris, Enchanting_table, Anvil, Chipped_anvil, Damaged_anvil, MixedNoBed, Mixed
        }
    }
}
