package meteorclient.gui.themes.meteor.widgets;

import meteorclient.gui.renderer.GuiRenderer;
import meteorclient.gui.themes.meteor.MeteorWidget;
import meteorclient.gui.widgets.WTooltip;

public class WMeteorTooltip extends WTooltip implements MeteorWidget {
    public WMeteorTooltip(String text) {
        super(text);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.quad(this, theme().backgroundColor.get());
    }
}
