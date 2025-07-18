package fuzs.fastitemframes.client;

import fuzs.fastitemframes.client.handler.ClientEventHandler;
import fuzs.fastitemframes.client.renderer.blockentity.ItemFrameBlockRenderer;
import fuzs.fastitemframes.init.ModRegistry;
import fuzs.fastitemframes.world.level.block.entity.ItemFrameBlockEntity;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import fuzs.puzzleslib.api.client.core.v1.context.BlockColorsContext;
import fuzs.puzzleslib.api.client.core.v1.context.BlockEntityRenderersContext;
import fuzs.puzzleslib.api.client.event.v1.renderer.ExtractRenderStateCallback;
import fuzs.puzzleslib.api.event.v1.entity.player.PlayerInteractEvents;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FastItemFramesClient implements ClientModConstructor {
    static final BlockColor ITEM_FRAME_BLOCK_COLOR_PROVIDER = (BlockState blockState, @Nullable BlockAndTintGetter blockAndTintGetter, @Nullable BlockPos blockPos, int tintIndex) -> {
        if (blockAndTintGetter != null && blockPos != null) {
            if (blockAndTintGetter.getBlockEntity(blockPos) instanceof ItemFrameBlockEntity blockEntity) {
                return blockEntity.getColor() != null ? blockEntity.getColor().rgb() : -1;
            }
        }

        return DyedItemColor.LEATHER_COLOR;
    };

    @Override
    public void onConstructMod() {
        registerEventHandlers();
    }

    private static void registerEventHandlers() {
        PlayerInteractEvents.ATTACK_BLOCK.register(ClientEventHandler::onAttackBlock);
        ExtractRenderStateCallback.EVENT.register(ClientEventHandler::onExtractRenderState);
    }

    @Override
    public void onRegisterBlockEntityRenderers(BlockEntityRenderersContext context) {
        context.registerBlockEntityRenderer(ModRegistry.ITEM_FRAME_BLOCK_ENTITY.value(), ItemFrameBlockRenderer::new);
    }

    @Override
    public void onRegisterBlockColorProviders(BlockColorsContext context) {
        context.registerBlockColor(ModRegistry.ITEM_FRAME_BLOCK.value(), ITEM_FRAME_BLOCK_COLOR_PROVIDER);
        context.registerBlockColor(ModRegistry.GLOW_ITEM_FRAME_BLOCK.value(), ITEM_FRAME_BLOCK_COLOR_PROVIDER);
    }
}
