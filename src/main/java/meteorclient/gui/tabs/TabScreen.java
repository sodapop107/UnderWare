package meteorclient.gui.tabs;

import meteorclient.gui.GuiTheme;
import meteorclient.gui.WidgetScreen;
import meteorclient.gui.utils.Cell;
import meteorclient.gui.widgets.WWidget;

public abstract class TabScreen extends WidgetScreen {
    public final Tab tab;

    public TabScreen(GuiTheme theme, Tab tab) {
        super(theme, tab.name);

        this.tab = tab;
    }

    public <T extends WWidget> Cell<T> addDirect(T widget) {
        return super.add(widget);
    }
}
