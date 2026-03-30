package com.lemoneater.theforgotten.block;

import com.lemoneater.theforgotten.TheForgotten;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModBlocks {

    // -- Palestone family --

    public static final Block PALESTONE = register(
            "palestone",
            Block::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.OFF_WHITE)
                    .sounds(BlockSoundGroup.STONE)
                    .requiresTool()
                    .strength(1.5f, 6.0f),
            true
    );

    public static final Block POLISHED_PALESTONE = register(
            "polished_palestone",
            Block::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.OFF_WHITE)
                    .sounds(BlockSoundGroup.STONE)
                    .requiresTool()
                    .strength(1.5f, 6.0f),
            true
    );

    public static final Block PALESTONE_BRICKS = register(
            "palestone_bricks",
            Block::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.OFF_WHITE)
                    .sounds(BlockSoundGroup.STONE)
                    .requiresTool()
                    .strength(1.5f, 6.0f),
            true
    );

    // -- Pale dirt (natural soil layer) --

    public static final Block PALE_DIRT = register(
            "pale_dirt",
            Block::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.OFF_WHITE)
                    .sounds(BlockSoundGroup.GRAVEL)
                    .strength(0.5f),
            true
    );

    // -- Portal --

    public static final Block FORGOTTEN_PORTAL = register(
            "forgotten_portal",
            ForgottenPortalBlock::new,
            AbstractBlock.Settings.create()
                    .noCollision()
                    .strength(-1.0f)
                    .dropsNothing()
                    .luminance(state -> 8)
                    .mapColor(MapColor.PALE_PURPLE),
            false // no block item for the portal
    );

    // -- Registration helper --

    private static Block register(String name, Function<AbstractBlock.Settings, Block> blockFactory, AbstractBlock.Settings settings, boolean shouldRegisterItem) {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(TheForgotten.MOD_ID, name));

        Block block = blockFactory.apply(settings.registryKey(blockKey));

        if (shouldRegisterItem) {
            RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(TheForgotten.MOD_ID, name));
            BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey));
            Registry.register(Registries.ITEM, itemKey, blockItem);
        }

        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    public static void initialize() {
        TheForgotten.LOGGER.info("Registering blocks for " + TheForgotten.MOD_ID);
    }
}
