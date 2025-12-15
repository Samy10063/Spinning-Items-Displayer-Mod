package com.spin.spinningitemsdisplayermod;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = SpinningItemsDisplayerMod.MODID, value = Dist.CLIENT)
public class SpinningItemRenderer {

    private static float rotation = 0.0f;
    private static ItemStack lastHoveredItem = ItemStack.EMPTY;

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;

        // Only process if we are in a container screen (inventory)
        if (screen instanceof AbstractContainerScreen) {
            AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
            Slot hoveredSlot = containerScreen.getSlotUnderMouse();

            if (hoveredSlot != null && !hoveredSlot.getItem().isEmpty()) {
                // Check if the item is draggable (only exclude AIR)
                if (isDraggableItem(hoveredSlot.getItem())) {
                    ItemStack currentHoveredItem = hoveredSlot.getItem().copy();

                    // Detect if the item has changed
                    if (!ItemStack.matches(currentHoveredItem, lastHoveredItem)) {
                        // Reset rotation when the item changes
                        rotation = 0.0f;
                        lastHoveredItem = currentHoveredItem;
                    }

                    // Render the spinning item with background and name
                    renderSpinningItemWithBackground(event.getGuiGraphics(), lastHoveredItem, hoveredSlot.getItem());
                }
            } else {
                lastHoveredItem = ItemStack.EMPTY;
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Update rotation each client tick (to the right)
        rotation += 2.0f;
        if (rotation >= 360.0f) {
            rotation = 0.0f;
        }
    }

    private static void renderSpinningItemWithBackground(GuiGraphics guiGraphics, ItemStack mainStack, ItemStack slotStack) {
        if (mainStack.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();

        // Size and position configuration
        float scale = 3.5f;
        int itemSize = (int) (16 * scale);
        int padding = 16;
        int textHeight = 12;
        int textPadding = 8;
        int lineHeight = 1;
        int extraBottomSpace = 20;

        // Calculate text width with line breaks
        String itemName = mainStack.getHoverName().getString();
        List<String> textLines = wrapText(itemName, mc.font, 100);
        int totalTextHeight = textLines.size() * textHeight + (textLines.size() - 1) * 2;

        // Calculate total height including stack count and extra space
        int stackCountHeight = (slotStack.getCount() > 1) ? textHeight + 6 : 0;
        int totalHeight = itemSize + padding * 2 + totalTextHeight + textPadding * 4 + stackCountHeight + extraBottomSpace;

        // Maximum width between item and text
        int maxTextWidth = getMaxTextWidth(textLines, mc.font);
        int totalWidth = Math.max(itemSize + padding * 2, maxTextWidth + padding * 2);

        // Center position on the left side of the screen
        int centerX = 60;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;

        // Calculate Y position of the item
        int itemY = centerY - totalTextHeight/2 - textPadding - stackCountHeight/2;

        // Render professional background (restored to its original appearance)
        renderProfessionalBackground(guiGraphics, centerX, centerY, totalWidth, totalHeight, textHeight, totalTextHeight);

        // Render the specific background for the item
        renderItemBackground(guiGraphics, centerX, itemY, itemSize);

        // Render the spinning item centered on the background
        renderCenteredSpinningItem(guiGraphics, mainStack, centerX, itemY, scale);

        // Render the item name below the separator line
        int textStartY = centerY + itemSize/2 + padding + lineHeight - 12;
        renderItemName(guiGraphics, textLines, centerX, textStartY);

        // Render stack count if greater than 1
        if (slotStack.getCount() > 1) {
            renderStackCount(guiGraphics, slotStack.getCount(), centerX, centerY + totalHeight/2 - textHeight - textPadding);
        }

        // Add professional glow
        renderProfessionalGlow(guiGraphics, centerX, centerY, totalWidth, totalHeight);
    }

    // New method to render the item background
    private static void renderItemBackground(GuiGraphics guiGraphics, int centerX, int itemY, int itemSize) {
        var poseStack = guiGraphics.pose();
        poseStack.pushMatrix();

        // Calculate position and size of the item background
        int itemBgPadding = 4; // Padding around the item
        int itemBgSize = itemSize + itemBgPadding * 2;
        int itemBgX = centerX - itemBgSize / 2;
        int itemBgY = itemY - itemBgSize / 2;
        int itemBgCornerRadius = 6; // Radius of rounded corners

        // Render item background with darker color and transparency
        guiGraphics.fill(itemBgX, itemBgY, itemBgX + itemBgSize, itemBgY + itemBgSize, 0xCC050505);

        // Subtle border for the item background
        guiGraphics.fill(itemBgX, itemBgY, itemBgX + itemBgSize, itemBgY + 1, 0xAA303030);
        guiGraphics.fill(itemBgX, itemBgY + itemBgSize - 1, itemBgX + itemBgSize, itemBgY + itemBgSize, 0xAA303030);
        guiGraphics.fill(itemBgX, itemBgY, itemBgX + 1, itemBgY + itemBgSize, 0xAA303030);
        guiGraphics.fill(itemBgX + itemBgSize - 1, itemBgY, itemBgX + itemBgSize, itemBgY + itemBgSize, 0xAA303030);

        // Rounded corners for the item background
        renderRoundedCorners(guiGraphics, itemBgX, itemBgY, itemBgSize, itemBgSize, itemBgCornerRadius);

        poseStack.popMatrix();
    }

    private static void renderCenteredSpinningItem(GuiGraphics guiGraphics, ItemStack stack, int centerX, int centerY, float scale) {
        // Render spinning for all items
        var poseStack = guiGraphics.pose();
        poseStack.pushMatrix();

        // Move to the center of the item area
        poseStack.translate(centerX, centerY);

        // Apply right rotation (2D) around the center
        poseStack.rotate((float) Math.toRadians(rotation));

        // Center the item at the rotation point
        poseStack.translate(-8 * scale, -8 * scale);

        // Apply scale
        poseStack.scale(scale, scale);

        // Render the item
        guiGraphics.renderItem(stack, 0, 0);

        // Restore the transformation state
        poseStack.popMatrix();
    }

    private static void renderProfessionalBackground(GuiGraphics guiGraphics, int centerX, int centerY, int width, int height, int textHeight, int totalTextHeight) {
        var poseStack = guiGraphics.pose();
        poseStack.pushMatrix();

        // Calculate background position (centered)
        int bgX = centerX - width / 2;
        int bgY = centerY - height / 2;
        int cornerRadius = 8; // Restored to original value

        // Render main background with gradient (original color restored)
        guiGraphics.fill(bgX, bgY, bgX + width, bgY + height, 0xBB1a1a1a);

        // Outer border
        guiGraphics.fill(bgX, bgY, bgX + width, bgY + 2, 0xFF404040);
        guiGraphics.fill(bgX, bgY + height - 2, bgX + width, bgY + height, 0xFF404040);
        guiGraphics.fill(bgX, bgY, bgX + 2, bgY + height, 0xFF404040);
        guiGraphics.fill(bgX + width - 2, bgY, bgX + width, bgY + height, 0xFF404040);

        // Bright inner border
        guiGraphics.fill(bgX + 2, bgY + 2, bgX + width - 2, bgY + 3, 0xFF606060);
        guiGraphics.fill(bgX + 2, bgY + height - 3, bgX + width - 2, bgY + height - 2, 0xFF606060);
        guiGraphics.fill(bgX + 2, bgY + 2, bgX + 3, bgY + height - 2, 0xFF606060);
        guiGraphics.fill(bgX + width - 3, bgY + 2, bgX + width - 2, bgY + height - 2, 0xFF606060);

        // Improved rounded corners (original radius restored)
        renderRoundedCorners(guiGraphics, bgX, bgY, width, height, cornerRadius);

        // Separator line between item and text
        int separatorY = centerY - totalTextHeight/2 + 33;
        guiGraphics.fill(bgX + 8, separatorY, bgX + width - 8, separatorY + 1, 0xFF505050);

        poseStack.popMatrix();
    }

    private static void renderRoundedCorners(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius) {
        // Top left corner
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                if (i * i + j * j > radius * radius) {
                    guiGraphics.fill(x + i, y + j, x + i + 1, y + j + 1, 0x00000000);
                }
            }
        }

        // Top right corner
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                if (i * i + j * j > radius * radius) {
                    guiGraphics.fill(x + width - i - 1, y + j, x + width - i, y + j + 1, 0x00000000);
                }
            }
        }

        // Bottom left corner
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                if (i * i + j * j > radius * radius) {
                    guiGraphics.fill(x + i, y + height - j - 1, x + i + 1, y + height - j, 0x00000000);
                }
            }
        }

        // Bottom right corner
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                if (i * i + j * j > radius * radius) {
                    guiGraphics.fill(x + width - i - 1, y + height - j - 1, x + width - i, y + height - j, 0x00000000);
                }
            }
        }
    }

    private static void renderItemName(GuiGraphics guiGraphics, List<String> textLines, int centerX, int startY) {
        var poseStack = guiGraphics.pose();
        poseStack.pushMatrix();

        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;

        // Render each line of text
        for (int i = 0; i < textLines.size(); i++) {
            String line = textLines.get(i);
            int textWidth = font.width(line);
            int textX = centerX - textWidth / 2;
            int textY = startY + i * (12 + 2); // 12 pixels height + 2 spacing

            // Render text shadow
            guiGraphics.drawString(font, line, textX + 1, textY + 1, 0xAA000000, false);

            // Render main text with pure white color
            guiGraphics.drawString(font, line, textX, textY, 0xFFFFFFFF, false);
        }

        poseStack.popMatrix();
    }

    private static void renderStackCount(GuiGraphics guiGraphics, int count, int centerX, int textY) {
        var poseStack = guiGraphics.pose();
        poseStack.pushMatrix();

        String countText = "x" + count;
        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        int textWidth = font.width(countText);
        int textX = centerX - textWidth / 2;

        // Render text shadow
        guiGraphics.drawString(font, countText, textX + 1, textY + 1, 0xAA000000, false);

        // Render count text with bright yellow color
        guiGraphics.drawString(font, countText, textX, textY, 0xFFFFFF00, false);

        poseStack.popMatrix();
    }

    private static void renderProfessionalGlow(GuiGraphics guiGraphics, int centerX, int centerY, int width, int height) {
        var poseStack = guiGraphics.pose();
        poseStack.pushMatrix();

        // Calculate glow position
        int glowPadding = 8;
        int glowX = centerX - width / 2 - glowPadding;
        int glowY = centerY - height / 2 - glowPadding;
        int glowWidth = width + glowPadding * 2;
        int glowHeight = height + glowPadding * 2;

        // Create subtle glow effect with multiple layers
        for (int i = 0; i < 4; i++) {
            int alpha = 20 - i * 5;
            int currentPadding = i * 2;
            int currentX = glowX + currentPadding;
            int currentY = glowY + currentPadding;
            int currentWidth = glowWidth - currentPadding * 2;
            int currentHeight = glowHeight - currentPadding * 2;

            // Subtle golden glow
            int glowColor = (alpha << 24) | 0xFFD700;

            // Render glow frame
            guiGraphics.fill(currentX, currentY, currentX + currentWidth, currentY + 1, glowColor);
            guiGraphics.fill(currentX, currentY + currentHeight - 1, currentX + currentWidth, currentY + currentHeight, glowColor);
            guiGraphics.fill(currentX, currentY, currentX + 1, currentY + currentHeight, glowColor);
            guiGraphics.fill(currentX + currentWidth - 1, currentY, currentX + currentWidth, currentY + currentHeight, glowColor);
        }

        poseStack.popMatrix();
    }

    private static boolean isDraggableItem(ItemStack stack) {
        // Check if the item is draggable (only exclude AIR and basic category icons)
        if (stack.isEmpty()) return false;

        // Exclude only the AIR item
        if (stack.getItem() == Items.AIR) return false;

        // Exclude only obvious category icons (those that are not really draggable items)
        String itemName = stack.getItem().getDescriptionId().toLowerCase();

        return !itemName.contains("search") &&
                !itemName.contains("inventory") &&
                !itemName.contains("hotbar");
    }

    private static List<String> wrapText(String text, net.minecraft.client.gui.Font font, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine.toString() + " " + word;
            if (font.width(testLine) <= maxWidth) {
                if (currentLine.length() == 0) {
                    currentLine.append(word);
                } else {
                    currentLine.append(" ").append(word);
                }
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // If a word is longer than the maximum width, split it
                    for (int i = 0; i < word.length(); i++) {
                        String testChar = currentLine.toString() + word.charAt(i);
                        if (font.width(testChar) <= maxWidth) {
                            currentLine.append(word.charAt(i));
                        } else {
                            lines.add(currentLine.toString());
                            currentLine = new StringBuilder(String.valueOf(word.charAt(i)));
                        }
                    }
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private static int getMaxTextWidth(List<String> lines, net.minecraft.client.gui.Font font) {
        int maxWidth = 0;
        for (String line : lines) {
            int lineWidth = font.width(line);
            if (lineWidth > maxWidth) {
                maxWidth = lineWidth;
            }
        }
        return maxWidth;
    }
}