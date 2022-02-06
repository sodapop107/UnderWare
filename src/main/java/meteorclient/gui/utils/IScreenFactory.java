package meteorclient.gui.utils;

import meteorclient.gui.GuiTheme;
import meteorclient.gui.WidgetScreen;

public interface IScreenFactory {
    WidgetScreen createScreen(GuiTheme theme);
}
