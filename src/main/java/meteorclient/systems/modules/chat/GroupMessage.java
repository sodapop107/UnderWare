package meteorclient.systems.modules.chat;

import meteorclient.systems.modules.Categories;
import meteorclient.events.game.SendMessageEvent;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.settings.StringListSetting;
import meteorclient.settings.StringSetting;
import meteorclient.systems.modules.Module;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.client.network.PlayerListEntry;

import java.util.List;

public class GroupMessage extends Module {
    SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> players = sgGeneral.add(new StringListSetting.Builder()
        .name("players")
        .description("Players to message.")
        .build()
    );

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
        .name("command")
        .description("How the message command is set up on the server.")
        .defaultValue("/msg {player} {message}")
        .build()
    );

    public GroupMessage() {
        super(Categories.Chat, "group-chat", "Talk with people privately with /msg.");
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        for(String playerString: players.get()) {
            for(PlayerListEntry onlinePlayer: mc.getNetworkHandler().getPlayerList()) {
                if(onlinePlayer.getProfile().getName().equalsIgnoreCase(playerString)) {
                    mc.player.sendChatMessage(command.get().replace("{player}", onlinePlayer.getProfile().getName()).replace("{message}", event.message));
                    break;
                }
            }
        }

        event.cancel();
    }


}
