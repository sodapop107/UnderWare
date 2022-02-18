package meteorclient.systems.hud.modules;

import baritone.api.BaritoneAPI;
import baritone.api.process.IBaritoneProcess;

import meteorclient.systems.hud.HUD;
import meteorclient.systems.hud.modules.DoubleTextHudElement;

public class BaritoneHud extends DoubleTextHudElement {
    public BaritoneHud(HUD hud) {
        super(hud, "Baritone", "Displays what baritone is doing.", "Baritone: ");
    }

    @Override
    protected String getRight() {
        IBaritoneProcess process = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingControlManager().mostRecentInControl().orElse(null);

        if (process == null) return "-";

        return process.displayName();


    }
}
