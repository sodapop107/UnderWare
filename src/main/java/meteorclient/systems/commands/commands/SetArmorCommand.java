package meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import meteorclient.systems.commands.Command;

import net.minecraft.command.CommandSource;
import net.minecraft.screen.slot.SlotActionType;

public class SetArmorCommand extends Command {

    public SetArmorCommand() {
        super("armor", "Move hand item to armor slots.");
    }

    private void a(int a){
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + mc.player.getInventory().selectedSlot, a, SlotActionType.SWAP, mc.player);
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(literal("boot").executes(a -> {
            a(36);
            return 1;
        }));
        builder.then(literal("legging").executes(a -> {
            a(37);
            return 1;
        }));
        builder.then(literal("chest").executes(a -> {
            a(38);
            return 1;
        }));
        builder.then(literal("head").executes(a -> {
            a(39);
            return 1;
        }));

    }


}
