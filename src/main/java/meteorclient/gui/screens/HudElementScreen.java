package meteorclient.gui.screens;

import meteorclient.gui.GuiTheme;
import meteorclient.gui.WindowScreen;
import meteorclient.gui.renderer.GuiRenderer;
import meteorclient.gui.widgets.containers.WContainer;
import meteorclient.gui.widgets.containers.WHorizontalList;
import meteorclient.gui.widgets.pressable.WButton;
import meteorclient.gui.widgets.pressable.WCheckbox;
import meteorclient.systems.Systems;
import meteorclient.systems.hud.HUD;
import meteorclient.systems.hud.modules.HudElement;
import meteorclient.utils.Utils;
import meteorclient.utils.misc.NbtUtils;
import net.minecraft.nbt.NbtCompound;

import static meteorclient.utils.Utils.getWindowWidth;

public class HudElementScreen extends WindowScreen {
    public final HudElement element;
    private WContainer settings;

    public HudElementScreen(GuiTheme theme, HudElement element) {
        super(theme, element.title);

        this.element = element;
    }

    @Override
    public void initWidgets() {
        // Description
        add(theme.label(element.description, getWindowWidth() / 2.0));

        // Settings
        if (element.settings.sizeGroups() > 0) {
            settings = add(theme.verticalList()).expandX().widget();
            settings.add(theme.settings(element.settings)).expandX();

            add(theme.horizontalSeparator()).expandX();
        }

        // Bottom
        WHorizontalList bottomList = add(theme.horizontalList()).expandX().widget();

        //   Active
        bottomList.add(theme.label("Active:"));
        WCheckbox active = bottomList.add(theme.checkbox(element.active)).widget();
        active.action = () -> {
            if (element.active != active.checked) element.toggle();
        };

        WButton reset = bottomList.add(theme.button(GuiRenderer.RESET)).expandCellX().right().widget();
        reset.action = () -> {
            if (element.active != element.defaultActive) element.active = active.checked = element.defaultActive;
        };
    }

    @Override
    public void tick() {
        super.tick();

        if (settings != null) {
            element.settings.tick(settings, theme);
        }
    }

    @Override
    protected void onRenderBefore(float delta) {
        if (!Utils.canUpdate()) {
            Systems.get(HUD.class).render(delta, hudElement -> true);
        }
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(element.title, element.toTag());
    }

    @Override
    public boolean fromClipboard() {
        NbtCompound clipboard = NbtUtils.fromClipboard(element.toTag());

        if (clipboard != null) {
            element.fromTag(clipboard);
            return true;
        }

        return false;
    }
}
