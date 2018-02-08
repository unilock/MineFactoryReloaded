package powercrystals.minefactoryreloaded.gui.container;

import cofh.core.gui.slot.SlotRemoveOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.entity.player.InventoryPlayer;

import powercrystals.minefactoryreloaded.tile.machine.TileEntityBioReactor;

public class ContainerBioReactor extends ContainerFactoryInventory {

	public ContainerBioReactor(TileEntityBioReactor tileentity, InventoryPlayer inv) {

		super(tileentity, inv);
	}

	@Override
	public void detectAndSendChanges() {

		super.detectAndSendChanges();
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).sendWindowProperty(this, 100, ((TileEntityBioReactor) _te).getBurnTime());
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void updateProgressBar(int var, int value) {

		super.updateProgressBar(var, value);
		if (var == 100) ((TileEntityBioReactor) _te).setBurnTime(value);
	}

	@Override
	protected void addSlots() {

		super.addSlots();

		for (int i = 0; i < 9; i++) {
			addSlotToContainer(new SlotRemoveOnly(_te, 9 + i, 8 + 18 * i, 83));
		}
	}

	@Override
	protected int getPlayerInventoryVerticalOffset() {

		return 113;
	}

}
