package com.lemoneater.theforgotten;

import com.lemoneater.theforgotten.block.ModBlocks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;

import net.minecraft.client.render.RenderLayer;

public class TheForgottenClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FORGOTTEN_PORTAL, RenderLayer.getTranslucent());
    }
}
