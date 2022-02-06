package meteorclient.mixin;

import meteorclient.UnderWare;
import meteorclient.events.entity.DamageEvent;
import meteorclient.events.entity.player.CanWalkOnFluidEvent;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.modules.movement.AntiLevitation;
import meteorclient.systems.modules.player.OffhandCrash;
import meteorclient.systems.modules.render.HandView;
import meteorclient.systems.modules.render.NoRender;
import meteorclient.utils.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteorclient.UnderWare.mc;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamageHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (Utils.canUpdate() && world.isClient) UnderWare.EVENT_BUS.post(DamageEvent.get((LivingEntity) (Object) this, source));
    }

    @Inject(method = "canWalkOnFluid", at = @At("HEAD"), cancellable = true)
    private void onCanWalkOnFluid(Fluid fluid, CallbackInfoReturnable<Boolean> info) {
        if ((Object) this != mc.player) return;
        CanWalkOnFluidEvent event = UnderWare.EVENT_BUS.post(CanWalkOnFluidEvent.get(fluid));

        info.setReturnValue(event.walkOnFluid);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"))
    private boolean travelHasStatusEffectProxy(LivingEntity self, StatusEffect statusEffect) {
        if (statusEffect == StatusEffects.LEVITATION && Modules.get().isActive(AntiLevitation.class)) return false;
        return self.hasStatusEffect(statusEffect);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasNoGravity()Z"))
    private boolean travelHasNoGravityProxy(LivingEntity self) {
        if (self.hasStatusEffect(StatusEffects.LEVITATION) && Modules.get().isActive(AntiLevitation.class)) {
            return !Modules.get().get(AntiLevitation.class).isApplyGravity();
        }
        return self.hasNoGravity();
    }

    @Inject(method = "spawnItemParticles", at = @At("HEAD"), cancellable = true)
    private void spawnItemParticles(ItemStack stack, int count, CallbackInfo info) {
        NoRender noRender = Modules.get().get(NoRender.class);
        if (noRender.noEatParticles() && stack.isFood()) info.cancel();
    }

    @Inject(method = "onEquipStack", at = @At("HEAD"), cancellable = true)
    private void onEquipStack(ItemStack stack, CallbackInfo info) {
        if ((Object) this == mc.player && Modules.get().get(OffhandCrash.class).isAntiCrash()) {
            info.cancel();
        }
    }

    @ModifyArg(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;swingHand(Lnet/minecraft/util/Hand;Z)V"))
    private Hand setHand(Hand hand) {
        HandView handView = Modules.get().get(HandView.class);
        if ((Object) this == mc.player && handView.isActive()) {
            if (handView.swingMode.get() == HandView.SwingMode.None) return hand;
            return handView.swingMode.get() == HandView.SwingMode.Offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        }
        return hand;
    }
}
