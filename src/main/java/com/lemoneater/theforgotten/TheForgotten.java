package com.lemoneater.theforgotten;

import com.lemoneater.theforgotten.block.ModBlocks;
import com.lemoneater.theforgotten.portal.PortalDestination;
import com.lemoneater.theforgotten.portal.PortalHelper;
import com.lemoneater.theforgotten.world.ModDimensions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheForgotten implements ModInitializer {
    public static final String MOD_ID = "the-forgotten";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Loading The Forgotten!");

        ModBlocks.initialize();

        registerPortalActivation();
    }

    private void registerPortalActivation() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }

            // Must click on reinforced deepslate
            BlockPos clickedPos = hitResult.getBlockPos();
            if (!world.getBlockState(clickedPos).isOf(Blocks.REINFORCED_DEEPSLATE)) {
                return ActionResult.PASS;
            }

            ItemStack heldItem = player.getStackInHand(hand);

            // Determine portal destination based on item + current dimension
            PortalDestination destination = getActivationDestination(world, heldItem);
            if (destination == null) {
                return ActionResult.PASS;
            }

            // Get the interior position (block adjacent to the clicked face)
            BlockPos interiorPos = clickedPos.offset(hitResult.getSide());

            // Try to light the portal
            if (PortalHelper.tryLightPortal(world, interiorPos, destination)) {
                // Consume or damage the activation item (unless creative)
                if (!player.isCreative()) {
                    if (heldItem.isOf(Items.FLINT_AND_STEEL)) {
                        heldItem.damage(1, player);
                    } else {
                        heldItem.decrement(1);
                    }
                }

                // Play activation sound
                world.playSound(null, clickedPos,
                        SoundEvents.BLOCK_END_PORTAL_SPAWN,
                        SoundCategory.BLOCKS,
                        1.0f, 1.0f);

                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });
    }

    /**
     * Determine portal destination based on current dimension and held item.
     *
     * Outside The Forgotten: echo shard → FORGOTTEN
     * Inside The Forgotten:
     *   echo shard → OVERWORLD
     *   flint & steel → NETHER
     *   eye of ender → END
     */
    private PortalDestination getActivationDestination(net.minecraft.world.World world, ItemStack heldItem) {
        boolean inForgotten = world.getRegistryKey() == ModDimensions.THE_FORGOTTEN_WORLD;

        if (inForgotten) {
            if (heldItem.isOf(Items.ECHO_SHARD)) return PortalDestination.OVERWORLD;
            if (heldItem.isOf(Items.FLINT_AND_STEEL)) return PortalDestination.NETHER;
            if (heldItem.isOf(Items.ENDER_EYE)) return PortalDestination.END;
        } else {
            if (heldItem.isOf(Items.ECHO_SHARD)) return PortalDestination.FORGOTTEN;
        }

        return null;
    }
}
