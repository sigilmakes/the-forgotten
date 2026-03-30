package com.lemoneater.theforgotten.block;

import com.lemoneater.theforgotten.portal.PortalDestination;
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
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.block.WireOrientation;

import org.jetbrains.annotations.Nullable;

public class ForgottenPortalBlock extends Block {

    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;
    public static final EnumProperty<PortalDestination> DESTINATION = EnumProperty.of("destination", PortalDestination.class);

    protected static final VoxelShape X_SHAPE = Block.createCuboidShape(0, 0, 6, 16, 16, 10);
    protected static final VoxelShape Z_SHAPE = Block.createCuboidShape(6, 0, 0, 10, 16, 16);

    public ForgottenPortalBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(AXIS, Direction.Axis.X)
                .with(DESTINATION, PortalDestination.FORGOTTEN));
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(AXIS)) {
            case Z -> Z_SHAPE;
            default -> X_SHAPE;
        };
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler collisionHandler, boolean piercingMovement) {
        if (world.isClient() || entity.hasPortalCooldown()) {
            return;
        }

        if (!(world instanceof ServerWorld serverWorld)) return;
        MinecraftServer server = serverWorld.getServer();

        PortalDestination destination = state.get(DESTINATION);
        RegistryKey<World> targetKey = destination.getTargetDimension();

        ServerWorld targetWorld = server.getWorld(targetKey);
        if (targetWorld == null) return;

        // Apply coordinate scaling for portal search position
        double scale = DimensionType.getCoordinateScaleFactor(
                serverWorld.getDimension(), targetWorld.getDimension());
        int targetX = (int) (entity.getX() * scale);
        int targetZ = (int) (entity.getZ() * scale);
        BlockPos searchPos = new BlockPos(targetX, entity.getBlockY(), targetZ);

        // Determine what destination the return portal should have
        PortalDestination returnDestination = PortalDestination.getReturnDestination(world.getRegistryKey());

        // Get the axis of the portal we're standing in
        Direction.Axis sourceAxis = state.get(AXIS);

        // Search for an existing portal with matching destination in the target dimension
        BlockPos targetPos;
        BlockPos nearestPortal = PortalHelper.findNearestPortal(targetWorld, searchPos, returnDestination);

        if (nearestPortal != null) {
            // Found an existing return portal — land beside it
            targetPos = PortalHelper.findPositionBesidePortal(targetWorld, nearestPortal);
        } else {
            // No existing portal — find safe ground and build one
            BlockPos safePos = findSafePosition(targetWorld, searchPos);
            PortalHelper.buildPortalFrame(targetWorld, safePos, sourceAxis, returnDestination);
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

        BlockPos result = searchColumnForSafeSpot(world, x, z);
        if (result != null) return result;

        // If the exact column is solid, try nearby offsets in a spiral
        for (int radius = 1; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
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
        for (int dy = 1; dy <= 2; dy++) {
            world.setBlockState(platformPos.up(dy), net.minecraft.block.Blocks.AIR.getDefaultState());
        }
        return platformPos.up();
    }

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
        Direction.Axis axis = state.get(AXIS);

        boolean hasVerticalSupport = isFrameOrPortal(world, pos.up()) || isFrameOrPortal(world, pos.down());
        boolean hasHorizontalSupport = axis == Direction.Axis.X
                ? isFrameOrPortal(world, pos.east()) || isFrameOrPortal(world, pos.west())
                : isFrameOrPortal(world, pos.north()) || isFrameOrPortal(world, pos.south());

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
        builder.add(AXIS, DESTINATION);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Ambient sound — rare sculk catalyst bloom
        if (random.nextInt(200) == 0) {
            world.playSoundAtBlockCenterClient(
                    pos,
                    SoundEvents.BLOCK_SCULK_CATALYST_BLOOM,
                    SoundCategory.BLOCKS,
                    0.3f, 0.4f + random.nextFloat() * 0.2f, false
            );
        }

        // Destination-colored particles
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();

            double vx = (random.nextDouble() - 0.5) * 0.02;
            double vy = random.nextDouble() * 0.04 + 0.01;
            double vz = (random.nextDouble() - 0.5) * 0.02;

            PortalDestination destination = state.get(DESTINATION);
            switch (destination) {
                case OVERWORLD -> world.addParticleClient(ParticleTypes.HAPPY_VILLAGER, x, y, z, vx, vy, vz);
                case NETHER -> world.addParticleClient(ParticleTypes.SMALL_FLAME, x, y, z, vx, vy * 0.5, vz);
                case END -> world.addParticleClient(ParticleTypes.PORTAL, x, y, z, vx, vy, vz);
                default -> world.addParticleClient(ParticleTypes.SCULK_SOUL, x, y, z, vx, vy, vz);
            }
        }

        // Enchant glyphs
        if (random.nextInt(8) == 0) {
            Direction.Axis axis = state.get(AXIS);
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();

            double vx = (random.nextDouble() - 0.5) * 0.05;
            double vy = random.nextDouble() * 0.02;
            double vz = (random.nextDouble() - 0.5) * 0.05;

            if (axis == Direction.Axis.X) {
                vz += (random.nextBoolean() ? 1 : -1) * 0.03;
            } else {
                vx += (random.nextBoolean() ? 1 : -1) * 0.03;
            }

            world.addParticleClient(ParticleTypes.ENCHANT, x, y, z, vx, vy, vz);
        }
    }
}
