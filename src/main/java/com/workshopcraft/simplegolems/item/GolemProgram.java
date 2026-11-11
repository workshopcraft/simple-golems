package com.workshopcraft.simplegolems.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
//?}

import java.util.ArrayList;
import java.util.List;

// a program written onto a program card, currently a courier item filter
public record GolemProgram(Kind kind, List<ItemStack> filter) {
    public static final GolemProgram COURIER_EMPTY = new GolemProgram(Kind.COURIER_FILTER, List.of());
    public static final Codec<GolemProgram> CODEC = RecordCodecBuilder.create(i -> i.group(
            Kind.CODEC.optionalFieldOf("kind", Kind.COURIER_FILTER).forGetter(GolemProgram::kind),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("filter", List.of()).forGetter(GolemProgram::filter)
    ).apply(i, GolemProgram::new));
    //? if >=1.20.5 {
    public static final StreamCodec<RegistryFriendlyByteBuf, GolemProgram> STREAM_CODEC = StreamCodec.composite(
            Kind.STREAM_CODEC, GolemProgram::kind,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(9)), GolemProgram::filter,
            GolemProgram::new);
    //?}

    public GolemProgram {
        filter = List.copyOf(filter);
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (ItemStack s : filter) {
            if (!s.isEmpty() && s.is(stack.getItem())) return true;
        }
        return false;
    }

    public boolean isEmpty() {
        for (ItemStack s : filter) if (!s.isEmpty()) return false;
        return true;
    }

    public List<ItemStack> paddedFilter() {
        List<ItemStack> out = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) out.add(i < filter.size() ? filter.get(i).copy() : ItemStack.EMPTY);
        return out;
    }

    public enum Kind implements StringRepresentable {
        COURIER_FILTER("courier_filter");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        //? if >=1.20.5 {
        public static final StreamCodec<io.netty.buffer.ByteBuf, Kind> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Kind::ordinal);
        //?}

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
