package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Module;
import meteorclient.utils.misc.ReflectionHelper;

import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;

public class ReloadSoundSystem extends Module {

    public ReloadSoundSystem() {
        super(Categories.BANANAPLUS, "reload-sounds", "Reloads Minecraft's sound system");
    }

    @Override
    public void onActivate() {
        SoundSystem soundSystem = ReflectionHelper.getPrivateValue(SoundManager.class, mc.getSoundManager(), "soundSystem", "field_5590");
        soundSystem.reloadSounds();
        toggle();
    }
}
