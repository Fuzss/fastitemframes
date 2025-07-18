package fuzs.fastitemframes.world.level.block.entity;

import fuzs.fastitemframes.FastItemFrames;
import fuzs.fastitemframes.init.ModRegistry;
import fuzs.fastitemframes.world.level.block.ItemFrameBlock;
import fuzs.puzzleslib.api.block.v1.entity.TickingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public class ItemFrameBlockEntity extends BlockEntity implements TickingBlockEntity {
    static final String TAG_COLOR = FastItemFrames.id("color").toString();
    static final String TAG_ITEM_FRAME = FastItemFrames.id("item_frame").toString();

    @Nullable
    private ItemFrame itemFrame;
    @Nullable
    private CompoundTag storedTag;
    @Nullable
    private Integer color;

    public ItemFrameBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModRegistry.ITEM_FRAME_BLOCK_ENTITY.value(), pos, blockState);
    }

    /**
     * @see net.minecraft.server.level.ServerEntity#sendChanges()
     */
    @Override
    public void serverTick() {
        if (this.hasLevel() && ((ServerLevel) this.getLevel()).getServer().getTickCount() % 10 == 0) {
            ItemStack itemStack = this.getItem();
            if (itemStack.getItem() instanceof MapItem) {
                MapId mapId = itemStack.get(DataComponents.MAP_ID);
                MapItemSavedData mapItemSavedData = MapItem.getSavedData(mapId, this.getLevel());
                if (mapItemSavedData != null) {
                    for (ServerPlayer serverPlayer : ((ServerLevel) this.getLevel()).players()) {
                        mapItemSavedData.tickCarriedBy(serverPlayer, itemStack);
                        Packet<?> packet = mapItemSavedData.getUpdatePacket(mapId, serverPlayer);
                        if (packet != null) {
                            serverPlayer.connection.send(packet);
                        }
                    }
                }
            }
        }
    }

    public void load(ItemFrame itemFrame) {
        CompoundTag compoundTag = new CompoundTag();
        itemFrame.addAdditionalSaveData(compoundTag);
        this.loadItemFrame(compoundTag);
        if (ModRegistry.ITEM_FRAME_COLOR_ATTACHMENT_TYPE.has(itemFrame)) {
            this.setColor(ModRegistry.ITEM_FRAME_COLOR_ATTACHMENT_TYPE.get(itemFrame));
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_ITEM_FRAME)) {
            this.loadItemFrame(tag.getCompoundOrEmpty(TAG_ITEM_FRAME));
        }
        this.color = tag.contains(TAG_COLOR) ? tag.getIntOr(TAG_COLOR, 0) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag itemFrameTag = this.getItemFrameTag();
        if (itemFrameTag != null) {
            tag.put(TAG_ITEM_FRAME, itemFrameTag);
        }
        if (this.color != null) {
            tag.putInt(TAG_COLOR, this.color);
        }
    }

    @Nullable
    private CompoundTag getItemFrameTag() {
        ItemFrame itemFrame = this.getEntityRepresentation();
        if (itemFrame != null) {
            CompoundTag compoundTag = new CompoundTag();
            itemFrame.addAdditionalSaveData(compoundTag);
            return compoundTag;
        } else if (this.storedTag != null) {
            return this.storedTag;
        }

        return null;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public void preRemoveSideEffects(BlockPos blockPos, BlockState blockState) {
        super.preRemoveSideEffects(blockPos, blockState);
        this.getEntityRepresentation().dropItem((ServerLevel) this.level, null, false);
        // not sure if this is necessary since the block entity is about to be deleted as well
        this.setChanged();
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter dataComponentGetter) {
        super.applyImplicitComponents(dataComponentGetter);
        DyedItemColor dyedItemColor = dataComponentGetter.get(DataComponents.DYED_COLOR);
        if (dyedItemColor != null) {
            this.color = dyedItemColor.rgb();
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.color != null) {
            components.set(DataComponents.DYED_COLOR, new DyedItemColor(this.color));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove(TAG_COLOR);
    }

    public void setColor(int color) {
        if (this.color == null || this.color != color) {
            this.color = color;
        }
    }

    public OptionalInt getColor() {
        return this.color != null ? OptionalInt.of(this.color) : OptionalInt.empty();
    }

    public ItemStack getItem() {
        ItemFrame itemFrame = this.getEntityRepresentation();
        return itemFrame != null ? itemFrame.getItem() : ItemStack.EMPTY;
    }

    public boolean isInvisible() {
        return this.getBlockState().getValue(ItemFrameBlock.INVISIBLE);
    }

    public void markUpdated() {
        if (this.hasLevel()) {
            this.getLevel().setBlock(this.getBlockPos(), this.getUpdatedBlockState(), 2);
            this.setChanged();
            this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    private BlockState getUpdatedBlockState() {

        BlockState blockState = this.getBlockState();
        ItemFrame itemFrame = this.getEntityRepresentation();
        if (itemFrame != null) {

            if (itemFrame.getItem().isEmpty() && blockState.getValue(ItemFrameBlock.INVISIBLE)) {
                blockState = blockState.setValue(ItemFrameBlock.INVISIBLE, Boolean.FALSE);
            }

            return blockState.setValue(ItemFrameBlock.MAP, itemFrame.hasFramedMap())
                    .setValue(ItemFrameBlock.DYED, this.getColor().isPresent());
        }

        return blockState;
    }

    @Nullable
    public ItemFrame getEntityRepresentation() {
        return this.getEntityRepresentation(false);
    }

    @Nullable
    public ItemFrame getEntityRepresentation(boolean skipInit) {
        if (this.itemFrame == null && this.hasLevel()) {
            EntityType<? extends HangingEntity> type = ((HangingEntityItem) this.getBlockState()
                    .getBlock()
                    .asItem()).type;
            ItemFrame itemFrame = (ItemFrame) type.create(this.getLevel(), EntitySpawnReason.LOAD);
            if (!skipInit) this.initItemFrame(itemFrame, this.storedTag);
            this.storedTag = null;
            return this.itemFrame = itemFrame;
        } else {
            return this.itemFrame;
        }
    }

    private void loadItemFrame(CompoundTag compoundTag) {
        ItemFrame itemFrame = this.getEntityRepresentation(true);
        if (itemFrame != null) {
            this.initItemFrame(itemFrame, compoundTag);
        } else {
            this.storedTag = compoundTag.copy();
        }
    }

    private void initItemFrame(ItemFrame itemFrame, @Nullable CompoundTag compoundTag) {
        itemFrame.setItem(ItemStack.EMPTY, false);
        BlockPos pos = this.getBlockPos();
        // set this first to prevent a warning when reading the wrong position from nbt
        itemFrame.setPos(pos.getX(), pos.getY(), pos.getZ());
        if (compoundTag != null) itemFrame.readAdditionalSaveData(compoundTag);
        // set this again in case the nbt contained a wrong position
        itemFrame.setPos(pos.getX(), pos.getY(), pos.getZ());
        // force block facing e.g. when the item frame has been copied with nbt
        itemFrame.setDirection(this.getBlockState().getValue(ItemFrameBlock.FACING));
        // just make those always invisible for client rendering, the actual block invisibility status is tracked via a block state
        itemFrame.setInvisible(true);
    }
}
