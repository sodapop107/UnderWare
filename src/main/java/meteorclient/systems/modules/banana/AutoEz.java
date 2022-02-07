package meteorclient.systems.modules.banana;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import meteorclient.systems.modules.Categories;
import meteorclient.utils.misc.Timer;
import meteorclient.events.game.GameJoinedEvent;
import meteorclient.events.packets.PacketEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.gui.GuiTheme;
import meteorclient.gui.widgets.WWidget;
import meteorclient.gui.widgets.pressable.WButton;
import meteorclient.settings.*;
import meteorclient.systems.friends.Friends;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import meteorclient.utils.Utils;
import meteorclient.utils.misc.MeteorStarscript;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class AutoEz extends Module {
    private final SettingGroup sgAutoEz = settings.createGroup("Auto Ez");
    private final SettingGroup sgTotemPops = settings.createGroup("Totem Pops");
    private final SettingGroup sgAutoCope = settings.createGroup("Auto Cope");

    private final Setting<Boolean> autoEz = sgAutoEz.add(new BoolSetting.Builder().name("auto-ez").description("Sends a custom msg when you kill a player").defaultValue(false).build());
    private final Setting<Boolean> randomA = sgAutoEz.add(new BoolSetting.Builder().name("randomise").description("Selects a random message from your autoEz message list.").defaultValue(false).visible(autoEz::get).build());
    private final Setting<List<String>> messagesA = sgAutoEz.add(new StringListSetting.Builder().name("auto-ez-messages").description("Messages to use for autoEz.").defaultValue(List.of("(enemy) got ezed by Cracked B+ by BedTrap | Kills: (killCount)", "(enemy) should have bought BedTrap instead of Banana+ | Kills: (killCount)", "{server.player_count} ppl saw u die to the power of Cracked B+ by BedTrap Kills: (killCount)", "Monke (enemy) down! Cracked B+ by BedTrap | Kills: (killCount)", "Currently on a (killCount) killstreak thanks to BedTrap which cracked B+", "Check this op KD of (KD) thanks to Cracked B+ by BedTrap", "Buy BedTrap at: https://discord.gg/chJNFZzTgq Kills: (killCount)")).onChanged(strings -> recompileEz()).visible(autoEz::get).build());
    private final Setting<Boolean> totemPops = sgTotemPops.add(new BoolSetting.Builder().name("totem-pops").description("Sends a custom msg when u pop a players totem.").defaultValue(false).build());
    private final Setting<Boolean> ignoreFriends = sgTotemPops.add(new BoolSetting.Builder().name("ignore-Friends").description("Only working for totem pops :/").defaultValue(true).visible(totemPops::get).build());
    private final Setting<Integer> msgDelay = sgTotemPops.add(new IntSetting.Builder().name("Msg Delay").description("In ticks, 20 ticks = 1 sec").defaultValue(60).min(1).sliderMax(200).visible(totemPops::get).build());
    private final Setting<Double> totemRange = sgTotemPops.add(new DoubleSetting.Builder().name("totem-range").description("The radius in which it will announce totem pops.").defaultValue(7).min(1).sliderMax(16).max(20).visible(totemPops::get).build());
    private final Setting<Boolean> randomT = sgTotemPops.add(new BoolSetting.Builder().name("randomise").description("Selects a random message from your totem pop message list.").defaultValue(false).visible(totemPops::get).build());
    private final Setting<List<String>> messagesT = sgTotemPops.add(new StringListSetting.Builder().name("pop-messages").description("Messages to use for totem pops, u can use (enemy) and startscript shortcuts in totem msgs").defaultValue(List.of("Easily popped (enemy) with the power of Cracked B+ by BedTrap", "(enemy) needs a new totem :* | Cracked B+ by BedTrap", "(enemy) popping! Cracked B+ by BedTrap", "Monke (enemy) almost down! Cracked B+ by BedTrap")).onChanged(strings -> recompilePop()).visible(totemPops::get).build());
    private final Setting<Boolean> autoCope = sgAutoCope.add(new BoolSetting.Builder().name("auto-cope").description("Sends a custom msg when you die").defaultValue(false).build());
    private final Setting<Boolean> randomC = sgAutoCope.add(new BoolSetting.Builder().name("randomise").description("Selects a random message from your autoCope message list.").defaultValue(false).visible(autoCope::get).build());
    private final Setting<List<String>> messagesC = sgAutoCope.add(new StringListSetting.Builder().name("auto-cope-messages").description("Messages to use for autoCope.").defaultValue(List.of("Don't die like I did", "I do not believe in my death", "At least i died using Banana+", "Monke down!", "Pass on my Banana+ access", "Fake! Banana+ users never die!")).onChanged(strings -> recompileCope()).visible(autoCope::get).build());

    private final List<Script> autoEzScripts = new ArrayList<>();
    private final List<Script> popScripts = new ArrayList<>();
    private final List<Script> autoCopeScripts = new ArrayList<>();
    String autoEzMsg;
    String autoPopMsg;
    String autoCopeMsg;
    private int messageEzI;
    private int messagePopI;
    private int messageCopeI;
    public String[] killWords = {"died", "blew", "by", "slain", "fucked", "killed", "separated", "punched", "shoved", "crystal", "nuked"};
    String enemyName;
    private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIdMap = new Object2IntOpenHashMap<>();
    private final Random random = new Random();
    boolean shouldEz = false;
    int killTime = 10;
    boolean dead;
    Modules modules = Modules.get();

    public AutoEz() {
        super(Categories.BANANAPLUS, "auto-monke", "(killCount) = killStreak, (enemy) = player u killed, (KD) = kills/deaths, u can also use starscript {} see doc down below");
    }

    @Override
    public void onActivate() {
        messageEzI = 0;
        messagePopI = 0;
        messageCopeI = 0;
        totemPopMap.clear();
        chatIdMap.clear();
        recompilePop();
        recompileEz();
        if (!modules.isActive(MonkeStats.class)) modules.get(MonkeStats.class).toggle();
    }

    private void recompile(List<String> messages, List<Script> scripts) {
        scripts.clear();

        for (int i = 0; i < messages.size(); i++) {
            Parser.Result result = Parser.parse(messages.get(i));

            if (result.hasErrors()) {
                if (Utils.canUpdate()) {
                    MeteorStarscript.printChatError(i, result.errors.get(0));
                }

                continue;
            }

            scripts.add(Compiler.compile(result));
        }
    }

    private void recompileEz() {
        recompile(messagesA.get(), autoEzScripts);
    }

    private void recompilePop() {
        recompile(messagesT.get(), popScripts);
    }

    private void recompileCope() {
        recompile(messagesC.get(), autoCopeScripts);
    }

    private Timer msgTimer = new Timer();

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket) {
            if (!totemPops.get()) return;

            EntityStatusS2CPacket p = (EntityStatusS2CPacket) event.packet;

            if (p.getStatus() != 35) return;

            Entity entity = p.getEntity(mc.world);

            if (!(entity instanceof PlayerEntity)) return;

            if (entity == null || (entity.equals(mc.player)) || (Friends.get().isFriend(((PlayerEntity) entity)) && ignoreFriends.get())) return;

            int pops = totemPopMap.getOrDefault(entity.getUuid(), 0);
            totemPopMap.put(entity.getUuid(), ++pops);

            int i;
            if (randomT.get()) {
                i = Utils.random(0, popScripts.size());
            } else {
                if (messagePopI >= popScripts.size()) messagePopI = 0;
                i = messagePopI++;
            }

            if (mc.player.distanceTo(entity) <= totemRange.get() && !mc.player.isDead() && msgTimer.passedTicks(msgDelay.get()) && !messagesT.get().isEmpty()) {
                msgTimer.reset();
                autoPopMsg = MeteorStarscript.ss.run(popScripts.get(i));
                mc.player.sendChatMessage(autoPopMsg.replace("(enemy)", entity.getEntityName()).replace("(killCount)", MonkeStats.killStreak.toString()).replace("(KD)", MonkeStats.kD.toString()).replace("(kills)", MonkeStats.kills.toString()).replace("(deaths)", MonkeStats.deaths.toString()));
            }
        }

        if ((event.packet instanceof GameMessageS2CPacket)) {
            String msg = ((GameMessageS2CPacket) event.packet).getMessage().getString();
            for (String word : killWords) {

                if (msg.contains(word) && msg.contains(mc.player.getDisplayName().getString()) && ((GameMessageS2CPacket) event.packet).getSender().toString().contains("000000000") && !mc.player.isDead()) {
                    enemyName = msg.substring(0, msg.indexOf(" "));

                    if (enemyName.contains(mc.player.getDisplayName().getString())) {
                        dead = true;
                        return;
                    }

                    if (!autoEzScripts.isEmpty() || !messagesA.get().isEmpty()) {
                        shouldEz = true;
                    }
                }
            }
        }
    }

    private Timer timer = new Timer();

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Post event) {
        if (!shouldEz) {
            timer.reset();
        }

        if (shouldEz && !dead && timer.passedTicks(killTime)) {

            int i;
            if (randomA.get()) {
                i = Utils.random(0, autoEzScripts.size());
            } else {
                if (messageEzI >= autoEzScripts.size()) messageEzI = 0;
                i = messageEzI++;
            }

            autoEzMsg = MeteorStarscript.ss.run(autoEzScripts.get(i));

            if (autoEz.get()) {
                mc.player.sendChatMessage(autoEzMsg.replace("(enemy)", enemyName).replace("(killCount)", MonkeStats.killStreak.toString()).replace("(KD)", MonkeStats.kD.toString()).replace("(kills)", MonkeStats.kills.toString()).replace("(deaths)", MonkeStats.deaths.toString()));
            }

            shouldEz = false;
        }

        if (dead) {
            timer.reset();
            shouldEz = false;

            if (autoCope.get()) {
                int i;
                if (randomC.get()) {
                    i = Utils.random(0, autoCopeScripts.size());
                } else {
                    if (messageCopeI >= autoCopeScripts.size()) messageCopeI = 0;
                    i = messageCopeI++;
                }

                autoCopeMsg = MeteorStarscript.ss.run(autoCopeScripts.get(i));

                mc.player.sendChatMessage(autoCopeMsg.replace("(enemy)", enemyName).replace("(killCount)", MonkeStats.killStreak.toString()).replace("(KD)", MonkeStats.kD.toString()).replace("(kills)", MonkeStats.kills.toString()).replace("(deaths)", MonkeStats.deaths.toString()));
            }

            dead = false;
        }
    }

    private int getChatId(Entity entity) {
        return chatIdMap.computeIntIfAbsent(entity.getUuid(), value -> random.nextInt());
    }

    @EventHandler
    public void onGameJoin(GameJoinedEvent event) {
        if (!modules.isActive(MonkeStats.class)) modules.get(MonkeStats.class).toggle();
        shouldEz = false;
    }

    @Override
    public void onDeactivate() {
        shouldEz = false;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton help = theme.button("Open documentation.");
        help.action = () -> Util.getOperatingSystem().open("https://github.com/MeteorDevelopment/meteor-client/wiki/Starscript");

        return help;
    }
}
