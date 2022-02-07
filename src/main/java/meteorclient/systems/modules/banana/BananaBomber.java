package meteorclient.systems.modules.banana;

import com.google.common.util.concurrent.AtomicDouble;

import it.unimi.dsi.fastutil.ints.*;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.misc.BDamageUtils;
import meteorclient.utils.entity.BEntityUtils;
import meteorclient.events.entity.EntityAddedEvent;
import meteorclient.events.entity.EntityRemovedEvent;
import meteorclient.events.packets.PacketEvent;
import meteorclient.events.render.Render2DEvent;
import meteorclient.events.render.Render3DEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.mixininterface.IBox;
import meteorclient.mixininterface.IRaycastContext;
import meteorclient.mixininterface.IVec3d;
import meteorclient.renderer.ShapeMode;
import meteorclient.renderer.text.TextRenderer;
import meteorclient.settings.*;
import meteorclient.systems.friends.Friends;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.modules.combat.KillAura;
import meteorclient.utils.entity.EntityUtils;
import meteorclient.utils.entity.Target;
import meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteorclient.utils.misc.Keybind;
import meteorclient.utils.misc.Vec3;
import meteorclient.utils.player.FindItemResult;
import meteorclient.utils.player.InvUtils;
import meteorclient.utils.player.PlayerUtils;
import meteorclient.utils.player.Rotations;
import meteorclient.utils.render.NametagUtils;
import meteorclient.utils.render.color.SettingColor;
import meteorclient.utils.world.BlockIterator;
import meteorclient.utils.world.BlockUtils;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BananaBomber extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgFacePlace = settings.createGroup("Face Place");
    private final SettingGroup sgSurround = settings.createGroup("Surround");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").description("Range in which to target players.").defaultValue(10).min(0).sliderMax(16).build());
    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug-mode").description("Informs you about what the CA is doing.").defaultValue(false).build());
    private final Setting<Boolean> predictMovement = sgGeneral.add(new BoolSetting.Builder().name("predict-movement").description("Predicts target movement.").defaultValue(false).build());
    private final Setting<Boolean> ignoreTerrain = sgGeneral.add(new BoolSetting.Builder().name("ignore-terrain").description("Completely ignores terrain if it can be blown up by end crystals.").defaultValue(true).build());
    public final Setting<Boolean> fullAnvils = sgGeneral.add(new BoolSetting.Builder().name("full-anvils").description("Completely ignores gaps between anvil blocks for damage calc.").defaultValue(false).build());
    public final Setting<Boolean> fullEchest = sgGeneral.add(new BoolSetting.Builder().name("full-E-Chest").description("Completely ignores gaps between E-chest blocks for damage calc.").defaultValue(false).build());
    public final Setting<Boolean> centerDamage = sgGeneral.add(new BoolSetting.Builder().name("Center-Damage").description("Get damage from the center of the blast.").defaultValue(false).build());
    public final Setting<Integer> explosionRadius = sgGeneral.add(new IntSetting.Builder().name("max-explosion-radius").description("Max crystal explosion radius").defaultValue(12).min(0).sliderMin(0).sliderMax(12).build());
    private final Setting<Integer> waiting = sgGeneral.add(new IntSetting.Builder().name("min-explosion-time").description("Min tick duration for crystal explosion").defaultValue(3).min(0).sliderMin(0).sliderMax(6).build());
    private final Setting<AutoSwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<AutoSwitchMode>().name("auto-switch").description("Switches to crystals in your hotbar once a target is found.").defaultValue(AutoSwitchMode.Normal).build());
    private final Setting<Boolean> noGapSwitch = sgGeneral.add(new BoolSetting.Builder().name("No Gap Switch").description("Disables normal auto switch when you are holding a gap.").defaultValue(true).visible(() -> autoSwitch.get() == AutoSwitchMode.Normal).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotates server-side towards the crystals being hit/placed.").defaultValue(true).build());
    private final Setting<YawStepMode> yawStepMode = sgGeneral.add(new EnumSetting.Builder<YawStepMode>().name("yaw-steps-mode").description("When to run the yaw steps check.").defaultValue(YawStepMode.Break).visible(rotate::get).build());
    private final Setting<Double> yawSteps = sgGeneral.add(new DoubleSetting.Builder().name("yaw-steps").description("Maximum number of degrees its allowed to rotate in one tick.").defaultValue(180).min(1).max(180).sliderMin(1).sliderMax(180).visible(rotate::get).build());
    private final Setting<Boolean> doPlace = sgPlace.add(new BoolSetting.Builder().name("place").description("If the CA should place crystals.").defaultValue(true).build());
    private final Setting<Double> PminDamage = sgPlace.add(new DoubleSetting.Builder().name("min-place-damage").description("Minimum place damage the crystal needs to deal to your target.").defaultValue(6).min(0).build());
    private final Setting<DamageIgnore> PDamageIgnore = sgPlace.add(new EnumSetting.Builder<DamageIgnore>().name("ignore-self-place-damage").description("When to ignore self damage when placing crystal.").defaultValue(DamageIgnore.Never).build());
    private final Setting<Double> PmaxDamage = sgPlace.add(new DoubleSetting.Builder().name("max-place-damage").description("Maximum place damage crystals can deal to yourself.").defaultValue(6).min(0).max(36).sliderMax(36).visible(() -> PDamageIgnore.get() != DamageIgnore.Always).build());
    private final Setting<Boolean> PantiSuicide = sgPlace.add(new BoolSetting.Builder().name("anti-suicide-place").description("Will not place crystals if they will pop / kill you.").defaultValue(true).visible(() -> PDamageIgnore.get() != DamageIgnore.Always).build());
    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder().name("place-delay").description("The delay in ticks to wait to place a crystal after it's exploded.").defaultValue(0).min(0).sliderMin(0).sliderMax(20).build());
    private final Setting<Boolean> placeIn = sgPlace.add(new BoolSetting.Builder().name("place-in-block").description("Forces the ca to place in blocks.").defaultValue(false).build());
    private final Setting<Integer> yDifference = sgPlace.add(new IntSetting.Builder().name("Y-difference").description("The minimum Y level difference for place in.").defaultValue(0).min(0).sliderMin(0).sliderMax(5).visible(placeIn::get).build());
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder().name("place-range").description("Range in which to place crystals.").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Double> placeWallsRange = sgPlace.add(new DoubleSetting.Builder().name("place-walls-range").description("Range in which to place crystals when behind blocks.").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Boolean> placement112 = sgPlace.add(new BoolSetting.Builder().name("1.12-placement").description("Uses 1.12 crystal placement.").defaultValue(false).build());
    private final Setting<SupportMode> support = sgPlace.add(new EnumSetting.Builder<SupportMode>().name("support").description("Places a support block in air if no other position have been found.").defaultValue(SupportMode.Disabled).build());
    private final Setting<Integer> supportDelay = sgPlace.add(new IntSetting.Builder().name("support-delay").description("Delay in ticks after placing support block.").defaultValue(1).min(0).visible(() -> support.get() != SupportMode.Disabled).build());
    private final Setting<Boolean> facePlace = sgFacePlace.add(new BoolSetting.Builder().name("face-place").description("Will face-place when target is below a certain health or armor durability threshold.").defaultValue(true).build());
    private final Setting<Boolean> slowFacePlace = sgFacePlace.add(new BoolSetting.Builder().name("slow-face-place").description("Will slow down face-place to save crystals.").defaultValue(false).build());
    private final Setting<SlowFacePlace> slowFPMode = sgFacePlace.add(new EnumSetting.Builder<SlowFacePlace>().name("slow-FP-mode").description("Mode to use for the slow face place.").defaultValue(SlowFacePlace.Auto).visible(slowFacePlace::get).build());
    private final Setting<Integer> slowFPDelay = sgFacePlace.add(new IntSetting.Builder().name("slow-FP-delay").description("The delay in ticks to wait to break a crystal for custom face place delay.").defaultValue(10).min(0).sliderMin(0).sliderMax(20).visible(() -> slowFacePlace.get() && slowFPMode.get() == SlowFacePlace.Custom).build());
    private final Setting<Boolean> surrHoldPause = sgFacePlace.add(new BoolSetting.Builder().name("Pause-on-surround-hold").description("Will pause face placing when surround hold is active.").defaultValue(true).visible(facePlace::get).build());
    private final Setting<Boolean> KAPause = sgFacePlace.add(new BoolSetting.Builder().name("Pause-on-KA").description("Will pause face placing when KA is active.").defaultValue(true).visible(facePlace::get).build());
    private final Setting<Boolean> CevPause = sgFacePlace.add(new BoolSetting.Builder().name("Pause-on-Cev-Break").description("Will pause face placing when Cev Breaker is active.").defaultValue(false).visible(facePlace::get).build());
    private final Setting<Boolean> greenHolers = sgFacePlace.add(new BoolSetting.Builder().name("Green-holers").description("Will automatically face-place when target's is in greenhole.").defaultValue(false).visible(facePlace::get).build());
    private final Setting<Boolean> faceSurrounded = sgFacePlace.add(new BoolSetting.Builder().name("face-surrounded").description("Will face-place even when target's face is surrounded.").defaultValue(false).visible(facePlace::get).build());
    private final Setting<Double> facePlaceHealth = sgFacePlace.add(new DoubleSetting.Builder().name("face-place-health").description("The health the target has to be at to start face placing.").defaultValue(8).min(0).sliderMin(0).sliderMax(36).visible(facePlace::get).build());
    private final Setting<Double> facePlaceDurability = sgFacePlace.add(new DoubleSetting.Builder().name("face-place-durability").description("The durability threshold percentage to be able to face-place.").defaultValue(2).min(0).sliderMin(0).sliderMax(100).visible(facePlace::get).build());
    private final Setting<Boolean> facePlaceArmor = sgFacePlace.add(new BoolSetting.Builder().name("face-place-missing-armor").description("Automatically starts face placing when a target misses a piece of armor.").defaultValue(false).visible(facePlace::get).build());
    private final Setting<Keybind> forceFacePlace = sgFacePlace.add(new KeybindSetting.Builder().name("force-face-place").description("Starts face place when this button is pressed.").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Boolean> surroundBreak = sgSurround.add(new BoolSetting.Builder().name("surround-break").description("Will automatically places a crystal next to target's surround.").defaultValue(false).build());
    private final Setting<SurroundBorHWhen> surroundBWhen = sgSurround.add(new EnumSetting.Builder<SurroundBorHWhen>().name("surround-break-when").description("When to start surround breaking.").defaultValue(SurroundBorHWhen.FaceTrapped).visible(surroundBreak::get).build());
    private final Setting<Boolean> facePlacePause = sgSurround.add(new BoolSetting.Builder().name("pause-while-faceplacing").description("Will pause surround breaking while face placing targets.").defaultValue(true).build());
    private final Setting<Boolean> ignoreBurrowed = sgSurround.add(new BoolSetting.Builder().name("ignore-burrowed").description("Will not try to surround break targets that are burrowed.").defaultValue(true).build());
    private final Setting<Boolean> surroundHold = sgSurround.add(new BoolSetting.Builder().name("surround-hold").description("Break crystals slower to hold on to their surround when their surround is broken.").defaultValue(false).build());
    private final Setting<SurroundBorHWhen> surroundHWhen = sgSurround.add(new EnumSetting.Builder<SurroundBorHWhen>().name("surround-hold-when").description("When to start surround holding.").defaultValue(SurroundBorHWhen.AnyTrapped).visible(surroundHold::get).build());
    private final Setting<SurroundHold> surroundHoldMode = sgSurround.add(new EnumSetting.Builder<SurroundHold>().name("surround-hold-mode").description("Mode to use for the surround hold.").defaultValue(SurroundHold.Auto).visible(surroundHold::get).build());
    private final Setting<Integer> surroundHoldDelay = sgSurround.add(new IntSetting.Builder().name("surround-hold-delay").description("The delay in ticks to wait to break a crystal for custom surround hold.").defaultValue(10).min(0).sliderMin(0).sliderMax(20).visible(() -> surroundHold.get() && surroundHoldMode.get() == SurroundHold.Custom).build());
    private final Setting<Boolean> doBreak = sgBreak.add(new BoolSetting.Builder().name("break").description("If the CA should break crystals.").defaultValue(true).build());
    private final Setting<Double> BminDamage = sgBreak.add(new DoubleSetting.Builder().name("min-break-damage").description("Minimum break damage the crystal needs to deal to your target.").defaultValue(6).min(0).build());
    private final Setting<DamageIgnore> BDamageIgnore = sgBreak.add(new EnumSetting.Builder<DamageIgnore>().name("ignore-self-break-damage").description("When to ignore self damage when breaking crystal.").defaultValue(DamageIgnore.Never).build());
    private final Setting<Double> BmaxDamage = sgBreak.add(new DoubleSetting.Builder().name("max-break-damage").description("Maximum break damage crystals can deal to yourself.").defaultValue(6).min(0).max(36).sliderMax(36).visible(() -> BDamageIgnore.get() != DamageIgnore.Always).build());
    private final Setting<Boolean> BantiSuicide = sgBreak.add(new BoolSetting.Builder().name("anti-suicide-break").description("Will not break crystals if they will pop / kill you.").defaultValue(true).visible(() -> BDamageIgnore.get() != DamageIgnore.Always).build());
    private final Setting<Boolean> sneak = sgBreak.add(new BoolSetting.Builder().name("sneak").description("Should it do sneak while attacking crystals.").defaultValue(false).build());
    private final Setting<CancelCrystalMode> cancelCrystalMode = sgBreak.add(new EnumSetting.Builder<CancelCrystalMode>().name("cancel-mode").description("Mode to use for the crystals to be removed from the world.").defaultValue(CancelCrystalMode.NoDesync).build());
    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder().name("break-delay").description("The delay in ticks to wait to break a crystal after it's placed.").defaultValue(0).min(0).sliderMin(0).sliderMax(20).build());
    private final Setting<Boolean> smartDelay = sgBreak.add(new BoolSetting.Builder().name("smart-delay").description("Only breaks crystals when the target can receive damage.").defaultValue(false).build());
    private final Setting<Integer> switchDelay = sgBreak.add(new IntSetting.Builder().name("switch-delay").description("The delay in ticks to wait to break a crystal after switching hotbar slot.").defaultValue(0).min(0).sliderMax(10).build());
    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder().name("break-range").description("Range in which to break crystals.").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Double> breakWallsRange = sgBreak.add(new DoubleSetting.Builder().name("break-walls-range").description("Range in which to break crystals when behind blocks.").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Boolean> onlyBreakOwn = sgBreak.add(new BoolSetting.Builder().name("only-own").description("Only breaks own crystals.").defaultValue(false).build());
    private final Setting<Boolean> attemptCheck = sgBreak.add(new BoolSetting.Builder().name("break-attempt-check").description("To pair break attempt with damage calc or not.").defaultValue(false).build());
    private final Setting<Integer> breakAttempts = sgBreak.add(new IntSetting.Builder().name("break-attempts").description("How many times to hit a crystal before stopping to target it.").defaultValue(2).sliderMin(1).sliderMax(5).build());
    private final Setting<Boolean> ageCheck = sgBreak.add(new BoolSetting.Builder().name("age-check").description("To check crystal age or not.").defaultValue(true).build());
    private final Setting<Integer> ticksExisted = sgBreak.add(new IntSetting.Builder().name("ticks-existed").description("Amount of ticks a crystal needs to have lived for it to be attacked by CrystalAura.").defaultValue(0).min(-1).visible(ageCheck::get).build());
    private final Setting<Integer> attackFrequency = sgBreak.add(new IntSetting.Builder().name("attack-frequency").description("Maximum hits to do per second.").defaultValue(25).min(1).sliderMax(30).build());
    private final Setting<Boolean> fastBreak = sgBreak.add(new BoolSetting.Builder().name("fast-break").description("Ignores break delay and tries to break the crystal as soon as it's spawned in the world.").defaultValue(true).build());
    private final Setting<Boolean> antiWeakness = sgBreak.add(new BoolSetting.Builder().name("anti-weakness").description("Switches to tools with high enough damage to explode the crystal with weakness effect.").defaultValue(true).build());
    private final Setting<Double> pauseAtHealth = sgPause.add(new DoubleSetting.Builder().name("pause-health").description("Pauses when you go below a certain health.").defaultValue(5).min(0).build());
    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses Crystal Aura when eating.").defaultValue(true).build());
    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses Crystal Aura when drinking.").defaultValue(true).build());
    private final Setting<Boolean> minePause = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses Crystal Aura when mining.").defaultValue(false).build());
    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder().name("swing").description("Renders hand swinging client side.").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders a block overlay over the block the crystals are being placed on.").defaultValue(true).build());
    private final Setting<Boolean> renderBreak = sgRender.add(new BoolSetting.Builder().name("break").description("Renders a block overlay over the block the crystals are broken on.").defaultValue(false).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the block overlay.").defaultValue(new SettingColor(255, 255, 255, 45)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the block overlay.").defaultValue(new SettingColor(255, 255, 255, 255)).build());
    private final Setting<Boolean> renderDamageText = sgRender.add(new BoolSetting.Builder().name("damage").description("Renders crystal damage text in the block overlay.").defaultValue(true).build());
    private final Setting<Double> damageTextScale = sgRender.add(new DoubleSetting.Builder().name("damage-scale").description("How big the damage text should be.").defaultValue(1.25).min(1).sliderMax(4).visible(renderDamageText::get).build());
    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder().name("render-time").description("How long to render for.").defaultValue(10).min(0).sliderMax(20).build());
    private final Setting<Integer> renderBreakTime = sgRender.add(new IntSetting.Builder().name("break-time").description("How long to render breaking for.").defaultValue(13).min(0).sliderMax(20).visible(renderBreak::get).build());

    private int breakTimer, placeTimer, switchTimer, ticksPassed;
    private final List<PlayerEntity> targets = new ArrayList<>();

    private final Vec3d vec3d = new Vec3d(0, 0, 0);
    private final Vec3d playerEyePos = new Vec3d(0, 0, 0);
    private final Vec3 vec3 = new Vec3();
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private final Box box = new Box(0, 0, 0, 0, 0, 0);

    private final Vec3d vec3dRayTraceEnd = new Vec3d(0, 0, 0);
    private RaycastContext raycastContext;

    private final IntSet placedCrystals = new IntOpenHashSet();
    private boolean placing;
    private int placingTimer;
    private final BlockPos.Mutable placingCrystalBlockPos = new BlockPos.Mutable();

    private final IntSet removed = new IntOpenHashSet();
    private final Int2IntMap attemptedBreaks = new Int2IntOpenHashMap();
    private final Int2IntMap waitingToExplode = new Int2IntOpenHashMap();
    private int attacks;

    private double serverYaw;

    private PlayerEntity bestTarget;
    private double bestTargetDamage;
    private int bestTargetTimer;

    private boolean didRotateThisTick;
    private boolean isLastRotationPos;
    private final Vec3d lastRotationPos = new Vec3d(0, 0, 0);
    private double lastYaw, lastPitch;
    private int lastRotationTimer;

    private int renderTimer, breakRenderTimer;
    private final BlockPos.Mutable renderPos = new BlockPos.Mutable();
    private final BlockPos.Mutable breakRenderPos = new BlockPos.Mutable();
    private double renderDamage;

    public BananaBomber() {
        super(Categories.BANANAPLUS, "banana-bomber", "Automatically places and attacks crystals.");
    }

    @Override
    public void onActivate() {
        breakTimer = 0;
        placeTimer = 0;
        ticksPassed = 0;

        raycastContext = new RaycastContext(new Vec3d(0, 0, 0), new Vec3d(0, 0, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        placing = false;
        placingTimer = 0;

        attacks = 0;

        serverYaw = mc.player.getYaw();

        bestTargetDamage = 0;
        bestTargetTimer = 0;

        lastRotationTimer = getLastRotationStopDelay();

        renderTimer = 0;
        breakRenderTimer = 0;
    }

    @Override
    public void onDeactivate() {
        targets.clear();

        placedCrystals.clear();

        attemptedBreaks.clear();
        waitingToExplode.clear();

        removed.clear();

        bestTarget = null;
    }

    private int getLastRotationStopDelay() {
        return Math.max(10, placeDelay.get() / 2 + breakDelay.get() / 2 + 10);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPreTick(TickEvent.Pre event) {
        didRotateThisTick = false;
        lastRotationTimer++;

        if (placing) {
            if (placingTimer > 0) placingTimer--;
            else placing = false;
        }

        if (ticksPassed < 20) ticksPassed++;
        else {
            ticksPassed = 0;
            attacks = 0;
        }

        if (bestTargetTimer > 0) bestTargetTimer--;
        bestTargetDamage = 0;

        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;
        if (switchTimer > 0) switchTimer--;

        if (renderTimer > 0) renderTimer--;
        if (breakRenderTimer > 0) breakRenderTimer--;

        for (IntIterator it = waitingToExplode.keySet().iterator(); it.hasNext(); ) {
            int id = it.nextInt();
            int ticks = waitingToExplode.get(id);

            if (ticks >= waiting.get()) {
                it.remove();
                removed.remove(id);
            } else {
                waitingToExplode.put(id, ticks + 1);
            }
        }

        if (PlayerUtils.shouldPause(minePause.get(), eatPause.get(), drinkPause.get()) || PlayerUtils.getTotalHealth() <= pauseAtHealth.get()) {
            if (debug.get()) warning("Pausing");
            return;
        }

        ((IVec3d) playerEyePos).set(mc.player.getPos().x, mc.player.getPos().y + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getPos().z);

        findTargets();

        if (targets.size() > 0) {
            if (!didRotateThisTick) doBreak();
            if (!didRotateThisTick) doPlace();
        }

        if ((cancelCrystalMode.get() == CancelCrystalMode.Hit)) {
            removed.forEach((java.util.function.IntConsumer) id -> Objects.requireNonNull(mc.world.getEntityById(id)).kill());
            removed.clear();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST - 666)
    private void onPreTickLast(TickEvent.Pre event) {
        if (rotate.get() && lastRotationTimer < getLastRotationStopDelay() && !didRotateThisTick) {
            Rotations.rotate(isLastRotationPos ? Rotations.getYaw(lastRotationPos) : lastYaw, isLastRotationPos ? Rotations.getPitch(lastRotationPos) : lastPitch, -100, null);
        }
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;

        if (placing && event.entity.getBlockPos().equals(placingCrystalBlockPos)) {
            placing = false;
            placingTimer = 0;
            placedCrystals.add(event.entity.getId());
        }

        if (fastBreak.get() && !didRotateThisTick && attacks < attackFrequency.get()) {
            if (!isSurroundHolding() ||
                    (facePlace.get() && slowFacePlace.get() && (bestTarget.getY() < placingCrystalBlockPos.getY()))) {
                double damage = getBreakDamage(event.entity, true);
                if (damage > BminDamage.get()) doBreak(event.entity);
            }
        }
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (event.entity instanceof EndCrystalEntity) {
            placedCrystals.remove(event.entity.getId());
            removed.remove(event.entity.getId());
            waitingToExplode.remove(event.entity.getId());
        }
    }

    private void setRotation(boolean isPos, Vec3d pos, double yaw, double pitch) {
        didRotateThisTick = true;
        isLastRotationPos = isPos;

        if (isPos) ((IVec3d) lastRotationPos).set(pos.x, pos.y, pos.z);
        else {
            lastYaw = yaw;
            lastPitch = pitch;
        }

        lastRotationTimer = 0;
    }

    private void doBreak() {
        if (!doBreak.get() || breakTimer > 0 || switchTimer > 0 || attacks >= attackFrequency.get()) return;

        double bestDamage = 0;
        Entity crystal = null;

        for (Entity entity : mc.world.getEntities()) {
            double damage = getBreakDamage(entity, true);

            if (damage > bestDamage) {
                bestDamage = damage;
                crystal = entity;
            }
        }

        if (crystal != null) doBreak(crystal);
    }

    private double getBreakDamage(Entity entity, boolean checkCrystalAge) {
        if (!(entity instanceof EndCrystalEntity)) return 0;

        if (onlyBreakOwn.get() && !placedCrystals.contains(entity.getId())) return 0;

        if (removed.contains(entity.getId())) return 0;

        if (attemptCheck.get()) {
            if (attemptedBreaks.get(entity.getId()) > breakAttempts.get()) return 0;
        }

        if (ageCheck.get()) {
            if (checkCrystalAge && entity.age < ticksExisted.get()) return 0;
        }

        if (isOutOfRange(entity.getPos(), entity.getBlockPos(), false)) return 0;

        if (centerDamage.get()) {
            blockPos.set(entity.getBlockPos()).move(0, 1, 0);
        } else blockPos.set(entity.getBlockPos()).move(0, -1, 0);

        if (BDamageIgnore.get() == DamageIgnore.Never || (BDamageIgnore.get() == DamageIgnore.SurroundedorBurrowed && !(BEntityUtils.isSurrounded(mc.player) || BEntityUtils.isBurrowed(mc.player)))) {
            double selfDamage = BDamageUtils.crystalDamage(mc.player, entity.getPos(), predictMovement.get(), blockPos, ignoreTerrain.get());
            if (selfDamage > BmaxDamage.get() || (BantiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player)))
                return 0;
        }

        double damage = getDamageToTargets(entity.getPos(), blockPos, true, false);
        boolean facePlaced = (facePlace.get() && shouldFacePlace(entity.getBlockPos()) || forceFacePlace.get().isPressed());

        if (!facePlaced && damage < BminDamage.get()) return 0;

        return damage;
    }

    private void getDelay() {
        if (isSurroundHolding()) {
            if (surroundHoldMode.get() == SurroundHold.Auto) breakTimer = 10;
            else if (surroundHoldMode.get() == SurroundHold.Custom) breakTimer = surroundHoldDelay.get();
            if (debug.get()) warning("Surround Holding");
        } else if (facePlace.get() && slowFacePlace.get() && (bestTarget.getY() < placingCrystalBlockPos.getY())) {
            if (slowFPMode.get() == SlowFacePlace.Auto) breakTimer = 10;
            else if (slowFPMode.get() == SlowFacePlace.Custom) breakTimer = slowFPDelay.get();
        } else breakTimer = breakDelay.get();
    }

    private void doBreak(Entity crystal) {
        if (antiWeakness.get()) {
            StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
            StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);

            if (weakness != null && (strength == null || strength.getAmplifier() <= weakness.getAmplifier())) {
                if (!isValidWeaknessItem(mc.player.getMainHandStack())) {
                    if (!InvUtils.swap(InvUtils.findInHotbar(this::isValidWeaknessItem).slot(), false)) return;

                    switchTimer = 1;
                    return;
                }
            }
        }

        boolean attacked = true;

        if (!rotate.get()) {
            attackCrystal(crystal);
            getDelay();
        } else {
            double yaw = Rotations.getYaw(crystal);
            double pitch = Rotations.getPitch(crystal, Target.Feet);

            if (doYawSteps(yaw, pitch)) {
                setRotation(true, crystal.getPos(), 0, 0);
                Rotations.rotate(yaw, pitch, 50, () -> attackCrystal(crystal));
                getDelay();
            } else {
                attacked = false;
            }
        }

        if (attacked) {
            removed.add(crystal.getId());
            attemptedBreaks.put(crystal.getId(), attemptedBreaks.get(crystal.getId()) + 1);
            waitingToExplode.put(crystal.getId(), 0);

            breakRenderPos.set(crystal.getBlockPos().down());
            breakRenderTimer = renderBreakTime.get();
        }
    }

    private boolean isValidWeaknessItem(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ToolItem) || itemStack.getItem() instanceof HoeItem) return false;

        ToolMaterial material = ((ToolItem) itemStack.getItem()).getMaterial();
        return material == ToolMaterials.DIAMOND || material == ToolMaterials.NETHERITE;
    }

    private void attackCrystal(Entity entity) {
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, sneak.get()));

        Hand hand = Hand.MAIN_HAND;
        if (renderSwing.get()) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        attacks++;

        if (debug.get()) warning("Breaking");
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchTimer = switchDelay.get();
        }
    }

    private void doPlace() {
        if (!doPlace.get() || placeTimer > 0) return;

        if (!InvUtils.findInHotbar(Items.END_CRYSTAL).found()) return;

        if (autoSwitch.get() == AutoSwitchMode.None && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL)
            return;

        for (Entity entity : mc.world.getEntities()) {
            if (getBreakDamage(entity, false) > 0) return;
        }

        AtomicDouble bestDamage = new AtomicDouble(0);
        AtomicReference<BlockPos.Mutable> bestBlockPos = new AtomicReference<>(new BlockPos.Mutable());
        AtomicBoolean isSupport = new AtomicBoolean(support.get() != SupportMode.Disabled);

        BlockIterator.register((int) Math.ceil(placeRange.get()), (int) Math.ceil(placeRange.get()), (bp, blockState) -> {
            boolean hasBlock = blockState.isOf(Blocks.BEDROCK) || blockState.isOf(Blocks.OBSIDIAN);
            if (!hasBlock && (!isSupport.get() || !blockState.getMaterial().isReplaceable())) return;

            blockPos.set(bp.getX(), bp.getY() + 1, bp.getZ());
            if (!mc.world.getBlockState(blockPos).isAir()) return;

            if (placement112.get()) {
                blockPos.move(0, 1, 0);
                if (!mc.world.getBlockState(blockPos).isAir()) return;
            }

            ((IVec3d) vec3d).set(bp.getX() + 0.5, bp.getY() + 1, bp.getZ() + 0.5);
            blockPos.set(bp).move(0, 1, 0);
            if (isOutOfRange(vec3d, blockPos, true)) return;

            if (PDamageIgnore.get() == DamageIgnore.Never || (PDamageIgnore.get() == DamageIgnore.SurroundedorBurrowed && !(BEntityUtils.isSurrounded(mc.player) || BEntityUtils.isBurrowed(mc.player)))) {
                double selfDamage = BDamageUtils.crystalDamage(mc.player, vec3d, predictMovement.get(), bp, ignoreTerrain.get());
                if (selfDamage > PmaxDamage.get() || (PantiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player)))
                    return;
            }

            double damage = getDamageToTargets(vec3d, bp, false, !hasBlock && support.get() == SupportMode.Fast);

            boolean facePlaced = (facePlace.get() && shouldFacePlace(blockPos)) || (forceFacePlace.get().isPressed());

            if (facePlaced && debug.get()) warning("Face placing");

            boolean surroundBreaking = (isSurroundBreaking() && shouldSurroundBreak(blockPos));

            if (surroundBreaking && debug.get()) warning("Surround Breaking");

            if ((!facePlaced && !surroundBreaking) && damage < PminDamage.get()) return;

            double x = bp.getX();
            double y = bp.getY() + 1;
            double z = bp.getZ();
            ((IBox) box).set(x, y, z, x + 1, y + (placement112.get() ? 1 : 2), z + 1);

            if (intersectsWithEntities(box)) return;

            if (damage > bestDamage.get() || (isSupport.get() && hasBlock)) {
                bestDamage.set(damage);
                bestBlockPos.get().set(bp);
            }

            if (hasBlock) isSupport.set(false);
        });

        BlockIterator.after(() -> {
            if (bestDamage.get() == 0) return;

            BlockHitResult result = getPlaceInfo(bestBlockPos.get());

            ((IVec3d) vec3d).set(
                    result.getBlockPos().getX() + 0.5 + result.getSide().getVector().getX() * 0.5,
                    result.getBlockPos().getY() + 0.5 + result.getSide().getVector().getY() * 0.5,
                    result.getBlockPos().getZ() + 0.5 + result.getSide().getVector().getZ() * 0.5
            );

            if (rotate.get()) {
                double yaw = Rotations.getYaw(vec3d);
                double pitch = Rotations.getPitch(vec3d);

                if (yawStepMode.get() == YawStepMode.Break || doYawSteps(yaw, pitch)) {
                    setRotation(true, vec3d, 0, 0);
                    Rotations.rotate(yaw, pitch, 50, () -> placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null));

                    placeTimer += placeDelay.get();
                }
            } else {
                placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null);
                placeTimer += placeDelay.get();
            }
        });
    }

    private BlockHitResult getPlaceInfo(BlockPos blockPos) {
        ((IVec3d) vec3d).set(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        for (Direction side : Direction.values()) {
            ((IVec3d) vec3dRayTraceEnd).set(
                    blockPos.getX() + 0.5 + side.getVector().getX() * 0.5,
                    blockPos.getY() + 0.5 + side.getVector().getY() * 0.5,
                    blockPos.getZ() + 0.5 + side.getVector().getZ() * 0.5
            );

            ((IRaycastContext) raycastContext).set(vec3d, vec3dRayTraceEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);

            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos)) {
                return result;
            }
        }

        Direction side = blockPos.getY() > vec3d.y ? Direction.DOWN : Direction.UP;
        return new BlockHitResult(vec3d, side, blockPos, false);
    }

    private void placeCrystal(BlockHitResult result, double damage, BlockPos supportBlock) {
        Item targetItem = supportBlock == null ? Items.END_CRYSTAL : Items.OBSIDIAN;

        FindItemResult item = InvUtils.findInHotbar(targetItem);
        if (!item.found()) return;

        int prevSlot = mc.player.getInventory().selectedSlot;

        if (!(mc.player.getOffHandStack().getItem() instanceof EndCrystalItem) && (autoSwitch.get() == AutoSwitchMode.Normal && noGapSwitch.get()) && (mc.player.getMainHandStack().getItem() instanceof EnchantedGoldenAppleItem))
            return;
        if (autoSwitch.get() != AutoSwitchMode.None && !item.isOffhand()) InvUtils.swap(item.slot(), false);

        Hand hand = item.getHand();
        if (hand == null) return;

        if (supportBlock == null) {
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, result));

            if (renderSwing.get()) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

            if (debug.get()) warning("Placing");

            placing = true;
            placingTimer = 4;
            if (placeIn.get() && (mc.player.getBlockPos().getY() + yDifference.get() <= bestTarget.getBlockPos().getY()))
                placingCrystalBlockPos.set(result.getBlockPos());
            else placingCrystalBlockPos.set(result.getBlockPos()).move(0, 1, 0);

            renderTimer = renderTime.get();
            renderPos.set(result.getBlockPos());
            renderDamage = damage;
        } else {
            BlockUtils.place(supportBlock, item, false, 0, renderSwing.get(), true, false);
            placeTimer += supportDelay.get();

            if (supportDelay.get() == 0) placeCrystal(result, damage, null);
        }

        if (autoSwitch.get() == AutoSwitchMode.Silent) InvUtils.swap(prevSlot, false);
    }

    @EventHandler
    private void onPacketSent(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            serverYaw = ((PlayerMoveC2SPacket) event.packet).getYaw((float) serverYaw);
        }
    }

    public boolean doYawSteps(double targetYaw, double targetPitch) {
        targetYaw = MathHelper.wrapDegrees(targetYaw) + 180;
        double serverYaw = MathHelper.wrapDegrees(this.serverYaw) + 180;

        if (distanceBetweenAngles(serverYaw, targetYaw) <= yawSteps.get()) return true;

        double delta = Math.abs(targetYaw - serverYaw);
        double yaw = this.serverYaw;

        if (serverYaw < targetYaw) {
            if (delta < 180) yaw += yawSteps.get();
            else yaw -= yawSteps.get();
        } else {
            if (delta < 180) yaw -= yawSteps.get();
            else yaw += yawSteps.get();
        }

        setRotation(false, null, yaw, targetPitch);
        Rotations.rotate(yaw, targetPitch, -100, null);
        return false;
    }

    private static double distanceBetweenAngles(double alpha, double beta) {
        double phi = Math.abs(beta - alpha) % 360;
        return phi > 180 ? 360 - phi : phi;
    }

    private boolean shouldFacePlace(BlockPos crystal) {
        for (PlayerEntity target : targets) {
            BlockPos pos = target.getBlockPos();
            if (CevPause.get() && Modules.get().isActive(CevBreaker.class)) return false;
            if (KAPause.get() && (Modules.get().isActive(KillAura.class) || Modules.get().isActive(PostTickKA.class)))
                return false;
            if (!faceSurrounded.get() && BEntityUtils.isFaceSurrounded(target)) return false;
            if (surrHoldPause.get() && surroundHold.get() && BEntityUtils.isSurroundBroken(target)) return false;

            if (crystal.getY() == pos.getY() + 1 && Math.abs(pos.getX() - crystal.getX()) <= 1 && Math.abs(pos.getZ() - crystal.getZ()) <= 1) {
                if (greenHolers.get() && BEntityUtils.isGreenHole(target)) return true;
                if (EntityUtils.getTotalHealth(target) <= facePlaceHealth.get()) return true;

                for (ItemStack itemStack : target.getArmorItems()) {
                    if (itemStack == null || itemStack.isEmpty()) {
                        if (facePlaceArmor.get()) return true;
                    } else {
                        if ((double) (itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage() * 100 <= facePlaceDurability.get())
                            return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean shouldSurroundBreak(BlockPos crystal) {
        for (PlayerEntity target : targets) {
            if (target != bestTarget) continue;
            BlockPos pos = bestTarget.getBlockPos();
            if (!isSurroundBreaking()) return false;

            if (!BEntityUtils.isBedrock(pos.north(1)) && crystal.equals(pos.north(2))) return true;
            if (!BEntityUtils.isBedrock(pos.west(1)) && crystal.equals(pos.west(2))) return true;
            if (!BEntityUtils.isBedrock(pos.south(1)) && crystal.equals(pos.south(2))) return true;
            if (!BEntityUtils.isBedrock(pos.east(1)) && crystal.equals(pos.east(2))) return true;
        }
        return false;
    }

    private boolean isSurroundBreaking() {
        if (surroundBreak.get() && bestTarget != null) {
            if (facePlacePause.get() && shouldFacePlace(blockPos)) return false;
            if (!BEntityUtils.isSurrounded(bestTarget)) return false;
            if (BEntityUtils.isGreenHole(bestTarget)) return false;
            if (ignoreBurrowed.get() && BEntityUtils.isBurrowed(bestTarget)) return false;
            if (surroundBWhen.get() == SurroundBorHWhen.BothTrapped && (BEntityUtils.isTopTrapped(bestTarget) && BEntityUtils.isFaceSurrounded(bestTarget)))
                return true;
            else if (surroundBWhen.get() == SurroundBorHWhen.AnyTrapped && (BEntityUtils.isTopTrapped(bestTarget) || BEntityUtils.isFaceSurrounded(bestTarget)))
                return true;
            else if (surroundBWhen.get() == SurroundBorHWhen.TopTrapped && BEntityUtils.isTopTrapped(bestTarget))
                return true;
            else if (surroundBWhen.get() == SurroundBorHWhen.FaceTrapped && BEntityUtils.isFaceSurrounded(bestTarget))
                return true;
            else if (surroundBWhen.get() == SurroundBorHWhen.Always) return true;
            else return false;
        }
        return false;
    }

    private boolean isSurroundHolding() {
        if (surroundHold.get() && bestTarget != null && BEntityUtils.isSurroundBroken(bestTarget)) {
            if (surroundHWhen.get() == SurroundBorHWhen.BothTrapped && (BEntityUtils.isTopTrapped(bestTarget) && BEntityUtils.isFaceSurrounded(bestTarget)))
                return true;
            else if (surroundHWhen.get() == SurroundBorHWhen.AnyTrapped && (BEntityUtils.isTopTrapped(bestTarget) || BEntityUtils.isFaceSurrounded(bestTarget)))
                return true;
            else if (surroundHWhen.get() == SurroundBorHWhen.TopTrapped && BEntityUtils.isTopTrapped(bestTarget))
                return true;
            else if (surroundHWhen.get() == SurroundBorHWhen.FaceTrapped && BEntityUtils.isFaceSurrounded(bestTarget))
                return true;
            else if (surroundHWhen.get() == SurroundBorHWhen.Always) return true;
            else return false;
        }
        return false;
    }

    private boolean isOutOfRange(Vec3d vec3d, BlockPos blockPos, boolean place) {
        ((IRaycastContext) raycastContext).set(playerEyePos, vec3d, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        BlockHitResult result = mc.world.raycast(raycastContext);
        boolean behindWall = result == null || !result.getBlockPos().equals(blockPos);
        double distance = mc.player.getPos().distanceTo(vec3d);

        return distance > (behindWall ? (place ? placeWallsRange : breakWallsRange).get() : (place ? placeRange : breakRange).get());
    }

    private PlayerEntity getNearestTarget() {
        PlayerEntity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;

        for (PlayerEntity target : targets) {
            double distance = target.squaredDistanceTo(mc.player);

            if (distance < nearestDistance) {
                nearestTarget = target;
                nearestDistance = distance;
            }
        }

        return nearestTarget;
    }

    private double getDamageToTargets(Vec3d vec3d, BlockPos obsidianPos, boolean breaking, boolean fast) {
        double damage = 0;

        if (fast) {
            PlayerEntity target = getNearestTarget();
            if (!(smartDelay.get() && breaking && target.hurtTime > 0))
                damage = BDamageUtils.crystalDamage(target, vec3d, predictMovement.get(), obsidianPos, ignoreTerrain.get());
        } else {
            for (PlayerEntity target : targets) {
                if (smartDelay.get() && breaking && target.hurtTime > 0) continue;

                double dmg = BDamageUtils.crystalDamage(target, vec3d, predictMovement.get(), obsidianPos, ignoreTerrain.get());

                if (dmg > bestTargetDamage) {
                    bestTarget = target;
                    bestTargetDamage = dmg;
                    bestTargetTimer = 10;
                }

                damage += dmg;
            }
        }

        return damage;
    }

    @Override
    public String getInfoString() {
        return bestTarget != null && bestTargetTimer > 0 ? bestTarget.getGameProfile().getName() : null;
    }

    private void findTargets() {
        targets.clear();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.getAbilities().creativeMode || player == mc.player) continue;

            if (!player.isDead() && player.isAlive() && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) {
                targets.add(player);
            }
        }

        for (PlayerEntity player : FakePlayerManager.getPlayers()) {
            if (!player.isDead() && player.isAlive() && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) {
                targets.add(player);
            }
        }
    }

    private boolean intersectsWithEntities(Box box) {
        return EntityUtils.intersectsWithEntity(box, entity -> !entity.isSpectator() && !removed.contains(entity.getId()));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (renderTimer > 0 && render.get()) {
            event.renderer.box(renderPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }

        if (breakRenderTimer > 0 && renderBreak.get() && !mc.world.getBlockState(breakRenderPos).isAir()) {
            int preSideA = sideColor.get().a;
            sideColor.get().a -= 20;
            sideColor.get().validate();

            int preLineA = lineColor.get().a;
            lineColor.get().a -= 20;
            lineColor.get().validate();

            event.renderer.box(breakRenderPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);

            sideColor.get().a = preSideA;
            lineColor.get().a = preLineA;
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!render.get() || renderTimer <= 0 || !renderDamageText.get()) return;

        vec3.set(renderPos.getX() + 0.5, renderPos.getY() + 0.5, renderPos.getZ() + 0.5);

        if (NametagUtils.to2D(vec3, damageTextScale.get())) {
            NametagUtils.begin(vec3);
            TextRenderer.get().begin(1, false, true);

            String text = String.format("%.1f", renderDamage);
            double w = TextRenderer.get().getWidth(text) / 2;
            TextRenderer.get().render(text, -w, 0, lineColor.get(), true);

            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    public PlayerEntity getPlayerTarget() {
        if (bestTarget != null) {
            return bestTarget;
        } else {
            return null;
        }
    }

    public enum YawStepMode {
        Break, All,
    }

    public enum AutoSwitchMode {
        Normal, Silent, None
    }

    public enum SupportMode {
        Disabled, Accurate, Fast
    }

    public enum CancelCrystalMode {
        Hit, NoDesync
    }

    public enum DamageIgnore {
        Always, SurroundedorBurrowed, Never
    }

    public enum SurroundHold {
        Auto, Custom
    }

    public enum SurroundBorHWhen {
        Always, TopTrapped, FaceTrapped, BothTrapped, AnyTrapped
    }

    public enum SlowFacePlace {
        Auto, Custom
    }
}
