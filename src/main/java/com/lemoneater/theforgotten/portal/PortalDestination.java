package com.lemoneater.theforgotten.portal;

import com.lemoneater.theforgotten.world.ModDimensions;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.world.World;

/**
 * Destination type for portal blocks. Determines where the portal sends you
 * and what color the portal renders as.
 */
public enum PortalDestination implements StringIdentifiable {
    FORGOTTEN("forgotten", null),           // Pale/blue — goes to The Forgotten
    OVERWORLD("overworld", World.OVERWORLD), // Green — goes to Overworld
    NETHER("nether", World.NETHER),          // Red — goes to Nether
    END("end", World.END);                   // Purple — goes to End

    public static final com.mojang.serialization.Codec<PortalDestination> CODEC = StringIdentifiable.createCodec(PortalDestination::values);

    private final String name;
    private final RegistryKey<World> targetDimension;

    PortalDestination(String name, RegistryKey<World> targetDimension) {
        this.name = name;
        this.targetDimension = targetDimension;
    }

    @Override
    public String asString() {
        return name;
    }

    /**
     * Get the dimension this portal sends you to.
     * For FORGOTTEN, returns The Forgotten dimension.
     * For others, returns the corresponding vanilla dimension.
     */
    public RegistryKey<World> getTargetDimension() {
        if (this == FORGOTTEN) {
            return ModDimensions.THE_FORGOTTEN_WORLD;
        }
        return targetDimension;
    }

    /**
     * Get the return destination for auto-built portals.
     * When entering The Forgotten from the Overworld, the return portal should be OVERWORLD.
     * When entering the Overworld from The Forgotten, the return portal should be FORGOTTEN.
     */
    public static PortalDestination getReturnDestination(RegistryKey<World> sourceDimension) {
        if (sourceDimension == ModDimensions.THE_FORGOTTEN_WORLD) {
            return FORGOTTEN;
        } else if (sourceDimension == World.OVERWORLD) {
            return OVERWORLD;
        } else if (sourceDimension == World.NETHER) {
            return NETHER;
        } else if (sourceDimension == World.END) {
            return END;
        }
        return OVERWORLD; // fallback
    }
}
