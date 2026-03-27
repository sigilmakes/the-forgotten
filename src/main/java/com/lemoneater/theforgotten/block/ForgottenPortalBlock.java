package com.lemoneater.theforgotten.block;

import com.lemoneater.theforgotten.portal.PortalHelper;
import com.lemoneater.theforgotten.world.ModDimensions;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
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
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler collisionHandler, boolean piercingMovement) {
        if (world.isClient() || entity.hasPortalCooldown()) {
            return;
        }

        if (!(world instanceof ServerWorld serverWorld)) return;
        MinecraftServer server = serverWorld.getServer();

        // Determine target dimension
        RegistryKey<World> targetKey;
        if (world.getRegistryKey() == ModDimensions.THE_FORGOTTEN_WORLD) {
            targetKey = World.OVERWORLD;
        } else {
            targetKey = ModDimensions.THE_FORGOTTEN_WORLD;
        }

        ServerWorld targetWorld = server.getWorld(targetKey);
        if (targetWorld == null) return;

        // Get the axis of the portal we're standing in
        Direction.Axis sourceAxis = state.get(AXIS);

        // Try to find an existing portal in the target dimension
        BlockPos targetPos;
        BlockPos nearestPortal = PortalHelper.findNearestPortal(targetWorld, entity.getBlockPos());

        if (nearestPortal != null) {
            // Found an existing portal — land beside it
            targetPos = PortalHelper.findPositionBesidePortal(targetWorld, nearestPortal);
        } else {
            // No existing portal — find safe ground and build one
            BlockPos safePos = findSafePosition(targetWorld, entity.getBlockPos());
            PortalHelper.buildPortalFrame(targetWorld, safePos, sourceAxis);
            targetPos = PortalHelper.findPositionBesidePortal(targetWorld, safePos);
        }

        // Players get the travel-through-portal packet for screen effects
        TeleportTarget.PostDimensionTransition postTransition =
                entity instanceof ServerPlayerEntity
                        ? TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET
                        : TeleportTarget.NO_OP;

        entity.teleportTo(new TeleportTarget(
                targetWorld,
                Vec3d.ofBottomCenter(targetPos),
                Vec3d.ZERO,
                entity.getYaw(),
                entity.getPitch(),
                postTransition
        ));

        // Cooldown prevents immediate re-teleport (80 ticks = 4 seconds)
        entity.setPortalCooldown(80);
    }

    private BlockPos findSafePosition(ServerWorld world, BlockPos sourcePos) {
        int x = sourcePos.getX();
        int z = sourcePos.getZ();

        // Search upward from the cavern floor to find standing room inside the caves.
        // The dimension is 0-192 with bedrock at 0-5 and 187-192. Caverns live ~y24-160.
        // Look for: solid block with 2 air blocks above (room to stand).
        BlockPos result = searchColumnForSafeSpot(world, x, z);
        if (result != null) return result;

        // If the exact column is solid, try nearby offsets in a spiral
        for (int radius = 1; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue; // only perimeter
                    result = searchColumnForSafeSpot(world, x + dx, z + dz);
                    if (result != null) return result;
                }
            }
        }

        // Last resort — create a small palestone platform at y=64
        BlockPos platformPos = new BlockPos(x, 64, z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(platformPos.add(dx, 0, dz), ModBlocks.PALESTONE.getDefaultState());
            }
        }
        // Clear 2 blocks of air above the platform
        for (int dy = 1; dy <= 2; dy++) {
            world.setBlockState(platformPos.up(dy), net.minecraft.block.Blocks.AIR.getDefaultState());
        }
        return platformPos.up();
    }

    /**
     * Search a single column upward for a safe standing position.
     * Returns the position to stand on (1 above the solid block), or null if none found.
     */
    private BlockPos searchColumnForSafeSpot(ServerWorld world, int x, int z) {
        for (int y = world.getBottomY() + 6; y <= world.getTopYInclusive() - 2; y++) {
            BlockPos floor = new BlockPos(x, y, z);
            BlockPos feet = floor.up();
            BlockPos head = floor.up(2);

            if (!world.getBlockState(floor).isAir()
                    && world.getBlockState(feet).isAir()
                    && world.getBlockState(head).isAir()) {
                return feet;
            }
        }
        return null;
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
            world.playSoundAtBlockCenterClient(
                    pos,
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
            world.addParticleClient(ParticleTypes.SCULK_SOUL, x, y, z, vx, vy, vz);
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

            world.addParticleClient(ParticleTypes.ENCHANT, x, y, z, vx, vy, vz);
        }
    }

    // Block is registered as translucent via BlockRenderLayerMap in TheForgottenClient
}
