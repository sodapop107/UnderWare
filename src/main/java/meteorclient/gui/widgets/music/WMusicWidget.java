package meteorclient.gui.widgets.music;

import meteorclient.gui.widgets.containers.WTable;
import meteorclient.gui.GuiTheme;
import meteorclient.gui.tabs.builtin.MusicTab;

import java.util.ArrayList;
import java.util.List;

public abstract class WMusicWidget {
    protected List<WMusicWidget> childWidgets = new ArrayList<>();
    public void add(WTable parent, MusicTab.MusicScreen screen, GuiTheme theme) {
        for (WMusicWidget child : childWidgets) {
            child.add(parent, screen, theme);
        }
    }
}
