package meteorclient.gui.widgets.music;

import meteorclient.gui.widgets.containers.WTable;
import meteorclient.gui.GuiTheme;
import meteorclient.gui.screens.music.PlaylistViewScreen;
import meteorclient.gui.tabs.builtin.MusicTab;
import meteorclient.gui.widgets.input.WTextBox;
import meteorclient.utils.music.SearchUtils;

import static meteorclient.UnderWare.mc;

public class WSearchBar extends WMusicWidget {
    @Override
    public void add(WTable parent, MusicTab.MusicScreen screen, GuiTheme theme) {
        WTextBox box = parent.add(theme.textBox("")).expandX().widget();
        parent.add(theme.button("Search")).widget().action = () -> SearchUtils.search(box.get(), playlist -> mc.setScreen(new PlaylistViewScreen(theme, playlist, screen)));
        super.add(parent, screen, theme);
    }
}
