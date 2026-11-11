package com.workshopcraft.simplegolems.client;

import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.menu.ProgrammerMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

public class ProgrammerScreen extends AbstractContainerScreen<ProgrammerMenu> {
    private static final Identifier TEXTURE = SimpleGolems.id("textures/gui/programmer.png");

    public ProgrammerScreen(ProgrammerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.simplegolems.programmer.copy"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ProgrammerMenu.BTN_COPY))
                .bounds(leftPos + 124, topPos + 18, 44, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.simplegolems.programmer.clear"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ProgrammerMenu.BTN_CLEAR))
                .bounds(leftPos + 124, topPos + 44, 44, 18).build());
    }

    //? if >=26.1 {
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        graphics.text(font, Component.translatable("gui.simplegolems.programmer.card"), 16, 16, 0x404040, false);
        graphics.text(font, Component.translatable("gui.simplegolems.programmer.copy_to"), 16, 42, 0x404040, false);
    }
    //?} else {
    /*@Override
    protected void renderBg(GuiGraphics graphics, float a, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int xm, int ym) {
        super.renderLabels(graphics, xm, ym);
        graphics.drawString(font, Component.translatable("gui.simplegolems.programmer.card"), 16, 16, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.simplegolems.programmer.copy_to"), 16, 42, 0x404040, false);
    }
    *///?}
}
