package com.workshopcraft.simplegolems.item;

import com.workshopcraft.simplegolems.registry.ModContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=1.21.5 {
import net.minecraft.world.item.component.TooltipDisplay;
//?}
//? if <1.20.5 {
/*import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
*///?}

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ProgramCardItem extends Item {
    public ProgramCardItem(Properties properties) {
        super(properties);
    }

    public static GolemProgram program(ItemStack stack) {
        return ItemData.get(stack, ModContent.PROGRAM);
    }

    public static void write(ItemStack stack, GolemProgram program) {
        if (program == null || program.isEmpty()) ItemData.remove(stack, ModContent.PROGRAM);
        else ItemData.set(stack, ModContent.PROGRAM, program);
    }

    @Override
    //? if >=1.21.5 {
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
    //?} else if >=1.20.5 {
    /*public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Consumer<Component> out = lines::add;
    *///?} else {
    /*public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        Consumer<Component> out = lines::add;
    *///?}
        GolemProgram p = program(stack);
        if (p == null || p.isEmpty()) {
            out.accept(Component.translatable("tooltip.simplegolems.program.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        out.accept(Component.translatable("tooltip.simplegolems.program." + p.kind().getSerializedName()).withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < p.kind().settings().size(); i++) {
            if (p.option(i) != 0) out.accept(Component.translatable("%s: %s", settingName(p.kind(), i), valueName(p.kind(), i, p.option(i))).withStyle(ChatFormatting.DARK_GRAY));
        }
        List<Component> names = new ArrayList<>();
        for (ItemStack s : p.filter()) {
            if (!s.isEmpty()) names.add(s.getHoverName());
        }
        if (!names.isEmpty()) {
            Component list = names.get(0);
            for (int i = 1; i < names.size(); i++) list = Component.translatable("%s, %s", list, names.get(i));
            out.accept(Component.translatable("tooltip.simplegolems.filter." + p.kind().getSerializedName(), list).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!p.route().isEmpty()) out.accept(Component.translatable("tooltip.simplegolems.program.route", p.route().size()).withStyle(ChatFormatting.DARK_GRAY));
    }

    public static Component settingName(GolemProgram.Kind kind, int i) {
        return Component.translatable("program.simplegolems." + kind.settings().get(i).key());
    }

    // numbers show as they are, words come from the lang file
    public static Component valueName(GolemProgram.Kind kind, int i, int choice) {
        GolemProgram.Setting set = kind.settings().get(i);
        String v = set.values()[Math.floorMod(choice, set.values().length)];
        if (v.matches("m?\\d+")) return Component.literal(v.startsWith("m") ? "-" + v.substring(1) : v);
        return Component.translatable("program.simplegolems." + set.key() + "." + v);
    }
}
