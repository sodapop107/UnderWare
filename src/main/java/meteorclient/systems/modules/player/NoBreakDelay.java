package meteorclient.systems.modules.player;

import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;

public class NoBreakDelay extends Module {
    public NoBreakDelay() {
        super(Categories.Player, "no-break-delay", "Completely removes the delay between breaking blocks.");
    }
}
