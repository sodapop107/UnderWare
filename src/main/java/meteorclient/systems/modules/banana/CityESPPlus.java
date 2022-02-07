package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.entity.BEntityUtils;
import meteorclient.events.render.Render3DEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.renderer.ShapeMode;
import meteorclient.settings.*;
import meteorclient.systems.modules.Module;
import meteorclient.utils.entity.SortPriority;
import meteorclient.utils.entity.TargetUtils;
import meteorclient.utils.render.color.SettingColor;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;


public class CityESPPlus extends Module {
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> renderExtender = sgRender.add(new DoubleSetting.Builder().name("Render-extender").description("The distance which it will increase the normal mc interaction manager by.").defaultValue(2).min(0).sliderMax(6).build());
    private final Setting<Boolean> PrioBurrowed = sgRender.add(new BoolSetting.Builder().name("Prioritise Burrow").description("Will prioritise rendering the burrow block.").defaultValue(true).build());
    private final Setting<Boolean> NoRenderSurrounded = sgRender.add(new BoolSetting.Builder().name("Not Surrounded").description("Will not render if the target is not surrounded.").defaultValue(true).build());
    private final Setting<Boolean> AvoidSelf = sgRender.add(new BoolSetting.Builder().name("Avoid Self").description("Will avoid targetting self surround.").defaultValue(true).build());
    private final Setting<Boolean> LastResort = sgRender.add(new BoolSetting.Builder().name("Last Resort").description("Will try to target your own surround as final option.").defaultValue(true).visible(AvoidSelf::get).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the rendering.").defaultValue(new SettingColor(230, 0, 255, 5)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the rendering.").defaultValue(new SettingColor(250, 0, 255, 255)).build());

    public BlockPos target;

    public CityESPPlus() {
        super(Categories.BANANAPLUS, "city-esp+", "Displays more blocks that can be broken in order to city another player.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        PlayerEntity targetEntity = TargetUtils.getPlayerTarget(mc.interactionManager.getReachDistance() + renderExtender.get(), SortPriority.LowestDistance);

        if (TargetUtils.isBadTarget(targetEntity, mc.interactionManager.getReachDistance() + 2)) {
            target = null;
        } else if (PrioBurrowed.get() && BEntityUtils.isBurrowed(targetEntity) && !Objects.requireNonNull(mc.world).getBlockState(targetEntity.getBlockPos()).isOf(Blocks.BEDROCK)) {
            target = targetEntity.getBlockPos();
        } else if (NoRenderSurrounded.get() && !BEntityUtils.isSurrounded(targetEntity)) {
            target = null;
        } else if (AvoidSelf.get()) {
            target = BEntityUtils.getTargetBlock(targetEntity);
            if (target == null && LastResort.get()) target = BEntityUtils.getCityBlock(targetEntity);
        } else target = BEntityUtils.getCityBlock(targetEntity);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (target == null) return;
        event.renderer.box(target, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
