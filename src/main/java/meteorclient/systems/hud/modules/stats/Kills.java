package meteorclient.systems.hud.modules.stats;

import meteorclient.systems.modules.banana.MonkeStats;
import meteorclient.systems.hud.HUD;
import meteorclient.systems.hud.modules.DoubleTextHudElement;

public class Kills extends DoubleTextHudElement {
    public Kills(HUD hud) {
        super(hud, "Kills", "Displays your total kill count", "Kills: ");
    }

    @Override
    protected String getRight() {
        return String.valueOf(MonkeStats.kills);
    }
}
