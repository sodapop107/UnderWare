package meteorclient.mixin;

import meteorclient.UnderWare;
import meteorclient.systems.config.Config;
import meteorclient.utils.Utils;
import meteorclient.utils.misc.Version;
import meteorclient.utils.network.Http;
import meteorclient.utils.network.MeteorExecutor;
import meteorclient.utils.player.TitleScreenCreditsRenderer;
import meteorclient.utils.render.prompts.OkPrompt;
import meteorclient.utils.render.prompts.YesNoPrompt;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    public TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawStringWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", ordinal = 0))
    private void onRenderIdkDude(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (Utils.firstTimeTitleScreen) {
            Utils.firstTimeTitleScreen = false;
            UnderWare.LOG.info("Checking latest version of UnderWare");

            MeteorExecutor.execute(() -> {
                String res = Http.get("https://meteorclient.com/api/version").sendString();
                if (res == null) return;

                Version latestVer = new Version(res);

                if (latestVer.isHigherThan(UnderWare.VERSION)) {
                    YesNoPrompt.create()
                        .title("New Update")
                        .message("A new version of Meteor has been released.")
                        .message("Your version: %s", UnderWare.VERSION)
                        .message("Latest version: %s", latestVer)
                        .message("Do you want to update?")
                        .onYes(() -> Util.getOperatingSystem().open("https://meteorclient.com/"))
                        .onNo(() -> OkPrompt.create()
                            .title("Are you sure?")
                            .message("Using old versions of Meteor is not recommended")
                            .message("and could report in issues.")
                            .id("new-update-no")
                            .show())
                        .id("new-update")
                        .show();
                }
            });
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (Config.get().titleScreenCredits.get()) TitleScreenCreditsRenderer.render(matrices);
    }
}
