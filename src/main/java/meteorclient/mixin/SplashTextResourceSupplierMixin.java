package meteorclient.mixin;

import meteorclient.systems.config.Config;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Mixin(SplashTextResourceSupplier.class)
public class SplashTextResourceSupplierMixin {
    private boolean override = true;
    private final Random random = new Random();

    private final List<String> meteorSplashes = getMeteorSplashes();

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void onApply(CallbackInfoReturnable<String> cir) {
        if (Config.get() == null || !Config.get().titleScreenSplashes.get()) return;

        if (override) cir.setReturnValue(meteorSplashes.get(random.nextInt(meteorSplashes.size())));
        override = !override;
    }

    private static List<String> getMeteorSplashes() {
        return Arrays.asList(
                "UnderWare on Crack!",
                "Star UnderWare on GitHub!",
                "Based utility mod.",
                "§6Vbu_ §fbased god",
                "§4UnderWare on Crack!",
                "§6UnderWare on Crack!"
        );
    }

}
