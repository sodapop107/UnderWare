package meteorclient.gui.themes.meteor.widgets;

import meteorclient.gui.themes.meteor.MeteorWidget;
import meteorclient.gui.widgets.WTopBar;
import meteorclient.utils.render.color.Color;

public class WMeteorTopBar extends WTopBar implements MeteorWidget {
    @Override
    protected Color getButtonColor(boolean pressed, boolean hovered) {
        return theme().backgroundColor.get(pressed, hovered);
    }

    @Override
    protected Color getNameColor() {
        return theme().textColor.get();
    }
}
