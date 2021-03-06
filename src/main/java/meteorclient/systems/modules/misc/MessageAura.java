package meteorclient.systems.modules.misc;

//Updated by squidoodly 24/07/2020

import meteorclient.events.entity.EntityAddedEvent;
import meteorclient.settings.BoolSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.settings.StringSetting;
import meteorclient.systems.friends.Friends;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

public class MessageAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
            .name("message")
            .description("The specified message sent to the player.")
            .defaultValue("Meteor on Crack!")
            .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Will not send any messages to people friended.")
            .defaultValue(false)
            .build()
    );

    public MessageAura() {
        super(Categories.Misc, "message-aura", "Sends a specified message to any player that enters render distance.");
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof PlayerEntity) || event.entity.getUuid().equals(mc.player.getUuid())) return;

        if (!ignoreFriends.get() || (ignoreFriends.get() && !Friends.get().isFriend((PlayerEntity)event.entity))) {
            mc.player.sendChatMessage("/msg " + event.entity.getEntityName() + " " + message.get());
        }
    }
}
