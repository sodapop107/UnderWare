package meteorclient.systems.modules.chat;

import java.util.HashMap;

import meteorclient.systems.modules.Categories;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;

import meteorclient.events.game.ReceiveMessageEvent;
import meteorclient.gui.GuiTheme;
import meteorclient.gui.widgets.WWidget;
import meteorclient.gui.widgets.containers.WTable;
import meteorclient.gui.widgets.input.WTextBox;
import meteorclient.gui.widgets.pressable.WMinus;
import meteorclient.gui.widgets.pressable.WPlus;
import meteorclient.settings.BoolSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.settings.StringSetting;
import meteorclient.systems.modules.Module;
import meteorclient.utils.misc.MeteorStarscript;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import meteordevelopment.starscript.utils.StarscriptError;

public class ChatBot extends Module {

    public final HashMap<String, String> commands = new HashMap<>() {{
        put("ping", "Pong!");
        put("tps", "Current TPS: {server.tps}");
        put("time", "It's currently {server.time}");
        put("time", "It's currently {server.time}");
        put("pos", "I am @ {player.pos}");
    }};

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> prefix = sgGeneral.add(new StringSetting.Builder()
        .name("prefix")
        .description("Command prefix for the bot.")
        .defaultValue("!")
        .build()
    );

    private final Setting<Boolean> help = sgGeneral.add(new BoolSetting.Builder()
        .name("help")
        .description("Add help command.")
        .defaultValue(true)
        .build()
    );

    public ChatBot() {
        super(Categories.Chat, "chat-bot", "Bot which automatically responds to chat messages.");
    }

    private String currMsgK = "", currMsgV = "";

    @EventHandler
    private void onMessageRecieve(ReceiveMessageEvent event) {
        String msg = event.getMessage().getString();
        if (help.get() && msg.endsWith(prefix.get()+"help")) {
            mc.getNetworkHandler().sendPacket(new ChatMessageC2SPacket("Avaliable commands: " + String.join(", ", commands.keySet())));
            return;
        }
        for (String cmd : commands.keySet()) {
            if (msg.endsWith(prefix.get()+cmd)) {
                Script script = compile(commands.get(cmd));
                if (script == null) mc.player.sendChatMessage("An error occurred");
                try {
                    mc.player.sendChatMessage(MeteorStarscript.ss.run(script));
                } catch (StarscriptError e) {
                    MeteorStarscript.printChatError(e);
                    mc.player.sendChatMessage("An error occurred");
                }
                return;
            }
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        fillTable(theme, table);
        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        table.clear();
        commands.keySet().forEach((key) -> {
            table.add(theme.label(key)).expandCellX();
            table.add(theme.label(commands.get(key))).expandCellX();
            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                commands.remove(key);
                fillTable(theme,table);
            };
            table.row();
        });
        WTextBox textBoxK = table.add(theme.textBox(currMsgK)).minWidth(100).expandX().widget();
        textBoxK.action = () -> {
            currMsgK = textBoxK.get();
        };
        WTextBox textBoxV = table.add(theme.textBox(currMsgV)).minWidth(100).expandX().widget();
        textBoxV.action = () -> {
            currMsgV = textBoxV.get();
        };
        WPlus add = table.add(theme.plus()).widget();
        add.action = () -> {
            if (currMsgK != ""  && currMsgV != "") {
                commands.put(currMsgK, currMsgV);
                currMsgK = ""; currMsgV = "";
                fillTable(theme,table);
            }
        };
        table.row();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        NbtCompound messTag = new NbtCompound();
        commands.keySet().forEach((key) -> {
            messTag.put(key, NbtString.of(commands.get(key)));
        });

        tag.put("commands", messTag);
        return tag;
    }

    @Override
    public Module fromTag(NbtCompound tag) {

        commands.clear();
        if (tag.contains("commands")) {
            NbtCompound msgs = tag.getCompound("commands");
            msgs.getKeys().forEach((key) -> {
                commands.put(key, msgs.getString(key));
            });
        }

        return super.fromTag(tag);
    }

    private static Script compile(String script) {
        if (script == null) return null;
        Parser.Result result = Parser.parse(script);
        if (result.hasErrors()) {
            MeteorStarscript.printChatError(result.errors.get(0));
            return null;
        }
        return Compiler.compile(result);
    }
}
