package meteorclient.systems.modules.banana;

import meteorclient.systems.modules.Categories;
import meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteorclient.events.packets.PacketEvent;
import meteorclient.systems.modules.Module;

import net.minecraft.block.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.BlockPos;

public class AntiClick extends Module {
    public AntiClick() {
        super(Categories.BANANAPLUS, "anti-click", "Prevents clicking on openable blocks.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.world == null) return;
        SurroundPlus sp = Modules.get().get(SurroundPlus.class);
        if (sp.isActive() && sp.pauseAntiClick.get()) return;
        if (mc.player != null && mc.player.isSneaking()) return;
        if (!(event.packet instanceof PlayerInteractBlockC2SPacket)) return;

        BlockPos blockPos = ((PlayerInteractBlockC2SPacket) event.packet).getBlockHitResult().getBlockPos();
        boolean BlockIsOpenable = mc.world.getBlockState(blockPos).getBlock() instanceof AnvilBlock || mc.world.getBlockState(blockPos).getBlock() instanceof CraftingTableBlock || mc.world.getBlockState(blockPos).getBlock() instanceof ChestBlock || mc.world.getBlockState(blockPos).getBlock() instanceof TrappedChestBlock || mc.world.getBlockState(blockPos).getBlock() instanceof BarrelBlock || mc.world.getBlockState(blockPos).getBlock() instanceof EnderChestBlock || mc.world.getBlockState(blockPos).getBlock() instanceof ShulkerBoxBlock || mc.world.getBlockState(blockPos).getBlock() instanceof FurnaceBlock || mc.world.getBlockState(blockPos).getBlock() instanceof LoomBlock || mc.world.getBlockState(blockPos).getBlock() instanceof CartographyTableBlock || mc.world.getBlockState(blockPos).getBlock() instanceof FletchingTableBlock || mc.world.getBlockState(blockPos).getBlock() instanceof GrindstoneBlock || mc.world.getBlockState(blockPos).getBlock() instanceof SmithingTableBlock || mc.world.getBlockState(blockPos).getBlock() instanceof StonecutterBlock || mc.world.getBlockState(blockPos).getBlock() instanceof BlastFurnaceBlock || mc.world.getBlockState(blockPos).getBlock() instanceof SmokerBlock || mc.world.getBlockState(blockPos).getBlock() instanceof HopperBlock || mc.world.getBlockState(blockPos).getBlock() instanceof DispenserBlock || mc.world.getBlockState(blockPos).getBlock() instanceof DropperBlock || mc.world.getBlockState(blockPos).getBlock() instanceof LecternBlock || mc.world.getBlockState(blockPos).getBlock() instanceof BeaconBlock || mc.world.getBlockState(blockPos).getBlock() instanceof EnchantingTableBlock;

        if(BlockIsOpenable) {
            event.cancel();
        }
    }
}
