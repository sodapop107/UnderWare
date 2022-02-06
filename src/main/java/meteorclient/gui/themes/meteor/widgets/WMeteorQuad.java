package meteorclient.gui.themes.meteor.widgets;

import meteorclient.gui.renderer.GuiRenderer;
import meteorclient.gui.widgets.WQuad;
import meteorclient.utils.render.color.Color;

public class WMeteorQuad extends WQuad {
    public WMeteorQuad(Color color) {
        super(color);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.quad(x, y, width, height, color);
    }
}
