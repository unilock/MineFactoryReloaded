package powercrystals.minefactoryreloaded.gui.container;

import cofh.core.gui.slot.SlotRemoveOnly;

import cofh.core.util.helpers.InventoryHelper;
import net.minecraft.entity.player.InventoryPlayer;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import powercrystals.minefactoryreloaded.tile.machine.TileEntityAutoEnchanter;

public class ContainerAutoEnchanter extends ContainerFactoryPowered {

	private TileEntityAutoEnchanter _enchanter;

	public ContainerAutoEnchanter(TileEntityAutoEnchanter enchanter, InventoryPlayer inv) {

		super(enchanter, inv);

		_enchanter = enchanter;
	}

	@Override
	protected void addSlots() {

		IItemHandler handler = InventoryHelper.getItemHandlerCap(_enchanter, null);
		addSlotToContainer(new SlotItemHandler(handler, 0, 8, 24));
		addSlotToContainer(new SlotRemoveOnly(_te, 1, 8, 54));
	}

	@Override
	public void detectAndSendChanges() {

		super.detectAndSendChanges();
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).sendWindowProperty(this, 100, _enchanter.getTargetLevel());
		}
	}

	@Override
	public void updateProgressBar(int var, int value) {

		super.updateProgressBar(var, value);
		if (var == 100) _enchanter.setTargetLevel(value);
	}

}
