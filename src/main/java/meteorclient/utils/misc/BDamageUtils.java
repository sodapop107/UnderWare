package meteorclient.utils.misc;

import meteorclient.systems.modules.banana.BananaBomber;
import meteorclient.UnderWare;
import meteorclient.events.game.GameJoinedEvent;
import meteorclient.mixininterface.IExplosion;
import meteorclient.mixininterface.IRaycastContext;
import meteorclient.mixininterface.IVec3d;
import meteorclient.systems.friends.Friends;
import meteorclient.systems.modules.Modules;
import meteorclient.systems.modules.movement.NoFall;
import meteorclient.utils.Utils;
import meteorclient.utils.entity.EntityUtils;
import meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteorclient.utils.player.PlayerUtils;
import meteorclient.utils.world.Dimension;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.explosion.Explosion;

import java.util.Objects;

import static meteorclient.UnderWare.mc;

public class BDamageUtils {
    private static final Vec3d vec3d = new Vec3d(0, 0, 0);
    private static Explosion explosion;
    private static RaycastContext raycastContext;

    public static void init() {
        UnderWare.EVENT_BUS.subscribe(BDamageUtils.class);
    }

    @EventHandler
    private static void onGameJoined(GameJoinedEvent event) {
        explosion = new Explosion(mc.world, null, 0, 0, 0, 6, false, Explosion.DestructionType.DESTROY);
        raycastContext = new RaycastContext(null, null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);
    }

    public static double crystalDamage(PlayerEntity player, Vec3d crystal, boolean predictMovement, BlockPos obsidianPos, boolean ignoreTerrain) {
        if (player == null) return 0;
        if (EntityUtils.getGameMode(player) == GameMode.CREATIVE && !(player instanceof FakePlayerEntity)) return 0;

        ((IVec3d) vec3d).set(player.getPos().x, player.getPos().y, player.getPos().z);
        if (predictMovement)
            ((IVec3d) vec3d).set(vec3d.x + player.getVelocity().x, vec3d.y + player.getVelocity().y, vec3d.z + player.getVelocity().z);

        double modDistance = Math.sqrt(vec3d.squaredDistanceTo(crystal));
        if (modDistance > Modules.get().get(BananaBomber.class).explosionRadius.get()) return 0;

        double exposure = getExposure(crystal, player, predictMovement, raycastContext, obsidianPos, ignoreTerrain);
        double impact = (1 - (modDistance / 12)) * exposure;
        double damage = ((impact * impact + impact) * 42 + 1);

        damage = getDamageForDifficulty(damage);
        damage = DamageUtil.getDamageLeft((float) damage, (float) player.getArmor(), (float) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());
        damage = resistanceReduction(player, damage);

        ((IExplosion) explosion).set(crystal, 6, false);
        damage = blastProtReduction(player, damage, explosion);

        return damage < 0 ? 0 : damage;
    }

    public static double crystalDamage(PlayerEntity player, Vec3d crystal) {
        return crystalDamage(player, crystal, false, null, false);
    }

    public static double crystalDamageForTotem(PlayerEntity player, Vec3d crystal, boolean predictMovement, BlockPos obsidianPos, boolean ignoreTerrain) {
        if (player == null) return 0;
        if (EntityUtils.getGameMode(player) == GameMode.CREATIVE && !(player instanceof FakePlayerEntity)) return 0;

        ((IVec3d) vec3d).set(player.getPos().x, player.getPos().y, player.getPos().z);
        if (predictMovement)
            ((IVec3d) vec3d).set(vec3d.x + player.getVelocity().x, vec3d.y + player.getVelocity().y, vec3d.z + player.getVelocity().z);

        double modDistance = Math.sqrt(vec3d.squaredDistanceTo(crystal));
        if (modDistance > 12) return 0;

        double exposure = getExposure(crystal, player, predictMovement, raycastContext, obsidianPos, ignoreTerrain);
        double impact = (1 - (modDistance / 12)) * exposure;
        double damage = ((impact * impact + impact) * 42 + 1);

        damage = getDamageForDifficulty(damage);
        damage = DamageUtil.getDamageLeft((float) damage, (float) player.getArmor(), (float) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());
        damage = resistanceReduction(player, damage);

        ((IExplosion) explosion).set(crystal, 6, false);
        damage = blastProtReduction(player, damage, explosion);

        return damage < 0 ? 0 : damage;
    }

    public static double crystalDamageForTotem(PlayerEntity player, Vec3d crystal) {
        return crystalDamage(player, crystal, false, null, false);
    }

    public static double getSwordDamage(PlayerEntity entity, boolean charged) {
        double damage = 0;
        if (charged) {
            if (entity.getActiveItem().getItem() == Items.NETHERITE_SWORD) {
                damage += 8;
            } else if (entity.getActiveItem().getItem() == Items.DIAMOND_SWORD) {
                damage += 7;
            } else if (entity.getActiveItem().getItem() == Items.GOLDEN_SWORD) {
                damage += 4;
            } else if (entity.getActiveItem().getItem() == Items.IRON_SWORD) {
                damage += 6;
            } else if (entity.getActiveItem().getItem() == Items.STONE_SWORD) {
                damage += 5;
            } else if (entity.getActiveItem().getItem() == Items.WOODEN_SWORD) {
                damage += 4;
            }
            damage *= 1.5;
        }

        if (entity.getActiveItem().getEnchantments() != null) {
            if (EnchantmentHelper.get(entity.getActiveItem()).containsKey(Enchantments.SHARPNESS)) {
                int level = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, entity.getActiveItem());
                damage += (0.5 * level) + 0.5;
            }
        }

        if (entity.getActiveStatusEffects().containsKey(StatusEffects.STRENGTH)) {
            int strength = Objects.requireNonNull(entity.getStatusEffect(StatusEffects.STRENGTH)).getAmplifier() + 1;
            damage += 3 * strength;
        }

        damage = resistanceReduction(entity, damage);
        damage = DamageUtil.getDamageLeft((float) damage, (float) entity.getArmor(), (float) entity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());
        damage = normalProtReduction(entity, damage);

        return damage < 0 ? 0 : damage;
    }

    public static double bedDamage(LivingEntity player, Vec3d bed) {
        if (player instanceof PlayerEntity && ((PlayerEntity) player).getAbilities().creativeMode) return 0;

        double modDistance = Math.sqrt(player.squaredDistanceTo(bed));
        if (modDistance > 10) return 0;

        double exposure = Explosion.getExposure(bed, player);
        double impact = (1.0 - (modDistance * 0.1)) * exposure;
        double damage = (impact * impact + impact) * 35 + 1;

        damage = getDamageForDifficulty(damage);
        damage = resistanceReduction(player, damage);
        damage = DamageUtil.getDamageLeft((float) damage, (float) player.getArmor(), (float) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

        ((IExplosion) explosion).set(bed, 5, true);
        damage = blastProtReduction(player, damage, explosion);

        if (damage < 0) damage = 0;
        return damage;
    }

    public static double anchorDamage(LivingEntity player, Vec3d anchor) {
        mc.world.removeBlock(new BlockPos(anchor), false);
        double damage = bedDamage(player, anchor);
        mc.world.setBlockState(new BlockPos(anchor), Blocks.RESPAWN_ANCHOR.getDefaultState());
        return damage;
    }

    private static double getDamageForDifficulty(double damage) {
        return switch (mc.world.getDifficulty()) {
            case PEACEFUL -> 0;
            case EASY -> Math.min(damage * 0.5 + 1, damage);
            case HARD -> damage * 1.5;
            default -> damage;
        };
    }

    private static double normalProtReduction(Entity player, double damage) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.GENERIC);
        if (protLevel > 20) protLevel = 20;

        damage *= 1 - (protLevel * 0.04);
        return damage < 0 ? 0 : damage;
    }

    private static double blastProtReduction(Entity player, double damage, Explosion explosion) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.explosion(explosion));
        if (protLevel > 20) protLevel = 20;

        damage *= (1 - (protLevel * 0.04));
        return damage < 0 ? 0 : damage;
    }

    private static double resistanceReduction(LivingEntity player, double damage) {
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int lvl = (player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
            damage *= (1 - (lvl * 0.2));
        }

        return damage < 0 ? 0 : damage;
    }

    private static double getExposure(Vec3d source, Entity entity, boolean predictMovement, RaycastContext raycastContext, BlockPos obsidianPos, boolean ignoreTerrain) {
        Box box = entity.getBoundingBox();
        if (predictMovement) {
            Vec3d v = entity.getVelocity();
            box.offset(v.x, v.y, v.z);
        }

        double d = 1 / ((box.maxX - box.minX) * 2 + 1);
        double e = 1 / ((box.maxY - box.minY) * 2 + 1);
        double f = 1 / ((box.maxZ - box.minZ) * 2 + 1);
        double g = (1 - Math.floor(1 / d) * d) * 0.5;
        double h = (1 - Math.floor(1 / f) * f) * 0.5;

        if (!(d < 0) && !(e < 0) && !(f < 0)) {
            int i = 0;
            int j = 0;

            for (double k = 0; k <= 1; k += d) {
                for (double l = 0; l <= 1; l += e) {
                    for (double m = 0; m <= 1; m += f) {
                        double n = MathHelper.lerp(k, box.minX, box.maxX);
                        double o = MathHelper.lerp(l, box.minY, box.maxY);
                        double p = MathHelper.lerp(m, box.minZ, box.maxZ);

                        ((IVec3d) vec3d).set(n + g, o, p + h);
                        ((IRaycastContext) raycastContext).set(vec3d, source, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);

                        if (raycast(raycastContext, obsidianPos, ignoreTerrain).getType() == HitResult.Type.MISS) i++;

                        j++;
                    }
                }
            }

            return (double) i / j;
        }

        return 0;
    }

    private static BlockHitResult raycast(RaycastContext context, BlockPos obsidianPos, boolean ignoreTerrain) {
        BananaBomber BBomber = Modules.get().get(BananaBomber.class);
        return BlockView.raycast(context.getStart(), context.getEnd(), context, (raycastContext, blockPos) -> {
            BlockState blockState;
            if (blockPos.equals(obsidianPos)) blockState = Blocks.OBSIDIAN.getDefaultState();
            else {
                blockState = mc.world.getBlockState(blockPos);
                if (blockState.getBlock() instanceof AnvilBlock && BBomber.fullAnvils.get())
                    blockState = Blocks.OBSIDIAN.getDefaultState();
                if (blockState.getBlock() instanceof EnderChestBlock && BBomber.fullEchest.get())
                    blockState = Blocks.OBSIDIAN.getDefaultState();
                if (blockState.getBlock().getBlastResistance() < 600 && ignoreTerrain)
                    blockState = Blocks.AIR.getDefaultState();
            }

            Vec3d vec3d = raycastContext.getStart();
            Vec3d vec3d2 = raycastContext.getEnd();

            VoxelShape voxelShape = raycastContext.getBlockShape(blockState, mc.world, blockPos);
            BlockHitResult blockHitResult = mc.world.raycastBlock(vec3d, vec3d2, blockPos, voxelShape, blockState);
            VoxelShape voxelShape2 = VoxelShapes.empty();
            BlockHitResult blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, blockPos);

            double d = blockHitResult == null ? Double.MAX_VALUE : raycastContext.getStart().squaredDistanceTo(blockHitResult.getPos());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : raycastContext.getStart().squaredDistanceTo(blockHitResult2.getPos());

            return d <= e ? blockHitResult : blockHitResult2;
        }, (raycastContext) -> {
            Vec3d vec3d = raycastContext.getStart().subtract(raycastContext.getEnd());
            return BlockHitResult.createMissed(raycastContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), new BlockPos(raycastContext.getEnd()));
        });
    }

    public static double possibleHealthReductions() {
        return possibleHealthReductions(true, true);
    }

    public static double possibleHealthReductions(boolean entities, boolean fall) {
        double damageTaken = 0;

        if (entities) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity && damageTaken < crystalDamageForTotem(mc.player, entity.getPos())) {
                    damageTaken = crystalDamageForTotem(mc.player, entity.getPos());
                } else if (entity instanceof PlayerEntity && damageTaken < getSwordDamage((PlayerEntity) entity, true)) {
                    if (!Friends.get().isFriend((PlayerEntity) entity) && mc.player.getPos().distanceTo(entity.getPos()) < 5) {
                        if (((PlayerEntity) entity).getActiveItem().getItem() instanceof SwordItem) {
                            damageTaken = getSwordDamage((PlayerEntity) entity, true);
                        }
                    }
                }
            }

            if (PlayerUtils.getDimension() != Dimension.Overworld) {
                for (BlockEntity blockEntity : Utils.blockEntities()) {
                    BlockPos bp = blockEntity.getPos();
                    Vec3d pos = new Vec3d(bp.getX(), bp.getY(), bp.getZ());

                    if (blockEntity instanceof BedBlockEntity && damageTaken < bedDamage(mc.player, pos)) {
                        damageTaken = bedDamage(mc.player, pos);
                    }
                }
            }
        }

        if (fall) {
            if (!Modules.get().isActive(NoFall.class) && mc.player.fallDistance > 3) {
                double damage = mc.player.fallDistance * 0.5;

                if (damage > damageTaken && !EntityUtils.isAboveWater(mc.player)) {
                    damageTaken = damage;
                }
            }
        }

        return damageTaken;
    }
}
