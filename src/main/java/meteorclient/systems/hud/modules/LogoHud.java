package meteorclient.systems.hud.modules;

import meteorclient.renderer.GL;
import meteorclient.renderer.Renderer2D;
import meteorclient.settings.DoubleSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.systems.hud.HUD;
import meteorclient.systems.hud.HudRenderer;
import net.minecraft.util.Identifier;

import static meteorclient.utils.Utils.WHITE;

public class LogoHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the logo.")
        .defaultValue(3)
        .min(0.1)
        .sliderRange(0.1, 10)
        .build()
    );

    private final Identifier TEXTURE = new Identifier("under-ware", "textures/underware.png");

    public LogoHud(HUD hud) {
        super(hud, "logo", "Shows the Meteor logo in the HUD.");
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(64 * scale.get(), 64 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        GL.bindTexture(TEXTURE);
        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(box.getX(), box.getY(), box.width, box.height, WHITE);
        Renderer2D.TEXTURE.render(null);
    }
}
