package meteorclient.mixin;

import meteorclient.UnderWare;
import meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;
import meteorclient.events.game.GameLeftEvent;
import meteorclient.events.game.OpenScreenEvent;
import meteorclient.events.game.ResourcePacksReloadedEvent;
import meteorclient.events.game.WindowResizedEvent;
import meteorclient.events.world.TickEvent;
import meteorclient.gui.WidgetScreen;
import meteorclient.mixininterface.IMinecraftClient;
import meteorclient.systems.config.Config;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.modules.render.UnfocusedCPU;
import meteorclient.utils.Utils;
import meteorclient.utils.misc.MeteorStarscript;
import meteorclient.utils.network.OnlinePlayers;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import meteordevelopment.starscript.utils.Error;
import meteordevelopment.starscript.utils.StarscriptError;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(value = MinecraftClient.class, priority = 1001)
public abstract class MinecraftClientMixin implements IMinecraftClient {
    @Unique private boolean doItemUseCalled;
    @Unique private boolean rightClick;
    @Unique private long lastTime;
    @Unique private boolean firstFrame;

    @Shadow public ClientWorld world;
    @Shadow @Final public Mouse mouse;
    @Shadow @Final private Window window;
    @Shadow public Screen currentScreen;

    @Shadow protected abstract void doItemUse();
    @Shadow public abstract Profiler getProfiler();
    @Shadow public abstract boolean isWindowFocused();

    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        UnderWare.INSTANCE.onInitializeClient();
        firstFrame = true;
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void onPreTick(CallbackInfo info) {
        OnlinePlayers.update();
        doItemUseCalled = false;

        getProfiler().push("under-ware_pre_update");
        UnderWare.EVENT_BUS.post(TickEvent.Pre.get());
        getProfiler().pop();

        if (rightClick && !doItemUseCalled && interactionManager != null) doItemUse();
        rightClick = false;
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo info) {
        getProfiler().push("under-ware_post_update");
        UnderWare.EVENT_BUS.post(TickEvent.Post.get());
        getProfiler().pop();
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onDoItemUse(CallbackInfo info) {
        doItemUseCalled = true;
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, CallbackInfo info) {
        if (world != null) {
            UnderWare.EVENT_BUS.post(GameLeftEvent.get());
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof WidgetScreen) screen.mouseMoved(mouse.getX() * window.getScaleFactor(), mouse.getY() * window.getScaleFactor());

        OpenScreenEvent event = OpenScreenEvent.get(screen);
        UnderWare.EVENT_BUS.post(event);

        if (event.isCancelled()) info.cancel();
    }

    @Redirect(method = "doItemUse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;crosshairTarget:Lnet/minecraft/util/hit/HitResult;", ordinal = 1))
    private HitResult doItemUseMinecraftClientCrosshairTargetProxy(MinecraftClient client) {
        return UnderWare.EVENT_BUS.post(ItemUseCrosshairTargetEvent.get(client.crosshairTarget)).target;
    }

    @ModifyVariable(method = "reloadResources(Z)Ljava/util/concurrent/CompletableFuture;", at = @At("STORE"), ordinal = 0)
    private CompletableFuture<Void> onReloadResourcesNewCompletableFuture(CompletableFuture<Void> completableFuture) {
        completableFuture.thenRun(() -> UnderWare.EVENT_BUS.post(ResourcePacksReloadedEvent.get()));

        return completableFuture;
    }

    @ModifyArg(method = "updateWindowTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setTitle(Ljava/lang/String;)V"))
    private String setTitle(String original) {
        if (Config.get() == null || !Config.get().customWindowTitle.get()) return original;

        String customTitle = Config.get().customWindowTitleText.get();
        Parser.Result result = Parser.parse(customTitle);

        if (result.hasErrors()) {
            for (Error error : result.errors) MeteorStarscript.printChatError(error);
        }
        else {
            Script script = Compiler.compile(result);

            try {
                customTitle = MeteorStarscript.ss.run(script);
            } catch (StarscriptError e) {
                MeteorStarscript.printChatError(e);
            }
        }

        return customTitle;
    }

    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void onResolutionChanged(CallbackInfo info) {
        UnderWare.EVENT_BUS.post(WindowResizedEvent.get());
    }

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void onGetFramerateLimit(CallbackInfoReturnable<Integer> info) {
        if (Modules.get().isActive(UnfocusedCPU.class) && !isWindowFocused()) info.setReturnValue(1);
    }

    // Time delta

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(CallbackInfo info) {
        long time = System.currentTimeMillis();

        if (firstFrame) {
            lastTime = time;
            firstFrame = false;
        }

        Utils.frameTime = (time - lastTime) / 1000.0;
        lastTime = time;
    }

    // Interface

    @Override
    public void rightClick() {
        rightClick = true;
    }
}
