package com.lemoneater.theforgotten;

import com.lemoneater.theforgotten.block.ModBlocks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;

import net.minecraft.client.render.BlockRenderLayer;

public class TheForgottenClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.putBlock(ModBlocks.FORGOTTEN_PORTAL, BlockRenderLayer.TRANSLUCENT);
    }
}
