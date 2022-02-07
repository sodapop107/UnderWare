package meteorclient.systems.hud.modules.stats;

import meteorclient.systems.modules.banana.MonkeStats;
import meteorclient.systems.hud.HUD;
import meteorclient.systems.hud.modules.DoubleTextHudElement;

public class KD extends DoubleTextHudElement {
    public KD(HUD hud) {
        super(hud, "Kill/Death", "Displays your kills to death ratio", "K/D: ");
    }

    @Override
    protected String getRight() {
        return String.valueOf(MonkeStats.kD);
    }

}
