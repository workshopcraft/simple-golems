package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.item.GolemProgram;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

public class SweeperWork implements JobWork {
    public static final SweeperWork INSTANCE = new SweeperWork();

    @Override
    public boolean wants(ItemEntity item) {
        return true;
    }

    // a program's filter is the only items it picks up, or the items it leaves
    @Override
    public boolean wants(GolemEntity golem, ItemEntity item) {
        GolemProgram p = golem.program();
        if (p == null || !p.hasFilter()) return true;
        return p.matches(item.getItem()) != p.is(0, "blacklist");
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        return null;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return false;
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return 0;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
    }
}
