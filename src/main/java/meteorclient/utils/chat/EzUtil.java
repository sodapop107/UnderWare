package meteorclient.utils.chat;

import meteorclient.UnderWare;
import meteorclient.systems.modules.chat.PopCounter;
import meteorclient.systems.modules.combat.BedAura;
import meteorclient.utils.misc.Wrapper;
import meteorclient.utils.misc.Placeholders;
import meteorclient.utils.misc.Stats;
import meteorclient.utils.misc.StringHelper;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.modules.combat.CrystalAura;
import meteorclient.systems.modules.combat.KillAura;
import meteorclient.utils.player.ChatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static meteorclient.UnderWare.mc;

public class EzUtil {
    public static List<String> currentTargets = new ArrayList<>();

    public static void sendAutoEz(String playerName) {
        increaseKC();
        PopCounter popCounter = Modules.get().get(PopCounter.class);
        List<String> ezMessages = popCounter.ezMessages.get();
        if (ezMessages.isEmpty()) {
            ChatUtils.warning("Your auto ez message list is empty!");
            return;
        }
        String ezMessage = ezMessages.get(new Random().nextInt(ezMessages.size()));
        if (ezMessage.contains("{player}")) ezMessage = ezMessage.replace("{player}", playerName);
        if (popCounter.doPlaceholders.get()) ezMessage = Placeholders.apply(ezMessage);
        if (popCounter.killStr.get()) { ezMessage = ezMessage + " | Killstreak: " + Stats.killStreak; }
        if (popCounter.suffix.get()) { ezMessage = ezMessage + " | UnderWare " + UnderWare.VERSION; }
        mc.player.sendChatMessage(ezMessage);
        if (popCounter.pmEz.get()) Wrapper.messagePlayer(playerName, StringHelper.stripName(playerName, ezMessage));
    }

    public static void increaseKC() {
        Stats.kills++;
        Stats.killStreak++;
    }

    public static void updateTargets() {
        currentTargets.clear();
        ArrayList<Module> modules = new ArrayList<>();
        modules.add(Modules.get().get(CrystalAura.class));
        modules.add(Modules.get().get(KillAura.class));
        modules.add(Modules.get().get(BedAura.class));
        for (Module module : modules) currentTargets.add(module.getInfoString());
    }

}
