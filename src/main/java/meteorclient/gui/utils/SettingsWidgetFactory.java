package meteorclient.gui.utils;

import meteorclient.gui.GuiTheme;
import meteorclient.gui.widgets.WWidget;
import meteorclient.settings.Settings;

public interface SettingsWidgetFactory {
    WWidget create(GuiTheme theme, Settings settings, String filter);
}
