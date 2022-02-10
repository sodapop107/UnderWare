package meteorclient.systems.modules.movement;

import meteorclient.events.entity.BoatMovementEvent;
import meteorclient.events.entity.LivingEntityMoveEvent;
import meteorclient.mixininterface.IVec3d;
import meteorclient.settings.BoolSetting;
import meteorclient.settings.DoubleSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;
import meteorclient.utils.player.PlayerUtils;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;

public class EntitySpeed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum BoatTurn {
        Cursor,
        Normal,
        Move
    }
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Horizontal speed in blocks per second.")
        .defaultValue(10)
        .min(0)
        .sliderMax(50)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Use speed only when standing on a block.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> inWater = sgGeneral.add(new BoolSetting.Builder()
        .name("in-water")
        .description("Use speed when in water.")
        .defaultValue(false)
        .build()
    );

    public EntitySpeed() {
        super(Categories.Movement, "entity-speed", "Makes you go faster when riding entities.");
    }

    public static boolean pressingLeft = false;
    public static boolean pressingRight = false;
    public static boolean pressingBack = false;
    public static boolean pressingForward = false;

    @EventHandler
    private void onLivingEntityMove(LivingEntityMoveEvent event) {
        if (event.entity.getPrimaryPassenger() != mc.player) return;

        // Check for onlyOnGround and inWater
        LivingEntity entity = event.entity;
        if (onlyOnGround.get() && !entity.isOnGround()) return;
        if (!inWater.get() && entity.isTouchingWater()) return;

        // Set horizontal velocity
        Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
        ((IVec3d) event.movement).setXZ(vel.x, vel.z);
    }

    @EventHandler
    private void boatMoved(BoatMovementEvent event) {
        if (event.boat.getPrimaryPassenger() != mc.player) return;

        BoatEntity entity = event.boat;
        if(onlyOnGround.get() && !entity.isOnGround()) return;
        if(!inWater.get() && entity.isTouchingWater()) return;
        if(pressingLeft || pressingRight) if(!pressingForward && !pressingBack) return;

        Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
        ((IVec3d) event.movement).setXZ(vel.x, vel.z);
    }

    @Override
    public void onDeactivate() {
        pressingLeft = false;
        pressingRight = false;
        pressingBack = false;
        pressingForward = false;
    }
}
