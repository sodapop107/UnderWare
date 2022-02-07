package meteorclient.systems.hud.modules.stats;

import meteorclient.systems.modules.banana.MonkeStats;
import meteorclient.systems.hud.HUD;
import meteorclient.systems.hud.modules.DoubleTextHudElement;

public class KillStreak extends DoubleTextHudElement {
    public KillStreak(HUD hud) {
        super(hud, "KillStreak", "Displays your current killStreak", "KillStreak: ");
    }

    @Override
    protected String getRight() {
        return String.valueOf(MonkeStats.killStreak);
    }
}

