package com.workshopcraft.simplegolems.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplegolems.entity.Job;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
//?}

import java.util.ArrayList;
import java.util.List;

// a program written onto a program card: which job it is for, a 3x3 filter, a few job settings and a guard's patrol route
public record GolemProgram(Kind kind, List<ItemStack> filter, List<Integer> options, List<BlockPos> route) {
    public static final int OPTIONS = 3;
    public static final int MAX_ROUTE = 16;
    public static final GolemProgram COURIER_EMPTY = new GolemProgram(Kind.COURIER_FILTER, List.of());
    public static final Codec<GolemProgram> CODEC = RecordCodecBuilder.create(i -> i.group(
            Kind.CODEC.optionalFieldOf("kind", Kind.COURIER_FILTER).forGetter(GolemProgram::kind),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("filter", List.of()).forGetter(GolemProgram::filter),
            Codec.INT.listOf().optionalFieldOf("options", List.of()).forGetter(GolemProgram::options),
            BlockPos.CODEC.listOf().optionalFieldOf("route", List.of()).forGetter(GolemProgram::route)
    ).apply(i, GolemProgram::new));
    //? if >=1.20.5 {
    public static final StreamCodec<RegistryFriendlyByteBuf, GolemProgram> STREAM_CODEC = StreamCodec.composite(
            Kind.STREAM_CODEC, GolemProgram::kind,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(9)), GolemProgram::filter,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(OPTIONS)), GolemProgram::options,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROUTE)), GolemProgram::route,
            GolemProgram::new);
    //?}

    public GolemProgram {
        filter = List.copyOf(filter);
        options = List.copyOf(options);
        route = List.copyOf(route);
    }

    public GolemProgram(Kind kind, List<ItemStack> filter) {
        this(kind, filter, List.of(), List.of());
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (ItemStack s : filter) {
            if (!s.isEmpty() && s.is(stack.getItem())) return true;
        }
        return false;
    }

    public boolean hasFilter() {
        for (ItemStack s : filter) if (!s.isEmpty()) return true;
        return false;
    }

    public boolean isEmpty() {
        if (hasFilter() || !route.isEmpty()) return false;
        for (int i = 0; i < OPTIONS; i++) if (option(i) != 0) return false;
        return true;
    }

    // the chosen value of a setting, 0 being the job's normal behaviour
    public int option(int i) {
        int n = kind.choices(i);
        return i < options.size() && n > 0 ? Math.floorMod(options.get(i), n) : 0;
    }

    public String value(int i) {
        return kind.settings().get(i).values()[option(i)];
    }

    // a number setting, or the fallback when it is left on its normal value
    public int number(int i, int fallback) {
        String v = value(i);
        return option(i) == 0 ? fallback : Integer.parseInt(v.startsWith("m") ? "-" + v.substring(1) : v);
    }

    public boolean is(int i, String v) {
        return i < kind.settings().size() && value(i).equals(v);
    }

    public List<ItemStack> paddedFilter() {
        List<ItemStack> out = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) out.add(i < filter.size() ? filter.get(i).copy() : ItemStack.EMPTY);
        return out;
    }

    public List<Integer> paddedOptions() {
        List<Integer> out = new ArrayList<>(OPTIONS);
        for (int i = 0; i < OPTIONS; i++) out.add(option(i));
        return out;
    }

    public record Setting(String key, String... values) {
    }

    public enum Kind implements StringRepresentable {
        COURIER_FILTER("courier_filter", Job.COURIER),
        GUARD("guard", Job.GUARD, new Setting("mobs", "priority", "ignore")),
        FARMER("farmer", Job.FARMER, new Setting("replant", "all", "half", "none"), new Setting("surplus", "chest", "compost"), new Setting("till", "off", "on")),
        LUMBER("lumber", Job.LUMBER, new Setting("leave", "0", "1", "2", "4", "8", "16"), new Setting("grid", "off", "on")),
        MINER("miner", Job.MINER, new Setting("junk", "keep", "drop", "void"), new Setting("pattern", "quarry", "branch", "stairs"),
                new Setting("stop_at", "off", "48", "32", "16", "0", "m16", "m32", "m48")),
        RANCHER("rancher", Job.RANCHER, new Setting("herd", "default", "4", "8", "12", "16", "24", "32")),
        SHEPHERD("shepherd", Job.SHEPHERD, new Setting("breed", "off", "4", "8", "12", "16", "24", "32")),
        FISHER("fisher", Job.FISHER, new Setting("keep", "all", "fish", "treasure")),
        SMELTER("smelter", Job.SMELTER, new Setting("fuel", "default", "1", "4", "8", "16")),
        SWEEPER("sweeper", Job.SWEEPER, new Setting("mode", "whitelist", "blacklist"));

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        //? if >=1.20.5 {
        public static final StreamCodec<io.netty.buffer.ByteBuf, Kind> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Kind::ordinal);
        //?}

        private final String name;
        public final Job job;
        private final List<Setting> settings;

        Kind(String name, Job job, Setting... settings) {
            this.name = name;
            this.job = job;
            this.settings = List.of(settings);
        }

        public List<Setting> settings() {
            return settings;
        }

        public int choices(int i) {
            return i < settings.size() ? settings.get(i).values().length : 0;
        }

        public static @Nullable Kind of(Job job) {
            for (Kind k : values()) if (k.job == job) return k;
            return null;
        }

        public static Kind byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : COURIER_FILTER;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
