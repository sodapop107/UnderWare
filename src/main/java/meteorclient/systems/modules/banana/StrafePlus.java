package meteorclient.systems.modules.banana;

import meteorclient.events.entity.player.PlayerMoveEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.mixininterface.IVec3d;
import meteorclient.settings.*;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.modules.world.Timer;
import meteorclient.utils.Utils;
import meteorclient.utils.player.PlayerUtils;
import meteorclient.utils.world.TickRate;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.entity.effect.StatusEffects;

public class StrafePlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("Speed Value");
    private final SettingGroup sgFriction = settings.createGroup("Frictions");
    private final SettingGroup sgPotions = settings.createGroup("Potions");
    private final SettingGroup sgAC = settings.createGroup("Anti Cheat");

    public final Setting<Double> groundTimer = sgGeneral.add(new DoubleSetting.Builder().name("Ground-Timer").description("Ground timer override.").defaultValue(1).sliderMin(0.01).sliderMax(10).build());
    public final Setting<Double> airTimer = sgGeneral.add(new DoubleSetting.Builder().name("Air-Timer").description("Air timer override.").defaultValue(1.088).sliderMin(0.01).sliderMax(10).build());
    private final Setting<Boolean> autoJump = sgGeneral.add(new BoolSetting.Builder().name("Auto Jump").defaultValue(false).build());
    private final Setting<JumpWhen> jumpIf = sgGeneral.add(new EnumSetting.Builder<JumpWhen>().name("Jump When").defaultValue(JumpWhen.Walking).visible(autoJump::get).build());
    private final Setting<Boolean> lowHop = sgGeneral.add(new BoolSetting.Builder().name("Low Hop").defaultValue(false).visible(autoJump::get).build());
    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder().name("Low Hop Height").defaultValue(0.37).sliderMax(1).visible(() -> autoJump.get() && lowHop.get()).build());
    private final Setting<Boolean> sprintBool = sgGeneral.add(new BoolSetting.Builder().name("Auto Sprint").defaultValue(true).build());
    private final Setting<Boolean> TPSSync = sgGeneral.add(new BoolSetting.Builder().name("TPS Sync").defaultValue(false).build());
    private final Setting<Boolean> sneak = sgSpeed.add(new BoolSetting.Builder().name("Use sneak speed").defaultValue(false).build());
    private final Setting<Double> sneakspeedVal = sgSpeed.add(new DoubleSetting.Builder().name("Sneak speed").defaultValue(0.85).sliderRange(0, 2).range(0, 2).visible(sneak::get).build());
    private final Setting<Boolean> ground = sgSpeed.add(new BoolSetting.Builder().name("Use ground speed").defaultValue(true).build());
    private final Setting<Double> groundspeedVal = sgSpeed.add(new DoubleSetting.Builder().name("Ground speed").defaultValue(1.15).sliderRange(0, 2).range(0, 2).visible(ground::get).build());
    private final Setting<Boolean> air = sgSpeed.add(new BoolSetting.Builder().name("Use air speed").defaultValue(true).build());
    private final Setting<Double> airspeedVal = sgSpeed.add(new DoubleSetting.Builder().name("Air Speed").defaultValue(0.95).sliderRange(0, 2).range(0, 2).visible(air::get).build());
    private final Setting<Double> airFriction = sgFriction.add(new DoubleSetting.Builder().name("Air Friction").defaultValue(0.022f).sliderRange(0, 1).range(0, 1).build());
    private final Setting<Double> waterFriction = sgFriction.add(new DoubleSetting.Builder().name("Water Friction").defaultValue(0.11f).sliderRange(0, 1).range(0, 1).build());
    private final Setting<Double> lavaFriction = sgFriction.add(new DoubleSetting.Builder().name("Lava Friction").defaultValue(0.465f).sliderRange(0, 1).range(0, 1).build());
    private final Setting<Boolean> useJump = sgPotions.add(new BoolSetting.Builder().name("Apply Jump Boost Effect").defaultValue(true).build());
    private final Setting<Boolean> useSpeed = sgPotions.add(new BoolSetting.Builder().name("Apply Speed Effect").defaultValue(true).build());
    private final Setting<Boolean> useSlow = sgPotions.add(new BoolSetting.Builder().name("Apply Slowness Effect").defaultValue(true).build());
    private final Setting<Boolean> hungerCheck = sgAC.add(new BoolSetting.Builder().name("Hunger check").description("Pauses strafe when hunger reaches 3 or less drumsticks").defaultValue(true).build());
    public final Setting<Boolean> onFallFlying = sgAC.add(new BoolSetting.Builder().name("on-fall-flying").description("Uses strafe+ on fall flying.").defaultValue(false).build());

    public StrafePlus() {
        super(Categories.BANANAPLUS, "strafe+", "Increase speed and control");
    }

    private boolean jump() {
        return switch (jumpIf.get()) {
            case Sprinting -> mc.player.isSprinting() && (mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0);
            case Walking -> mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0;
            default -> false;
        };
    }

    private float FinalSpeed;

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;
        if (autoJump.get()) {
            if (!mc.player.isOnGround() || mc.player.isSneaking() || !jump()) return;
            if (lowHop.get()) ((IVec3d) mc.player.getVelocity()).setY(height.get());
            else mc.player.jump();
        }

        if (((mc.options.keyForward.isPressed() || mc.options.keyBack.isPressed() || mc.options.keyLeft.isPressed() || mc.options.keyRight.isPressed())
                && mc.player.isOnGround()) && autoJump.get()) {
            if (mc.player.getActiveStatusEffects().containsValue(mc.player.getStatusEffect(StatusEffects.JUMP_BOOST)) && useJump.get()) {
                mc.player.setVelocity(mc.player.getVelocity().x, (mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1f + height.get(), mc.player.getVelocity().z);
            } else {
                mc.player.setVelocity(mc.player.getVelocity().x, height.get(), mc.player.getVelocity().z);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (mc.player.input.movementForward != 0 | mc.player.input.movementSideways != 0) {
            if (sprintBool.get()) mc.player.setSprinting(true);

            if (hungerCheck.get() && (mc.player.getHungerManager().getFoodLevel() <= 6)) return;
            if (!sneak.get() && mc.player.isSneaking()) return;
            if (!ground.get() && mc.player.isOnGround()) return;
            if (!air.get() && !mc.player.isOnGround()) return;
            if (!onFallFlying.get() && mc.player.isFallFlying()) return;

            if (mc.player.isOnGround()) Modules.get().get(Timer.class).setOverride(PlayerUtils.isMoving() ? groundTimer.get() : Timer.OFF);
            else Modules.get().get(Timer.class).setOverride(PlayerUtils.isMoving() ? airTimer.get() : Timer.OFF);

            FinalSpeed *= frictionValues();

            double[] airV = getSpeedTransform(Math.max((airspeedVal.get() / 2.5) * FinalSpeed * (getDefaultSpeed() / 0.15321), 0.15321));
            double[] groundV = getSpeedTransform(getDefaultSpeed() * (0.2873 / 0.15321) * groundspeedVal.get());
            double[] sneakV = getSpeedTransform(getDefaultSpeed() * (0.2873 / 0.15321) * sneakspeedVal.get());

            if (!mc.player.isOnGround()) {
                mc.player.setVelocity(airV[0], mc.player.getVelocity().y, airV[1]);
            } else {
                FinalSpeed = 1;
                if (!mc.player.isSneaking()) mc.player.setVelocity(groundV[0], mc.player.getVelocity().y, groundV[1]);
                else mc.player.setVelocity(sneakV[0], mc.player.getVelocity().y, sneakV[1]);
            }
        }
    }

    public double[] getSpeedTransform(final double speed) {
        float forward = mc.player.input.movementForward,
                sideways = mc.player.input.movementSideways,
                yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw) * mc.getTickDelta();
        return getSpeedTransform(speed, forward, sideways, yaw);
    }

    public static double[] getSpeedTransform(final double speed, float forwards, float sideways, float yawDegrees) {
        return getSpeedTransform(speed, forwards, sideways, Math.toRadians(yawDegrees));
    }

    public static double[] getSpeedTransform(final double speed, float forwards, float sideways, double yaw) {
        if (forwards != 0) {
            if (sideways > 0) yaw += forwards > 0 ? -Math.PI / 4 : Math.PI / 4;
            else if (sideways < 0) yaw += forwards > 0 ? Math.PI / 4 : -Math.PI / 4;

            sideways = 0;

            if (forwards > 0) forwards = 1;
            else if (forwards < 0) forwards = -1;
        }

        yaw += Math.PI / 2;

        return new double[]{
                forwards * speed * Math.cos(yaw) + sideways * speed * Math.sin(yaw),
                forwards * speed * Math.sin(yaw) - sideways * speed * Math.cos(yaw)
        };
    }

    private double getDefaultSpeed() {
        double baseSpeed = 0.15321 * getTPSMatch();
        if (mc.player.hasStatusEffect(StatusEffects.SPEED) && useSpeed.get()) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }

        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS) && useSlow.get()) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            baseSpeed /= 1.0 + 0.2 * (amplifier + 1);
        }
        return baseSpeed;
    }

    private float frictionValues() {
        float airF = (1 - airFriction.get().floatValue());
        float waterF = (1 - waterFriction.get().floatValue());
        float lavaF = (1 - lavaFriction.get().floatValue());

        if (mc.player.isInLava()) return lavaF;
        if (mc.player.isSubmergedInWater()) return waterF;
        return airF;
    }

    private float getTPSMatch() {
        if (TPSSync.get()) return (TickRate.INSTANCE.getTickRate() / 20);
        return 1;
    }

    public enum JumpWhen {
        Sprinting, Walking
    }
}
