package meteorclient.systems.modules.banana;

import meteorclient.events.game.GameJoinedEvent;
import meteorclient.events.packets.PacketEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class MonkeStats extends Module {

    public MonkeStats() {
        super(Categories.BANANAPLUS, "monke-Stats", "Keeps track of your stats such as: kills, deaths, killstreak and so on.");
    }

    public String[] killWords = {"died", "blew", "by", "slain", "fucked", "killed", "separated", "punched", "shoved", "crystal", "nuked"};
    String enemyName;
    boolean killed = false;

    boolean dead;
    public static Integer kills = 0;
    public static Integer deaths = 0;
    public static Integer highScore = 0;
    public static Integer killStreak = 0;
    public static Double kD = (double) 0;

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if ((event.packet instanceof GameMessageS2CPacket)) {
            String msg = ((GameMessageS2CPacket) event.packet).getMessage().getString();

            for (String word : killWords) {
                if (msg.contains(word) && msg.contains(mc.player.getDisplayName().getString()) && ((GameMessageS2CPacket) event.packet).getSender().toString().contains("000000000") && !mc.player.isDead()) {
                    enemyName = msg.substring(0, msg.indexOf(" "));

                    if (enemyName.contains(mc.player.getDisplayName().getString())) {
                        dead = true;
                        return;
                    }
                    killed = true;
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Post event) {
        if (deaths == 0) {
            kD = (double) kills;
        } else {
            kD = (double) (kills / deaths);
            Double roundKD = Math.round(kD * 100.0) / 100.0;
        }


        if (killed && !dead) {
            kills++;
            killStreak++;

            killed = false;
        }

        if (dead) {
            killStreak = 0;
            deaths++;

            dead = false;
        }
    }


    @EventHandler
    public void onGameJoin(GameJoinedEvent event) {
        killStreak = 0;
        killed = false;
    }

    @Override
    public void onDeactivate() {
        kills = 0;
        killStreak = 0;
        deaths = 0;
        killed = false;
    }
}

