package meteorclient.gui.themes.meteor.widgets;

import meteorclient.gui.renderer.GuiRenderer;
import meteorclient.gui.themes.meteor.MeteorWidget;
import meteorclient.gui.widgets.containers.WView;

public class WMeteorView extends WView implements MeteorWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (canScroll && hasScrollBar) {
            renderer.quad(handleX(), handleY(), handleWidth(), handleHeight(), theme().scrollbarColor.get(handlePressed, handleMouseOver));
        }
    }
}
