package meteorclient.mixin;

import meteorclient.MeteorClient;
import meteorclient.systems.modules.Category;
import meteorclient.systems.modules.Module;
import meteorclient.systems.modules.Modules;
import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CrashReport.class)
public class CrashReportMixin {
    @Inject(method = "addStackTrace", at = @At("TAIL"))
    private void onAddStackTrace(StringBuilder sb, CallbackInfo info) {
        if (Modules.get() != null) {
            sb.append("\n\n");
            sb.append("-- UnderWare --\n");
            sb.append("Version: ").append(MeteorClient.VERSION).append("\n");

            if (!MeteorClient.DEV_BUILD.isEmpty()) {
                sb.append("Dev Build: ").append(MeteorClient.DEV_BUILD).append("\n");
            }

            for (Category category : Modules.loopCategories()) {
                List<Module> modules = Modules.get().getGroup(category);
                boolean active = false;

                for (Module module : modules) {
                    if (module != null && module.isActive()) {
                        active = true;
                        break;
                    }
                }

                if (active) {
                    sb.append("\n");
                    sb.append("[").append(category).append("]:").append("\n");

                    for (Module module : modules) {
                        if (module != null && module.isActive()) {
                            sb.append(module.name).append("\n");
                        }
                    }
                }
            }
        }
    }
}
