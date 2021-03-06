package net.dragon9815.playerinterfacemod.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.dragon9815.playerinterfacemod.client.gui.GuiTrashInventory;
import net.dragon9815.playerinterfacemod.client.settings.Keybindings;
import net.dragon9815.playerinterfacemod.network.PacketHandler;
import net.dragon9815.playerinterfacemod.network.message.MessageOpenGui;
import net.minecraft.client.Minecraft;

public class KeyEventHandler {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (Keybindings.openInventory.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new MessageOpenGui(Minecraft.getMinecraft().thePlayer.getUniqueID(), GuiTrashInventory.GUI_ID));
            //Minecraft.getMinecraft().thePlayer.openGui(Reference.MOD_ID, GuiTrashInventory.GUI_ID, Minecraft.getMinecraft().theWorld, 0, 0, 0);
        }
    }
}
