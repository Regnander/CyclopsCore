package org.cyclops.cyclopscore.client.particle;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * Particle that appears underneath blocks for simulating drops.
 * Courtesy of BuildCraft: https://github.com/BuildCraft/BuildCraft/blob/master/common/buildcraft/energy/render/EntityDropParticleFX.java
 *
 */
@SideOnly(Side.CLIENT)
public class ExtendedEntityDropParticleFX extends EntityFX {

    /**
     * The height of the current bob
     */
    private int bobTimer;

    /**
     * Make a new instance.
     * @param world The world.
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param z Z coordinate.
     * @param particleRed Red color.
     * @param particleGreen Green color.
     * @param particleBlue Blue color.
     */
    public ExtendedEntityDropParticleFX(World world, double x, double y, double z, float particleRed, float particleGreen, float particleBlue) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.xSpeed = this.ySpeed = this.zSpeed = 0.0D;

        this.particleRed = particleRed;
        this.particleGreen = particleGreen;
        this.particleBlue = particleBlue;

        this.setParticleTextureIndex(113);
        this.setSize(0.01F, 0.01F);
        this.particleGravity = 0.06F;
        this.bobTimer = 40;
        this.particleMaxAge = (int) (64.0D / (Math.random() * 0.8D + 0.2D));
        this.xSpeed = this.ySpeed = this.zSpeed = 0.0D;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        this.ySpeed -= (double) this.particleGravity;

        if (this.bobTimer-- > 0) {
            this.xSpeed *= 0.02D;
            this.ySpeed *= 0.02D;
            this.zSpeed *= 0.02D;
            this.setParticleTextureIndex(113);
        } else {
            this.setParticleTextureIndex(112);
        }

        this.moveEntity(this.xSpeed, this.ySpeed, this.zSpeed);
        this.xSpeed *= 0.9800000190734863D;
        this.ySpeed *= 0.9800000190734863D;
        this.zSpeed *= 0.9800000190734863D;

        if (this.particleMaxAge-- <= 0) {
            this.setExpired();
        }

        if (this.isCollided) {
            this.setParticleTextureIndex(114);

            this.xSpeed *= 0.699999988079071D;
            this.zSpeed *= 0.699999988079071D;
        }

        BlockPos blockPos = new BlockPos(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ));
        IBlockState blockState = this.worldObj.getBlockState(blockPos);
        Material material = blockState.getBlock().getMaterial(blockState);

        if (material.isLiquid() || material.isSolid()) {
            float h = 1;
            if(worldObj.getBlockState(blockPos).getBlock() instanceof BlockLiquid) {
                h = BlockLiquid.getLiquidHeightPercent((Integer) this.worldObj.getBlockState(blockPos).getValue(BlockLiquid.LEVEL));
            }
            double d0 = (double) ((float) (MathHelper.floor_double(this.posY) + 1) - h);

            if (this.posY < d0) {
                this.setExpired();
            }
        }
    }
}
