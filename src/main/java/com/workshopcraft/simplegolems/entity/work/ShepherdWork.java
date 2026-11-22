package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.Mc;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import com.workshopcraft.simplegolems.item.GolemProgram;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

// shears grown sheep in its area; the wool goes to storage. A program can also dye them one colour and breed them up to a count
public class ShepherdWork implements JobWork {
    public static final ShepherdWork INSTANCE = new ShepherdWork();

    @Override
    public boolean wants(ItemEntity item) {
        return item.getItem().is(ItemTags.WOOL);
    }

    // the colour of the first dye in the program's filter
    private static @Nullable DyeColor colour(GolemEntity golem) {
        GolemProgram p = golem.program();
        if (p == null) return null;
        for (ItemStack f : p.filter()) {
            DyeColor c = Mc.dye(f);
            if (c != null) return c;
        }
        return null;
    }

    private static int breedCap(GolemEntity golem) {
        GolemProgram p = golem.program();
        return p == null ? 0 : p.number(0, 0);
    }

    private static boolean isDye(ItemStack s, @Nullable DyeColor c) {
        return c != null && Mc.dye(s) == c;
    }

    private static boolean isFeed(ItemStack s) {
        //? if >=1.20.5 {
        return s.is(ItemTags.SHEEP_FOOD);
        //?} else {
        /*return s.is(net.minecraft.world.item.Items.WHEAT);
        *///?}
    }

    private static boolean shearable(Sheep s) {
        return s.isAlive() && s.readyForShearing();
    }

    private static boolean dyeable(GolemEntity golem, Sheep s, @Nullable DyeColor c) {
        return c != null && s.isAlive() && !s.isSheared() && s.getColor() != c && golem.carries(d -> isDye(d, c));
    }

    private static List<Animal> breedable(GolemEntity golem, ServerLevel level) {
        int cap = breedCap(golem);
        return cap <= 0 ? List.of() : RancherWork.breedable(level, golem.getArea(), a -> a instanceof Sheep, cap);
    }

    private static boolean wanted(GolemEntity golem, Sheep s, @Nullable DyeColor c, List<Animal> breed) {
        return shearable(s) || dyeable(golem, s, c) || breed.contains(s) && golem.carries(ShepherdWork::isFeed);
    }

    private static @Nullable Sheep near(GolemEntity golem, ServerLevel level, AABB box, BlockPos from) {
        DyeColor c = colour(golem);
        List<Animal> breed = breedable(golem, level);
        return level.getEntitiesOfClass(Sheep.class, box, s -> wanted(golem, s, c, breed)).stream()
                .min(Comparator.comparingDouble(s -> s.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(from)))).orElse(null);
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        // sheep standing on a flat area marked on the ground count too
        DyeColor c = colour(golem);
        List<Animal> breed = breedable(golem, level);
        Sheep s = level.getEntitiesOfClass(Sheep.class, area.box().expandTowards(0, 1, 0), e -> wanted(golem, e, c, breed) && !skip.test(e.blockPosition())).stream()
                .min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
        return s == null ? null : s.blockPosition();
    }

    // dye or feed it is out of, when storage has some
    @Override
    public @Nullable Predicate<ItemStack> restock(GolemEntity golem) {
        if (!(golem.level() instanceof ServerLevel level)) return null;
        DyeColor c = colour(golem);
        Predicate<ItemStack> want = null;
        if (c != null && !golem.carries(d -> isDye(d, c))
                && !level.getEntitiesOfClass(Sheep.class, golem.getArea().box(), s -> s.isAlive() && !s.isSheared() && s.getColor() != c).isEmpty()) {
            want = d -> isDye(d, c);
        } else if (!golem.carries(ShepherdWork::isFeed) && !breedable(golem, level).isEmpty()) {
            want = ShepherdWork::isFeed;
        }
        Predicate<ItemStack> w = want;
        return w != null && Storage.nearest(level, golem.getArea(), golem.position(), k -> k.hasAnyMatching(w), p -> false) != null ? w : null;
    }

    @Override
    public boolean keeps(GolemEntity golem, int slot) {
        ItemStack s = golem.getCarried().get(slot);
        return isDye(s, colour(golem)) || breedCap(golem) > 0 && isFeed(s);
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(Sheep.class, new AABB(pos).inflate(2.5), ShepherdWork::shearable).isEmpty();
    }

    @Override
    public boolean isValid(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return near(golem, level, new AABB(pos).inflate(2.5), pos) != null;
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return 10;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        Sheep s = near(golem, level, golem.getBoundingBox().inflate(3.0), golem.blockPosition());
        if (s == null) return;
        DyeColor c = colour(golem);
        if (!shearable(s) && dyeable(golem, s, c)) {
            for (ItemStack d : golem.getCarried()) {
                if (!isDye(d, c)) continue;
                s.setColor(c);
                d.shrink(1);
                golem.carriedChanged();
                level.playSound(null, s, SoundEvents.DYE_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                return;
            }
        }
        if (!shearable(s)) {
            for (ItemStack f : golem.getCarried()) {
                if (!isFeed(f) || !RancherWork.ready(s)) continue;
                f.shrink(1);
                golem.carriedChanged();
                s.setInLove(null);
                golem.playSound(Mc.eatSound(), 0.5F, 1.2F);
                return;
            }
            return;
        }
        ItemStack tool = golem.getTool();
        if (tool.isEmpty()) return;
        AABB around = s.getBoundingBox().inflate(4.0);
        Set<ItemEntity> before = new HashSet<>(level.getEntitiesOfClass(ItemEntity.class, around));
        //? if >=1.21.2 {
        s.shear(level, SoundSource.NEUTRAL, tool);
        //?} else {
        /*s.shear(SoundSource.NEUTRAL);
        *///?}
        Mc.wear(tool, golem);
        // the wool goes straight into its carry, so none of it lands out of reach over a fence
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, around, i -> i.isAlive() && !before.contains(i) && wants(i))) {
            Storage.pickUp(golem, e);
        }
    }
}
