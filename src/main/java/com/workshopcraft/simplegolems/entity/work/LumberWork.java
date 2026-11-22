package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.Mc;
import com.workshopcraft.simplegolems.registry.ModTags;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import com.workshopcraft.simplegolems.item.GolemProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class LumberWork implements JobWork {
    public static final LumberWork INSTANCE = new LumberWork();
    private static final int MAX_LOGS = 256;
    private static final int MAX_REACH = 12;

    record Tree(List<BlockPos> logs, List<BlockPos> leaves, List<BlockPos> bases) {
    }

    @Override
    public boolean wants(ItemEntity item) {
        ItemStack s = item.getItem();
        return Mc.tags(item).contains(Storage.WORK_TAG) || s.is(ModTags.SAPLINGS) || s.is(ModTags.LOG_ITEMS)
                || s.is(Items.STICK) || s.is(Items.APPLE);
    }

    private static boolean isBase(ServerLevel level, BlockPos pos, BlockState state) {
        return state.is(ModTags.LOGS) && level.getBlockState(pos.below()).is(BlockTags.SUBSTRATE_OVERWORLD);
    }

    // logs in the program's filter pick which trees it fells; with none, it fells every tree
    static boolean fells(@Nullable GolemProgram p, Block log) {
        if (p == null) return true;
        boolean logs = false;
        for (ItemStack f : p.filter()) {
            if (!f.is(ModTags.LOG_ITEMS)) continue;
            if (f.getItem() instanceof BlockItem bi && bi.getBlock() == log) return true;
            logs = true;
        }
        return !logs;
    }

    // saplings in the filter pick what it plants; with none, any sapling
    static Predicate<ItemStack> saplings(@Nullable GolemProgram p) {
        if (p == null) return s -> true;
        List<ItemStack> chosen = p.filter().stream().filter(f -> f.is(ModTags.SAPLINGS)).toList();
        return s -> chosen.isEmpty() || chosen.stream().anyMatch(f -> f.is(s.getItem()));
    }

    private static boolean grid(@Nullable GolemProgram p) {
        return p != null && p.is(1, "on");
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        GolemProgram prog = golem.program();
        List<BlockPos> bases = new ArrayList<>();
        List<BlockPos> orphans = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(area.min(), area.max())) {
            BlockState s = level.getBlockState(p);
            if (!s.is(ModTags.LOGS) || !fells(prog, s.getBlock())) continue;
            if (!golem.canHarvest(s) || skip.test(p)) continue;
            BlockPos imm = p.immutable();
            if (isBase(level, p, s)) bases.add(imm);
            else orphans.add(imm);
        }
        bases.sort(Comparator.comparingDouble(p -> golem.distanceToSqr(Vec3.atCenterOf(p))));
        // a set number of grown trees is always left standing
        int leave = prog == null ? 0 : prog.number(0, 0);
        boolean holding = leave > 0 && trees(level, area, prog) <= leave;
        for (BlockPos p : holding ? List.<BlockPos>of() : bases) {
            if (tree(level, p) != null) return p;
        }
        // floating leftovers after an axe broke mid-fell: retarget the lowest remaining log of that tree
        orphans.sort(Comparator.<BlockPos>comparingInt(p -> (int) p.getY()).thenComparingDouble(p -> golem.distanceToSqr(Vec3.atCenterOf(p))));
        Set<BlockPos> seen = new HashSet<>();
        for (BlockPos p : orphans) {
            if (seen.contains(p)) continue;
            Tree t = tree(level, p);
            if (t == null) continue;
            seen.addAll(t.logs());
            if (holding && !t.bases().isEmpty()) continue;
            BlockPos lowest = t.logs().stream().min(Comparator.comparingInt(p2 -> (int) p2.getY())).orElse(p);
            if (!skip.test(lowest) && golem.canHarvest(level.getBlockState(lowest))) return lowest;
        }
        return grid(prog) ? gridSpot(golem, level, area, skip) : null;
    }

    // grown trees of the kinds it fells, each counted once
    private static int trees(ServerLevel level, WorkArea area, @Nullable GolemProgram prog) {
        Set<BlockPos> seen = new HashSet<>();
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(area.min(), area.max())) {
            BlockState s = level.getBlockState(p);
            if (seen.contains(p) || !s.is(ModTags.LOGS) || !fells(prog, s.getBlock()) || !isBase(level, p, s)) continue;
            Tree t = tree(level, p.immutable());
            if (t == null) continue;
            seen.addAll(t.logs());
            n++;
        }
        return n;
    }

    private static final int GRID = 4;

    // ground on the planting grid with room for a tree, when it has a sapling to hand or in storage
    private static @Nullable BlockPos gridSpot(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        Predicate<ItemStack> ok = saplings(golem.program());
        boolean have = golem.carries(s -> s.is(ModTags.SAPLINGS) && ok.test(s))
                || Storage.nearest(level, area, golem.position(), c -> c.hasAnyMatching(s -> s.is(ModTags.SAPLINGS) && ok.test(s)), skip) != null;
        if (!have) return null;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = area.min().getX(); x <= area.max().getX(); x++) {
            for (int z = area.min().getZ(); z <= area.max().getZ(); z++) {
                if (Math.floorMod(x - area.min().getX() - GRID / 2, GRID) != 0 || Math.floorMod(z - area.min().getZ() - GRID / 2, GRID) != 0) continue;
                for (int y = area.max().getY(); y >= area.min().getY(); y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!isGridSpot(level, p)) continue;
                    double d = golem.distanceToSqr(Vec3.atCenterOf(p));
                    if (!skip.test(p) && d < bestDist) {
                        best = p;
                        bestDist = d;
                    }
                    break;
                }
            }
        }
        return best;
    }

    private static boolean isGridSpot(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (!s.canBeReplaced() || !s.getFluidState().isEmpty() || !level.getBlockState(p.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) return false;
        for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, 0, -1), p.offset(1, 1, 1))) {
            BlockState ns = level.getBlockState(n);
            if (ns.is(ModTags.LOGS) || ns.is(BlockTags.SAPLINGS)) return false;
        }
        return true;
    }

    // a grown tree has natural leaves touching its logs; log cabins and stripped builds are left alone
    static @Nullable Tree tree(ServerLevel level, BlockPos base) {
        if (!level.getBlockState(base).is(ModTags.LOGS)) return null;
        Block kind = level.getBlockState(base).getBlock();
        Set<BlockPos> logs = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        logs.add(base);
        queue.add(base);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, 0, -1), p.offset(1, 1, 1))) {
                if (logs.contains(n) || Math.abs(n.getX() - base.getX()) > MAX_REACH || Math.abs(n.getZ() - base.getZ()) > MAX_REACH) continue;
                BlockState s = level.getBlockState(n);
                if (!s.is(kind)) continue;
                BlockPos imm = n.immutable();
                logs.add(imm);
                queue.add(imm);
                if (logs.size() > MAX_LOGS) return null;
            }
        }
        Map<BlockPos, Integer> depth = new HashMap<>();
        for (BlockPos log : logs) {
            for (Direction d : Direction.values()) {
                BlockPos n = log.relative(d);
                if (!logs.contains(n) && ownedLeaf(level, n, 1)) {
                    depth.putIfAbsent(n, 1);
                }
            }
        }
        if (depth.isEmpty()) return null;
        ArrayDeque<BlockPos> leafQueue = new ArrayDeque<>(depth.keySet());
        while (!leafQueue.isEmpty()) {
            BlockPos p = leafQueue.poll();
            int dp = depth.get(p);
            if (dp >= LeavesBlock.DECAY_DISTANCE - 1) continue;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (depth.containsKey(n) || logs.contains(n)) continue;
                if (ownedLeaf(level, n, dp + 1)) {
                    depth.put(n, dp + 1);
                    leafQueue.add(n);
                }
            }
        }
        List<BlockPos> bases = new ArrayList<>();
        for (BlockPos log : logs) {
            if (level.getBlockState(log.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) bases.add(log);
        }
        List<BlockPos> sorted = new ArrayList<>(logs);
        sorted.sort(Comparator.comparingInt((BlockPos p) -> -p.getY()));
        return new Tree(sorted, new ArrayList<>(depth.keySet()), bases);
    }

    // natural leaves whose nearest log is this far away, so leaves kept alive by a neighbouring tree stay
    private static boolean ownedLeaf(ServerLevel level, BlockPos pos, int dist) {
        BlockState s = level.getBlockState(pos);
        return s.getBlock() instanceof LeavesBlock && s.hasProperty(LeavesBlock.PERSISTENT) && !s.getValue(LeavesBlock.PERSISTENT)
                && s.getValue(LeavesBlock.DISTANCE) == dist;
    }

    // the stump of the natural tree whose trunk or branch this log belongs to
    public static @Nullable BlockPos baseUnder(ServerLevel level, BlockPos log) {
        if (!level.getBlockState(log).is(ModTags.LOGS)) return null;
        for (int y = 1; y <= 24; y++) {
            for (BlockPos p : BlockPos.betweenClosed(log.offset(-3, -y, -3), log.offset(3, -y, 3))) {
                BlockState s = level.getBlockState(p);
                if (!isBase(level, p, s)) continue;
                Tree t = tree(level, p.immutable());
                if (t != null && t.logs().contains(log)) return p.immutable();
            }
        }
        return null;
    }

    public static boolean isTreeLeaf(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.getBlock() instanceof LeavesBlock && s.hasProperty(LeavesBlock.PERSISTENT) && !s.getValue(LeavesBlock.PERSISTENT);
    }

    public static void breakLeaf(GolemEntity golem, ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(s, level, pos, null, golem, golem.getTool());
        breakQuietly(level, pos, s, true);
        for (ItemStack d : merge(drops)) Storage.collectOrDrop(golem, level, Vec3.atCenterOf(pos), d);
    }

    @Override
    public Predicate<ItemStack> climbWith(GolemEntity golem) {
        return s -> s.is(ModTags.LOG_ITEMS);
    }

    @Override
    public String climbSupplies() {
        return "logs";
    }

    @Override
    public boolean climbClears(ServerLevel level, BlockPos pos) {
        return isTreeLeaf(level, pos);
    }

    @Override
    public boolean fromGround() {
        return true;
    }

    @Override
    public double climbAbove() {
        return 2.0;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModTags.LOGS);
    }

    @Override
    public boolean isValid(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return isValid(level, pos) || grid(golem.program()) && isGridSpot(level, pos);
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        if (!isValid(level, pos)) return 10;
        Tree tree = tree(level, pos);
        int logs = tree == null ? 1 : tree.logs().size();
        return Math.min(200, golem.digTicks(level.getBlockState(pos), pos) * (1 + logs / 4));
    }

    private static boolean canFell(ItemStack tool, int logs) {
        if (tool.isEmpty()) return false;
        if (!tool.isDamageableItem()) return true;
        return tool.getMaxDamage() - tool.getDamageValue() >= logs;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        if (!isValid(level, pos)) {
            if (grid(golem.program()) && isGridSpot(level, pos)) {
                if (!level.getBlockState(pos).isAir()) level.destroyBlock(pos, true);
                replant(golem, level, pos, new ArrayList<>());
            }
            return;
        }
        Tree tree = tree(level, pos);
        if (tree == null) return;
        ItemStack axe = golem.getTool();
        // wait for a fresh axe rather than leave floating tops and no sapling
        if (!canFell(axe, tree.logs().size())) return;
        List<ItemStack> drops = new ArrayList<>();
        List<BlockPos> cleared = new ArrayList<>();
        int particles = 0;
        for (BlockPos log : tree.logs()) {
            ItemStack tool = golem.getTool();
            if (tool.isEmpty()) break;
            BlockState s = level.getBlockState(log);
            drops.addAll(Block.getDrops(s, level, log, level.getBlockEntity(log), golem, tool));
            breakQuietly(level, log, s, particles++ < 12);
            cleared.add(log);
            Mc.wear(tool, golem);
        }
        if (cleared.size() == tree.logs().size()) {
            for (BlockPos leaf : tree.leaves()) {
                BlockState s = level.getBlockState(leaf);
                drops.addAll(Block.getDrops(s, level, leaf, null, golem, golem.getTool()));
                breakQuietly(level, leaf, s, particles++ < 24);
            }
        }
        for (BlockPos base : tree.bases()) {
            if (!cleared.contains(base) || !level.getBlockState(base).isAir()) continue;
            golem.setSaplingTrouble(!replant(golem, level, base, drops));
        }
        Vec3 at = Vec3.atBottomCenterOf(pos).add(0, 0.5, 0);
        for (ItemStack d : merge(drops)) Storage.collectOrDrop(golem, level, at, d);
    }

    private static boolean replant(GolemEntity golem, ServerLevel level, BlockPos base, List<ItemStack> drops) {
        Predicate<ItemStack> pick = saplings(golem.program());
        if (plant(level, base, drops, pick)) return true;
        List<ItemEntity> near = level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(3), e -> e.isAlive() && e.getItem().is(ModTags.SAPLINGS));
        if (near.stream().anyMatch(e -> plantFrom(level, base, e, pick))) return true;
        if (plant(level, base, golem.getCarried(), pick)) {
            golem.carriedChanged();
            return true;
        }
        // nothing dropped, so borrow a sapling from storage in the area
        BlockPos chest = Storage.nearest(level, golem.getArea(), golem.position(), c -> saplingSlot(c, level, base, pick) >= 0, p -> false);
        Container c = chest == null ? null : Storage.containerAt(level, chest);
        if (c == null) return false;
        int slot = saplingSlot(c, level, base, pick);
        if (slot < 0) return false;
        boolean[] ok = {false};
        golem.useStorage(chest, () -> {
            ItemStack one = c.removeItem(slot, 1);
            plant(level, base, List.of(one), pick);
            if (!one.isEmpty()) Storage.insert(c, one);
            c.setChanged();
            ok[0] = true;
        });
        return ok[0];
    }

    private static @Nullable BlockState saplingFor(ItemStack stack, ServerLevel level, BlockPos pos, Predicate<ItemStack> pick) {
        if (stack.isEmpty() || !stack.is(ModTags.SAPLINGS) || !pick.test(stack) || !(stack.getItem() instanceof BlockItem bi)) return null;
        BlockState sapling = bi.getBlock().defaultBlockState();
        return sapling.canSurvive(level, pos) ? sapling : null;
    }

    private static boolean plant(ServerLevel level, BlockPos pos, List<ItemStack> stacks, Predicate<ItemStack> pick) {
        for (ItemStack s : stacks) {
            BlockState sapling = saplingFor(s, level, pos, pick);
            if (sapling != null) {
                level.setBlockAndUpdate(pos, sapling);
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static boolean plantFrom(ServerLevel level, BlockPos pos, ItemEntity e, Predicate<ItemStack> pick) {
        ItemStack s = e.getItem().copy();
        if (!plant(level, pos, List.of(s), pick)) return false;
        if (s.isEmpty()) e.discard();
        else e.setItem(s);
        return true;
    }

    private static int saplingSlot(Container c, ServerLevel level, BlockPos pos, Predicate<ItemStack> pick) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (saplingFor(c.getItem(i), level, pos, pick) != null) return i;
        }
        return -1;
    }

    public static void breakQuietly(ServerLevel level, BlockPos pos, BlockState state, boolean effects) {
        if (effects) level.levelEvent(2001, pos, Block.getId(state));
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    public static List<ItemStack> merge(List<ItemStack> drops) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack d : drops) {
            if (d.isEmpty()) continue;
            ItemStack left = d.copy();
            for (ItemStack o : out) {
                if (Mc.sameItem(o, left) && o.getCount() < o.getMaxStackSize()) {
                    int n = Math.min(left.getCount(), o.getMaxStackSize() - o.getCount());
                    o.grow(n);
                    left.shrink(n);
                    if (left.isEmpty()) break;
                }
            }
            if (!left.isEmpty()) out.add(left);
        }
        return out;
    }
}
