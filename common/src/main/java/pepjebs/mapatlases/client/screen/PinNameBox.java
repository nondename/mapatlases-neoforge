package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.ClientMarkersRenderer;

/**
 * Marker editor inspired by Antique Atlas' marker finalizer.
 *
 * Instead of hiding marker selection behind a mouse-wheel carousel, all
 * available Map Atlases pin styles are visible at once. The selected icon is
 * remembered between placements, which makes placing several related markers
 * much less tedious.
 */
public class PinNameBox extends EditBox {

    private static final int PIN_COUNT = 9;
    private static final int CELL_SIZE = 22;
    private static final int ICON_SCALE = 2;
    private static final int PALETTE_GAP = 8;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_GAP = 4;

    private static int currentIndex = 0;

    private final Font font;
    private final Runnable onDone;
    private final Runnable onCancel;

    private int hoveredPin = -1;
    private boolean saveHovered = false;
    private boolean cancelHovered = false;

    public PinNameBox(Font pFont, int pX, int pY, int pWidth, int pHeight,
                      Component pMessage, Runnable onDone, Runnable onCancel) {
        super(pFont, pX, pY, pWidth, pHeight, pMessage);
        this.font = pFont;
        this.onDone = onDone;
        this.onCancel = onCancel;
        this.active = false;
        this.visible = false;
        this.setFocused(false);
        this.setCanLoseFocus(true);
        this.setMaxLength(32);
    }

    public int getIndex() {
        return currentIndex;
    }

    private int paletteWidth() {
        return PIN_COUNT * CELL_SIZE;
    }

    private int paletteX() {
        return getX() + (getWidth() - paletteWidth()) / 2;
    }

    private int paletteY() {
        return getY() + getHeight() + 25;
    }

    private int buttonsY() {
        return paletteY() + CELL_SIZE + 9;
    }

    private int halfButtonWidth() {
        return (getWidth() - BUTTON_GAP) / 2;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 30);

        graphics.drawCenteredString(font,
                Component.translatable("message.map_atlases.marker_editor"),
                getX() + getWidth() / 2, getY() - 15, 0xFFFFFF);

        graphics.drawString(font,
                Component.translatable("message.map_atlases.marker_name"),
                getX(), getY() - 2 - font.lineHeight, 0xDDDDDD, false);

        super.renderWidget(graphics, mouseX, mouseY, partialTicks);

        int px = paletteX();
        int py = paletteY();

        graphics.drawCenteredString(font,
                Component.translatable("message.map_atlases.marker_type"),
                getX() + getWidth() / 2, py - 12, 0xDDDDDD);

        graphics.fill(px - 3, py - 3,
                px + paletteWidth() + 3, py + CELL_SIZE + 3,
                0xB0101010);

        hoveredPin = -1;
        if (MapAtlasesMod.MOONLIGHT) {
            for (int i = 0; i < PIN_COUNT; i++) {
                int cellX = px + i * CELL_SIZE;
                boolean hovered = mouseX >= cellX && mouseX < cellX + CELL_SIZE
                        && mouseY >= py && mouseY < py + CELL_SIZE;
                if (hovered) hoveredPin = i;

                if (i == currentIndex) {
                    graphics.fill(cellX, py, cellX + CELL_SIZE, py + CELL_SIZE, 0xCCB9A16B);
                    graphics.fill(cellX + 1, py + 1, cellX + CELL_SIZE - 1, py + CELL_SIZE - 1, 0xCC2B2418);
                } else if (hovered) {
                    graphics.fill(cellX, py, cellX + CELL_SIZE, py + CELL_SIZE, 0x886F6654);
                }

                pose.pushPose();
                pose.translate(cellX + CELL_SIZE / 2f, py + CELL_SIZE / 2f, 0);
                pose.scale(ICON_SCALE, ICON_SCALE, 1);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                ClientMarkersRenderer.renderDecorationPreview(graphics, 0, 0, i,
                        i == currentIndex || hovered, 255);
                pose.popPose();
            }
        }

        int by = buttonsY();
        int bw = halfButtonWidth();
        int cancelX = getX() + bw + BUTTON_GAP;

        saveHovered = mouseX >= getX() && mouseX < getX() + bw
                && mouseY >= by && mouseY < by + BUTTON_HEIGHT;
        cancelHovered = mouseX >= cancelX && mouseX < cancelX + bw
                && mouseY >= by && mouseY < by + BUTTON_HEIGHT;

        renderButton(graphics, getX(), by, bw, BUTTON_HEIGHT,
                Component.translatable("gui.done"), saveHovered);
        renderButton(graphics, cancelX, by, bw, BUTTON_HEIGHT,
                Component.translatable("gui.cancel"), cancelHovered);

        pose.popPose();
    }

    private void renderButton(GuiGraphics graphics, int x, int y, int w, int h,
                              Component label, boolean hovered) {
        graphics.fill(x, y, x + w, y + h, hovered ? 0xCC6F6654 : 0xCC2B2418);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1,
                hovered ? 0xCC3A3326 : 0xCC17130E);
        graphics.drawCenteredString(font, label, x + w / 2,
                y + (h - font.lineHeight) / 2 + 1, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && active && canConsumeInput()) {
            onDone.run();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && active) {
            onCancel.run();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int px = paletteX();
        int py = paletteY();
        if (mouseY >= py && mouseY < py + CELL_SIZE
                && mouseX >= px && mouseX < px + paletteWidth()) {
            int selected = (int) ((mouseX - px) / CELL_SIZE);
            currentIndex = Math.max(0, Math.min(PIN_COUNT - 1, selected));
            playClick();
            return true;
        }

        int by = buttonsY();
        int bw = halfButtonWidth();
        if (mouseY >= by && mouseY < by + BUTTON_HEIGHT) {
            if (mouseX >= getX() && mouseX < getX() + bw) {
                playClick();
                onDone.run();
                return true;
            }
            int cancelX = getX() + bw + BUTTON_GAP;
            if (mouseX >= cancelX && mouseX < cancelX + bw) {
                playClick();
                onCancel.run();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int px = paletteX();
        int py = paletteY();
        if (mouseX >= px && mouseX < px + paletteWidth()
                && mouseY >= py && mouseY < py + CELL_SIZE && delta != 0) {
            currentIndex = Math.floorMod(currentIndex - (int) Math.signum(delta), PIN_COUNT);
            playClick();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
