package meteorclient.gui.tabs.builtin;

import meteorclient.gui.GuiTheme;
import meteorclient.gui.GuiThemes;
import meteorclient.gui.tabs.Tab;
import meteorclient.gui.tabs.TabScreen;
import net.minecraft.client.gui.screen.Screen;

public class ModulesTab extends Tab {
    public ModulesTab() {
        super("Modules");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return theme.modulesScreen();
    }

    @Override
    public boolean isScreen(Screen screen) {
        return GuiThemes.get().isModulesScreen(screen);
    }
}
