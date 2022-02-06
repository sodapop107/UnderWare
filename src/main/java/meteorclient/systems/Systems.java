package meteorclient.systems;

import meteorclient.MeteorClient;
import meteorclient.events.game.GameLeftEvent;
import meteorclient.systems.accounts.Accounts;
import meteorclient.systems.commands.Commands;
import meteorclient.systems.config.Config;
import meteorclient.systems.friends.Friends;
import meteorclient.systems.hud.HUD;
import meteorclient.systems.macros.Macros;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.profiles.Profiles;
import meteorclient.systems.proxies.Proxies;
import meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.orbit.EventHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Systems {
    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends System>, System<?>> systems = new HashMap<>();
    private static final List<Runnable> preLoadTasks = new ArrayList<>(1);

    public static void addPreLoadTask(Runnable task) {
        preLoadTasks.add(task);
    }

    public static void init() {
        System<?> config = add(new Config());
        config.init();
        config.load();

        add(new Modules());
        add(new Commands());
        add(new Friends());
        add(new Macros());
        add(new Accounts());
        add(new Waypoints());
        add(new Profiles());
        add(new Proxies());
        add(new HUD());

        MeteorClient.EVENT_BUS.subscribe(Systems.class);
    }

    private static System<?> add(System<?> system) {
        systems.put(system.getClass(), system);
        MeteorClient.EVENT_BUS.subscribe(system);
        system.init();

        return system;
    }

    // save/load

    @EventHandler
    private static void onGameLeft(GameLeftEvent event) {
        save();
    }

    public static void save(File folder) {
        long start = java.lang.System.currentTimeMillis();
        MeteorClient.LOG.info("Saving");

        for (System<?> system : systems.values()) system.save(folder);

        MeteorClient.LOG.info("Saved in {} milliseconds.", java.lang.System.currentTimeMillis() - start);
    }

    public static void save() {
        save(null);
    }

    public static void load(File folder) {
        long start = java.lang.System.currentTimeMillis();
        MeteorClient.LOG.info("Loading");

        for (Runnable task : preLoadTasks) task.run();
        for (System<?> system : systems.values()) system.load(folder);

        MeteorClient.LOG.info("Loaded in {} milliseconds", java.lang.System.currentTimeMillis() - start);
    }

    public static void load() {
        load(null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends System<?>> T get(Class<T> klass) {
        return (T) systems.get(klass);
    }
}
