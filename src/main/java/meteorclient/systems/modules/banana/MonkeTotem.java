package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.misc.BDamageUtils;
import meteorclient.events.packets.PacketEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.settings.BoolSetting;
import meteorclient.settings.IntSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.systems.modules.Module;
import meteorclient.utils.player.FindItemResult;
import meteorclient.utils.player.InvUtils;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class MonkeTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").description("The ticks between slot movements.").defaultValue(0).min(0).sliderMax(10).build());
    private final Setting<Integer> redHealth = sgGeneral.add(new IntSetting.Builder().name("Max-red-health").description("The max red health to contribute.").defaultValue(20).min(0).max(20).sliderMax(20).build());
    private final Setting<Integer> yelHealth = sgGeneral.add(new IntSetting.Builder().name("Max-Yellow-Health").description("The max absorption health to contribute.").defaultValue(8).min(0).max(16).sliderMax(16).build());
    private final Setting<Integer> minHealth = sgGeneral.add(new IntSetting.Builder().name("Min-Total-Health").description("The min (red + yellow) health to hold a totem at.").defaultValue(14).min(0).max(36).sliderMax(36).build());
    private final Setting<Boolean> explosion = sgGeneral.add(new BoolSetting.Builder().name("explosion").description("Will hold a totem when explosion damage could kill you.").defaultValue(false).build());

    public boolean locked;
    private int totems, ticks;

    public MonkeTotem() {
        super(Categories.BANANAPLUS, "Monke-totem", "An auto totem with custom absorption amount. (If issues occur with normal offhand, use monkhand)");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        FindItemResult result = InvUtils.find(Items.TOTEM_OF_UNDYING);
        totems = result.count();

        if (totems <= 0) locked = false;
        else if (ticks >= delay.get()) {
            boolean low = (Math.min(mc.player.getHealth(), redHealth.get()) + Math.min(mc.player.getAbsorptionAmount(), yelHealth.get())) - BDamageUtils.possibleHealthReductions(explosion.get(), false) <= minHealth.get();

            locked = low;

            if (locked && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InvUtils.move().from(result.slot()).toOffhand();
            }

            ticks = 0;
            return;
        }

        ticks++;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != 35) return;

        Entity entity = p.getEntity(mc.world);
        if (entity == null || !(entity.equals(mc.player))) return;

        ticks = 0;
    }

    public boolean isLocked() {
        return isActive() && locked;
    }

    @Override
    public String getInfoString() {
        return String.valueOf(totems);
    }
}
