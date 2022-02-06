package meteorclient.settings;

import meteorclient.gui.GuiTheme;
import meteorclient.gui.WidgetScreen;
import meteorclient.utils.misc.IChangeable;
import meteorclient.utils.misc.ICopyable;
import meteorclient.utils.misc.ISerializable;
import net.minecraft.block.Block;

public interface IBlockData<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> {
    WidgetScreen createScreen(GuiTheme theme, Block block, BlockDataSetting<T> setting);
}
