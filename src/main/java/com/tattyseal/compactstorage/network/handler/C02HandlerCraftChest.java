package com.tattyseal.compactstorage.network.handler;

import com.tattyseal.compactstorage.CompactStorage;
import com.tattyseal.compactstorage.network.packet.C02PacketCraftChest;
import com.tattyseal.compactstorage.tileentity.TileEntityChestBuilder;
import com.tattyseal.compactstorage.util.LogHelper;
import com.tattyseal.compactstorage.util.StorageInfo;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Arrays;
import java.util.List;

public class C02HandlerCraftChest implements IMessageHandler<C02PacketCraftChest, IMessage>
{
	@Override
	public IMessage onMessage(C02PacketCraftChest message, MessageContext ctx)
	{
		TileEntityChestBuilder builder = (TileEntityChestBuilder) FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(message.dimension).getTileEntity(new BlockPos(message.x, message.y, message.z));
		
		List<ItemStack> items = Arrays.asList(builder.items);
		List<ItemStack> requiredItems = builder.info.getMaterialCost(message.type);
		
		boolean hasRequiredMaterials = true;
		
		for(int slot = 0; slot < items.size(); slot++)
		{
			ItemStack stack = items.get(slot);

			if(stack != null && slot < requiredItems.size() && requiredItems.get(slot) != null)
			{
				if(OreDictionary.itemMatches(requiredItems.get(slot), stack, false) && stack.getCount() >=  requiredItems.get(slot).getCount())
				{
					hasRequiredMaterials = true;
				}
				else
				{
					if(requiredItems.get(slot) != null && requiredItems.get(slot).getCount() == 0)
					{
						hasRequiredMaterials = true;
					}
					else
					{
						hasRequiredMaterials = false;
					}
					break;
				}
			}
			else
			{
				hasRequiredMaterials = false;
				break;
			}
		}

		LogHelper.dump("HAS REQ MATS: " + hasRequiredMaterials);
		
		if(hasRequiredMaterials)
		{
			ItemStack stack = new ItemStack((Item) (message.type.equals(StorageInfo.Type.BACKPACK) ? CompactStorage.backpack : ItemBlock.getItemFromBlock(CompactStorage.chest)), 1);
			
			NBTTagCompound tag = new NBTTagCompound();
			tag.setIntArray("size", new int[]{message.info.getSizeX(), message.info.getSizeY()});
			tag.setString("color", message.color);
			stack.setTagCompound(tag);
			
			EntityItem item = new EntityItem(ctx.getServerHandler().playerEntity.world, message.x, message.y + 1, message.z, stack);
			ctx.getServerHandler().playerEntity.world.spawnEntity(item);

			LogHelper.dump("SPAWNED ITEM ENTITY");
			
			for(int x = 0; x < requiredItems.size(); x++)
			{
				builder.decrStackSize(x, requiredItems.get(x).getCount());

				LogHelper.dump("DECREASED ITEMS IN INVENTORY");
			}
		}
		
		return null;
	}
}
