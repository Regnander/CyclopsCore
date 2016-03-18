package org.cyclops.cyclopscore.config.configurable;

import com.google.common.collect.Lists;
import lombok.experimental.Delegate;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.cyclops.cyclopscore.block.property.BlockProperty;
import org.cyclops.cyclopscore.block.property.BlockPropertyManagerComponent;
import org.cyclops.cyclopscore.block.property.IBlockPropertyManager;
import org.cyclops.cyclopscore.config.extendedconfig.ExtendedConfig;

import java.util.List;
import java.util.Random;

/**
 * Block that extends from BlockLeaves that can hold ExtendedConfigs
 * @author rubensworks
 *
 */
public abstract class ConfigurableBlockLeaves extends BlockLeaves implements IConfigurableBlock {

    @Delegate private IBlockPropertyManager propertyManager;
    @Override protected BlockStateContainer createBlockState() {
        return (propertyManager = new BlockPropertyManagerComponent(this)).createDelegatedBlockState();
    }

    // This is to make sure that the MC properties are also loaded.
    @BlockProperty(ignore = true)
    public static final IProperty[] _COMPAT = {DECAYABLE, CHECK_DECAY};

    @SuppressWarnings("rawtypes")
    protected ExtendedConfig eConfig = null;
    protected boolean hasGui = false;
    
    /**
     * Make a new blockState instance.
     * @param eConfig Config for this blockState.
     */
    @SuppressWarnings({ "rawtypes" })
    public ConfigurableBlockLeaves(ExtendedConfig eConfig) {
        this.setConfig(eConfig);
        this.setUnlocalizedName(eConfig.getUnlocalizedName());
    }

    @Override
    public boolean hasGui() {
        return hasGui;
    }

    private void setConfig(@SuppressWarnings("rawtypes") ExtendedConfig eConfig) {
        this.eConfig = eConfig;
    }

    @Override
    public ExtendedConfig<?> getConfig() {
        return eConfig;
    }

    @Override
    public abstract Item getItemDropped(IBlockState blockState, Random random, int zero);

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos blockPos, IBlockState blockState, float chance, int fortune) {
        if(!world.isRemote) {
            List<ItemStack> items = getDrops(world, blockPos, blockState, fortune);

            for (ItemStack item : items) {
                if (world.rand.nextFloat() <= chance) {
                    this.spawnAsEntity(world, blockPos, item);
                }
            }
        }
    }
    
    @Override
    public boolean isOpaqueCube(IBlockState blockState) {
        return false;
    }
    
    @Override
    public boolean isNormalCube(IBlockState blockState) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess world, BlockPos blockPos, EnumFacing side) {
        return !(isTranslucent(blockState)) || super.shouldSideBeRendered(blockState, world, blockPos, side);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void getSubBlocks(Item item, CreativeTabs creativeTabs, List list) {
        list.add(new ItemStack(item, 1, 0));
    }

    @Override
    public boolean isLeaves(IBlockState blockState, IBlockAccess world, BlockPos blockPos) {
        return true;
    }

    @Override
    public BlockPlanks.EnumType getWoodType(int meta) {
        return null;
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return Lists.newArrayList(new ItemStack(this));
    }
}
