package meteorclient.systems.modules.player;

import meteorclient.settings.BoolSetting;
import meteorclient.settings.Setting;
import meteorclient.settings.SettingGroup;
import meteorclient.settings.StatusEffectListSetting;
import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;
import meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.effect.StatusEffect;

import java.util.List;

import static net.minecraft.entity.effect.StatusEffects.*;

public class PotionSaver extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<StatusEffect>> effects = sgGeneral.add(new StatusEffectListSetting.Builder()
        .name("effects")
        .description("The effects to preserve.")
        .defaultValue(
            STRENGTH,
            ABSORPTION,
            RESISTANCE,
            FIRE_RESISTANCE,
            SPEED,
            HASTE,
            REGENERATION,
            WATER_BREATHING,
            SATURATION,
            LUCK,
            SLOW_FALLING,
            DOLPHINS_GRACE,
            CONDUIT_POWER,
            HERO_OF_THE_VILLAGE
        )
        .build()
    );

    public final Setting<Boolean> onlyWhenStationary = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-stationary")
        .description("Only freezes effects when you aren't moving.")
        .defaultValue(false)
        .build()
    );

    public PotionSaver() {
        super(Categories.Player, "potion-saver", "Stops potion effects ticking when you stand still.");
    }

    public boolean shouldFreeze(StatusEffect effect) {
        return isActive() && (!onlyWhenStationary.get() || !PlayerUtils.isMoving()) && !mc.player.getStatusEffects().isEmpty() && effects.get().contains(effect);
    }
}
