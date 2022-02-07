package meteorclient.systems.modules.banana;

import meteorclient.events.packets.PacketEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.settings.*;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.modules.movement.NoFall;
import meteorclient.systems.modules.player.AntiHunger;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Sniper extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> base = sgGeneral.add(new DoubleSetting.Builder().name("hide-base").description("Base for the exponent number for hiding rubberband.").defaultValue(10).min(1.001).max(2147483647).sliderMin(1.001).sliderMax(2147483647).build());
    private final Setting<Double> exponent = sgGeneral.add(new DoubleSetting.Builder().name("hide-exponent").description("Exponent for the base number for hiding rubberband.").defaultValue(5).min(0.001).max(2147483647).sliderMin(0.001).sliderMax(2147483647).build());
    private final Setting<Boolean> antiHungerPause = sgGeneral.add(new BoolSetting.Builder().name("anti-hunger-pause").description("Pauses anti hunger when you are spoofing").defaultValue(true).build());
    private final Setting<Boolean> noFallPause = sgGeneral.add(new BoolSetting.Builder().name("no-fall-pause").description("Pauses no fall when you are spoofing").defaultValue(true).build());
    private final Setting<Boolean> bows = sgGeneral.add(new BoolSetting.Builder().name("bows").defaultValue(true).build());
    private final Setting<Boolean> tridents = sgGeneral.add(new BoolSetting.Builder().name("tridents").defaultValue(true).build());
    private final Setting<Integer> timeout = sgGeneral.add(new IntSetting.Builder().name("timeout").defaultValue(5000).min(100).max(20000).sliderMin(100).sliderMax(20000).build());
    private final Setting<Integer> spoofs = sgGeneral.add(new IntSetting.Builder().name("spoofs").defaultValue(10).min(1).max(300).sliderMin(1).sliderMax(300).build());
    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder().name("sprint").defaultValue(false).build());
    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder().name("bypass").defaultValue(false).build());
    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").defaultValue(false).build());

    public Sniper() {
        super(Categories.BANANAPLUS, "sniper", "They used this on harambe... very sad.");
    }

    private long lastShootTime;
    private boolean spoofed;

    private boolean isBow() {
        assert mc.player != null;
        return mc.player.getMainHandStack().getItem() == Items.BOW || mc.player.getOffHandStack().getItem() == Items.BOW;
    }

    private boolean isTrident() {
        assert mc.player != null;
        return mc.player.getMainHandStack().getItem() == Items.TRIDENT || mc.player.getOffHandStack().getItem() == Items.TRIDENT;
    }

    @Override
    public void onActivate() {
        lastShootTime = System.currentTimeMillis();
    }

    AntiHunger AH = Modules.get().get(AntiHunger.class);
    NoFall NF = Modules.get().get(NoFall.class);

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if ((isBow() && bows.get()) || (isTrident() && tridents.get())) {
            assert mc.player != null;
            if (mc.player.getItemUseTime() > 0 && mc.options.keyUse.isPressed()) {
                if (antiHungerPause.get() && AH.isActive()) AH.toggle();
                if (noFallPause.get() && NF.isActive()) NF.toggle();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        assert mc.player != null;
        if (spoofed && !mc.options.keyUse.isPressed()) {
            if (antiHungerPause.get() && !AH.isActive()) AH.toggle();
            if (noFallPause.get() && !NF.isActive()) NF.toggle();
        }
    }


    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet == null) return;

        if (event.packet instanceof PlayerActionC2SPacket) {
            PlayerActionC2SPacket packet = (PlayerActionC2SPacket) event.packet;

            if (packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                if ((isBow() && bows.get()) || (isTrident() && tridents.get())) {
                    if (debug.get()) warning("Attempting to spoof");
                    spoofed = false;
                    doSpoofs();
                }
            }
        }
    }

    private float value = (float) Math.pow(base.get(), -exponent.get());

    private void doSpoofs() {
        if (System.currentTimeMillis() - lastShootTime >= timeout.get()) {
            lastShootTime = System.currentTimeMillis();

            if (sprint.get())
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

            for (int index = 0; index < spoofs.get(); ++index) {
                if (bypass.get()) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + value, mc.player.getZ(), false));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - value, mc.player.getZ(), true));
                } else {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - value, mc.player.getZ(), true));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + value, mc.player.getZ(), false));
                }
            }

            spoofed = true;
            if (debug.get()) warning("Spoof completed");
        }
    }
}
