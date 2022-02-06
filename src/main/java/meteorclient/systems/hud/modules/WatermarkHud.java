package meteorclient.systems.hud.modules;

import meteorclient.MeteorClient;
import meteorclient.systems.hud.HUD;

public class WatermarkHud extends DoubleTextHudElement {
    public WatermarkHud(HUD hud) {
        super(hud, "watermark", "Displays a UnderWare watermark.", "UnderWare ");
    }

    @Override
    protected String getRight() {
        if (MeteorClient.DEV_BUILD.isEmpty()) {
            return MeteorClient.VERSION.toString();
        }

        return MeteorClient.VERSION + " " + MeteorClient.DEV_BUILD;
    }
}
