package meteorclient.systems.modules.banana;

import meteorclient.events.world.TickEvent;
import meteorclient.settings.*;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.modules.world.Timer;

import meteordevelopment.orbit.EventHandler;

public class ReverseStepTimer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder().name("timer-speed").description("Timer for fall speed.").defaultValue(5).min(0).sliderRange(0, 1000).build());
    private final Setting<Double> fallDistance = sgGeneral.add(new DoubleSetting.Builder().name("fall-distance").description("The maximum fall distance this setting will activate at.").defaultValue(3).min(0).build());
    private final Setting<Boolean> whileJumping = sgGeneral.add(new BoolSetting.Builder().name("while-jumping").description("Should it be active while jump key held.").defaultValue(false).build());
    private final Setting<Integer> pullDelay = sgGeneral.add(new IntSetting.Builder().name("pull-delay").description("Amount of ticks it should wait before pulling you after you jump.").defaultValue(20).min(1).sliderMax(60).build());
    private final Setting<Boolean> resetDelay = sgGeneral.add(new BoolSetting.Builder().name("reset-on-ground").description("Should it be reset pull delay upon touching the ground.").defaultValue(true).build());

    public ReverseStepTimer() {
        super(Categories.BANANAPLUS, "reverse-step-timer", "Tries to bypass strict server for reverse step.");
    }

    private meteorclient.utils.misc.Timer inAirTime = new meteorclient.utils.misc.Timer();
    boolean didJump = false;

    @Override
    public void onActivate() {
        didJump = false;
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.options.keyJump.isPressed()) {
            inAirTime.reset();
            didJump = true;
        }

        if ((inAirTime.passedTicks(pullDelay.get()) && didJump) || (mc.player.isOnGround() && resetDelay.get())) {
            didJump = false;
        }

        if (mc.player.isOnGround() || mc.player.isHoldingOntoLadder() || mc.player.isSubmergedInWater() || mc.player.isInLava() || (mc.options.keyJump.isPressed() && whileJumping.get()) || mc.player.noClip || didJump || mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, (float) -(fallDistance.get()) + 0.01, 0.0))) {
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
            return;
        } else Modules.get().get(Timer.class).setOverride(this.timer.get());
    }
}
