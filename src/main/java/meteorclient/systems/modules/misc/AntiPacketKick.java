package meteorclient.systems.modules.misc;

import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;

public class AntiPacketKick extends Module {
    public AntiPacketKick() {
        super(Categories.Misc, "anti-packet-kick", "Attempts to prevent you from being disconnected by large packets.");
    }
}
