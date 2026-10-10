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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.particle.EffectRenderer;
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
import net.minecraftforge.client.event.RenderHandEvent;
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
import net.minecraftforge.event.world.ExplosionEvent;

/**
 * UBM - Ultimate Boost Mod (arquivo único, sem ASM)
 *
 * Comando: /ubm 0 (desliga) | /ubm 1 (liga)
 */
@Mod(modid = UbmMod.MODID, name = "Ultimate Boost", version = "1.2", acceptedMinecraftVersions = "[1.7.10]")
public class UbmMod {

    public static final String MODID = "ubm";
    public static UbmMod instance;

    static volatile boolean enabled = false;
    static Configuration cfg;

    static final int IOAB_BASE_BUDGET = 800;
    static final int IOAB_MIN_BUDGET  = 200;
    static final int IOAB_MAX_BUDGET  = 2400;
    static final int IOAB_REEVAL      = 10;
    static final int TH_A = 80, TH_B = 60, TH_C = 40, TH_D = 25, TH_E = 12;

    static final int TE_CHEST  = 32;
    static final int TE_HOPPER = 32;
    static final int TE_MODS   = 48;

    static final int ITEM_MERGE_CELL = 3;
    static final int ITEM_LIFE       = 2400;
    static final int JUNK_LIFE       = 200;
    static final int ITEM_CAP        = 32;
    static final int ITEM_FAR        = 96;
    static final int XP_CAP          = 4;
    static final int XP_FAR          = 64;

    static final int SPAWN_DENY_PCT = 40;
    static final int DESPAWN_DIST   = 64;
    static final int MOB_CHUNK      = 24;
    static final int MOB_WORLD      = 300;
    static final int ANIMAL_CHUNK   = 10;
    static final int FOLLOW_RANGE   = 16;
    static final int REAP_MARGIN    = 2;
    static final int ARROW_LIFE     = 200;
    static final int EXPL_PER_TICK  = 6;
    static final int SLIME_CAP      = 10;
    static final int TNT_CAP        = 24;
    static final int PROJ_CAP       = 40;

    static final int CULL_LIVING   = 64;
    static final int CULL_DYN      = 48;
    static final int CULL_TE       = 32;
    static final int NAME_DIST     = 8;
    static final int PART_DROP_PCT = 50;
    static final int PART_DIST     = 20;
    static final int RD_MAX        = 3;
    static final float FOV_MAX     = 70f;
    static final int FPS_CAP       = 260;
    static final int UNFOCUS_MS    = 100;

    static final double CUBE_MAX_DIST = 64.0 * 64.0;

    static final Random RND = new Random();
    static final UUID FOLLOW_ID = UUID.fromString("0b3d5a7e-1f2c-4d6b-9e8a-7c5b3a1f0d2e");
    static final Set<String> JUNK = new HashSet<String>();

    static volatile int fps = 0;
    static volatile double mspt = 0;
    static int skippedPerSec = 0, skippedThisSec = 0;
    static int cullPerSec = 0, cullLiving = 0, cullDyn = 0, cullTe = 0;
    static int itemsMerged = 0, orbsMerged = 0, spawnsDenied = 0, despawned = 0;
    static int mobCapKilled = 0, chunksReaped = 0, teSlept = 0, genDenied = 0;
    static int partCleared = 0, soundsBarred = 0, partDropped = 0;
    static int itemsFarKilled = 0, orbsFarKilled = 0, projKilled = 0;
    static int slimeDenied = 0, tntDenied = 0, followCut = 0, dimsUnloaded = 0;
    static int frameLiving = 0, frameDyn = 0, frameTe = 0;
    static int currentBudget = IOAB_BASE_BUDGET;
    static int processedThisTick = 0;
    static int lowMsptStreak = 0, highMsptStreak = 0;

    public UbmMod() { instance = this; }

    public static void setEnabled(boolean b) { enabled = b; }
    public static boolean isEnabled() { return enabled; }

    // ================= IOAB =================
    static class Score { int value = 50; int cooldown = 0; }
    static final Map<EntityLivingBase, Score> SCORES = new WeakHashMap<EntityLivingBase, Score>();

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

    static double sq(double v) { return v * v; }

    // ================= TE =================
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
            if (n.contains("TileEntityChest") || n.contains("TileEntityEnderChest")
                || n.contains("TileEntityEnchantmentTable")) r = 1;
            else if (n.contains("TileEntityHopper") || n.contains("TileEntityDispenser")
                || n.contains("TileEntityDropper")) r = 2;
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
        if (el instanceof EntityTameable || el instanceof IBossDisplayData
            || el instanceof EntityVillager) return false;
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
            Integer v = ReflectionHelper.getPrivateValue(ServerConfigurationManager.class, scm,
                "viewDistance", "field_72402_d");
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
                if (Math.abs(c.xPosition - px) <= rad && Math.abs(c.zPosition - pz) <= rad) {
                    near = true; break;
                }
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
        a.applyModifier(new AttributeModifier(FOLLOW_ID, "ubm follow",
            -(1.0 - FOLLOW_RANGE / base), 1).setSaved(false));
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
        gr.setOrCreateGameRule("randomTickSpeed", enabled ? "1" : "3");
        gr.setOrCreateGameRule("doFireTick", enabled ? "false" : "true");
        for (WorldServer w : srv.worldServers) if (w != null) wake(w);
    }

    // ================= CICLO DE VIDA =================
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        cfg = new Configuration(e.getSuggestedConfigurationFile());
        cfg.load();
        enabled = cfg.getBoolean("ativo", "geral", false, "Liga/desliga tudo");
        String[] junk = cfg.getStringList("itens_lixo", "geral",
            new String[] { "minecraft:cobblestone", "minecraft:dirt", "minecraft:gravel",
                "minecraft:netherrack", "minecraft:rotten_flesh", "minecraft:wheat_seeds",
                "minecraft:sapling", "minecraft:stone" },
            "Itens que somem rápido");
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
    }

    static void saveState() {
        if (cfg == null) return;
        cfg.get("geral", "ativo", false).set(enabled);
        cfg.save();
    }

    // ================= SERVIDOR =================
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

            if (enabled) {
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
            }
        }

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent e) {
            if (e.world.isRemote || !(e.world instanceof WorldServer)) return;
            if (e.phase != TickEvent.Phase.START) return;
            if (!enabled) return;
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
            } catch (Throwable t) { }
        }

        @SubscribeEvent
        public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
            if (!enabled) return;
            EntityLivingBase en = e.entityLiving;
            if (en == null || en.worldObj == null || en.worldObj.isRemote) return;
            if (en instanceof EntityPlayer) return;
            if (en instanceof IBossDisplayData) return;
            if (en.riddenByEntity != null || en.ridingEntity != null) return;
            if (en.hurtTime > 0 || en.deathTime > 0 || en.getHealth() <= 0f) return;

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

            if (interval > 1 && (worldTick + en.getEntityId()) % interval != 0L) {
                e.setCanceled(true);
                skippedThisSec++;
            }
        }

        @SubscribeEvent
        public void onJoin(EntityJoinWorldEvent e) {
            if (!enabled || e.world.isRemote) return;
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
            if (!enabled || e.world.isRemote) return;
            try {
                double p = SPAWN_DENY_PCT / 100.0;
                if (!(e.entityLiving instanceof IMob)) p = p / 2.0;
                if (RND.nextDouble() < p) { e.setResult(Event.Result.DENY); spawnsDenied++; }
            } catch (Throwable t) { }
        }

        @SubscribeEvent
        public void onExplosionStart(ExplosionEvent.Start e) {
            if (!enabled || e.world.isRemote) return;
            try {
                if (++explThisTick > EXPL_PER_TICK) e.setCanceled(true);
            } catch (Throwable t) { }
        }

        @SubscribeEvent
        public void onAllowDespawn(LivingSpawnEvent.AllowDespawn e) {
            if (!enabled || e.world.isRemote) return;
            try {
                if (!(e.entityLiving instanceof EntityLiving)) return;
                EntityLiving el = (EntityLiving) e.entityLiving;
                double n = nearestPlayerSq(e.world, el.posX, el.posY, el.posZ);
                if (n < 0) return;
                if (n > sq(DESPAWN_DIST)) { e.setResult(Event.Result.ALLOW); despawned++; }
            } catch (Throwable t) { }
        }
    }

    // ================= WORLDGEN =================
    public static class TerrainEvents {
        static final Set<String> D2 = set("DEAD_BUSH", "LILYPAD", "SHROOM", "BIG_SHROOM", "FLOWERS");
        static final Set<String> P1 = set("LAKE", "LAVA", "NETHER_LAVA", "NETHER_LAVA2");

        static Set<String> set(String... a) {
            Set<String> s = new HashSet<String>();
            for (String x : a) s.add(x);
            return s;
        }

        @SubscribeEvent
        public void onDecorate(DecorateBiomeEvent.Decorate e) {
            if (!enabled) return;
            try {
                String n = e.type.name();
                if (n.equals("LAKE") || D2.contains(n)) { e.setResult(Event.Result.DENY); genDenied++; }
            } catch (Throwable t) { }
        }

        @SubscribeEvent
        public void onPopulate(PopulateChunkEvent.Populate e) {
            if (!enabled) return;
            try {
                if (P1.contains(e.type.name())) { e.setResult(Event.Result.DENY); genDenied++; }
            } catch (Throwable t) { }
        }
    }

    // ================= RENDER VAZIO =================
    @SideOnly(Side.CLIENT)
    public static class NoRender extends Render {
        final Render inner;

        public NoRender() {
            this.inner = null;
            try { setRenderManager(RenderManager.instance); } catch (Throwable t) { }
        }

        public NoRender(Render inner) {
            this.inner = inner;
            try { setRenderManager(RenderManager.instance); } catch (Throwable t) { }
        }

        @Override
        public void doRender(Entity e, double x, double y, double z, float yaw, float pt) { }

        @Override
        public void doRenderShadowAndFire(Entity e, double x, double y, double z, float yaw, float pt) { }

        @Override
        protected ResourceLocation getEntityTexture(Entity e) {
            try { return TextureMap.locationBlocksTexture; } catch (Throwable t) { return null; }
        }
    }

    @SideOnly(Side.CLIENT)
    public static class NoTesr extends TileEntitySpecialRenderer {
        final TileEntitySpecialRenderer inner;

        public NoTesr() { this.inner = null; }
        public NoTesr(TileEntitySpecialRenderer inner) { this.inner = inner; }

        @Override
        public void renderTileEntityAt(TileEntity te, double x, double y, double z, float pt) { }
    }

    @SideOnly(Side.CLIENT)
    public static class ThrottledFx extends EffectRenderer {
        public ThrottledFx(World w, TextureManager tm) { super(w, tm); }

        @Override
        public void addEffect(net.minecraft.client.particle.EntityFX fx) {
            if (enabled) {
                try {
                    if (RND.nextInt(100) < PART_DROP_PCT) { partDropped++; return; }
                    Entity v = Minecraft.getMinecraft().renderViewEntity;
                    if (v != null && fx.getDistanceSqToEntity(v) > sq(PART_DIST)) { partDropped++; return; }
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

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onOverlayPre(RenderGameOverlayEvent.Pre e) {
            if (!enabled) return;
            if (e.type == RenderGameOverlayEvent.ElementType.TEXT) return;
            e.setCanceled(true);
        }

        @SubscribeEvent
        public void onOverlayPost(RenderGameOverlayEvent.Post e) {
            if (!enabled) return;
            if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc == null || mc.thePlayer == null) return;
                EntityPlayer p = mc.thePlayer;
                int w = e.resolution.getScaledWidth();
                float hp = Math.max(0, Math.min(1, p.getHealth() / p.getMaxHealth()));
                float fd = Math.max(0, Math.min(1, p.getFoodStats().getFoodLevel() / 20.0f));
                Gui.drawRect(4, 4, 104, 11, 0xFF000000);
                Gui.drawRect(4, 4, 4 + (int)(100 * hp), 11, hp > 0.5f ? 0xFF00CC00 : (hp > 0.25f ? 0xFFCCAA00 : 0xFFCC0000));
                Gui.drawRect(4, 14, 104, 21, 0xFF000000);
                Gui.drawRect(4, 14, 4 + (int)(100 * fd), 21, 0xFFFF8800);
                Gui.drawRect(w - 104, 4, w - 4, 11, 0xFF000000);
                Gui.drawRect(w - 104, 4, w - 104 + (int)(100 * p.experience), 11, 0xFFCC00FF);
                mc.fontRenderer.drawStringWithShadow("Lv " + p.experienceLevel, w - 130, 4, 0xFFFFFFFF);
            } catch (Throwable t) { }
        }

        @SubscribeEvent public void onHand(RenderHandEvent e) { if (enabled) e.setCanceled(true); }
        @SubscribeEvent public void onHighlight(DrawBlockHighlightEvent e) { if (enabled) e.setCanceled(true); }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onRenderLiving(RenderLivingEvent.Pre e) {
            if (!enabled) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) return;
            if (e.entity == mc.thePlayer) return;
            e.setCanceled(true);
            cullLiving++;
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onSpecials(RenderLivingEvent.Specials.Pre e) {
            if (enabled) e.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onPlayer(RenderPlayerEvent.Pre e) {
            if (!enabled) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && e.entityPlayer == mc.thePlayer) e.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onPlayerSpecials(RenderPlayerEvent.Specials.Pre e) {
            if (enabled) e.setCanceled(true);
        }

        @SubscribeEvent public void onFogDensity(EntityViewRenderEvent.FogDensity e) { if (enabled) e.density = 0f; }
        @SubscribeEvent public void onFogColors(EntityViewRenderEvent.FogColors e) {
            if (!enabled) return;
            e.red = 0.5f; e.green = 0.5f; e.blue = 0.5f;
        }

        @SubscribeEvent
        public void onSound(PlaySoundEvent e) {
            if (!enabled) return;
            try { e.result = null; soundsBarred++; } catch (Throwable t) { }
        }

        // ============ CUBOS PRETOS ============
        @SubscribeEvent
        public void onWorldLast(RenderWorldLastEvent e) {
            if (!enabled) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;

            EntityPlayer p = mc.thePlayer;
            double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * e.partialTicks;
            double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * e.partialTicks;
            double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * e.partialTicks;

            List list = mc.theWorld.loadedEntityList;
            if (list == null || list.isEmpty()) return;

            boolean firstPerson = mc.gameSettings.thirdPersonView == 0;

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
                    if (dSq > CUBE_MAX_DIST) continue;

                    float s = boxSize(en);
                    if (s <= 0) continue;

                    float cy = (float)(ey + en.height * 0.5);
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

        // ============ RENDER TICK ============
        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                frameLiving = 0; frameDyn = 0; frameTe = 0;
                if (enabled) {
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
                    System.out.println("[UBM] wrapAll geral falhou: " + t);
                }
            }
            if (enabled) clearParticles(mc);

            frames++;
            long now = System.currentTimeMillis();
            if (now - lastFps >= 1000) {
                fps = frames; frames = 0; lastFps = now;
                cullPerSec = cullLiving + cullDyn + cullTe;
                cullLiving = 0; cullDyn = 0; cullTe = 0;
            }

            if (lastEnabled != (enabled ? 1 : 0)) {
                lastEnabled = enabled ? 1 : 0;
                applySettings(mc);
            }
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
                if (enabled) {
                    mc.theWorld.setRainStrength(0.0F);
                    mc.theWorld.setThunderStrength(0.0F);
                }
            } catch (Throwable t) { }
        }

        // ============ WRAP ============
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

                        try {
                            m.put(en.getKey(), new NoRender((Render) v));
                        } catch (Throwable t) { }
                    }
                }
            } catch (Throwable t) {
                System.out.println("[UBM] wrapAll entity falhou: " + t);
            }

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

                        try {
                            m.put(en.getKey(), new NoTesr((TileEntitySpecialRenderer) v));
                        } catch (Throwable t) { }
                    }
                }
            } catch (Throwable t) {
                System.out.println("[UBM] wrapAll tesr falhou: " + t);
            }
        }

        void clearParticles(Minecraft mc) {
            if (mc == null || mc.effectRenderer == null) return;
            if (!FX_CHECKED) {
                FX_CHECKED = true;
                String[] names = { "fxLayers", "field_78876_b" };
                for (int i = 0; i < names.length; i++) {
                    try {
                        Field f = EffectRenderer.class.getDeclaredField(names[i]);
                        f.setAccessible(true); FX_LAYERS = f; break;
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
            try {
                applySettingsInternal(mc, gs);
            } catch (Throwable t) {
                System.out.println("[UBM] applySettings falhou: " + t);
            }
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
            if (enabled) {
                gs.fancyGraphics = false;
                gs.clouds = false;
                gs.viewBobbing = false;
                gs.snooperEnabled = false;
                gs.advancedOpengl = false;
                gs.enableVsync = false;
                gs.ambientOcclusion = 0;
                gs.particleSetting = 2;
                gs.renderDistanceChunks = Math.min(oRender, RD_MAX);
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
            e.left.add("\u00A7c[UBM]\u00A7r " + (enabled ? "\u00A7aON" : "\u00A77OFF")
                + " | fps " + fps + " | mspt " + String.format("%.1f", mspt)
                + " | budget " + currentBudget);
            if (enabled) {
                e.left.add("\u00A7c[UBM]\u00A7r skip " + skippedPerSec
                    + " | cull " + cullPerSec
                    + " | TE " + totalSleeping()
                    + " | som " + soundsBarred
                    + " | part " + partDropped);
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
                s.addChatMessage(new ChatComponentText("\u00A7c[UBM]\u00A7r "
                    + (enabled ? "\u00A7aON" : "\u00A77OFF")));
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
                s.addChatMessage(new ChatComponentText("\u00A7c[UBM]\u00A77 DESLIGADO\u00A7r"));
            } else if (v == 1) {
                setEnabled(true);
                saveState();
                currentBudget = IOAB_BASE_BUDGET;
                SCORES.clear();
                s.addChatMessage(new ChatComponentText("\u00A7c[UBM]\u00A7a ATIVADO\u00A7r"));
                s.addChatMessage(new ChatComponentText("\u00A7c[UBM]\u00A7r IOAB + TE dormindo + culling + cubos + HUD simples"));
            } else {
                s.addChatMessage(new ChatComponentText("\u00A7cUse /ubm 0 ou /ubm 1"));
            }
        }
    }
}
