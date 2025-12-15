package com.spin.spinningitemsdisplayermod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemBed;
import net.minecraft.item.ItemBlock; // <-- NUEVA IMPORTACIÓN CLAVE
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms; // <-- LÍNEA DE IMPORTACIÓN CORREGIDA
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.List;

public class ItemDisplayHandler {
    // Variables para el item actual bajo el cursor
    private ItemStack currentItem = ItemStack.EMPTY;
    private float rotation = 0.0f;
    private int displayX = 10;
    private int displayY = 40;
    private int displaySize = 50;

    // --- CONSTANTES PARA LAS NUEVAS CARACTERÍSTICAS ---
    private static final int POTION_EFFECT_OFFSET = 999999999; // Cuánto bajar cuando hay efectos de poción
    private static final int MAX_TEXT_WIDTH = 100;      // Ancho máximo del texto antes de saltar de línea

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

    // --- NUEVO MÉTODO AUXILIAR ---
    /**
     * Calcula el desplazamiento vertical necesario para evitar superponerse con los efectos de poción.
     * @return El número de píxeles que se debe bajar el modelo y el texto.
     */
    private int getVerticalOffset() {
        Minecraft mc = Minecraft.getMinecraft();
        // Si el jugador existe y tiene efectos de poción activos, devuelve el desplazamiento.
        if (mc.player != null && !mc.player.getActivePotionEffects().isEmpty()) {
            return POTION_EFFECT_OFFSET;
        }
        return 0; // Si no, no hay desplazamiento.
    }

    // --- MÉTODO DE RENDERIZO FINAL CON TODAS LAS MEJORAS ---
    private void drawSpinningItem(ItemStack stack, GuiScreen gui) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderItem renderItem = mc.getRenderItem();

        GlStateManager.pushMatrix();

        // 1. Calculamos el desplazamiento vertical
        int yOffset = getVerticalOffset();

        // 2. Posicionamos el origen aplicando el desplazamiento
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

        // 3. --- RENDERIZO DE TEXTO CON SOMBRA, AJUSTE DE LÍNEA Y CANTIDAD ---
        String itemName = stack.getDisplayName();

        List<String> lines = mc.fontRenderer.listFormattedStringToWidth(itemName, MAX_TEXT_WIDTH);

        int textY = displayY + yOffset + displaySize + 55;

        // --- NUEVO: Posición del texto movida a la derecha ---
        // El texto ahora empieza 5 píxeles a la derecha del área del item.
        int textX = displayX + displaySize - 30;

        // Dibujamos cada línea del nombre del item
        for (String line : lines) {
            mc.fontRenderer.drawString(line, textX, textY, 0xFFFFFF, true);
            textY += mc.fontRenderer.FONT_HEIGHT;
        }

        // --- NUEVO: Mostramos la cantidad del item si es mayor a 1 ---
        int count = stack.getCount();
        if (count > 1) {
            String countStr = "Stack: " + String.valueOf(count); // Añadimos "Cantidad:" para mayor claridad
            mc.fontRenderer.drawString(countStr, textX, textY, 0xFFFFFF, true);
        }
    }
}