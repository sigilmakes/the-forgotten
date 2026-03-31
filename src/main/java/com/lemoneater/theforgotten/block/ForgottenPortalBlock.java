package com.lemoneater.theforgotten.block;

import com.lemoneater.theforgotten.world.ModDimensions;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;

import org.jetbrains.annotations.Nullable;

public class ForgottenPortalBlock extends Block {

    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

    protected static final VoxelShape X_SHAPE = Block.createCuboidShape(0, 0, 6, 16, 16, 10);
    protected static final VoxelShape Z_SHAPE = Block.createCuboidShape(6, 0, 0, 10, 16, 16);

    public ForgottenPortalBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AXIS, Direction.Axis.X));
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        switch (state.get(AXIS)) {
            case Z:
                return Z_SHAPE;
            case X:
            default:
                return X_SHAPE;
        }
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient() || entity.hasPortalCooldown()) {
            return;
        }

        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;

        // Determine target dimension
        RegistryKey<World> targetKey;
        if (world.getRegistryKey() == ModDimensions.THE_FORGOTTEN_WORLD) {
            targetKey = World.OVERWORLD;
        } else {
            targetKey = ModDimensions.THE_FORGOTTEN_WORLD;
        }

        ServerWorld targetWorld = server.getWorld(targetKey);
        if (targetWorld == null) return;

        // Find a safe position in the target dimension
        BlockPos targetPos = findSafePosition(targetWorld, player.getBlockPos());

        player.teleportTo(new TeleportTarget(
                targetWorld,
                Vec3d.ofBottomCenter(targetPos),
                Vec3d.ZERO,
                player.getYaw(),
                player.getPitch(),
                TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET
        ));

        // Cooldown prevents immediate re-teleport (80 ticks = 4 seconds)
        player.setPortalCooldown(80);
    }

    private BlockPos findSafePosition(ServerWorld world, BlockPos sourcePos) {
        int x = sourcePos.getX();
        int z = sourcePos.getZ();

        // For ceiling dimensions (The Forgotten, Nether-style), search BOTTOM-UP.
        // Top-down finds the roof surface; bottom-up finds the cavern floor.
        // For open dimensions (Overworld), search TOP-DOWN to find the surface.
        boolean hasCeiling = !world.getDimension().hasSkyLight();

        if (hasCeiling) {
            return findSafePositionCavern(world, x, z);
        } else {
            return findSafePositionSurface(world, x, z);
        }
    }

    /**
     * Bottom-up search for ceiling dimensions (nether-style caverns).
     * Finds the first cavern floor: solid block with 2 air blocks above.
     */
    private BlockPos findSafePositionCavern(ServerWorld world, int x, int z) {
        for (int y = world.getBottomY(); y <= world.getTopYInclusive() - 2; y++) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockPos abovePos = checkPos.up();
            BlockPos above2Pos = checkPos.up(2);

            if (!world.getBlockState(checkPos).isAir()
                    && world.getBlockState(abovePos).isAir()
                    && world.getBlockState(above2Pos).isAir()) {
                return abovePos;
            }
        }

        // No cavern found — search a small area around the target for any opening
        for (int radius = 1; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue; // only check perimeter
                    for (int y = world.getBottomY(); y <= world.getTopYInclusive() - 2; y++) {
                        BlockPos checkPos = new BlockPos(x + dx, y, z + dz);
                        if (!world.getBlockState(checkPos).isAir()
                                && world.getBlockState(checkPos.up()).isAir()
                                && world.getBlockState(checkPos.up(2)).isAir()) {
                            return checkPos.up();
                        }
                    }
                }
            }
        }

        // Absolute fallback — carve a small room at Y=32
        return createPlatform(world, x, 32, z);
    }

    /**
     * Top-down search for open dimensions (overworld-style).
     * Finds the surface: first solid block from the top with 2 air blocks above.
     */
    private BlockPos findSafePositionSurface(ServerWorld world, int x, int z) {
        for (int y = world.getTopYInclusive(); y >= world.getBottomY(); y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockPos abovePos = checkPos.up();
            BlockPos above2Pos = checkPos.up(2);

            if (!world.getBlockState(checkPos).isAir()
                    && world.getBlockState(abovePos).isAir()
                    && world.getBlockState(above2Pos).isAir()) {
                return abovePos;
            }
        }

        // No safe spot found — create a small platform at sea level
        return createPlatform(world, x, 64, z);
    }

    /**
     * Emergency fallback: create a 3x3 palestone platform and clear space above.
     */
    private BlockPos createPlatform(ServerWorld world, int x, int y, int z) {
        BlockPos platformPos = new BlockPos(x, y, z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(platformPos.add(dx, 0, dz), ModBlocks.PALESTONE.getDefaultState());
                // Clear 2 blocks above for headroom
                world.setBlockState(platformPos.add(dx, 1, dz), net.minecraft.block.Blocks.AIR.getDefaultState());
                world.setBlockState(platformPos.add(dx, 2, dz), net.minecraft.block.Blocks.AIR.getDefaultState());
            }
        }
        return platformPos.up();
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        // Break the portal if the frame is no longer valid
        Direction.Axis axis = state.get(AXIS);

        // Simple check: if any adjacent frame position is no longer reinforced deepslate
        // and not another portal block, break this portal block
        boolean hasVerticalSupport = isFrameOrPortal(world, pos.up()) || isFrameOrPortal(world, pos.down());
        boolean hasHorizontalSupport = axis == Direction.Axis.X
                ? isFrameOrPortal(world, pos.north()) || isFrameOrPortal(world, pos.south())
                : isFrameOrPortal(world, pos.east()) || isFrameOrPortal(world, pos.west());

        if (!hasVerticalSupport && !hasHorizontalSupport) {
            world.breakBlock(pos, false);
        }
    }

    private boolean isFrameOrPortal(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof ForgottenPortalBlock
                || state.isOf(net.minecraft.block.Blocks.REINFORCED_DEEPSLATE);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Ambient sound — low, eerie hum (rarer than nether portal)
        if (random.nextInt(200) == 0) {
            world.playSound(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_SCULK_CATALYST_BLOOM,
                    SoundCategory.BLOCKS,
                    0.3f, 0.4f + random.nextFloat() * 0.2f, false
            );
        }

        // Sparse pale particles drifting upward — dust motes in old light
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();

            // Slow upward drift with slight horizontal wander
            double vx = (random.nextDouble() - 0.5) * 0.02;
            double vy = random.nextDouble() * 0.04 + 0.01;
            double vz = (random.nextDouble() - 0.5) * 0.02;

            // Sculk soul particles — pale blue-green, ethereal
            world.addParticle(ParticleTypes.SCULK_SOUL, x, y, z, vx, vy, vz);
        }

        // Occasional enchant glyphs floating outward from the portal face
        if (random.nextInt(8) == 0) {
            Direction.Axis axis = state.get(AXIS);
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();

            double vx = (random.nextDouble() - 0.5) * 0.05;
            double vy = random.nextDouble() * 0.02;
            double vz = (random.nextDouble() - 0.5) * 0.05;

            // Push particles outward from the portal face
            if (axis == Direction.Axis.X) {
                vz += (random.nextBoolean() ? 1 : -1) * 0.03;
            } else {
                vx += (random.nextBoolean() ? 1 : -1) * 0.03;
            }

            world.addParticle(ParticleTypes.ENCHANT, x, y, z, vx, vy, vz);
        }
    }

    // Block is registered as translucent via BlockRenderLayerMap in TheForgottenClient
}
