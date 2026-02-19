package com.lemoneater.theforgotten;

import com.lemoneater.theforgotten.block.ModBlocks;
import com.lemoneater.theforgotten.portal.PortalHelper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

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

            // Must be using main hand with an echo shard
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }

            ItemStack heldItem = player.getStackInHand(hand);
            if (!heldItem.isOf(Items.ECHO_SHARD)) {
                return ActionResult.PASS;
            }

            // Must click on reinforced deepslate
            BlockPos clickedPos = hitResult.getBlockPos();
            if (!world.getBlockState(clickedPos).isOf(Blocks.REINFORCED_DEEPSLATE)) {
                return ActionResult.PASS;
            }

            // Get the interior position (block adjacent to the clicked face)
            BlockPos interiorPos = clickedPos.offset(hitResult.getSide());

            // Try to light the portal
            if (PortalHelper.tryLightPortal(world, interiorPos)) {
                // Consume the echo shard (unless creative mode)
                if (!player.isCreative()) {
                    heldItem.decrement(1);
                }

                // Play a dramatic sound
                world.playSound(null, clickedPos,
                        SoundEvents.BLOCK_END_PORTAL_SPAWN,
                        SoundCategory.BLOCKS,
                        1.0f, 1.0f);

                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });
    }
}
