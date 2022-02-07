package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;

public class VanillaAutoJump extends Module {
    public VanillaAutoJump() {
        super(Categories.BANANAPLUS, "vanilla-auto-jump", "Toggles vanilla auto jump in minecraft.");
    }

    @Override
    public void onActivate() {
        mc.options.autoJump = true;
    }

    @Override
    public void onDeactivate() {
        mc.options.autoJump = false;
    }
}


