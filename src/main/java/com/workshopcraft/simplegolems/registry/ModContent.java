package com.workshopcraft.simplegolems.registry;

import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.work.ExDeorum;
import com.workshopcraft.simplegolems.block.ProgrammerBlock;
import com.workshopcraft.simplegolems.block.ProgrammerBlockEntity;
import com.workshopcraft.simplegolems.block.UnfiredProgrammerBlock;
import com.workshopcraft.simplegolems.item.CardItem;
import com.workshopcraft.simplegolems.item.GolemItem;
import com.workshopcraft.simplegolems.item.GolemProgram;
import com.workshopcraft.simplegolems.item.GolemSettings;
import com.workshopcraft.simplegolems.item.ItemData;
import com.workshopcraft.simplegolems.item.MarkerCorners;
import com.workshopcraft.simplegolems.item.MarkerSetup;
import com.workshopcraft.simplegolems.item.MarkerStickItem;
import com.workshopcraft.simplegolems.item.ProgramCardItem;
import com.workshopcraft.simplegolems.menu.ProgrammerMenu;
//? if <1.20.5 {
/*import com.workshopcraft.simplegolems.item.ShapedNbtSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;
*///?}
import net.minecraft.core.Registry;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponentType;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.Set;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class ModContent {
    //? if >=1.20.5 {
    public static final DataComponentType<Job> JOB = DataComponentType.<Job>builder().persistent(Job.CODEC).networkSynchronized(Job.STREAM_CODEC).build();
    public static final DataComponentType<DyeColor> COLOUR = DataComponentType.<DyeColor>builder().persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC).build();
    public static final DataComponentType<GolemSettings> SETTINGS = DataComponentType.<GolemSettings>builder().persistent(GolemSettings.CODEC).networkSynchronized(GolemSettings.STREAM_CODEC).build();
    public static final DataComponentType<MarkerCorners> MARKER = DataComponentType.<MarkerCorners>builder().persistent(MarkerCorners.CODEC).networkSynchronized(MarkerCorners.STREAM_CODEC).build();
    public static final DataComponentType<MarkerSetup> MARKER_SETUP = DataComponentType.<MarkerSetup>builder().persistent(MarkerSetup.CODEC).networkSynchronized(MarkerSetup.STREAM_CODEC).build();
    public static final DataComponentType<Boolean> AUGMENTED = DataComponentType.<Boolean>builder().persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL).build();
    public static final DataComponentType<ItemStack> PROGRAM_CARD = DataComponentType.<ItemStack>builder().persistent(ItemStack.OPTIONAL_CODEC).networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC).build();
    public static final DataComponentType<GolemProgram> PROGRAM = DataComponentType.<GolemProgram>builder().persistent(GolemProgram.CODEC).networkSynchronized(GolemProgram.STREAM_CODEC).build();
    //?} else {
    /*public static final ItemData.Key<Job> JOB = new ItemData.Key<>("job", Job.CODEC);
    public static final ItemData.Key<DyeColor> COLOUR = new ItemData.Key<>("colour", DyeColor.CODEC);
    public static final ItemData.Key<GolemSettings> SETTINGS = new ItemData.Key<>("settings", GolemSettings.CODEC);
    public static final ItemData.Key<MarkerCorners> MARKER = new ItemData.Key<>("marker", MarkerCorners.CODEC);
    public static final ItemData.Key<MarkerSetup> MARKER_SETUP = new ItemData.Key<>("marker_setup", MarkerSetup.CODEC);
    public static final ItemData.Key<Boolean> AUGMENTED = new ItemData.Key<>("augmented", com.mojang.serialization.Codec.BOOL);
    public static final ItemData.Key<ItemStack> PROGRAM_CARD = new ItemData.Key<>("program_card", ItemStack.CODEC);
    public static final ItemData.Key<GolemProgram> PROGRAM = new ItemData.Key<>("program", GolemProgram.CODEC);
    *///?}

    public static final ResourceKey<EntityType<?>> GOLEM_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleGolems.id("golem"));
    public static EntityType<GolemEntity> GOLEM_ENTITY;

    public static Item GOLEM_CORE;
    public static Item AUGMENTED_GOLEM_CORE;
    public static Item GOLEM;
    public static Item CLAY_COOKIE;
    public static Item MARKER_STICK;
    public static Item PROGRAM_CARD_ITEM;
    public static Block UNFIRED_PROGRAMMER;
    public static Block PROGRAMMER;
    public static Item UNFIRED_PROGRAMMER_ITEM;
    public static Item PROGRAMMER_ITEM;
    public static BlockEntityType<ProgrammerBlockEntity> PROGRAMMER_ENTITY;
    public static MenuType<ProgrammerMenu> PROGRAMMER_MENU;
    public static final Map<Job, CardItem> CARDS = new EnumMap<>(Job.class);
    public static CreativeModeTab TAB;

    private ModContent() {}

    //? if >=1.20.5 {
    public static void registerComponents(BiConsumer<Identifier, DataComponentType<?>> r) {
        r.accept(SimpleGolems.id("job"), JOB);
        r.accept(SimpleGolems.id("colour"), COLOUR);
        r.accept(SimpleGolems.id("settings"), SETTINGS);
        r.accept(SimpleGolems.id("marker"), MARKER);
        r.accept(SimpleGolems.id("marker_setup"), MARKER_SETUP);
        r.accept(SimpleGolems.id("augmented"), AUGMENTED);
        r.accept(SimpleGolems.id("program_card"), PROGRAM_CARD);
        r.accept(SimpleGolems.id("program"), PROGRAM);
    }
    //?}

    //? if <1.20.5 {
    /*public static void registerRecipeSerializers(BiConsumer<Identifier, RecipeSerializer<?>> r) {
        r.accept(SimpleGolems.id("shaped_nbt"), new ShapedNbtSerializer());
    }

    *///?}
    public static void registerEntities(BiConsumer<Identifier, EntityType<?>> r) {
        GOLEM_ENTITY = EntityType.Builder.of(GolemEntity::new, MobCategory.MISC)
        //? if >=1.21.2 {
                .sized(0.7F, 0.9F).eyeHeight(0.65F).clientTrackingRange(10).build(GOLEM_KEY);
        r.accept(GOLEM_KEY.identifier(), GOLEM_ENTITY);
        //?} else {
                /*.sized(0.7F, 0.9F).clientTrackingRange(10).build("golem");
        r.accept(GOLEM_KEY.location(), GOLEM_ENTITY);
        *///?}
    }

    private static Item.Properties item(String name) {
        //? if >=1.21.2 {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, SimpleGolems.id(name)));
        //?} else {
        /*return new Item.Properties();
        *///?}
    }

    private static BlockBehaviour.Properties block(String name, MapColor colour, float strength, SoundType sound) {
        //? if >=1.21.2 {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, SimpleGolems.id(name)))
        //?} else {
        /*return BlockBehaviour.Properties.of()
        *///?}
                .mapColor(colour).strength(strength).sound(sound);
    }

    public static void registerBlocks(BiConsumer<Identifier, Block> r) {
        UNFIRED_PROGRAMMER = new UnfiredProgrammerBlock(block("unfired_programmer", MapColor.CLAY, 0.6F, SoundType.GRAVEL));
        r.accept(SimpleGolems.id("unfired_programmer"), UNFIRED_PROGRAMMER);
        PROGRAMMER = new ProgrammerBlock(block("programmer", MapColor.TERRACOTTA_ORANGE, 1.25F, SoundType.STONE));
        r.accept(SimpleGolems.id("programmer"), PROGRAMMER);
    }

    public static void registerBlockEntities(BiConsumer<Identifier, BlockEntityType<?>> r) {
        //? if >=26.1 {
        PROGRAMMER_ENTITY = new BlockEntityType<>(ProgrammerBlockEntity::new, Set.of(PROGRAMMER));
        //?} else {
        /*PROGRAMMER_ENTITY = BlockEntityType.Builder.of(ProgrammerBlockEntity::new, PROGRAMMER).build(null);
        *///?}
        r.accept(SimpleGolems.id("programmer"), PROGRAMMER_ENTITY);
    }

    public static void registerMenus(BiConsumer<Identifier, MenuType<?>> r) {
        PROGRAMMER_MENU = new MenuType<>(ProgrammerMenu::client, FeatureFlags.VANILLA_SET);
        r.accept(SimpleGolems.id("programmer"), PROGRAMMER_MENU);
    }

    public static void registerItems(BiConsumer<Identifier, Item> r) {
        GOLEM_CORE = new Item(item("golem_core"));
        r.accept(SimpleGolems.id("golem_core"), GOLEM_CORE);
        AUGMENTED_GOLEM_CORE = new Item(item("augmented_golem_core"));
        r.accept(SimpleGolems.id("augmented_golem_core"), AUGMENTED_GOLEM_CORE);
        GOLEM = new GolemItem(item("golem").stacksTo(1));
        r.accept(SimpleGolems.id("golem"), GOLEM);
        CLAY_COOKIE = new Item(item("clay_cookie"));
        r.accept(SimpleGolems.id("clay_cookie"), CLAY_COOKIE);
        MARKER_STICK = new MarkerStickItem(item("marker_stick").stacksTo(1));
        r.accept(SimpleGolems.id("marker_stick"), MARKER_STICK);
        PROGRAM_CARD_ITEM = new ProgramCardItem(item("program_card").stacksTo(16));
        r.accept(SimpleGolems.id("program_card"), PROGRAM_CARD_ITEM);
        //? if >=1.21.2 {
        UNFIRED_PROGRAMMER_ITEM = new BlockItem(UNFIRED_PROGRAMMER, item("unfired_programmer").useBlockDescriptionPrefix());
        PROGRAMMER_ITEM = new BlockItem(PROGRAMMER, item("programmer").useBlockDescriptionPrefix());
        //?} else {
        /*UNFIRED_PROGRAMMER_ITEM = new BlockItem(UNFIRED_PROGRAMMER, item("unfired_programmer"));
        PROGRAMMER_ITEM = new BlockItem(PROGRAMMER, item("programmer"));
        *///?}
        r.accept(SimpleGolems.id("unfired_programmer"), UNFIRED_PROGRAMMER_ITEM);
        r.accept(SimpleGolems.id("programmer"), PROGRAMMER_ITEM);
        for (Job job : Job.values()) {
            if (job == Job.BLANK) continue;
            String name = job.getSerializedName() + "_card";
            CardItem card = new CardItem(job, item(name).stacksTo(16));
            CARDS.put(job, card);
            r.accept(SimpleGolems.id(name), card);
        }
    }

    public static ItemStack card(Job job) {
        CardItem card = CARDS.get(job);
        return card == null ? ItemStack.EMPTY : new ItemStack(card);
    }

    public static void registerCreativeTabs(BiConsumer<Identifier, CreativeModeTab> r) {
        TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup." + SimpleGolems.MOD_ID))
                .icon(() -> GolemItem.of(Job.LUMBER, DyeColor.ORANGE))
                .displayItems((params, out) -> {
                    out.accept(GOLEM_CORE);
                    out.accept(AUGMENTED_GOLEM_CORE);
                    out.accept(CLAY_COOKIE);
                    out.accept(MARKER_STICK);
                    out.accept(PROGRAM_CARD_ITEM);
                    out.accept(UNFIRED_PROGRAMMER_ITEM);
                    out.accept(PROGRAMMER_ITEM);
                    for (Job job : Job.values()) if (shown(job) && CARDS.containsKey(job)) out.accept(CARDS.get(job));
                    for (Job job : Job.values()) if (shown(job)) out.accept(GolemItem.of(job, null));
                    for (DyeColor c : DyeColor.values()) out.accept(GolemItem.of(Job.BLANK, c));
                })
                .build();
        r.accept(SimpleGolems.id("main"), TAB);
    }

    // the Ex Deorum programs only show when it is installed
    private static boolean shown(Job job) {
        return !job.needsExDeorum() || ExDeorum.loaded();
    }

    public static <T> BiConsumer<Identifier, T> direct(Registry<? super T> registry) {
        return (id, value) -> Registry.register(registry, id, value);
    }

    public static void registerAllDirect() {
        //? if >=1.20.5 {
        registerComponents(direct(BuiltInRegistries.DATA_COMPONENT_TYPE));
        //?}
        registerEntities(direct(BuiltInRegistries.ENTITY_TYPE));
        registerBlocks(direct(BuiltInRegistries.BLOCK));
        registerBlockEntities(direct(BuiltInRegistries.BLOCK_ENTITY_TYPE));
        registerMenus(direct(BuiltInRegistries.MENU));
        registerItems(direct(BuiltInRegistries.ITEM));
        registerCreativeTabs(direct(BuiltInRegistries.CREATIVE_MODE_TAB));
        //? if <1.20.5 {
        /*registerRecipeSerializers(direct(BuiltInRegistries.RECIPE_SERIALIZER));
        *///?}
    }
}
