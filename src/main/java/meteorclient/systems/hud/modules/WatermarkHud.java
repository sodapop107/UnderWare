package meteorclient.systems.hud.modules;

import meteorclient.UnderWare;
import meteorclient.systems.hud.HUD;

public class WatermarkHud extends DoubleTextHudElement {
    public WatermarkHud(HUD hud) {
        super(hud, "watermark", "Displays a UnderWare watermark.", "UnderWare ");
    }

    @Override
    protected String getRight() {
        if (UnderWare.DEV_BUILD.isEmpty()) {
            return UnderWare.VERSION.toString();
        }

        return UnderWare.VERSION + " " + UnderWare.DEV_BUILD;
    }
}
