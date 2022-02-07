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

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;


public class BurrowESP extends Module {
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the rendering.").defaultValue(new SettingColor(230, 0, 255, 5)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the rendering.").defaultValue(new SettingColor(250, 0, 255, 255)).build());
    private final Setting<Boolean> renderWebbed = sgRender.add(new BoolSetting.Builder().name("Render webbed").description("Will render if the target is webbed").defaultValue(true).build());
    private final Setting<SettingColor> WebsideColor = sgRender.add(new ColorSetting.Builder().name("web-side-color").description("The side color of the rendering for webs.").defaultValue(new SettingColor(240, 250, 65, 35)).visible(renderWebbed::get).build());
    private final Setting<SettingColor> WeblineColor = sgRender.add(new ColorSetting.Builder().name("web-line-color").description("The line color of the rendering for webs.").defaultValue(new SettingColor(0, 0, 0, 0)).visible(renderWebbed::get).build());

    public BlockPos target;
    public boolean isTargetWebbed;
    public boolean isTargetBurrowed;

    public BurrowESP() {
        super(Categories.BANANAPLUS, "Burrow-ESP", "Displays if the closest target to you is burrowed / webbed.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        PlayerEntity targetEntity = TargetUtils.getPlayerTarget(mc.interactionManager.getReachDistance() + 2, SortPriority.LowestDistance);

        if (TargetUtils.isBadTarget(targetEntity, mc.interactionManager.getReachDistance() + 2)) {
            target = null;
        } else if (renderWebbed.get() && BEntityUtils.isWebbed(targetEntity)) {
            target = targetEntity.getBlockPos();
        } else if (BEntityUtils.isBurrowed(targetEntity)) {
            target = targetEntity.getBlockPos();
        } else target = null;

        isTargetWebbed = (target != null && BEntityUtils.isWebbed(targetEntity));
        isTargetBurrowed = (target != null && BEntityUtils.isBurrowed(targetEntity));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (target == null) return;
        if (isTargetWebbed) event.renderer.box(target, WebsideColor.get(), WeblineColor.get(), shapeMode.get(), 0);
        else if (isTargetBurrowed) event.renderer.box(target, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
