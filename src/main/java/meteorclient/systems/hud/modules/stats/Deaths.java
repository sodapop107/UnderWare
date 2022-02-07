package meteorclient.systems.hud.modules.stats;

import meteorclient.systems.modules.banana.MonkeStats;
import meteorclient.systems.hud.HUD;
import meteorclient.systems.hud.modules.DoubleTextHudElement;

public class Deaths extends DoubleTextHudElement {
    public Deaths(HUD hud) {
        super(hud, "Deaths", "Displays your total death count", "Deaths: ");
    }

    @Override
    protected String getRight() {
        return String.valueOf(MonkeStats.deaths);
    }
}
