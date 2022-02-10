package meteorclient.systems.modules.misc;

import baritone.api.BaritoneAPI;

import meteorclient.systems.modules.Categories;
import meteordevelopment.orbit.EventHandler;

import meteorclient.events.world.TickEvent;
import meteorclient.systems.modules.Module;
import meteorclient.settings.DoubleSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;

public class PauseOnUnloadedChunk extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> readahead = sgGeneral.add(new DoubleSetting.Builder()
        .name("Readahead")
        .description("How far the module should 'look ahead' for unloaded chunks.")
        .min(1)
        .max(40)
        .sliderMin(1)
        .sliderMax(40)
        .defaultValue(12)
        .build()
    );

    public PauseOnUnloadedChunk() {
        super(Categories.Misc, "pause-on-unloaded", "Pauses Baritone when attempting to enter an unloaded chunk.");
    }

    private boolean paused;
    private int pausedChunkX;
    private int pausedChunkZ;

    @Override
    public void onActivate() {
        paused = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        int chunkX = (int) ((mc.player.getX() + (mc.player.getVelocity().getX() * readahead.get())) / 32);
        int chunkZ = (int) ((mc.player.getZ() + (mc.player.getVelocity().getZ() * readahead.get())) / 32);

        if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ) && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing() && !paused) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
            info("Entering unloaded chunk, pausing Baritone.");
            paused = true;
            pausedChunkX = chunkX;
            pausedChunkZ = chunkZ;
        } else if(paused && mc.world.getChunkManager().isChunkLoaded(pausedChunkX, pausedChunkZ)) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
            info("Chunk was loaded, resuming Baritone.");
            paused = false;
        }
    }
}
