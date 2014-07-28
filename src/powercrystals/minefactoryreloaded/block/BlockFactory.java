package powercrystals.minefactoryreloaded.block;

import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.raytracer.RayTracer;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;
import cofh.api.block.IDismantleable;
import cofh.render.hitbox.ICustomHitBox;
import cofh.render.hitbox.RenderHitbox;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.IFluidContainerItem;

import powercrystals.minefactoryreloaded.api.rednet.connectivity.IRedNetConnection;
import powercrystals.minefactoryreloaded.api.rednet.connectivity.RedNetConnectionType;
import powercrystals.minefactoryreloaded.core.IEntityCollidable;
import powercrystals.minefactoryreloaded.core.ITankContainerBucketable;
import powercrystals.minefactoryreloaded.core.ITraceable;
import powercrystals.minefactoryreloaded.core.MFRLiquidMover;
import powercrystals.minefactoryreloaded.gui.MFRCreativeTab;
import powercrystals.minefactoryreloaded.setup.Machine;
import powercrystals.minefactoryreloaded.tile.base.TileEntityBase;

public class BlockFactory extends BlockContainer implements IRedNetConnection, IDismantleable
{
	protected boolean providesPower;

	protected BlockFactory(float hardness)
	{
		super(Machine.MATERIAL);
		setHardness(hardness);
		setStepSound(soundTypeMetal);
		setCreativeTab(MFRCreativeTab.tab);
		setHarvestLevel("pickaxe", 0);
	}

	protected BlockFactory(Material material)
	{
		super(material);
		setCreativeTab(MFRCreativeTab.tab);
		setHarvestLevel("pickaxe", 0);
	}

	@Override
	public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player)
	{ // HACK: called before block is destroyed by the player prior to the player getting the drops. destroy block here.
		// hack is needed because the player sets the block to air *before* getting the drops. woo good logic from mojang.
		if (!player.capabilities.isCreativeMode)
		{
			dropBlockAsItem(world, x, y, z, meta, 0);
			world.setBlock(x, y, z, Blocks.air, 0, 7);
		}
	}

	@Override
	public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta)
	{
	}

	@Override
	public final boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float xOffset, float yOffset, float zOffset)
	{
		PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, x, y, z, side, world);
		if (MinecraftForge.EVENT_BUS.post(e) || e.getResult() == Result.DENY || e.useBlock == Result.DENY)
			return false;

		activationOffsets(xOffset, yOffset, zOffset);
		return activated(world, x, y, z, player, side);
	}
	
	protected void activationOffsets(float xOffset, float yOffset, float zOffset) {}
	
	protected boolean activated(World world, int x, int y, int z, EntityPlayer player, int side)
	{
		TileEntity te = world.getTileEntity(x, y, z);
		if (te == null)
		{
			return false;
		}
		ItemStack ci = player.inventory.getCurrentItem();
		if (ci != null && te instanceof ITankContainerBucketable)
		{
			boolean isFluidContainer = ci.getItem() instanceof IFluidContainerItem;
			if ((isFluidContainer || FluidContainerRegistry.isEmptyContainer(ci)) &&
					((ITankContainerBucketable)te).allowBucketDrain(ci))
			{
				if (MFRLiquidMover.manuallyDrainTank((ITankContainerBucketable)te, player))
				{
					return true;
				}
			}
			if ((isFluidContainer || FluidContainerRegistry.isFilledContainer(ci)) &&
					((ITankContainerBucketable)te).allowBucketFill(ci))
			{
				if (MFRLiquidMover.manuallyFillTank((ITankContainerBucketable)te, player))
				{
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean canDismantle(EntityPlayer player, World world, int x, int y, int z)
	{
		return true;
	}

	@Override
	public ArrayList<ItemStack> dismantleBlock(EntityPlayer player, World world, int x, int y, int z,
			boolean returnBlock)
	{
		ArrayList<ItemStack> list = getDrops(world, x, y, z, world.getBlockMetadata(x, y, z), 0);

		world.setBlockToAir(x, y, z);
		if (!returnBlock)
            for (ItemStack item : list)
                    dropBlockAsItem(world, x, y, z, item);
		return list;
	}

	public void getBlockInfo(IBlockAccess world, int x, int y, int z, ForgeDirection side,
			EntityPlayer player, List<IChatComponent> info, boolean debug)
	{
		TileEntity tile = world.getTileEntity(x, y, z);
		if (tile instanceof TileEntityBase)
			((TileEntityBase)tile).getTileInfo(info, side, player, debug);
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z)
	{
		onNeighborBlockChange(world, x, y, z, this);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block blockId)
	{
		super.onNeighborBlockChange(world, x, y, z, blockId);
		if (world.isRemote)
		{
			return;
		}

		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof TileEntityBase)
		{
			((TileEntityBase)te).onNeighborBlockChange();
		}
	}
	
	@Override
	public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ)
    {
		TileEntity te = world.getTileEntity(x, y, z);
		
		if (te instanceof TileEntityBase)
		{
			((TileEntityBase)te).onNeighborTileChange(tileX, tileY, tileZ);
		}
    }

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z)
	{
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof IEntityCollidable)
		{
			float shrinkAmount = 0.125F;
			return AxisAlignedBB.getBoundingBox(x + shrinkAmount, y + shrinkAmount, z + shrinkAmount,
					x + 1 - shrinkAmount, y + 1 - shrinkAmount, z + 1 - shrinkAmount);
		}
		else
		{
			return super.getCollisionBoundingBoxFromPool(world, x, y, z);
		}
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
	{
		if (world.isRemote)
			return;

		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof IEntityCollidable)
			((IEntityCollidable)te).onEntityCollided(entity);

		super.onEntityCollidedWithBlock(world, x, y, z, entity);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB collisionTest, List collisionBoxList, Entity entity)
	{
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof ITraceable)
		{
			List<IndexedCuboid6> cuboids = new LinkedList<IndexedCuboid6>();
			((ITraceable)te).addTraceableCuboids(cuboids, false, false);
			for (IndexedCuboid6 c : cuboids)
			{
				AxisAlignedBB aabb = c.toAABB();
				if (collisionTest.intersectsWith(aabb))
					collisionBoxList.add(aabb);
			}
		}
		else
		{
			super.addCollisionBoxesToList(world, x, y, z, collisionTest, collisionBoxList, entity);
		}
	}

	protected ThreadLocal<Boolean> draw = new ThreadLocal<Boolean>();

	@Override
	public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end)
	{
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof ITraceable)
		{
			List<IndexedCuboid6> cuboids = new LinkedList<IndexedCuboid6>();
			((ITraceable)te).addTraceableCuboids(cuboids, true, draw.get() == Boolean.TRUE);
			return RayTracer.instance().rayTraceCuboids(new Vector3(start), new Vector3(end), cuboids, new BlockCoord(x, y, z), this);
		}
		return super.collisionRayTrace(world, x, y, z, start, end);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onBlockHighlight(DrawBlockHighlightEvent event) {
		MovingObjectPosition mop = event.target;
		int x = mop.blockX, y = mop.blockY, z = mop.blockZ;
		if (mop.typeOfHit != MovingObjectType.BLOCK)
			return;
		EntityPlayer player = event.player;
		World world = player.worldObj;
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof ITraceable) {
			MovingObjectPosition part = RayTracer.retraceBlock(world, player, x, y, z);
			if (part == null)
				return;
			int subHit = part.subHit;
			if (te instanceof ICustomHitBox)
			{
				ICustomHitBox tile = ((ICustomHitBox)te);
				if (tile.shouldRenderCustomHitBox(subHit, player))
				{
					event.setCanceled(true);
					RenderHitbox.drawSelectionBox(player, mop, event.partialTicks, tile.getCustomHitBox(subHit, player));
					return;
				}
			}

			if (((ITraceable)te).isLargePart(player, subHit))
			{
				draw.set(Boolean.TRUE);
				RayTracer.retraceBlock(world, player, x, y, z);
				draw.set(null);
			}
		}
	}

	@Override
	public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side)
	{
		return 0;
	}

	@Override
	public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side)
	{
		return isProvidingWeakPower(world, x, y, z, side);
	}

	@Override
	public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side)
	{
		return true;
	}

	@Override
	public boolean isNormalCube()
	{
		return !providesPower;
	}

	@Override
	public boolean canProvidePower()
	{
		return providesPower;
	}
	
	@Override
	public int damageDropped(int meta)
	{
		return meta;
	}

	@Override
	public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_)
	{
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister ir)
	{
		blockIcon = ir.registerIcon("minefactoryreloaded:" + getUnlocalizedName());
	}

	@Override
	public RedNetConnectionType getConnectionType(World world, int x, int y, int z, ForgeDirection side)
	{
		if (providesPower)
			return RedNetConnectionType.DecorativeSingle;
		else
			return RedNetConnectionType.ForcedDecorativeSingle;
	}
}
