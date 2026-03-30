package com.lemoneater.theforgotten.portal;

import com.lemoneater.theforgotten.block.ForgottenPortalBlock;
import com.lemoneater.theforgotten.block.ModBlocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Detects rectangular frames of reinforced deepslate and fills them with portal blocks.
 * Works similarly to nether portal frame detection but uses reinforced deepslate.
 */
public class PortalHelper {

    private static final int MIN_WIDTH = 2;
    private static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_HEIGHT = 21;

    /**
     * Try to light a portal from the given interior position.
     * Checks both X and Z axis orientations.
     */
    public static boolean tryLightPortal(World world, BlockPos pos, PortalDestination destination) {
        Optional<PortalFrame> frameX = detectFrame(world, pos, Direction.Axis.X);
        if (frameX.isPresent()) {
            fillPortal(world, frameX.get(), destination);
            return true;
        }

        Optional<PortalFrame> frameZ = detectFrame(world, pos, Direction.Axis.Z);
        if (frameZ.isPresent()) {
            fillPortal(world, frameZ.get(), destination);
            return true;
        }

        return false;
    }

    private static Optional<PortalFrame> detectFrame(World world, BlockPos pos, Direction.Axis axis) {
        Direction widthDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

        // Find the bottom of the interior
        BlockPos bottomPos = pos;
        for (int i = 0; i < MAX_HEIGHT; i++) {
            BlockPos below = bottomPos.down();
            if (isFrameBlock(world, below)) break;
            if (!isInteriorBlock(world, below)) return Optional.empty();
            bottomPos = below;
        }

        // Find the left edge
        BlockPos leftPos = bottomPos;
        for (int i = 0; i < MAX_WIDTH; i++) {
            BlockPos next = leftPos.offset(widthDir.getOpposite());
            if (isFrameBlock(world, next)) break;
            if (!isInteriorBlock(world, next)) return Optional.empty();
            leftPos = next;
        }

        // Measure width
        int width = 0;
        for (int i = 0; i < MAX_WIDTH; i++) {
            BlockPos check = leftPos.offset(widthDir, i);
            if (isFrameBlock(world, check)) break;
            if (!isInteriorBlock(world, check)) return Optional.empty();
            width++;
        }
        if (width < MIN_WIDTH || width > MAX_WIDTH) return Optional.empty();

        // Measure height
        int height = 0;
        for (int i = 0; i < MAX_HEIGHT; i++) {
            BlockPos check = leftPos.up(i);
            if (isFrameBlock(world, check)) break;
            if (!isInteriorBlock(world, check)) return Optional.empty();
            height++;
        }
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) return Optional.empty();

        // Validate frame walls
        for (int w = 0; w < width; w++) {
            if (!isFrameBlock(world, leftPos.offset(widthDir, w).down())) return Optional.empty();
            if (!isFrameBlock(world, leftPos.offset(widthDir, w).up(height))) return Optional.empty();
        }
        for (int h = 0; h < height; h++) {
            if (!isFrameBlock(world, leftPos.up(h).offset(widthDir.getOpposite()))) return Optional.empty();
            if (!isFrameBlock(world, leftPos.offset(widthDir, width).up(h))) return Optional.empty();
        }

        // Validate corners
        if (!isFrameBlock(world, leftPos.offset(widthDir.getOpposite()).down())
                || !isFrameBlock(world, leftPos.offset(widthDir, width).down())
                || !isFrameBlock(world, leftPos.offset(widthDir.getOpposite()).up(height))
                || !isFrameBlock(world, leftPos.offset(widthDir, width).up(height))) {
            return Optional.empty();
        }

        // Verify all interior blocks are air/replaceable
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                if (!isInteriorBlock(world, leftPos.offset(widthDir, w).up(h))) return Optional.empty();
            }
        }

        return Optional.of(new PortalFrame(leftPos, width, height, axis, widthDir));
    }

    private static void fillPortal(World world, PortalFrame frame, PortalDestination destination) {
        BlockState portalState = ModBlocks.FORGOTTEN_PORTAL.getDefaultState()
                .with(ForgottenPortalBlock.AXIS, frame.axis)
                .with(ForgottenPortalBlock.DESTINATION, destination);

        for (int h = 0; h < frame.height; h++) {
            for (int w = 0; w < frame.width; w++) {
                BlockPos portalPos = frame.bottomLeft.offset(frame.widthDir, w).up(h);
                world.setBlockState(portalPos, portalState);
            }
        }
    }

    /**
     * Search for an existing portal block near the given position in the target world.
     * Only matches portals with the specified destination type.
     */
    public static BlockPos findNearestPortal(ServerWorld world, BlockPos around, PortalDestination destination) {
        // Scale search radius by target dimension's coordinate_scale, matching vanilla behavior.
        // Overworld/End (scale 1.0) → 128 blocks, Nether/Forgotten (scale 8.0) → 16 blocks.
        // Prevents cross-linking when coordinate scaling compresses distant source portals
        // into nearby target positions.
        int searchRadius = Math.max(16, (int) (128 / world.getDimension().coordinateScale()));
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        int minY = world.getBottomY();
        int maxY = world.getTopYInclusive();

        for (int r = 0; r <= searchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int x = around.getX() + dx;
                    int z = around.getZ() + dz;

                    for (int y = minY; y <= maxY; y++) {
                        BlockPos check = new BlockPos(x, y, z);
                        BlockState state = world.getBlockState(check);
                        if (state.isOf(ModBlocks.FORGOTTEN_PORTAL)
                                && state.get(ForgottenPortalBlock.DESTINATION) == destination) {
                            double dist = check.getSquaredDistance(around);
                            if (dist < nearestDist) {
                                nearestDist = dist;
                                nearest = check;
                            }
                        }
                    }
                }
            }
            if (nearest != null) break;
        }

        return nearest;
    }

    /**
     * Find a safe standing position adjacent to a portal block.
     */
    public static BlockPos findPositionBesidePortal(ServerWorld world, BlockPos portalPos) {
        Direction.Axis axis = Direction.Axis.X;
        BlockState portalState = world.getBlockState(portalPos);
        if (portalState.isOf(ModBlocks.FORGOTTEN_PORTAL)) {
            axis = portalState.get(ForgottenPortalBlock.AXIS);
        }

        Direction[] exits = axis == Direction.Axis.X
                ? new Direction[]{Direction.NORTH, Direction.SOUTH}
                : new Direction[]{Direction.EAST, Direction.WEST};

        for (Direction dir : exits) {
            BlockPos candidate = portalPos.offset(dir);
            if (world.getBlockState(candidate).isAir()
                    && world.getBlockState(candidate.up()).isAir()) {
                return candidate;
            }
        }

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos candidate = portalPos.offset(dir);
            if (world.getBlockState(candidate).isAir()
                    && world.getBlockState(candidate.up()).isAir()) {
                return candidate;
            }
        }

        return portalPos.offset(exits[0]);
    }

    /**
     * Build a minimal 2×3 portal frame of reinforced deepslate and fill it with portal blocks.
     */
    public static BlockPos buildPortalFrame(ServerWorld world, BlockPos bottomLeft, Direction.Axis axis, PortalDestination destination) {
        Direction widthDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        int width = 2;
        int height = 3;

        BlockState frameBlock = Blocks.REINFORCED_DEEPSLATE.getDefaultState();
        BlockState portalState = ModBlocks.FORGOTTEN_PORTAL.getDefaultState()
                .with(ForgottenPortalBlock.AXIS, axis)
                .with(ForgottenPortalBlock.DESTINATION, destination);

        // Clear interior
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                BlockPos p = bottomLeft.offset(widthDir, w).up(h);
                world.setBlockState(p, Blocks.AIR.getDefaultState());
            }
        }

        // Bottom wall
        for (int w = -1; w <= width; w++) {
            world.setBlockState(bottomLeft.offset(widthDir, w).down(), frameBlock);
        }

        // Top wall
        for (int w = -1; w <= width; w++) {
            world.setBlockState(bottomLeft.offset(widthDir, w).up(height), frameBlock);
        }

        // Left wall
        for (int h = -1; h <= height; h++) {
            world.setBlockState(bottomLeft.offset(widthDir, -1).up(h), frameBlock);
        }

        // Right wall
        for (int h = -1; h <= height; h++) {
            world.setBlockState(bottomLeft.offset(widthDir, width).up(h), frameBlock);
        }

        // Fill portal interior
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                BlockPos portalPos = bottomLeft.offset(widthDir, w).up(h);
                world.setBlockState(portalPos, portalState);
            }
        }

        // Ensure a solid platform under the entire frame + exits
        Direction[] exits = axis == Direction.Axis.X
                ? new Direction[]{Direction.NORTH, Direction.SOUTH}
                : new Direction[]{Direction.EAST, Direction.WEST};

        // Platform under frame and exit areas
        for (int w = -2; w <= width + 1; w++) {
            BlockPos floorBase = bottomLeft.offset(widthDir, w).down();
            if (world.getBlockState(floorBase).isAir()) {
                world.setBlockState(floorBase, ModBlocks.PALESTONE.getDefaultState());
            }
            for (Direction dir : exits) {
                BlockPos exitFloor = floorBase.offset(dir);
                if (world.getBlockState(exitFloor).isAir()) {
                    world.setBlockState(exitFloor, ModBlocks.PALESTONE.getDefaultState());
                }
            }
        }

        // Clear air beside the portal so entities can step out
        for (Direction dir : exits) {
            for (int h = 0; h < 2; h++) {
                BlockPos exitPos = bottomLeft.offset(dir).up(h);
                if (!world.getBlockState(exitPos).isAir()) {
                    world.setBlockState(exitPos, Blocks.AIR.getDefaultState());
                }
            }
        }

        return bottomLeft;
    }

    private static boolean isFrameBlock(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(Blocks.REINFORCED_DEEPSLATE);
    }

    private static boolean isInteriorBlock(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || state.isOf(ModBlocks.FORGOTTEN_PORTAL);
    }

    private record PortalFrame(BlockPos bottomLeft, int width, int height, Direction.Axis axis, Direction widthDir) {
    }
}
