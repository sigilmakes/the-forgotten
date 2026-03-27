package com.lemoneater.theforgotten.block;

import com.lemoneater.theforgotten.TheForgotten;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static final BlockEntityType<ForgottenPortalBlockEntity> FORGOTTEN_PORTAL =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    RegistryKey.of(RegistryKeys.BLOCK_ENTITY_TYPE, Identifier.of(TheForgotten.MOD_ID, "forgotten_portal")),
                    FabricBlockEntityTypeBuilder.create(ForgottenPortalBlockEntity::new, ModBlocks.FORGOTTEN_PORTAL).build()
            );

    public static void initialize() {
        TheForgotten.LOGGER.info("Registering block entities for " + TheForgotten.MOD_ID);
    }
}
