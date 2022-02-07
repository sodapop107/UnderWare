package meteorclient.systems.modules.banana;

import meteorclient.events.meteor.MouseButtonEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.settings.*;
import meteorclient.systems.friends.Friend;
import meteorclient.systems.friends.Friends;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;
import meteorclient.utils.misc.Keybind;
import meteorclient.utils.misc.input.KeyAction;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import net.minecraft.entity.player.PlayerEntity;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

public class BindClickFriend extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<MMode> mMode = sgGeneral.add(new EnumSetting.Builder<MMode>().name("Mode").description("The mode at which to follow the player.").defaultValue(MMode.BindClickFollow).build());
    private final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder().name("follow-keybind").description("What key to press to start following someone").defaultValue(Keybind.fromKey(-1)).visible(() -> mMode.get() == MMode.BindClickFollow).build());
    private final Setting<Boolean> message = sgGeneral.add(new BoolSetting.Builder().name("message").description("Sends a message to the player when you add them as a friend.").defaultValue(false).build());

    public BindClickFriend() {
        super(Categories.BANANAPLUS, "bind-click-friend", "Adds or removes a player as a friend when the bound key is pressed.");
    }

    boolean pressed = false;

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (mMode.get() != MMode.MiddleClickToFollow) return;

        if (event.action == KeyAction.Press && event.button == GLFW_MOUSE_BUTTON_MIDDLE && mc.currentScreen == null && mc.targetedEntity != null && mc.targetedEntity instanceof PlayerEntity) {
            if (!Friends.get().isFriend((PlayerEntity) mc.targetedEntity)) {
                Friends.get().add(new Friend((PlayerEntity) mc.targetedEntity));
                if (message.get())
                    mc.player.sendChatMessage("/msg " + mc.targetedEntity.getEntityName() + " I just friended you on UnderWare.");
            } else {
                Friends.get().remove(Friends.get().get((PlayerEntity) mc.targetedEntity));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onTick(TickEvent.Post event) {

        if (keybind != null && mMode.get() != MMode.MiddleClickToFollow) {

            if (!keybind.get().isPressed()) {
                pressed = false;
            }

            if (keybind.get().isPressed() && !pressed && mc.currentScreen == null && mc.targetedEntity != null && mc.targetedEntity instanceof PlayerEntity) {

                if (!Friends.get().isFriend((PlayerEntity) mc.targetedEntity)) {
                    Friends.get().add(new Friend((PlayerEntity) mc.targetedEntity));
                    if (message.get())
                        mc.player.sendChatMessage("/msg " + mc.targetedEntity.getEntityName() + " I just friended you on UnderWare.");
                } else {
                    Friends.get().remove(Friends.get().get((PlayerEntity) mc.targetedEntity));
                }

                pressed = true;
            }
        }
    }

    public enum MMode {
        MiddleClickToFollow, BindClickFollow
    }
}

