package com.lemoneater.theforgotten.world;

import com.lemoneater.theforgotten.TheForgotten;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ModDimensions {
    public static final RegistryKey<World> THE_FORGOTTEN_WORLD = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of(TheForgotten.MOD_ID, "the_forgotten")
    );
}
