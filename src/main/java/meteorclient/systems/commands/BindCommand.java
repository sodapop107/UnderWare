package meteorclient.systems.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteorclient.systems.commands.arguments.ModuleArgumentType;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class BindCommand extends Command {
    public BindCommand() {
        super("bind", "Binds a specified module to the next pressed key.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("module", ModuleArgumentType.module()).executes(context -> {
            Module module = context.getArgument("module", Module.class);
            Modules.get().setModuleToBind(module);

            module.info("Press a key to bind the module to.");
            return SINGLE_SUCCESS;
        }));
    }
}
