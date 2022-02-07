package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.misc.Timer;
import meteorclient.events.world.TickEvent;
import meteorclient.settings.IntSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.modules.render.Freecam;

import meteordevelopment.orbit.EventHandler;

public class Twerk extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> twerkDelay = sgGeneral.add(new IntSetting.Builder().name("Twerk Delay").description("In ticks").defaultValue(5).min(1).sliderMax(10).build());

    private boolean hasTwerked = false;

    private Timer onTwerk = new Timer();

    public Twerk() {
        super(Categories.BANANAPLUS, "twerk", "Twerk like the true queen Miley Cyrus");
    }

    public boolean doVanilla() {
        return hasTwerked && !Modules.get().isActive(Freecam.class);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!hasTwerked && !mc.player.isSneaking()) {
            onTwerk.reset();
            hasTwerked = true;
        }

        if (onTwerk.passedTicks(twerkDelay.get()) && hasTwerked) {
            hasTwerked = false;
        }
    }

    @Override
    public void onDeactivate() {
        hasTwerked = false;
        onTwerk.reset();
    }
}
