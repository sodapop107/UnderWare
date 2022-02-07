package meteorclient.systems.modules.banana;

import meteorclient.events.meteor.MouseButtonEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.settings.BoolSetting;
import meteorclient.settings.EnumSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.modules.combat.AutoTotem;
import meteorclient.systems.modules.combat.AutoWeb;
import meteorclient.systems.modules.combat.CrystalAura;
import meteorclient.systems.modules.player.EXPThrower;
import meteorclient.utils.misc.input.KeyAction;
import meteorclient.utils.player.FindItemResult;
import meteorclient.utils.player.InvUtils;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.item.*;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class Monkhand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Item> item = sgGeneral.add(new EnumSetting.Builder<Item>().name("item").description("Which item to hold in your offhand.").defaultValue(Item.Crystal).build());
    private final Setting<Boolean> hotbar = sgGeneral.add(new BoolSetting.Builder().name("hotbar").description("Whether to use items from your hotbar.").defaultValue(false).build());
    private final Setting<Boolean> rightClick = sgGeneral.add(new BoolSetting.Builder().name("right-click").description("Only holds the item in your offhand when you are holding right click.").defaultValue(false).build());
    private final Setting<Boolean> swordGap = sgGeneral.add(new BoolSetting.Builder().name("sword-gap").description("Holds an Enchanted Golden Apple when you are holding a sword.").defaultValue(true).build());
    private final Setting<Boolean> crystalCa = sgGeneral.add(new BoolSetting.Builder().name("crystal-on-ca").description("Holds a crystal when you have Crystal Aura enabled.").defaultValue(true).build());
    private final Setting<Boolean> crystalCev = sgGeneral.add(new BoolSetting.Builder().name("crystal-on-cev-breaker").description("Holds a crystal when you have Cev Breaker enabled.").defaultValue(true).build());
    private final Setting<Boolean> crystalMine = sgGeneral.add(new BoolSetting.Builder().name("crystal-on-mine").description("Holds a crystal when you are mining.").defaultValue(false).build());
    private final Setting<Boolean> web = sgGeneral.add(new BoolSetting.Builder().name("Web-on-AutoWeb").description("Holds webs when AutoWeb is on.").defaultValue(false).build());
    private final Setting<Boolean> AutoXP = sgGeneral.add(new BoolSetting.Builder().name("Xp-on-xp-thrower").description("Holds Bottles of Enchanting when Xp thrower is on.").defaultValue(false).build());
    private final Setting<Boolean> RocketBow = sgGeneral.add(new BoolSetting.Builder().name("Crossbow-rocket").description("Holds a rocket if you are holding a crossbow.").defaultValue(false).build());

    private boolean isClicking;
    private boolean sentMessage;
    private Item currentItem;

    public Monkhand() {
        super(Categories.BANANAPLUS, "monkhand", "[B+ Modified] Allows you to hold specified items in your offhand.");
    }

    @Override
    public void onActivate() {
        sentMessage = false;
        isClicking = false;
        currentItem = item.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        AutoTotem autoTotem = Modules.get().get(AutoTotem.class);
        MonkeTotem monkeTotem = Modules.get().get(MonkeTotem.class);

        if ((mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof AxeItem) && swordGap.get())
            currentItem = Item.EGap;

        else if ((Modules.get().isActive(CrystalAura.class) || Modules.get().isActive(BananaBomber.class) && crystalCa.get()) || mc.interactionManager.isBreakingBlock() && crystalMine.get())
            currentItem = Item.Crystal;
        else if (Modules.get().isActive(CevBreaker.class) && crystalCev.get()) currentItem = Item.Crystal;
        else if (Modules.get().isActive(EXPThrower.class) && AutoXP.get()) currentItem = Item.Exp;
        else if ((mc.player.getMainHandStack().getItem() instanceof CrossbowItem) && RocketBow.get())
            currentItem = Item.Firework;
        else if (Modules.get().isActive(AutoWeb.class) && web.get()) currentItem = Item.Web;
        else currentItem = item.get();

        if (mc.player.getOffHandStack().getItem() != currentItem.item) {
            FindItemResult item = InvUtils.find(itemStack -> itemStack.getItem() == currentItem.item, hotbar.get() ? 0 : 9, 35);

            if (!item.found()) {
                if (!sentMessage) {
                    warning("Chosen item not found.");
                    sentMessage = true;
                }
            } else if ((isClicking || !rightClick.get()) && (!autoTotem.isLocked() && !monkeTotem.isLocked()) && !item.isOffhand()) {
                InvUtils.move().from(item.slot()).toOffhand();
                sentMessage = false;
            }
        } else if (!isClicking && rightClick.get()) {
            if (autoTotem.isActive() || monkeTotem.isActive()) {
                FindItemResult totem = InvUtils.find(itemStack -> itemStack.getItem() == Items.TOTEM_OF_UNDYING, hotbar.get() ? 0 : 9, 35);

                if (totem.found() && !totem.isOffhand()) {
                    InvUtils.move().from(totem.slot()).toOffhand();
                }
            } else {
                FindItemResult empty = InvUtils.find(ItemStack::isEmpty, hotbar.get() ? 0 : 9, 35);
                if (empty.found()) InvUtils.move().fromOffhand().to(empty.slot());
            }
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        isClicking = mc.currentScreen == null && (!Modules.get().get(AutoTotem.class).isLocked() || !Modules.get().get(MonkeTotem.class).isLocked()) && !usableItem() && !mc.player.isUsingItem() && event.action == KeyAction.Press && event.button == GLFW_MOUSE_BUTTON_RIGHT;
    }

    private boolean usableItem() {
        return mc.player.getMainHandStack().getItem() == Items.BOW || mc.player.getMainHandStack().getItem() == Items.TRIDENT || mc.player.getMainHandStack().getItem() == Items.CROSSBOW || mc.player.getMainHandStack().getItem().isFood();
    }

    @Override
    public String getInfoString() {
        return item.get().name();
    }

    public enum Item {
        Totem(Items.TOTEM_OF_UNDYING),
        EGap(Items.ENCHANTED_GOLDEN_APPLE),
        Gap(Items.GOLDEN_APPLE),
        Crystal(Items.END_CRYSTAL),
        Exp(Items.EXPERIENCE_BOTTLE),
        Obsidian(Items.OBSIDIAN),
        Firework(Items.FIREWORK_ROCKET),
        Web(Items.COBWEB),
        Shield(Items.SHIELD);

        net.minecraft.item.Item item;

        Item(net.minecraft.item.Item item) {
            this.item = item;
        }
    }
}
