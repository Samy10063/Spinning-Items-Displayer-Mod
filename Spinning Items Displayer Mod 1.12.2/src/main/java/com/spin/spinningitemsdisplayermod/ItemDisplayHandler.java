package com.spin.spinningitemsdisplayermod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemBed;
import net.minecraft.item.ItemBlock; // <-- KEY NEW IMPORT
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms; // <-- CORRECTED IMPORT LINE
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.List;

public class ItemDisplayHandler {
    // Variables for the current item under the cursor
    private ItemStack currentItem = ItemStack.EMPTY;
    private float rotation = 0.0f;
    private int displayX = 10;
    private int displayY = 40;
    private int displaySize = 50;

    // --- CONSTANTS FOR THE NEW FEATURES ---
    private static final int POTION_EFFECT_OFFSET = 999999999; // How much to lower when there are potion effects
    private static final int MAX_TEXT_WIDTH = 100;      // Maximum text width before wrapping to the next line

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiContainer)) {
            return;
        }

        GuiContainer gui = (GuiContainer) event.getGui();
        Minecraft mc = Minecraft.getMinecraft();

        int mouseX = Mouse.getX() * gui.width / mc.displayWidth;
        int mouseY = gui.height - Mouse.getY() * gui.height / mc.displayHeight - 1;

        ItemStack hoveredItem = getHoveredItem(gui, mouseX, mouseY);

        if (!ItemStack.areItemStacksEqual(currentItem, hoveredItem)) {
            currentItem = hoveredItem;
            rotation = 0.0f;
        }

        if (!currentItem.isEmpty()) {
            drawSpinningItem(currentItem, event.getGui());
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!currentItem.isEmpty()) {
            rotation += 2.0f;
            if (rotation >= 360.0f) {
                rotation -= 360.0f;
            }
        }
    }

    private ItemStack getHoveredItem(GuiContainer gui, int mouseX, int mouseY) {
        for (int i = 0; i < gui.inventorySlots.inventorySlots.size(); i++) {
            if (gui.inventorySlots.getSlot(i).getHasStack() &&
                    gui.inventorySlots.getSlot(i).getStack().getItem() != null) {

                int slotX = gui.inventorySlots.getSlot(i).xPos;
                int slotY = gui.inventorySlots.getSlot(i).yPos;

                slotX += gui.getGuiLeft();
                slotY += gui.getGuiTop();

                if (mouseX >= slotX && mouseX < slotX + 18 &&
                        mouseY >= slotY && mouseY < slotY + 18) {
                    return gui.inventorySlots.getSlot(i).getStack();
                }
            }
        }

        return ItemStack.EMPTY;
    }

    // --- NEW AUXILIARY METHOD ---
    /**
     * Calculates the vertical offset needed to avoid overlapping with potion effects.
     * @return The number of pixels the model and text should be lowered.
     */
    private int getVerticalOffset() {
        Minecraft mc = Minecraft.getMinecraft();
        // If the player exists and has active potion effects, return the offset.
        if (mc.player != null && !mc.player.getActivePotionEffects().isEmpty()) {
            return POTION_EFFECT_OFFSET;
        }
        return 0; // If not, there is no offset.
    }

    // --- FINAL RENDERING METHOD WITH ALL IMPROVEMENTS ---
    private void drawSpinningItem(ItemStack stack, GuiScreen gui) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderItem renderItem = mc.getRenderItem();

        GlStateManager.pushMatrix();

        // 1. Calculate the vertical offset
        int yOffset = getVerticalOffset();

        // 2. Position the origin applying the offset
        GlStateManager.translate(displayX + displaySize - 15.0f, displayY + yOffset + displaySize - 14.0f, 100.0f);

        float baseScale = displaySize / 0.8f;
        ItemCameraTransforms.TransformType transformType;

        if (stack.getItem() instanceof ItemBed) {
            GlStateManager.scale(baseScale * 1.8, baseScale * 1.8, baseScale * 1.8);
            transformType = ItemCameraTransforms.TransformType.GROUND;
        } else {
            GlStateManager.scale(baseScale, baseScale, baseScale);
            transformType = ItemCameraTransforms.TransformType.FIXED;
        }

        GlStateManager.rotate(-180.0f, -1.0f, 0.0f, 0.0f);
        GlStateManager.rotate(rotation, 0.0f, -1.0f, 0.0f);

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.disableCull();

        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();

        renderItem.renderItem(stack, transformType);

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.enableCull();

        GlStateManager.popMatrix();

        // 3. --- TEXT RENDERING WITH SHADOW, LINE WRAP, AND COUNT ---
        String itemName = stack.getDisplayName();

        List<String> lines = mc.fontRenderer.listFormattedStringToWidth(itemName, MAX_TEXT_WIDTH);

        int textY = displayY + yOffset + displaySize + 55;

        // --- NEW: Text position moved to the right ---
        // The text is now positioned to the right of the item area.
        int textX = displayX + displaySize - 30;

        // Draw each line of the item's name
        for (String line : lines) {
            mc.fontRenderer.drawString(line, textX, textY, 0xFFFFFF, true);
            textY += mc.fontRenderer.FONT_HEIGHT;
        }

        // --- NEW: Show the item count if it's greater than 1 ---
        int count = stack.getCount();
        if (count > 1) {
            String countStr = "Stack: " + String.valueOf(count); // We add "Stack:" for clarity
            mc.fontRenderer.drawString(countStr, textX, textY, 0xFFFFFF, true);
        }
    }
}