package com.lemoneater.theforgotten.portal;

import com.lemoneater.theforgotten.block.ModBlocks;
import com.lemoneater.theforgotten.block.ForgottenPortalBlock;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
     *
     * @param world The world
     * @param pos   A position that should be inside the portal frame (air block)
     * @return true if a portal was successfully created
     */
    public static boolean tryLightPortal(World world, BlockPos pos) {
        // Try X axis (portal faces east-west, frame extends in Y and Z)
        Optional<PortalFrame> frameX = detectFrame(world, pos, Direction.Axis.X);
        if (frameX.isPresent()) {
            fillPortal(world, frameX.get());
            return true;
        }

        // Try Z axis (portal faces north-south, frame extends in Y and X)
        Optional<PortalFrame> frameZ = detectFrame(world, pos, Direction.Axis.Z);
        if (frameZ.isPresent()) {
            fillPortal(world, frameZ.get());
            return true;
        }

        return false;
    }

    /**
     * Detect a rectangular frame of reinforced deepslate around the given position.
     *
     * @param world The world
     * @param pos   A position inside the potential frame
     * @param axis  The axis the portal faces (X = portal in YZ plane, Z = portal in YX plane)
     * @return The detected frame, or empty if no valid frame found
     */
    private static Optional<PortalFrame> detectFrame(World world, BlockPos pos, Direction.Axis axis) {
        // The width direction depends on the portal axis
        // X-axis portal: width runs along Z
        // Z-axis portal: width runs along X
        Direction widthDir = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;

        // Find the bottom of the interior (walk down until we hit frame or go too far)
        BlockPos bottomPos = pos;
        for (int i = 0; i < MAX_HEIGHT; i++) {
            BlockPos below = bottomPos.down();
            if (isFrameBlock(world, below)) {
                break;
            }
            if (!isInteriorBlock(world, below)) {
                return Optional.empty();
            }
            bottomPos = below;
        }

        // Find the left edge (walk in negative width direction until we hit frame)
        BlockPos leftPos = bottomPos;
        for (int i = 0; i < MAX_WIDTH; i++) {
            BlockPos next = leftPos.offset(widthDir.getOpposite());
            if (isFrameBlock(world, next)) {
                break;
            }
            if (!isInteriorBlock(world, next)) {
                return Optional.empty();
            }
            leftPos = next;
        }

        // Measure width (walk in positive width direction from leftPos)
        int width = 0;
        for (int i = 0; i < MAX_WIDTH; i++) {
            BlockPos check = leftPos.offset(widthDir, i);
            if (isFrameBlock(world, check)) {
                break;
            }
            if (!isInteriorBlock(world, check)) {
                return Optional.empty();
            }
            width++;
        }

        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            return Optional.empty();
        }

        // Measure height (walk up from leftPos)
        int height = 0;
        for (int i = 0; i < MAX_HEIGHT; i++) {
            BlockPos check = leftPos.up(i);
            if (isFrameBlock(world, check)) {
                break;
            }
            if (!isInteriorBlock(world, check)) {
                return Optional.empty();
            }
            height++;
        }

        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            return Optional.empty();
        }

        // Validate the complete frame
        // Bottom wall: width blocks of reinforced deepslate below the interior
        for (int w = 0; w < width; w++) {
            if (!isFrameBlock(world, leftPos.offset(widthDir, w).down())) {
                return Optional.empty();
            }
        }

        // Top wall: width blocks of reinforced deepslate above the interior
        for (int w = 0; w < width; w++) {
            if (!isFrameBlock(world, leftPos.offset(widthDir, w).up(height))) {
                return Optional.empty();
            }
        }

        // Left wall: height blocks of reinforced deepslate
        for (int h = 0; h < height; h++) {
            if (!isFrameBlock(world, leftPos.up(h).offset(widthDir.getOpposite()))) {
                return Optional.empty();
            }
        }

        // Right wall: height blocks of reinforced deepslate
        for (int h = 0; h < height; h++) {
            if (!isFrameBlock(world, leftPos.offset(widthDir, width).up(h))) {
                return Optional.empty();
            }
        }

        // Corners (all four)
        if (!isFrameBlock(world, leftPos.offset(widthDir.getOpposite()).down()) // bottom-left
                || !isFrameBlock(world, leftPos.offset(widthDir, width).down()) // bottom-right
                || !isFrameBlock(world, leftPos.offset(widthDir.getOpposite()).up(height)) // top-left
                || !isFrameBlock(world, leftPos.offset(widthDir, width).up(height))) { // top-right
            return Optional.empty();
        }

        // Verify all interior blocks are air/replaceable
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                if (!isInteriorBlock(world, leftPos.offset(widthDir, w).up(h))) {
                    return Optional.empty();
                }
            }
        }

        return Optional.of(new PortalFrame(leftPos, width, height, axis, widthDir));
    }

    private static void fillPortal(World world, PortalFrame frame) {
        BlockState portalState = ModBlocks.FORGOTTEN_PORTAL.getDefaultState()
                .with(ForgottenPortalBlock.AXIS, frame.axis);

        for (int h = 0; h < frame.height; h++) {
            for (int w = 0; w < frame.width; w++) {
                BlockPos portalPos = frame.bottomLeft.offset(frame.widthDir, w).up(h);
                world.setBlockState(portalPos, portalState);
            }
        }
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
