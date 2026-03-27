package com.lemoneater.theforgotten.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/**
 * Stores the origin dimension for a portal block.
 * When a portal is built from the Overworld, it stores "minecraft:overworld".
 * When stepped on in The Forgotten, the portal sends you back to the stored origin.
 */
public class ForgottenPortalBlockEntity extends BlockEntity {

    private static final String ORIGIN_DIMENSION_KEY = "origin_dimension";

    @Nullable
    private RegistryKey<World> originDimension;

    public ForgottenPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORGOTTEN_PORTAL, pos, state);
    }

    @Nullable
    public RegistryKey<World> getOriginDimension() {
        return originDimension;
    }

    public void setOriginDimension(RegistryKey<World> dimension) {
        this.originDimension = dimension;
        markDirty();
    }

    @Override
    protected void readData(ReadView readView) {
        readView.read(ORIGIN_DIMENSION_KEY, Identifier.CODEC).ifPresent(id -> {
            this.originDimension = RegistryKey.of(RegistryKeys.WORLD, id);
        });
    }

    @Override
    protected void writeData(WriteView writeView) {
        if (originDimension != null) {
            writeView.put(ORIGIN_DIMENSION_KEY, Identifier.CODEC, originDimension.getValue());
        }
    }
}
