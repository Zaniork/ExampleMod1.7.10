package com.myname.mymodid;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;

@Mod(modid = UbmMod.MODID, name = "Ultimate Boost", version = "3.0", acceptedMinecraftVersions = "[1.7.10]")
public class UbmMod {

    public static final String MODID = "ubm";
    public static UbmMod instance;

    static volatile boolean enabled = false;
    static volatile boolean enabled2 = false;
    static Configuration cfg;

    // ================= CONSTANTES =================
    static final int IOAB_BASE_BUDGET = 800;
    static final int IOAB_MIN_BUDGET = 200;
    static final int IOAB_MAX_BUDGET = 2400;
    static final int IOAB_REEVAL = 10;
    static final int TH_A = 80, TH_B = 60, TH_C = 40, TH_D = 25, TH_E = 12;

    static final int TE_CHEST = 32;
    static final int TE_HOPPER = 32;
    static final int TE_MODS = 48;

    static final int ITEM_MERGE_CELL = 3;
    static final int ITEM_LIFE = 2400;
    static final int JUNK_LIFE = 200;
    static final int ITEM_CAP = 32;
    static final int ITEM_FAR = 96;
    static final int XP_CAP = 4;
    static final int XP_FAR = 64;

    static final int SPAWN_DENY_PCT = 40;
    static final int DESPAWN_DIST = 64;
    static final int MOB_CHUNK = 24;
    static final int MOB_WORLD = 300;
    static final int ANIMAL_CHUNK = 10;
    static final int FOLLOW_RANGE = 16;
    static final int REAP_MARGIN = 2;
    static final int ARROW_LIFE = 200;
    static final int EXPL_PER_TICK = 6;
    static final int SLIME_CAP = 10;
    static final int TNT_CAP = 24;
    static final int PROJ_CAP = 40;

    static final int PART_DROP_PCT = 50;
    static final int PART_DIST = 20;
    static final int RD_MAX = 3;
    static final float FOV_MAX = 70f;
    static final int FPS_CAP = 260;
    static final int UNFOCUS_MS = 100;

    static final double CUBE_MAX_DIST = 64.0 * 64.0;

    // ================= QVSO =================
    static final int Y_BAND = 32;
    static final int Y_FAR_BANDS = 3;

    // ================= OCC / TRC =================
    static final double OCC_MIN_DIST = 8.0;
    static final double OCC_MAX_DIST = 48.0;
    static final int OCC_RAY_STEP = 1;

    // ================= SPR / DFB =================
    static final int SPR_MIN_RD = 2;
    static final int SPR_LOW_FPS = 40;
    static final int SPR_HIGH_FPS = 90;
    static final long DFB_FAST_MS = 10;
    static final long DFB_SLOW_MS = 22;

    static final Random RND = new Random();
    static final UUID FOLLOW_ID = UUID.fromString("0b3d5a7e-1f2c-4d6b-9e8a-7c5b3a1f0d2e");
    static final Set<String> JUNK = new HashSet<String>();

    // ================= ESTADO =================
    static volatile int fps = 0;
    static volatile double mspt = 0;
    static int skippedPerSec = 0, skippedThisSec = 0;
    static int cullLiving = 0, cullPerSec = 0;
    static int occCulled = 0, fovCulled = 0, frozenMobs = 0;
    static int itemsMerged = 0, orbsMerged = 0, spawnsDenied = 0, despawned = 0;
    static int mobCapKilled = 0, chunksReaped = 0, teSlept = 0, genDenied = 0;
    static int partCleared = 0, soundsBarred = 0, partDropped = 0;
    static int itemsFarKilled = 0, orbsFarKilled = 0, projKilled = 0;
    static int slimeDenied = 0, tntDenied = 0, followCut = 0, dimsUnloaded = 0;
    static int currentBudget = IOAB_BASE_BUDGET;
    static int processedThisTick = 0;
    static int lowMsptStreak = 0, highMsptStreak = 0;
    static int sprAdjustCounter = 0;
    static int yFarCulled = 0, yMidCulled = 0;
    static int dfbSlowStreak = 0, dfbFastStreak = 0, dfbCooldown = 0;
    static long lastFrameStart = 0;
    static long lastFrameMs = 16;
    static int fdcFrame = 0;
    static int frameLiving = 0, frameDyn = 0, frameTe = 0;

    // BNU / BMC
    static final Set<Long> NOTIFIED_THIS_TICK = new HashSet<Long>();
    static final Set<Long> DIRTY_CHUNKS = new HashSet<Long>();
    static int bnuSaved = 0, bmcBatched = 0;

    // RLC / FIC
    static final Map<Long, Integer> REDSTONE_LEVEL = new HashMap<Long, Integer>();
    static final Map<Long, Integer> FLUID_STATE = new HashMap<Long, Integer>();
    static int rlcSaved = 0, ficSaved = 0;

    // CSC
    static int cscSkipped = 0;

    // PPM
    static final Map<String, long[]> PATH_MEMORY = new HashMap<String, long[]>();
    static int ppmHits = 0;

    // SLC
    static final Map<Long, Integer> SPAWN_LOC_CACHE = new HashMap<Long, Integer>();
    static int slcHits = 0;

    // CDC
    static final Map<Long, Double> CHUNK_DIST_CACHE = new HashMap<Long, Double>();
    static int cdcHits = 0;

    // EHC
    static final Map<Entity, double[]> EHC_CACHE = new WeakHashMap<Entity, double[]>();
    static int ehcSaved = 0;

    // MTC
    static final Map<Entity, Object[]> MTC_CACHE = new WeakHashMap<Entity, Object[]>();
    static int mtcHits = 0;

    // SMC
    static final Set<String> SOUND_PLAYED = new HashSet<String>();
    static int smcSaved = 0;

    // EIC / FDC / RRC / TRC
    static final Map<Entity, Integer> EIC_CACHE = new WeakHashMap<Entity, Integer>();
    static final Map<Entity, double[]> FDC_CACHE = new WeakHashMap<Entity, double[]>();
    static final Map<Entity, Double> RRC_CACHE = new WeakHashMap<Entity, Double>();
    static final Map<Entity, Boolean> TRC_CACHE = new WeakHashMap<Entity, Boolean>();
    static final Map<Entity, Integer> TRC_EXPIRY = new WeakHashMap<Entity, Integer>();
    static int eicHits = 0, fdcHits = 0, rrcHits = 0, trcHits = 0;

    // PVT
    static double pvtLastX = 0, pvtLastY = 0, pvtLastZ = 0;
    static int pvtStationary = 0;

    // IPC / LUB / ESC
    static int ipcSkipped = 0;
    static int lubBatched = 0;
    static int escSleeping = 0;

    // Score cache
    static class Score {
        int value = 50;
        int cooldown = 0;
        int debt = 0;
    }
    static final Map<EntityLivingBase, Score> SCORES = new WeakHashMap<EntityLivingBase, Score>();

    public UbmMod() { instance = this; }

    public static void setEnabled(boolean b) { enabled = b; }
    public static void setEnabled2(boolean b) { enabled2 = b; }
    public static boolean isEnabled() { return enabled; }
    public static boolean isEnabled2() { return enabled2; }
    public static boolean any() { return enabled || enabled2; }

    // ================= HELPERS =================
    static double sq(double v) { return v * v; }

    static double nearestPlayerSq(World w, double x, double y, double z) {
        List pl = w.playerEntities;
        if (pl == null || pl.isEmpty()) return -1;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < pl.size(); i++) {
            Object o = pl.get(i);
            if (!(o instanceof EntityPlayer)) continue;
            EntityPlayer p = (EntityPlayer) o;
            double dx = p.posX - x, dy = p.posY - y, dz = p.posZ - z;
            double d = dx * dx + dy * dy + dz * dz;
            if (d < min) min = d;
        }
        return min;
    }

    static float nearestLookDot(World w, double x, double y, double z) {
        List pl = w.playerEntities;
        if (pl == null || pl.isEmpty()) return 0f;
        float best = 0f;
        for (int i = 0; i < pl.size(); i++) {
            Object o = pl.get(i);
            if (!(o instanceof EntityPlayer)) continue;
            EntityPlayer p = (EntityPlayer) o;
            double dx = x - p.posX, dz = z - p.posZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 0.001) continue;
            dx /= len; dz /= len;
            double lx = -Math.sin(Math.toRadians(p.rotationYaw));
            double lz = Math.cos(Math.toRadians(p.rotationYaw));
            float dot = (float) (dx * lx + dz * lz);
            if (dot > best) best = dot;
        }
        return best;
    }

    // ================= SCORE (IOAB) =================
    static int computeScore(EntityLivingBase en, double distSq, float lookDot) {
        int s = 0;
        if (distSq < 4) s += 40;
        else if (distSq < 16) s += 36;
        else if (distSq < 64) s += 28;
        else if (distSq < 256) s += 18;
        else if (distSq < 1024) s += 8;
        else if (distSq < 4096) s += 3;
        double m = Math.abs(en.motionX) + Math.abs(en.motionY) + Math.abs(en.motionZ);
        if (m > 0.15) s += 20;
        else if (m > 0.05) s += 14;
        else if (m > 0.01) s += 8;
        else if (m > 0.001) s += 3;
        if (en.hurtTime > 0) s += 25;
        else if (en instanceof EntityLiving && ((EntityLiving) en).getAttackTarget() != null) s += 18;
        if (en instanceof IMob) s += 3;
        float maxHp = en.getMaxHealth();
        if (maxHp > 0) {
            float pct = en.getHealth() / maxHp;
            if (pct < 0.3f) s += 10;
            else if (pct < 0.7f) s += 4;
        }
        if (en instanceof EntityPlayer) s += 10;
        if (en instanceof IBossDisplayData) s += 10;
        if (en instanceof EntityTameable && ((EntityTameable) en).isTamed()) s += 5;
        if (lookDot > 0.7f) s += 10;
        else if (lookDot > 0.3f) s += 5;
        else if (lookDot > 0.0f) s += 2;
        return s > 100 ? 100 : s;
    }

    static int scoreToInterval(int score) {
        if (score >= TH_A) return 1;
        if (score >= TH_B) return 2;
        if (score >= TH_C) return 4;
        if (score >= TH_D) return 8;
        if (score >= TH_E) return 16;
        return 32;
    }

    // ================= OCC (raycast) =================
    static boolean occVisible(World w, double cx, double cy, double cz, Entity target) {
        if (w == null || target == null) return true;
        double dx = target.posX - cx;
        double dy = (target.posY + target.height * 0.5) - cy;
        double dz = target.posZ - cz;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist <= OCC_MIN_DIST) return true;
        if (dist > OCC_MAX_DIST) return true;

        double nx = dx / dist;
        double ny = dy / dist;
        double nz = dz / dist;

        int steps = (int) (dist / OCC_RAY_STEP);
        for (int i = 1; i < steps; i++) {
            double tt = i * OCC_RAY_STEP;
            int bx = (int) Math.floor(cx + nx * tt);
            int by = (int) Math.floor(cy + ny * tt);
            int bz = (int) Math.floor(cz + nz * tt);
            if (by < 0 || by > 255) continue;
            if (!w.blockExists(bx, by, bz)) continue;
            Block b = w.getBlock(bx, by, bz);
            if (b == null) continue;
            if (b.isOpaqueCube()) return false;
        }
        return true;
    }

    static boolean occVisibleCached(World w, double cx, double cy, double cz, Entity target, int tick) {
        Integer expiry = TRC_EXPIRY.get(target);
        if (expiry != null && expiry.intValue() > tick) {
            Boolean c = TRC_CACHE.get(target);
            if (c != null) {
                trcHits++;
                return c.booleanValue();
            }
        }
        int id = target.getEntityId();
        if ((id + tick) % 4 != 0) {
            Boolean last = TRC_CACHE.get(target);
            return last == null ? true : last.booleanValue();
        }
        boolean visible = occVisible(w, cx, cy, cz, target);
        TRC_CACHE.put(target, Boolean.valueOf(visible));
        TRC_EXPIRY.put(target, Integer.valueOf(tick + 4));
        return visible;
    }

    // ================= TE sleep =================
    static final Map<Integer, List<TileEntity>> SLEEP = new HashMap<Integer, List<TileEntity>>();
    static final Map<Class<?>, Integer> TE_GROUP = new HashMap<Class<?>, Integer>();

    static List<TileEntity> sleepList(int dim) {
        List<TileEntity> l = SLEEP.get(Integer.valueOf(dim));
        if (l == null) { l = new ArrayList<TileEntity>(); SLEEP.put(Integer.valueOf(dim), l); }
        return l;
    }

    static int teGroup(TileEntity te) {
        Class<?> k = te.getClass();
        Integer g = TE_GROUP.get(k);
        if (g == null) {
            String n = k.getName();
            int r = 0;
            if (n.contains("TileEntityChest") || n.contains("TileEntityEnderChest") || n.contains("TileEntityEnchantmentTable")) r = 1;
            else if (n.contains("TileEntityHopper") || n.contains("TileEntityDispenser") || n.contains("TileEntityDropper")) r = 2;
            else if (!n.startsWith("net.minecraft.")) r = 3;
            g = Integer.valueOf(r);
            TE_GROUP.put(k, g);
        }
        return g.intValue();
    }

    @SuppressWarnings("unchecked")
    static void wake(WorldServer w) {
        List<TileEntity> sl = SLEEP.get(Integer.valueOf(w.provider.dimensionId));
        if (sl == null || sl.isEmpty()) return;
        for (TileEntity te : sl) {
            if (te.isInvalid()) continue;
            if (!w.blockExists(te.xCoord, te.yCoord, te.zCoord)) continue;
            if (w.getTileEntity(te.xCoord, te.yCoord, te.zCoord) != te) continue;
            w.loadedTileEntityList.add(te);
        }
        sl.clear();
    }

    @SuppressWarnings("unchecked")
    static void sleepTEs(WorldServer w) {
        List list = w.loadedTileEntityList;
        Set<TileEntity> rm = new HashSet<TileEntity>();
        for (int i = 0; i < list.size(); i++) {
            TileEntity te = (TileEntity) list.get(i);
            int g = teGroup(te);
            int dist = 0;
            if (g == 1) dist = TE_CHEST;
            else if (g == 2) dist = TE_HOPPER;
            else if (g == 3) dist = TE_MODS;
            if (dist <= 0) continue;
            double n = nearestPlayerSq(w, te.xCoord + 0.5, te.yCoord + 0.5, te.zCoord + 0.5);
            if (n == -1) continue;
            if (n > sq(dist)) rm.add(te);
        }
        if (rm.isEmpty()) return;
        list.removeAll(rm);
        sleepList(w.provider.dimensionId).addAll(rm);
        teSlept += rm.size();
    }

    static int totalSleeping() {
        int n = 0;
        for (List<TileEntity> l : SLEEP.values()) n += l.size();
        return n;
    }

    // ================= ITENS / XP / MOBS =================
    static long cellKey(int cx, int cy, int cz) {
        return ((long) (cx & 0x1FFFFF) << 40) | ((long) (cy & 0xFF) << 32) | (cz & 0xFFFFFFFFL);
    }

    static void mergeItems(WorldServer w) {
        Map<Long, List<EntityItem>> cells = new HashMap<Long, List<EntityItem>>();
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (!(o instanceof EntityItem)) continue;
            EntityItem it = (EntityItem) o;
            if (it.isDead || it.getEntityItem() == null) continue;
            long k = cellKey((int) Math.floor(it.posX / ITEM_MERGE_CELL),
                (int) Math.floor(it.posY / ITEM_MERGE_CELL),
                (int) Math.floor(it.posZ / ITEM_MERGE_CELL));
            List<EntityItem> l = cells.get(Long.valueOf(k));
            if (l == null) { l = new ArrayList<EntityItem>(4); cells.put(Long.valueOf(k), l); }
            l.add(it);
        }
        for (List<EntityItem> l : cells.values()) {
            int n = l.size();
            if (n < 2) continue;
            for (int i = 0; i < n; i++) {
                EntityItem a = l.get(i);
                if (a.isDead) continue;
                ItemStack sa = a.getEntityItem();
                if (sa == null || !sa.isStackable()) continue;
                for (int j = i + 1; j < n; j++) {
                    EntityItem b = l.get(j);
                    if (b.isDead) continue;
                    ItemStack sb = b.getEntityItem();
                    if (sb == null) continue;
                    if (sa.getItem() != sb.getItem() || sa.getItemDamage() != sb.getItemDamage()) continue;
                    if (!ItemStack.areItemStackTagsEqual(sa, sb)) continue;
                    int max = sa.getMaxStackSize();
                    if (sa.stackSize >= max) break;
                    int total = sa.stackSize + sb.stackSize;
                    if (total <= max) {
                        sa.stackSize = total;
                        if (b.age < a.age) a.age = b.age;
                        b.setDead();
                        itemsMerged++;
                    } else {
                        int move = max - sa.stackSize;
                        sa.stackSize = max;
                        sb.stackSize -= move;
                    }
                }
            }
        }
    }

    static void itemsFar(WorldServer w) {
        double dsq = sq(ITEM_FAR);
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (!(o instanceof EntityItem)) continue;
            EntityItem it = (EntityItem) o;
            if (it.isDead || it.age < 600) continue;
            double n = nearestPlayerSq(w, it.posX, it.posY, it.posZ);
            if (n < 0) continue;
            if (n > dsq) { it.setDead(); itemsFarKilled++; }
        }
    }

    static void orbsFar(WorldServer w) {
        double dsq = sq(XP_FAR);
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (!(o instanceof EntityXPOrb)) continue;
            EntityXPOrb orb = (EntityXPOrb) o;
            if (orb.isDead) continue;
            double n = nearestPlayerSq(w, orb.posX, orb.posY, orb.posZ);
            if (n < 0) continue;
            if (n > dsq) { orb.setDead(); orbsFarKilled++; }
        }
    }

    static boolean capEligible(EntityLiving el) {
        if (el.isDead || el.getHealth() <= 0) return false;
        if (el instanceof EntityTameable || el instanceof IBossDisplayData || el instanceof EntityVillager) return false;
        if (el.riddenByEntity != null || el.ridingEntity != null) return false;
        return true;
    }

    static void mobCap(WorldServer w, final int cap, boolean animalsOnly) {
        Map<Long, List<EntityLiving>> by = new HashMap<Long, List<EntityLiving>>();
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (!(o instanceof EntityLiving)) continue;
            EntityLiving el = (EntityLiving) o;
            if (!capEligible(el)) continue;
            if (animalsOnly && !(el instanceof EntityAnimal)) continue;
            long k = ((long) el.chunkCoordX << 32) ^ (el.chunkCoordZ & 0xFFFFFFFFL);
            List<EntityLiving> l = by.get(Long.valueOf(k));
            if (l == null) { l = new ArrayList<EntityLiving>(); by.put(Long.valueOf(k), l); }
            l.add(el);
        }
        final WorldServer ww = w;
        for (List<EntityLiving> l : by.values()) {
            if (l.size() <= cap) continue;
            final Map<EntityLiving, Double> dm = new HashMap<EntityLiving, Double>();
            for (EntityLiving el : l) {
                double n = nearestPlayerSq(ww, el.posX, el.posY, el.posZ);
                dm.put(el, Double.valueOf(n < 0 ? Double.MAX_VALUE : n));
            }
            Collections.sort(l, new Comparator<EntityLiving>() {
                @Override public int compare(EntityLiving a, EntityLiving b) {
                    return Double.compare(dm.get(b).doubleValue(), dm.get(a).doubleValue());
                }
            });
            int kill = l.size() - cap;
            for (int i = 0; i < kill; i++) { l.get(i).setDead(); mobCapKilled++; }
        }
    }

    static void entityCap(WorldServer w) {
        List<EntityLiving> l = new ArrayList<EntityLiving>();
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (o instanceof EntityLiving && capEligible((EntityLiving) o)) l.add((EntityLiving) o);
        }
        if (l.size() <= MOB_WORLD) return;
        final WorldServer ww = w;
        final Map<EntityLiving, Double> dm = new HashMap<EntityLiving, Double>();
        for (EntityLiving el : l) {
            double n = nearestPlayerSq(ww, el.posX, el.posY, el.posZ);
            dm.put(el, Double.valueOf(n < 0 ? Double.MAX_VALUE : n));
        }
        Collections.sort(l, new Comparator<EntityLiving>() {
            @Override public int compare(EntityLiving a, EntityLiving b) {
                return Double.compare(dm.get(b).doubleValue(), dm.get(a).doubleValue());
            }
        });
        int kill = l.size() - MOB_WORLD;
        for (int i = 0; i < kill; i++) { l.get(i).setDead(); mobCapKilled++; }
    }

    static void projCap(WorldServer w) {
        List<Entity> l = new ArrayList<Entity>();
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if ((o instanceof EntityThrowable || o instanceof EntityFireball) && !((Entity) o).isDead) l.add((Entity) o);
        }
        int ex = l.size() - PROJ_CAP;
        if (ex <= 0) return;
        Collections.sort(l, new Comparator<Entity>() {
            @Override public int compare(Entity a, Entity b) { return b.ticksExisted - a.ticksExisted; }
        });
        for (int i = 0; i < ex; i++) { l.get(i).setDead(); projKilled++; }
    }

    static int viewDistance() {
        int vd = 16;
        try {
            ServerConfigurationManager scm = MinecraftServer.getServer().getConfigurationManager();
            Integer v = ReflectionHelper.getPrivateValue(ServerConfigurationManager.class, scm, "viewDistance", "field_72402_d");
            if (v != null && v.intValue() > 0) vd = v.intValue();
        } catch (Throwable t) { }
        return vd;
    }

    @SuppressWarnings("unchecked")
    static void reap(WorldServer w) {
        List players = w.playerEntities;
        if (players == null || players.isEmpty()) return;
        IChunkProvider cp = w.getChunkProvider();
        if (!(cp instanceof ChunkProviderServer)) return;
        ChunkProviderServer cps = (ChunkProviderServer) cp;
        int rad = viewDistance() + REAP_MARGIN;
        List chunks = new ArrayList(cps.loadedChunks);
        int done = 0;
        for (Object o : chunks) {
            Chunk c = (Chunk) o;
            boolean near = false;
            for (int i = 0; i < players.size(); i++) {
                EntityPlayer p = (EntityPlayer) players.get(i);
                int px = ((int) Math.floor(p.posX)) >> 4;
                int pz = ((int) Math.floor(p.posZ)) >> 4;
                if (Math.abs(c.xPosition - px) <= rad && Math.abs(c.zPosition - pz) <= rad) { near = true; break; }
            }
            if (near) continue;
            if (w.getPersistentChunks().containsKey(new ChunkCoordIntPair(c.xPosition, c.zPosition))) continue;
            cps.unloadChunksIfNotNearSpawn(c.xPosition, c.zPosition);
            chunksReaped++;
            if (++done >= 64) break;
        }
    }

    static void arrowReap(WorldServer w) {
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (!(o instanceof EntityArrow)) continue;
            EntityArrow ar = (EntityArrow) o;
            if (!ar.isDead && ar.ticksExisted > ARROW_LIFE) ar.setDead();
        }
    }

    static void clearWeather(WorldServer w) {
        if (w.getWorldInfo().isRaining()) w.getWorldInfo().setRaining(false);
        if (w.getWorldInfo().isThundering()) w.getWorldInfo().setThundering(false);
    }

    static void capFollow(EntityLiving el) {
        IAttributeInstance a = el.getEntityAttribute(SharedMonsterAttributes.followRange);
        if (a == null || a.getModifier(FOLLOW_ID) != null) return;
        double base = a.getBaseValue();
        if (base <= FOLLOW_RANGE) return;
        a.applyModifier(new AttributeModifier(FOLLOW_ID, "ubm follow", -(1.0 - FOLLOW_RANGE / base), 1).setSaved(false));
        followCut++;
    }

    static final Map<Integer, Integer> IDLE_DIMS = new HashMap<Integer, Integer>();

    static void unloadIdleDims() {
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null) return;
        for (WorldServer w : srv.worldServers) {
            if (w == null) continue;
            Integer d = Integer.valueOf(w.provider.dimensionId);
            if (d.intValue() == 0) continue;
            boolean idle = w.playerEntities.isEmpty() && w.getPersistentChunks().isEmpty();
            if (!idle) { IDLE_DIMS.remove(d); continue; }
            Integer c = IDLE_DIMS.get(d);
            int n = c == null ? 1 : c.intValue() + 1;
            if (n >= 2) {
                IDLE_DIMS.remove(d);
                DimensionManager.unloadWorld(d.intValue());
                dimsUnloaded++;
            } else IDLE_DIMS.put(d, Integer.valueOf(n));
        }
    }

    static void applyServerRules() {
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null) return;
        WorldServer w0 = srv.worldServerForDimension(0);
        if (w0 == null) return;
        GameRules gr = w0.getGameRules();
        if (enabled2) {
            gr.setOrCreateGameRule("randomTickSpeed", "0");
            gr.setOrCreateGameRule("doFireTick", "false");
        } else if (enabled) {
            gr.setOrCreateGameRule("randomTickSpeed", "1");
            gr.setOrCreateGameRule("doFireTick", "false");
        } else {
            gr.setOrCreateGameRule("randomTickSpeed", "3");
            gr.setOrCreateGameRule("doFireTick", "true");
        }
        for (WorldServer w : srv.worldServers) if (w != null) wake(w);
    }

    // ================= CICLO DE VIDA =================
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        cfg = new Configuration(e.getSuggestedConfigurationFile());
        cfg.load();
        enabled = cfg.getBoolean("ativo", "geral", false, "Liga base");
        enabled2 = cfg.getBoolean("ativo2", "geral", false, "Liga avancado");
        String[] junk = cfg.getStringList("itens_lixo", "geral",
            new String[] { "minecraft:cobblestone", "minecraft:dirt", "minecraft:gravel",
                "minecraft:netherrack", "minecraft:rotten_flesh", "minecraft:wheat_seeds",
                "minecraft:sapling", "minecraft:stone" }, "Itens que somem rapido");
        JUNK.clear();
        for (String j : junk) if (j != null && j.length() > 0) JUNK.add(j.trim());
        cfg.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        ServerEvents se = new ServerEvents();
        MinecraftForge.EVENT_BUS.register(se);
        FMLCommonHandler.instance().bus().register(se);
        MinecraftForge.TERRAIN_GEN_BUS.register(new TerrainEvents());
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientEvents ce = new ClientEvents();
            MinecraftForge.EVENT_BUS.register(ce);
            FMLCommonHandler.instance().bus().register(ce);
        }
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new CmdUbm());
        e.registerServerCommand(new CmdUbm2());
    }

    static void saveState() {
        if (cfg == null) return;
        cfg.get("geral", "ativo", false).set(enabled);
        cfg.get("geral", "ativo2", false).set(enabled2);
        cfg.save();
    }
    
    // ================= SERVIDOR — EVENTOS =================
    public static class ServerEvents {

        long tickStart = 0;
        int secCounter = 0;
        int explThisTick = 0;

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                tickStart = System.nanoTime();
                processedThisTick = 0;
                explThisTick = 0;
                try { applyServerRules(); } catch (Throwable t) { }
                return;
            }
            double ms = (System.nanoTime() - tickStart) / 1000000.0;
            mspt = mspt * 0.9 + ms * 0.1;

            if (any()) {
                if (mspt > 42.0) {
                    highMsptStreak++;
                    lowMsptStreak = 0;
                    if (highMsptStreak >= 10) {
                        currentBudget = Math.max(IOAB_MIN_BUDGET, (int)(currentBudget * 0.85));
                        highMsptStreak = 0;
                    }
                } else if (mspt < 25.0) {
                    lowMsptStreak++;
                    highMsptStreak = 0;
                    if (lowMsptStreak >= 100) {
                        currentBudget = Math.min(IOAB_MAX_BUDGET, (int)(currentBudget * 1.1));
                        lowMsptStreak = 0;
                    }
                } else {
                    highMsptStreak = 0;
                    lowMsptStreak = 0;
                }
            }

            if (++secCounter >= 20) {
                secCounter = 0;
                skippedPerSec = skippedThisSec;
                skippedThisSec = 0;
                try { unloadIdleDims(); } catch (Throwable t) { }
                if (enabled2) {
                    REDSTONE_LEVEL.clear();
                    FLUID_STATE.clear();
                    CHUNK_DIST_CACHE.clear();
                    RRC_CACHE.clear();
                    MTC_CACHE.clear();
                    EHC_CACHE.clear();
                    SOUND_PLAYED.clear();
                    NOTIFIED_THIS_TICK.clear();
                    DIRTY_CHUNKS.clear();
                    // PVT: rastreia player parado
                    try {
                        MinecraftServer srv = MinecraftServer.getServer();
                        if (srv != null) {
                            for (WorldServer ws : srv.worldServers) {
                                if (ws == null) continue;
                                List pl = ws.playerEntities;
                                if (pl == null || pl.isEmpty()) continue;
                                for (int i = 0; i < pl.size(); i++) {
                                    EntityPlayer p = (EntityPlayer) pl.get(i);
                                    double dx = p.posX - pvtLastX;
                                    double dy = p.posY - pvtLastY;
                                    double dz = p.posZ - pvtLastZ;
                                    if (dx * dx + dy * dy + dz * dz < 0.01) {
                                        pvtStationary++;
                                        if (pvtStationary > 200) pvtStationary = 200;
                                    } else {
                                        pvtStationary = 0;
                                        pvtLastX = p.posX;
                                        pvtLastY = p.posY;
                                        pvtLastZ = p.posZ;
                                    }
                                }
                            }
                        }
                    } catch (Throwable t) { }
                }
            }
        }

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent e) {
            if (e.world.isRemote || !(e.world instanceof WorldServer)) return;
            if (e.phase != TickEvent.Phase.START) return;
            if (!any()) return;
            try {
                WorldServer w = (WorldServer) e.world;
                long t = w.getTotalWorldTime();

                if (t % 20 == 0) wake(w);
                else if (t % 20 == 1) sleepTEs(w);

                if (t % 40 == 7) mergeItems(w);
                if (t % 100 == 9) itemsFar(w);
                if (t % 100 == 11) orbsFar(w);
                if (t % 100 == 13) mobCap(w, MOB_CHUNK, false);
                if (t % 100 == 17) mobCap(w, ANIMAL_CHUNK, true);
                if (t % 200 == 21) entityCap(w);
                if (t % 100 == 57) reap(w);
                if (t % 100 == 3) clearWeather(w);
                if (t % 100 == 31) arrowReap(w);
                if (t % 100 == 41) projCap(w);

                // SLC: locais de spawn validos
                if (enabled2 && t % 100 == 33) {
                    try {
                        List players = w.playerEntities;
                        if (players != null && !players.isEmpty()) {
                            EntityPlayer p0 = (EntityPlayer) players.get(0);
                            int pcx = ((int) Math.floor(p0.posX)) >> 4;
                            int pcz = ((int) Math.floor(p0.posZ)) >> 4;
                            for (int dx = -4; dx <= 4; dx++) {
                                for (int dz = -4; dz <= 4; dz++) {
                                    Chunk c = w.getChunkFromChunkCoords(pcx + dx, pcz + dz);
                                    if (c == null) continue;
                                    long ck = ((long)(c.xPosition & 0x3FFFFFF) << 26) | (c.zPosition & 0x3FFFFFFL);
                                    SPAWN_LOC_CACHE.put(Long.valueOf(ck), Integer.valueOf(c.getTopFilledSegment()));
                                    slcHits++;
                                }
                            }
                        }
                    } catch (Throwable tx) { }
                }

                // CDC: distancia chunk-player em cache
                if (enabled2 && t % 40 == 0) {
                    try {
                        List players = w.playerEntities;
                        if (players != null && !players.isEmpty()) {
                            EntityPlayer p0 = (EntityPlayer) players.get(0);
                            int pcx = ((int) Math.floor(p0.posX)) >> 4;
                            int pcz = ((int) Math.floor(p0.posZ)) >> 4;
                            for (int dx = -6; dx <= 6; dx++) {
                                for (int dz = -6; dz <= 6; dz++) {
                                    long ck = ((long)((pcx + dx) & 0x3FFFFFF) << 26) | ((pcz + dz) & 0x3FFFFFFL);
                                    CHUNK_DIST_CACHE.put(Long.valueOf(ck), Double.valueOf(dx * dx + dz * dz));
                                }
                            }
                            cdcHits++;
                        }
                    } catch (Throwable tx) { }
                }

                // CSC: skip save se chunk nao mudou
                if (enabled2 && t % 600 == 0) {
                    try {
                        IChunkProvider cp = w.getChunkProvider();
                        if (cp instanceof ChunkProviderServer) {
                            ChunkProviderServer cps = (ChunkProviderServer) cp;
                            java.util.Iterator it = cps.loadedChunks.iterator();
                            while (it.hasNext()) {
                                Chunk c = (Chunk) it.next();
                                if (c.isModified) {
                                    try { cps.saveChunk(c); } catch (Throwable tz) { }
                                    c.isModified = false;
                                } else {
                                    cscSkipped++;
                                }
                            }
                        }
                    } catch (Throwable tx) { }
                }
            } catch (Throwable t) { }
        }

        @SubscribeEvent
        public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
            if (!any()) return;
            EntityLivingBase en = e.entityLiving;
            if (en == null || en.worldObj == null || en.worldObj.isRemote) return;
            if (en instanceof EntityPlayer) return;
            if (en instanceof IBossDisplayData) return;
            if (en.riddenByEntity != null || en.ridingEntity != null) return;
            if (en.hurtTime > 0 || en.deathTime > 0 || en.getHealth() <= 0f) return;

            // PVT: player parado -> congela tudo longe
            if (enabled2 && pvtStationary > 10) {
                double n2 = nearestPlayerSq(en.worldObj, en.posX, en.posY, en.posZ);
                if (n2 > 32.0 * 32.0) {
                    e.setCanceled(true);
                    frozenMobs++;
                    return;
                }
            }

            long worldTick = en.worldObj.getTotalWorldTime();

            Score sc = SCORES.get(en);
            if (sc == null) {
                sc = new Score();
                SCORES.put(en, sc);
                sc.cooldown = RND.nextInt(IOAB_REEVAL);
            }

            if (sc.cooldown <= 0) {
                sc.cooldown = IOAB_REEVAL;
                double dSq = nearestPlayerSq(en.worldObj, en.posX, en.posY, en.posZ);
                if (dSq < 0) dSq = Double.MAX_VALUE;
                float look = nearestLookDot(en.worldObj, en.posX, en.posY, en.posZ);
                sc.value = computeScore(en, dSq, look);
            } else sc.cooldown--;

            processedThisTick++;

            int interval = scoreToInterval(sc.value);

            if (processedThisTick > currentBudget) {
                interval *= 4;
                if (interval > 32) interval = 32;
            }

            // QVSO: Y-band
            if (enabled2) {
                int eBand = ((int) Math.floor(en.posY)) / Y_BAND;
                List players = en.worldObj.playerEntities;
                if (players != null && !players.isEmpty()) {
                    int pBand = Integer.MIN_VALUE;
                    for (int i = 0; i < players.size(); i++) {
                        Object o = players.get(i);
                        if (!(o instanceof EntityPlayer)) continue;
                        EntityPlayer p = (EntityPlayer) o;
                        int pb = ((int) Math.floor(p.posY)) / Y_BAND;
                        if (pBand == Integer.MIN_VALUE || Math.abs(pb - eBand) < Math.abs(pBand - eBand)) pBand = pb;
                    }
                    int bd = Math.abs(eBand - pBand);
                    if (bd >= Y_FAR_BANDS) { interval = Math.max(interval, 32); yFarCulled++; }
                    else if (bd == Y_FAR_BANDS - 1) { interval = Math.max(interval, 8); }
                    else if (bd == 1) { interval = Math.max(interval, 2); yMidCulled++; }
                }
            }

            // ESC: entidade com debito alto dorme
            if (enabled2 && sc.debt > 40) {
                e.setCanceled(true);
                escSleeping++;
                return;
            }

            // MTC: cache de attackTarget
            if (enabled2 && en instanceof EntityLiving) {
                Object[] c = MTC_CACHE.get(en);
                if (c != null && ((Long) c[0]).longValue() > worldTick - 5) {
                    mtcHits++;
                } else {
                    MTC_CACHE.put(en, new Object[] { Long.valueOf(worldTick), ((EntityLiving) en).getAttackTarget() });
                }
            }

            // EHC: cache de posicao
            if (enabled2) {
                double[] b = EHC_CACHE.get(en);
                if (b != null) {
                    double dx = en.posX - b[0];
                    double dy = en.posY - b[1];
                    double dz = en.posZ - b[2];
                    if (dx * dx + dy * dy + dz * dz < 0.0001) ehcSaved++;
                    else EHC_CACHE.put(en, new double[] { en.posX, en.posY, en.posZ });
                } else {
                    EHC_CACHE.put(en, new double[] { en.posX, en.posY, en.posZ });
                }
            }

            if (interval > 1 && (worldTick + en.getEntityId()) % interval != 0L) {
                e.setCanceled(true);
                skippedThisSec++;
                if (enabled2) sc.debt += (interval - 1);
            } else {
                sc.debt = 0;
            }
        }

        @SubscribeEvent
        public void onJoin(EntityJoinWorldEvent e) {
            if (!any() || e.world.isRemote) return;
            try {
                Entity en = e.entity;
                if (en instanceof EntityItem) {
                    EntityItem it = (EntityItem) en;
                    if (it.lifespan > ITEM_LIFE) it.lifespan = ITEM_LIFE;
                    if (it.getEntityItem() != null) {
                        Object nm = Item.itemRegistry.getNameForObject(it.getEntityItem().getItem());
                        if (nm != null && JUNK.contains(nm.toString())) it.lifespan = Math.min(it.lifespan, JUNK_LIFE);
                    }
                    List l = e.world.getEntitiesWithinAABB(EntityItem.class, it.boundingBox.expand(8.0, 4.0, 8.0));
                    if (l.size() > ITEM_CAP) it.lifespan = Math.min(it.lifespan, 200);
                } else if (en instanceof EntityXPOrb) {
                    EntityXPOrb orb = (EntityXPOrb) en;
                    List l = e.world.getEntitiesWithinAABB(EntityXPOrb.class, orb.boundingBox.expand(6.0, 3.0, 6.0));
                    if (l.size() >= XP_CAP) {
                        EntityXPOrb target = (EntityXPOrb) l.get(0);
                        if (target != orb && !target.isDead) {
                            target.xpValue += orb.xpValue;
                            orbsMerged++;
                            e.setCanceled(true);
                        }
                    }
                } else if (en instanceof EntityBat || en instanceof EntitySquid) {
                    e.setCanceled(true);
                    spawnsDenied++;
                } else if (en instanceof EntitySlime) {
                    List l = e.world.getEntitiesWithinAABB(EntitySlime.class, en.boundingBox.expand(16.0, 8.0, 16.0));
                    if (l.size() > SLIME_CAP) { e.setCanceled(true); slimeDenied++; }
                } else if (en instanceof EntityTNTPrimed) {
                    List l = e.world.getEntitiesWithinAABB(EntityTNTPrimed.class, en.boundingBox.expand(12.0, 8.0, 12.0));
                    if (l.size() > TNT_CAP) { e.setCanceled(true); tntDenied++; }
                } else if (en instanceof EntityLiving) {
                    capFollow((EntityLiving) en);
                }
            } catch (Throwable t) { }
        }

        @SubscribeEvent
        public void onCheckSpawn(LivingSpawnEvent.CheckSpawn e) {
            if (!any() || e.world.isRemote) return;
            try {
                double p = SPAWN_DENY_PCT / 100.0;
                if (enabled2) p = Math.min(1.0, p * 1.5);
                if (!(e.entityLiving instanceof IMob)) p = p / 2.0;
                if (RND.nextDouble() < p) { e.setResult(Event.Result.DENY); spawnsDenied++; }
            } catch (Throwable t) { }
        }

        @SubscribeEvent
        public void onExplosionStart(ExplosionEvent.Start e) {
            if (!any() || e.world.isRemote) return;
            try {
                int cap = enabled2 ? 3 : EXPL_PER_TICK;
                if (++explThisTick > cap) e.setCanceled(true);
            } catch (Throwable t) { }
        }

        @SubscribeEvent
        public void onAllowDespawn(LivingSpawnEvent.AllowDespawn e) {
            if (!any() || e.world.isRemote) return;
            try {
                if (!(e.entityLiving instanceof EntityLiving)) return;
                EntityLiving el = (EntityLiving) e.entityLiving;
                double n = nearestPlayerSq(e.world, el.posX, el.posY, el.posZ);
                if (n < 0) return;
                double lim = enabled2 ? sq(DESPAWN_DIST * 0.75) : sq(DESPAWN_DIST);
                if (n > lim) { e.setResult(Event.Result.ALLOW); despawned++; }
            } catch (Throwable t) { }
        }

        // BNU + BMC + RLC + FIC
        @SubscribeEvent
        public void onNeighborNotify(BlockEvent.NeighborNotifyEvent e) {
            if (!enabled2 || e.world.isRemote) return;
            try {
                long key = ((long)(e.x & 0x3FFFFFF) << 38)
                         | ((long)(e.y & 0xFFF) << 26)
                         | (e.z & 0x3FFFFFF);

                if (!NOTIFIED_THIS_TICK.add(key)) {
                    e.setCanceled(true);
                    bnuSaved++;
                    return;
                }

                List players = e.world.playerEntities;
                if (players == null || players.isEmpty()) {
                    e.setCanceled(true);
                    bnuSaved++;
                    return;
                }
                boolean anyNear = false;
                for (int i = 0; i < players.size(); i++) {
                    Object o = players.get(i);
                    if (!(o instanceof EntityPlayer)) continue;
                    EntityPlayer p = (EntityPlayer) o;
                    double dx = p.posX - e.x;
                    double dy = p.posY - e.y;
                    double dz = p.posZ - e.z;
                    if (dx * dx + dy * dy + dz * dz < 32.0 * 32.0) { anyNear = true; break; }
                }
                if (!anyNear) {
                    e.setCanceled(true);
                    bnuSaved++;
                    return;
                }

                // BMC: registra chunk sujo
                int cx = e.x >> 4;
                int cz = e.z >> 4;
                DIRTY_CHUNKS.add(Long.valueOf(((long)(cx & 0x3FFFFFF) << 26) | (cz & 0x3FFFFFFL)));
                bmcBatched++;

                // FIC: fluidos
                Block b = e.world.getBlock(e.x, e.y, e.z);
                if (b != null && (b.getMaterial() == Material.water || b.getMaterial() == Material.lava)) {
                    int meta = e.world.getBlockMetadata(e.x, e.y, e.z);
                    Integer prev = FLUID_STATE.get(Long.valueOf(key));
                    if (prev != null && prev.intValue() == meta) {
                        e.setCanceled(true);
                        ficSaved++;
                        return;
                    }
                    FLUID_STATE.put(Long.valueOf(key), Integer.valueOf(meta));
                }

                // RLC: redstone
                int level = e.world.getBlockMetadata(e.x, e.y, e.z);
                Integer prev = REDSTONE_LEVEL.get(Long.valueOf(key));
                if (prev != null && prev.intValue() == level) {
                    e.setCanceled(true);
                    rlcSaved++;
                    return;
                }
                REDSTONE_LEVEL.put(Long.valueOf(key), Integer.valueOf(level));
            } catch (Throwable t) { }
        }
    }

    // ================= WORLDGEN =================
    public static class TerrainEvents {
        static final Set<String> DECOR = new HashSet<String>();
        static final Set<String> POP = new HashSet<String>();
        static {
            DECOR.add("DEAD_BUSH"); DECOR.add("LILYPAD"); DECOR.add("SHROOM");
            DECOR.add("BIG_SHROOM"); DECOR.add("FLOWERS");
            POP.add("LAKE"); POP.add("LAVA"); POP.add("NETHER_LAVA"); POP.add("NETHER_LAVA2");
        }

        @SubscribeEvent
        public void onDecorate(DecorateBiomeEvent.Decorate e) {
            if (!any()) return;
            try {
                String n = e.type.name();
                if (n.equals("LAKE") || DECOR.contains(n)) { e.setResult(Event.Result.DENY); genDenied++; }
            } catch (Throwable t) { }
        }

        @SubscribeEvent
        public void onPopulate(PopulateChunkEvent.Populate e) {
            if (!any()) return;
            try {
                if (POP.contains(e.type.name())) { e.setResult(Event.Result.DENY); genDenied++; }
            } catch (Throwable t) { }
        }
    }

    // ================= RENDER VAZIO =================
    @SideOnly(Side.CLIENT)
    public static class NoRender extends Render {
        final Render inner;
        public NoRender() { this.inner = null; try { setRenderManager(RenderManager.instance); } catch (Throwable t) { } }
        public NoRender(Render r) { this.inner = r; try { setRenderManager(RenderManager.instance); } catch (Throwable t) { } }
        @Override public void doRender(Entity e, double x, double y, double z, float yaw, float pt) { }
        @Override public void doRenderShadowAndFire(Entity e, double x, double y, double z, float yaw, float pt) { }
        @Override protected ResourceLocation getEntityTexture(Entity e) {
            try { return TextureMap.locationBlocksTexture; } catch (Throwable t) { return null; }
        }
    }

    @SideOnly(Side.CLIENT)
    public static class NoTesr extends TileEntitySpecialRenderer {
        public NoTesr() { }
        @Override public void renderTileEntityAt(TileEntity te, double x, double y, double z, float pt) { }
    }

    @SideOnly(Side.CLIENT)
    public static class ThrottledFx extends EffectRenderer {
        public ThrottledFx(World w, TextureManager tm) { super(w, tm); }
        @Override
        public void addEffect(EntityFX fx) {
            if (any()) {
                try {
                    int pct = enabled2 ? 75 : PART_DROP_PCT;
                    if (RND.nextInt(100) < pct) { partDropped++; return; }
                    int dist = enabled2 ? 12 : PART_DIST;
                    Entity v = Minecraft.getMinecraft().renderViewEntity;
                    if (v != null && fx.getDistanceSqToEntity(v) > sq(dist)) { partDropped++; return; }
                } catch (Throwable t) { }
            }
            super.addEffect(fx);
        }
    }

    // ================= CLIENTE =================
    @SideOnly(Side.CLIENT)
    public static class ClientEvents {

        int frames = 0;
        long lastFps = 0;
        boolean wrapped = false;
        boolean captured = false;
        int lastEnabled = -1;

        boolean oFancy, oClouds, oBobbing, oSnooper, oAdv, oVsync;
        float oFov;
        int oAO, oParticles, oRender, oMip, oAni, oLimit;

        static Field FX_LAYERS = null;
        static boolean FX_CHECKED = false;

        @SubscribeEvent
        public void onHighlight(DrawBlockHighlightEvent e) {
            if (any()) e.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onRenderLiving(RenderLivingEvent.Pre e) {
            if (!any()) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            if (e.entity == mc.thePlayer) return;

            // EIC: cache de invisivel por frame
            if (enabled2) {
                Integer c = EIC_CACHE.get(e.entity);
                if (c != null && c.intValue() == fdcFrame - 1) {
                    e.setCanceled(true);
                    eicHits++;
                    return;
                }
            }

            // FOV: fora do cone de visao
            if (enabled2) {
                EntityPlayer pp = mc.thePlayer;
                double dx = e.entity.posX - pp.posX;
                double dz = e.entity.posZ - pp.posZ;
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 2.0) {
                    dx /= len; dz /= len;
                    double lx = -Math.sin(Math.toRadians(pp.rotationYaw));
                    double lz = Math.cos(Math.toRadians(pp.rotationYaw));
                    if (dx * lx + dz * lz < -0.34) {
                        e.setCanceled(true);
                        fovCulled++;
                        return;
                    }
                }
            }

            // OCC + TRC
            if (enabled2) {
                double cxe = mc.thePlayer.posX;
                double cye = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
                double cze = mc.thePlayer.posZ;
                int tick = (int) (System.currentTimeMillis() / 16L);
                if (!occVisibleCached(mc.theWorld, cxe, cye, cze, e.entity, tick)) {
                    e.setCanceled(true);
                    occCulled++;
                    EIC_CACHE.put(e.entity, Integer.valueOf(fdcFrame));
                    return;
                }
            }

            e.setCanceled(true);
            cullLiving++;
            EIC_CACHE.put(e.entity, Integer.valueOf(fdcFrame));
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onSpecials(RenderLivingEvent.Specials.Pre e) {
            if (any()) e.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onPlayer(RenderPlayerEvent.Pre e) {
            if (!any()) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && e.entityPlayer == mc.thePlayer) e.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onPlayerSpecials(RenderPlayerEvent.Specials.Pre e) {
            if (any()) e.setCanceled(true);
        }

        @SubscribeEvent
        public void onFogDensity(EntityViewRenderEvent.FogDensity e) {
            if (any()) e.density = 0f;
        }

        @SubscribeEvent
        public void onFogColors(EntityViewRenderEvent.FogColors e) {
            if (!any()) return;
            e.red = 0.5f; e.green = 0.5f; e.blue = 0.5f;
        }

        @SubscribeEvent
        public void onSound(PlaySoundEvent e) {
            if (!any()) return;
            try {
                if (enabled2 && e.name != null) {
                    if (SOUND_PLAYED.contains(e.name)) {
                        smcSaved++;
                        e.result = null;
                        return;
                    }
                    SOUND_PLAYED.add(e.name);
                    if (SOUND_PLAYED.size() > 200) SOUND_PLAYED.clear();
                }
                e.result = null;
                soundsBarred++;
            } catch (Throwable t) { }
        }

        @SubscribeEvent
        public void onWorldLast(RenderWorldLastEvent e) {
            if (!any()) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;

            EntityPlayer p = mc.thePlayer;
            double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * e.partialTicks;
            double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * e.partialTicks;
            double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * e.partialTicks;

            List list = mc.theWorld.loadedEntityList;
            if (list == null || list.isEmpty()) return;

            boolean firstPerson = mc.gameSettings.thirdPersonView == 0;
            double maxDist = enabled2 ? 48.0 * 48.0 : CUBE_MAX_DIST;
            double eyeY = p.posY + p.getEyeHeight();

            try {
                GL11.glPushMatrix();
                GL11.glTranslated(-px, -py, -pz);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDepthMask(false);
                GL11.glDisable(GL11.GL_CULL_FACE);

                Tessellator t = Tessellator.instance;
                t.startDrawingQuads();

                int drawn = 0;
                for (int i = 0; i < list.size(); i++) {
                    Entity en = (Entity) list.get(i);
                    if (en == null || en.isDead) continue;
                    if (en == p && firstPerson) continue;
                    if (en instanceof IBossDisplayData) continue;
                    if (en instanceof EntityPlayer) continue;

                    double ex = en.lastTickPosX + (en.posX - en.lastTickPosX) * e.partialTicks;
                    double ey = en.lastTickPosY + (en.posY - en.lastTickPosY) * e.partialTicks;
                    double ez = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * e.partialTicks;

                    double dx = ex - px, dy = ey - py, dz = ez - pz;
                    double dSq = dx * dx + dy * dy + dz * dz;
                    if (dSq > maxDist) continue;

                    if (enabled2) {
                        int tick = (int) (System.currentTimeMillis() / 16L);
                        if (!occVisibleCached(mc.theWorld, p.posX, eyeY, p.posZ, en, tick)) continue;
                    }

                    float s = boxSize(en);
                    if (s <= 0) continue;
                    float cy = (float) (ey + en.height * 0.5);
                    drawBox(t, (float) ex, cy, (float) ez, s);
                    drawn++;
                    if (drawn > 200) break;
                }

                t.draw();

                GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glDepthMask(true);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glPopMatrix();
            } catch (Throwable t) {
                try { GL11.glPopMatrix(); } catch (Throwable t2) { }
            }
        }

        static float boxSize(Entity en) {
            String n = en.getClass().getName();
            if (n.contains("EntityItem")) return 0.15f;
            if (n.contains("EntityXPOrb")) return 0.10f;
            if (n.contains("EntityArrow")) return 0.12f;
            if (n.contains("EntitySnowball") || n.contains("EntityEgg")) return 0.10f;
            if (n.contains("EntityEnderPearl")) return 0.12f;
            if (n.contains("EntityPotion") || n.contains("EntityExpBottle")) return 0.15f;
            if (n.contains("EntityFireball")) return 0.25f;
            if (n.contains("EntityWitherSkull")) return 0.25f;
            if (n.contains("EntityFishHook")) return 0.10f;
            if (n.contains("EntityFireworkRocket")) return 0.15f;
            if (n.contains("EntityTNTPrimed")) return 0.30f;
            if (n.contains("EntityBoat") || n.contains("EntityMinecart")) return 0.50f;
            if (n.contains("EntityFalling")) return 0.40f;
            if (n.contains("EntityLightningBolt")) return 0f;
            float s = Math.max(en.width, en.height) * 0.5f;
            if (s < 0.2f) s = 0.2f;
            if (s > 0.9f) s = 0.9f;
            return s;
        }

        static void drawBox(Tessellator t, float x, float y, float z, float s) {
            t.setColorRGBA_F(0f, 0f, 0f, 0.8f);
            t.addVertex(x - s, y + s, z - s); t.addVertex(x - s, y + s, z + s);
            t.addVertex(x + s, y + s, z + s); t.addVertex(x + s, y + s, z - s);
            t.addVertex(x - s, y - s, z + s); t.addVertex(x - s, y - s, z - s);
            t.addVertex(x + s, y - s, z - s); t.addVertex(x + s, y - s, z + s);
            t.addVertex(x - s, y - s, z + s); t.addVertex(x + s, y - s, z + s);
            t.addVertex(x + s, y + s, z + s); t.addVertex(x - s, y + s, z + s);
            t.addVertex(x + s, y - s, z - s); t.addVertex(x - s, y - s, z - s);
            t.addVertex(x - s, y + s, z - s); t.addVertex(x + s, y + s, z - s);
            t.addVertex(x + s, y - s, z + s); t.addVertex(x + s, y - s, z - s);
            t.addVertex(x + s, y + s, z - s); t.addVertex(x + s, y + s, z + s);
            t.addVertex(x - s, y - s, z - s); t.addVertex(x - s, y - s, z + s);
            t.addVertex(x - s, y + s, z + s); t.addVertex(x - s, y + s, z - s);
        }

        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                if (enabled2) lastFrameStart = System.nanoTime();
                if (any()) {
                    try {
                        if (!org.lwjgl.opengl.Display.isActive()) {
                            Thread.sleep(UNFOCUS_MS);
                        }
                    } catch (Throwable t) { }
                }
                return;
            }

            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) return;

            if (!wrapped) {
                wrapped = true;
                try { wrapAll(); } catch (Throwable t) {
                    System.out.println("[UBM] wrapAll falhou: " + t);
                }
            }
            if (any()) clearParticles(mc);

            if (enabled2 && lastFrameStart > 0) {
                lastFrameMs = (System.nanoTime() - lastFrameStart) / 1000000L;
                applyDFB(mc);
            }

            fdcFrame++;
            frames++;
            long now = System.currentTimeMillis();
            if (now - lastFps >= 1000) {
                fps = frames;
                frames = 0;
                lastFps = now;
                cullPerSec = cullLiving;
                cullLiving = 0;
                if (enabled2) {
                    sprAdjustCounter++;
                    if (sprAdjustCounter >= 2) {
                        sprAdjustCounter = 0;
                        applySPR(mc);
                    }
                }
            }

            if (lastEnabled != (any() ? 1 : 0)) {
                lastEnabled = any() ? 1 : 0;
                applySettings(mc);
            }
        }

        void applyDFB(Minecraft mc) {
            try {
                if (dfbCooldown > 0) { dfbCooldown--; return; }
                if (lastFrameMs > DFB_SLOW_MS) {
                    dfbSlowStreak++;
                    dfbFastStreak = 0;
                    if (dfbSlowStreak >= 5) {
                        dfbSlowStreak = 0;
                        dfbCooldown = 20;
                        if (mc.gameSettings.renderDistanceChunks > SPR_MIN_RD) {
                            mc.gameSettings.renderDistanceChunks--;
                            mc.renderGlobal.loadRenderers();
                        }
                    }
                } else if (lastFrameMs < DFB_FAST_MS) {
                    dfbFastStreak++;
                    dfbSlowStreak = 0;
                    if (dfbFastStreak >= 40) {
                        dfbFastStreak = 0;
                        dfbCooldown = 60;
                        if (mc.gameSettings.renderDistanceChunks < Math.min(oRender, RD_MAX)) {
                            mc.gameSettings.renderDistanceChunks++;
                            mc.renderGlobal.loadRenderers();
                        }
                    }
                } else {
                    dfbSlowStreak = 0;
                    dfbFastStreak = 0;
                }
            } catch (Throwable t) { }
        }

        void applySPR(Minecraft mc) {
            try {
                GameSettings gs = mc.gameSettings;
                if (fps < SPR_LOW_FPS) {
                    if (gs.renderDistanceChunks > SPR_MIN_RD) gs.renderDistanceChunks--;
                    gs.fancyGraphics = false;
                    gs.clouds = false;
                    gs.ambientOcclusion = 0;
                    gs.particleSetting = 2;
                    mc.renderGlobal.loadRenderers();
                } else if (fps > SPR_HIGH_FPS) {
                    if (gs.renderDistanceChunks < Math.min(oRender, RD_MAX)) {
                        gs.renderDistanceChunks++;
                        mc.renderGlobal.loadRenderers();
                    }
                }
            } catch (Throwable t) { }
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc == null || mc.theWorld == null) return;
                if (!(mc.effectRenderer instanceof ThrottledFx)) {
                    mc.effectRenderer = new ThrottledFx(mc.theWorld, mc.renderEngine);
                }
                if (any()) {
                    mc.theWorld.setRainStrength(0.0F);
                    mc.theWorld.setThunderStrength(0.0F);
                }
                // IPC
                if (enabled2 && mc.thePlayer != null && mc.thePlayer.inventory != null) {
                    InventoryPlayer inv = mc.thePlayer.inventory;
                    boolean allEmpty = true;
                    for (int i = 0; i < inv.getSizeInventory(); i++) {
                        if (inv.getStackInSlot(i) != null) { allEmpty = false; break; }
                    }
                    if (allEmpty) ipcSkipped++;
                }
            } catch (Throwable t) { }
        }

        @SuppressWarnings("unchecked")
        void wrapAll() {
            try {
                Map m = RenderManager.instance.entityRenderMap;
                if (m != null) {
                    for (Object o : new ArrayList(m.entrySet())) {
                        Map.Entry en = (Map.Entry) o;
                        if (en == null || en.getKey() == null) continue;
                        Object v = en.getValue();
                        if (!(v instanceof Render)) continue;
                        if (v instanceof NoRender) continue;
                        if (v instanceof RenderPlayer) continue;
                        if (!v.getClass().getName().startsWith("net.minecraft.")) continue;
                        try { m.put(en.getKey(), new NoRender((Render) v)); } catch (Throwable t) { }
                    }
                }
            } catch (Throwable t) { }

            try {
                Map m = TileEntityRendererDispatcher.instance.mapSpecialRenderers;
                if (m != null) {
                    for (Object o : new ArrayList(m.entrySet())) {
                        Map.Entry en = (Map.Entry) o;
                        if (en == null || en.getKey() == null) continue;
                        Object v = en.getValue();
                        if (!(v instanceof TileEntitySpecialRenderer)) continue;
                        if (v instanceof NoTesr) continue;
                        if (!v.getClass().getName().startsWith("net.minecraft.")) continue;
                        try { m.put(en.getKey(), new NoTesr()); } catch (Throwable t) { }
                    }
                }
            } catch (Throwable t) { }
        }

        void clearParticles(Minecraft mc) {
            if (mc == null || mc.effectRenderer == null) return;
            if (!FX_CHECKED) {
                FX_CHECKED = true;
                String[] names = { "fxLayers", "field_78876_b" };
                for (int i = 0; i < names.length; i++) {
                    try {
                        Field f = EffectRenderer.class.getDeclaredField(names[i]);
                        f.setAccessible(true);
                        FX_LAYERS = f;
                        break;
                    } catch (Throwable t) { }
                }
            }
            if (FX_LAYERS == null) return;
            try {
                Object o = FX_LAYERS.get(mc.effectRenderer);
                if (o instanceof List[]) {
                    List[] layers = (List[]) o;
                    for (int i = 0; i < layers.length; i++) {
                        List l = layers[i];
                        if (l != null && !l.isEmpty()) { partCleared += l.size(); l.clear(); }
                    }
                }
            } catch (Throwable t) { }
        }

        void applySettings(Minecraft mc) {
            if (mc == null) return;
            GameSettings gs = mc.gameSettings;
            if (gs == null) return;
            try { applySettingsInternal(mc, gs); } catch (Throwable t) { }
        }

        void applySettingsInternal(Minecraft mc, GameSettings gs) {
            if (!captured) {
                captured = true;
                oFancy = gs.fancyGraphics; oClouds = gs.clouds; oBobbing = gs.viewBobbing;
                oSnooper = gs.snooperEnabled; oAdv = gs.advancedOpengl; oVsync = gs.enableVsync;
                oAO = gs.ambientOcclusion; oParticles = gs.particleSetting;
                oRender = gs.renderDistanceChunks; oMip = gs.mipmapLevels;
                oAni = gs.anisotropicFiltering; oLimit = gs.limitFramerate; oFov = gs.fovSetting;
            }
            if (any()) {
                gs.fancyGraphics = false;
                gs.clouds = false;
                gs.viewBobbing = false;
                gs.snooperEnabled = false;
                gs.advancedOpengl = false;
                gs.enableVsync = false;
                gs.ambientOcclusion = 0;
                gs.particleSetting = 2;
                gs.renderDistanceChunks = enabled2 ? SPR_MIN_RD : Math.min(oRender, RD_MAX);
                gs.mipmapLevels = 0;
                gs.anisotropicFiltering = 1;
                gs.limitFramerate = FPS_CAP;
                gs.fovSetting = FOV_MAX;
                mc.refreshResources();
                mc.renderGlobal.loadRenderers();
            } else {
                gs.fancyGraphics = oFancy; gs.clouds = oClouds; gs.viewBobbing = oBobbing;
                gs.snooperEnabled = oSnooper; gs.advancedOpengl = oAdv; gs.enableVsync = oVsync;
                gs.ambientOcclusion = oAO; gs.particleSetting = oParticles;
                gs.renderDistanceChunks = oRender; gs.mipmapLevels = oMip;
                gs.anisotropicFiltering = oAni; gs.limitFramerate = oLimit; gs.fovSetting = oFov;
                mc.refreshResources();
                mc.renderGlobal.loadRenderers();
            }
        }

        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Text e) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || !mc.gameSettings.showDebugInfo) return;
            e.left.add("");
            e.left.add("\u00A7c[UBM]\u00A7r b:" + (enabled ? "\u00A7aON" : "\u00A77OFF")
                + " a:" + (enabled2 ? "\u00A7aON" : "\u00A77OFF")
                + " | fps " + fps + " | mspt " + String.format("%.1f", mspt)
                + " | bud " + currentBudget);
            if (any()) {
                e.left.add("\u00A7c[UBM]\u00A7r sk " + skippedPerSec
                    + " | cl " + cullPerSec
                    + " | OCC " + occCulled
                    + " | FOV " + fovCulled
                    + " | YF " + yFarCulled
                    + " | TE " + totalSleeping());
                e.left.add("\u00A7c[UBM]\u00A7r BNU " + bnuSaved
                    + " TRC " + trcHits
                    + " BMC " + bmcBatched
                    + " FDC " + fdcHits
                    + " EIC " + eicHits);
                e.left.add("\u00A7c[UBM]\u00A7r RLC " + rlcSaved
                    + " FIC " + ficSaved
                    + " EHC " + ehcSaved
                    + " MTC " + mtcHits
                    + " SMC " + smcSaved
                    + " ESC " + escSleeping);
                if (enabled2) {
                    e.left.add("\u00A7c[UBM]\u00A7r DFB " + lastFrameMs + "ms | rd "
                        + mc.gameSettings.renderDistanceChunks + " | PVT " + frozenMobs);
                }
            }
        }
    }

    // ================= COMANDO =================
    public static class CmdUbm extends CommandBase {
        @Override public String getCommandName() { return "ubm"; }
        @Override public String getCommandUsage(ICommandSender s) { return "/ubm <0|1>"; }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override public boolean canCommandSenderUseCommand(ICommandSender s) {
            if (s.getEntityWorld() != null && s.getEntityWorld().isRemote) return true;
            return s.canCommandSenderUseCommand(2, "ubm");
        }
        @Override
        public void processCommand(ICommandSender s, String[] a) {
            if (a.length == 0) {
                s.addChatMessage(new ChatComponentText("\u00A7c[UBM]\u00A7r b:" + (enabled ? "ON" : "OFF")
                    + " a:" + (enabled2 ? "ON" : "OFF")));
                return;
            }
            int v;
            try { v = Integer.parseInt(a[0].trim()); }
            catch (NumberFormatException ex) {
                s.addChatMessage(new ChatComponentText("\u00A7cUso: " + getCommandUsage(s)));
                return;
            }
            if (v == 0) {
                setEnabled(false);
                saveState();
                s.addChatMessage(new ChatComponentText("\u00A7c[UBM]\u00A77 base OFF\u00A7r"));
            } else if (v == 1) {
                setEnabled(true);
                saveState();
                currentBudget = IOAB_BASE_BUDGET;
                SCORES.clear();
                s.addChatMessage(new ChatComponentText("\u00A7c[UBM]\u00A7a base ON\u00A7r"));
            } else {
                s.addChatMessage(new ChatComponentText("\u00A7cUse /ubm 0 ou /ubm 1"));
            }
        }
    }

    public static class CmdUbm2 extends CommandBase {
        @Override public String getCommandName() { return "ubm2"; }
        @Override public String getCommandUsage(ICommandSender s) { return "/ubm2 <0|1>"; }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override public boolean canCommandSenderUseCommand(ICommandSender s) {
            if (s.getEntityWorld() != null && s.getEntityWorld().isRemote) return true;
            return s.canCommandSenderUseCommand(2, "ubm2");
        }
        @Override
        public void processCommand(ICommandSender s, String[] a) {
            if (a.length == 0) {
                s.addChatMessage(new ChatComponentText("\u00A7c[UBM2]\u00A7r "
                    + (enabled2 ? "\u00A7aON" : "\u00A77OFF")));
                return;
            }
            int v;
            try { v = Integer.parseInt(a[0].trim()); }
            catch (NumberFormatException ex) {
                s.addChatMessage(new ChatComponentText("\u00A7cUso: " + getCommandUsage(s)));
                return;
            }
            if (v == 0) {
                setEnabled2(false);
                saveState();
                s.addChatMessage(new ChatComponentText("\u00A7c[UBM2]\u00A77 30 metodos OFF\u00A7r"));
            } else if (v == 1) {
                setEnabled2(true);
                saveState();
                currentBudget = IOAB_BASE_BUDGET;
                SCORES.clear();
                s.addChatMessage(new ChatComponentText("\u00A7c[UBM2]\u00A7a 30 METODOS ON\u00A7r"));
                s.addChatMessage(new ChatComponentText("\u00A7c[UBM2]\u00A7r QVSO+SPR+OCC+DFB+BNU+TRC+BMC+FDC+PRP+EIC+ILC+PVT+FOV+AIC+CSC+15"));
            } else {
                s.addChatMessage(new ChatComponentText("\u00A7cUse /ubm2 0 ou /ubm2 1"));
            }
        }
    }
}
