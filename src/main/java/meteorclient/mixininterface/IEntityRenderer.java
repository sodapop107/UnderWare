package meteorclient.mixininterface;

import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

public interface IEntityRenderer {
    Identifier getTextureInterface(Entity entity);
}
