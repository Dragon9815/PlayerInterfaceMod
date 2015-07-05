package net.stegr.plim.tileentity;

import com.sun.xml.internal.ws.client.dispatch.PacketDispatch;
import com.sun.xml.internal.ws.commons.xmlutil.Converter;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.stegr.plim.item.upgrade.IUpgrade;
import net.stegr.plim.network.DescriptionHandler;
import net.stegr.plim.reference.Reference;
import net.stegr.plim.utility.LogHelper;
import net.stegr.plim.utility.UpgradeRegistry;
import org.lwjgl.BufferUtils;

import java.util.Iterator;
import java.util.UUID;

public class TileEntityPlayerInterface extends TileEntityUpgradeable implements ISidedInventory
{
    private EntityPlayer boundPlayer;

    public boolean hasBuffer;
    public ItemStack[] bufferSlots;

    private boolean bindPlayer;
    private String uuid;

    private boolean doesTransfer;

    public TileEntityPlayerInterface()
    {
        super();

        boundPlayer = null;

        bindPlayer = false;
        uuid = "";

        hasBuffer = false;
        bufferSlots = new ItemStack[9];

        doesTransfer = false;

        validUpgrades.put(UpgradeRegistry.getUpgrade("buffer").getUpgradeID(), 1);
        validUpgrades.put(UpgradeRegistry.getUpgrade("transfer").getUpgradeID(), 1);
    }

    @Override
    public void updateEntity()
    {
        if(bindPlayer && !worldObj.isRemote)
        {
            Iterator it = MinecraftServer.getServer().getConfigurationManager().playerEntityList.iterator();
            EntityPlayer p;

            while (it.hasNext())
            {
                p = (EntityPlayer) it.next();

                if (p.getUniqueID().equals(UUID.fromString(uuid)))
                {
                    bindPlayer(p);
                    LogHelper.info("Successfully rebound player!");
                    uuid = "";
                    bindPlayer = false;
                }
            }

            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }

        if(hasBuffer && doesTransfer)
        {
            int var1 = firstStackInBuffer();
            if (var1 != -1)
            {
                ItemStack stack;

                stack = bufferSlots[var1].copy();

                if (stack.stackSize > 0)
                {
                    stack.stackSize = 1;

                    if(boundPlayer != null)
                    {
                        if (boundPlayer.inventory.addItemStackToInventory(stack))
                        {
                            bufferSlots[var1].stackSize--;
                        }
                    }
                }

                if (bufferSlots[var1].stackSize <= 0)
                {
                    bufferSlots[var1] = null;
                }
            }
        }

        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        this.markDirty();
    }

    private int firstStackInBuffer()
    {
        if(hasBuffer)
        {
            for (int i = 0; i < this.bufferSlots.length; ++i)
            {
                if (this.bufferSlots[i] != null)
                {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public int getSizeInventory()
    {
        int size = 0;

        if(boundPlayer != null && doesTransfer)
        {
            size += boundPlayer.inventory.getSizeInventory();
        }

        if(hasBuffer)
        {
            size += this.bufferSlots.length;
        }

        return size;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        ItemStack var1 = null;

        if(slot < 0)
        {
            throw new IllegalArgumentException("Slot number can't be negative!");
        }

        if(slot < this.bufferSlots.length && hasBuffer)
        {
            var1 = bufferSlots[slot];
        }
        else
        {
            if (boundPlayer != null && doesTransfer)
            {
                var1 = boundPlayer.inventory.getStackInSlot(slot - ((hasBuffer) ? this.bufferSlots.length : 0));
            }
        }

        return var1;
    }

    @Override
    public ItemStack decrStackSize(int slot, int par2)
    {
        ItemStack[] itemStacks = this.bufferSlots;

        if(slot < this.bufferSlots.length && hasBuffer)
        {
            if(itemStacks[slot] != null)
            {
                ItemStack stack;

                if(itemStacks[slot].stackSize <= par2)
                {
                    stack = itemStacks[slot];
                    itemStacks[slot] = null;
                    return stack;
                }
                else
                {
                    stack = itemStacks[slot].splitStack(par2);

                    if(itemStacks[slot].stackSize == 0)
                    {
                        itemStacks[slot] = null;
                    }

                    return stack;
                }
            }
        }
        else
        {
            if (boundPlayer != null && doesTransfer)
            {
                return boundPlayer.inventory.decrStackSize(slot - ((hasBuffer) ? this.bufferSlots.length : 0), par2);
            }
        }

        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int p_70304_1_)
    {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack itemStack)
    {
        ItemStack[] itemStacks = this.bufferSlots;

        if(slot < this.bufferSlots.length && hasBuffer)
        {
            itemStacks[slot] = itemStack;
        }
        else
        {
            if (boundPlayer != null && doesTransfer)
            {
                boundPlayer.inventory.setInventorySlotContents(slot - ((hasBuffer) ? this.bufferSlots.length : 0), itemStack);
            }
        }
    }

    @Override
    public String getInventoryName()
    {
        return "";
    }

    @Override
    public boolean hasCustomInventoryName()
    {
        return false;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer p_70300_1_)
    {
        return false;
    }

    @Override
    public void openInventory()
    {

    }

    @Override
    public void closeInventory()
    {

    }

    @Override
    public boolean isItemValidForSlot(int p_94041_1_, ItemStack p_94041_2_)
    {
        return doesTransfer;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag)
    {
        super.writeToNBT(tag);

        tag.setBoolean("hasBuffer", this.hasBuffer);
        tag.setBoolean("doesTransfer", this.doesTransfer);
        tag.setString("boundPlayer", (boundPlayer != null) ? boundPlayer.getUniqueID().toString() : "");

        NBTTagList tagList = new NBTTagList();

        for(int i = 0; i < bufferSlots.length; i++)
        {
            NBTTagCompound tag1 = new NBTTagCompound();

            if(bufferSlots[i] != null)
                bufferSlots[i].writeToNBT(tag1);

            tagList.appendTag(tag1);
        }

        tag.setTag("Buffer_Slots", tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.hasBuffer = tag.getBoolean("hasBuffer");
        this.doesTransfer = tag.getBoolean("doesTransfer");

        String uuid = tag.getString("boundPlayer");

        if(!uuid.equals(""))
        {
            bindPlayer = true;
            this.uuid = uuid;
        }
        else
        {
            this.boundPlayer = null;
        }

        if(tag.hasKey("Buffer_Slots"))
        {
            NBTTagList tagList = tag.getTagList("Buffer_Slots", 10);

            for(int i = 0; i < tagList.tagCount() && i < bufferSlots.length; i++)
            {
                NBTTagCompound tag1 = tagList.getCompoundTagAt(i);

                bufferSlots[i] = ItemStack.loadItemStackFromNBT(tag1);
            }
        }
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int p_94128_1_)
    {
        if(boundPlayer != null)
        {
            int[] temp = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};

            return temp;
        }

        return new int[0];
    }

    @Override
    public boolean canInsertItem(int p_102007_1_, ItemStack p_102007_2_, int p_102007_3_)
    {
        return doesTransfer;
    }

    @Override
    public boolean canExtractItem(int p_102008_1_, ItemStack p_102008_2_, int p_102008_3_)
    {
        return true;
    }

    @Override
    public void onUpgrade(IUpgrade upgrade)
    {
        if(upgrade.getUpgradeID().equals("buffer"))
        {
            hasBuffer = true;
        }
        else if(upgrade.getUpgradeID().equals("transfer"))
        {
            doesTransfer = true;
        }
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public void bindPlayer(EntityPlayer player)
    {
        boundPlayer = player;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public EntityPlayer getBoundPlayer()
    {
        return this.boundPlayer;
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
    {
        super.onDataPacket(net, pkt);
    }

    public Packet getDescriptionPacket()
    {
        ByteBuf buf = Unpooled.buffer();

        buf.writeInt(xCoord);
        buf.writeInt(yCoord);
        buf.writeInt(zCoord);

        writeToPacket(buf);
        return new FMLProxyPacket(buf, DescriptionHandler.CHANNEL);
    }

    protected void writeToPacket(ByteBuf buf)
    {
        ByteBufUtils.writeUTF8String(buf, ((boundPlayer != null) ? boundPlayer.getUniqueID().toString() : ""));
        buf.writeBoolean(this.hasBuffer);
        buf.writeBoolean(this.doesTransfer);
    }

    public void readFromPacket(ByteBuf buf)
    {

    }
}
