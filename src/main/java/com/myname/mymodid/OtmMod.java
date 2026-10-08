package com.myname.mymodid;

import java.lang.reflect.Field;
import java.util.List;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingEvent;

@Mod(modid = OtmMod.MODID, name = "OTM Performance", version = "5.0", acceptedMinecraftVersions = "[1.7.10]")
public class OtmMod {

    public static final String MODID = "otm";
    public static OtmMod instance;

    static int level = 0;

    static int d2 = 64, d4 = 96, d8 = 128, d16 = 192, d32 = 256;

    static int cullDist        = 96;
    static int playerCullDist  = 128;
    static int mobDespawnDist  = 128;
    static int tileDist        = 96;

    static int itemCapPerWorld  = 600;
    static int xpOrbCapPerWorld = 300;
    static int itemMergeDist    = 2;
    static int itemMergeAge     = 40;
    static int itemDespawnAge   = 6000;
    static int arrowDespawnAge  = 1200;

    static int forcedRenderDist = -1;
    static int fpsLow = 25, fpsHigh = 70, minRender = 4;

    static boolean adaptiveTick     = false;
    static boolean entityCull       = false;
    static boolean itemMerge        = false;
    static boolean xpMerge          = false;
    static boolean adaptiveClient   = false;
    static boolean pathThrottle     = false;
    static boolean aiTargetCull     = false;
    static boolean itemDespawnBoost = false;
    static boolean mobDespawnBoost  = false;
    static boolean xpOrbCull        = false;
    static boolean arrowCull        = false;
    static boolean itemEntityCap    = false;
    static boolean playerRenderCull = false;
    static boolean tileThrottle     = false;
    static boolean metrics = true;

    static volatile double msptAvg = 0;
    static volatile int fps = 0;
    static volatile int skippedPerSec = 0, culledPerSec = 0;
    static volatile int itemsMerged = 0, xpMerged = 0;
    static volatile int mobsDespawned = 0, tilesPaused = 0, pathsCleared = 0;
    static volatile int itemsCapped = 0;

    static int skippedCount = 0, culledCount = 0;
    static int itemsMergedCount = 0, xpMergedCount = 0;
    static int mobsDespawnedCount = 0, tilePausedCount = 0;
    static int pathsClearedCount = 0, itemsCappedCount = 0;

    public OtmMod() { instance = this; }

    static void applyLevel(int lvl) {
        level = MathHelper.clamp_int(lvl, 0, 5);

        if (level == 0) {
            adaptiveTick=false; entityCull=false; itemMerge=false; xpMerge=false;
            adaptiveClient=false; pathThrottle=false; aiTargetCull=false;
            itemDespawnBoost=false; mobDespawnBoost=false; xpOrbCull=false;
            arrowCull=false; itemEntityCap=false; playerRenderCull=false;
            tileThrottle=false;
            forcedRenderDist = -1;
            return;
        }

        adaptiveTick=true; entityCull=true; itemMerge=true; xpMerge=true;
        adaptiveClient=true; pathThrottle=true; aiTargetCull=true;
        itemDespawnBoost=true; mobDespawnBoost=true; xpOrbCull=true;
        arrowCull=true; itemEntityCap=true; playerRenderCull=true;
        tileThrottle=true;

        if (level == 1) {
            d2=64; d4=96; d8=128; d16=192; d32=256;
            cullDist=96; playerCullDist=128; mobDespawnDist=128; tileDist=96;
            itemCapPerWorld=600; xpOrbCapPerWorld=300;
            itemMergeDist=2; itemMergeAge=40;
            itemDespawnAge=6000; arrowDespawnAge=1200;
            forcedRenderDist = -1;
            fpsLow=25; fpsHigh=70; minRender=6;
        } else if (level == 2) {
            d2=32; d4=64; d8=96; d16=128; d32=192;
            cullDist=64; playerCullDist=96; mobDespawnDist=64; tileDist=64;
            itemCapPerWorld=300; xpOrbCapPerWorld=150;
            itemMergeDist=3; itemMergeAge=20;
            itemDespawnAge=3600; arrowDespawnAge=800;
            forcedRenderDist = 6;
            fpsLow=30; fpsHigh=75; minRender=4;
        } else if (level == 3) {
            d2=8; d4=12; d8=16; d16=20; d32=24;
            cullDist=16; playerCullDist=32; mobDespawnDist=32; tileDist=32;
            itemCapPerWorld=60; xpOrbCapPerWorld=30;
            itemMergeDist=6; itemMergeAge=5;
            itemDespawnAge=1200; arrowDespawnAge=200;
            forcedRenderDist = 2;
            fpsLow=60; fpsHigh=120; minRender=2;
        } else if (level == 4) {
            d2=4; d4=6; d8=8; d16=10; d32=12;
            cullDist=12; playerCullDist=16; mobDespawnDist=24; tileDist=16;
            itemCapPerWorld=30; xpOrbCapPerWorld=15;
            itemMergeDist=8; itemMergeAge=2;
            itemDespawnAge=600; arrowDespawnAge=100;
            forcedRenderDist = 2;
            fpsLow=70; fpsHigh=144; minRender=2;
        } else {
            // NÍVEL 5 - HARDCORE (mobs visíveis em 12 blocos pra não surgir do nada)
            d2=3; d4=4; d8=5; d16=6; d32=7;
            cullDist=12; playerCullDist=8; mobDespawnDist=20; tileDist=8;
            itemCapPerWorld=15; xpOrbCapPerWorld=8;
            itemMergeDist=10; itemMergeAge=1;
            itemDespawnAge=400; arrowDespawnAge=60;
            forcedRenderDist = 2;
            fpsLow=90; fpsHigh=144; minRender=2;
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Configuration c = new Configuration(e.getSuggestedConfigurationFile());
        c.load();
        int start = c.getInt("nivel_inicial", "geral", 0, 0, 5, "0=off 1=leve 2=medio 3=extremo 4=insano 5=hardcore");
        metrics = c.getBoolean("metricas_f3", "geral", true, "Mostra no F3");
        c.save();
        applyLevel(start);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        CommonEvents common = new CommonEvents();
        MinecraftForge.EVENT_BUS.register(common);
        FMLCommonHandler.instance().bus().register(common);
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientEvents cli = new ClientEvents();
            MinecraftForge.EVENT_BUS.register(cli);
            FMLCommonHandler.instance().bus().register(cli);
        }
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new CmdOtm());
    }

    // ================== SERVER ==================
    public static class CommonEvents {

        private long tickStart = 0;
        private int tickCounter = 0;

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                tickStart = System.nanoTime();
            } else {
                double ms = (System.nanoTime() - tickStart) / 1_000_000.0;
                msptAvg = msptAvg * 0.95 + ms * 0.05;
                if (++tickCounter >= 20) {
                    tickCounter = 0;
                    skippedPerSec=skippedCount; skippedCount=0;
                    culledPerSec=culledCount;   culledCount=0;
                    itemsMerged=itemsMergedCount; itemsMergedCount=0;
                    xpMerged=xpMergedCount; xpMergedCount=0;
                    mobsDespawned=mobsDespawnedCount; mobsDespawnedCount=0;
                    tilesPaused=tilePausedCount; tilePausedCount=0;
                    pathsCleared=pathsClearedCount; pathsClearedCount=0;
                    itemsCapped=itemsCappedCount; itemsCappedCount=0;
                }
            }
        }

        @SubscribeEvent
        public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
            if (level == 0 || !adaptiveTick) return;
            EntityLivingBase en = e.entityLiving;
            if (en == null || en.worldObj == null || en.worldObj.isRemote) return;
            if (en instanceof EntityPlayer) return;
            if (en instanceof IBossDisplayData) return;
            if (en.riddenByEntity != null || en.ridingEntity != null) return;
            if (en.hurtTime > 0 || en.deathTime > 0 || en.getHealth() <= 0f) return;

            if (en instanceof EntityLiving) {
                EntityLiving el = (EntityLiving) en;
                if (el.getAttackTarget() != null) return;

                if (aiTargetCull) {
                    double d = nearestPlayerSq(en);
                    if (d > (double) d8 * d8) el.setAttackTarget(null);
                }

                if (pathThrottle && !el.getNavigator().noPath()) {
                    double d = nearestPlayerSq(en);
                    if (d > (double) d8 * d8) {
                        el.getNavigator().clearPathEntity();
                        pathsClearedCount++;
                    }
                }
            }

            double minSq = nearestPlayerSq(en);
            int interval = 1;
            if (minSq > (double) d2 * d2)  interval = 2;
            if (minSq > (double) d4 * d4)  interval = 4;
            if (minSq > (double) d8 * d8)  interval = 8;
            if (minSq > (double) d16 * d16) interval = 16;
            if (minSq > (double) d32 * d32) interval = 32;

            if (interval > 1) {
                long t = en.worldObj.getTotalWorldTime() + en.getEntityId();
                if (t % interval != 0L) { e.setCanceled(true); skippedCount++; }
            }
        }

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            if (level == 0) return;
            World w = e.world;
            if (w == null || w.isRemote) return;
            long now = w.getTotalWorldTime();

            if ((itemMerge || xpMerge) && now % 20L == 0L) {
                if (itemMerge) doMergeItems(w);
                if (xpMerge)   doMergeXP(w);
            }
            if ((itemDespawnBoost || mobDespawnBoost || xpOrbCull || arrowCull) && now % 40L == 0L) {
                doEntityHousekeeping(w);
            }
            if (itemEntityCap && now % 60L == 0L) doEntityCaps(w);
            if (tileThrottle && now % 40L == 0L) doTileThrottle(w);
        }

        private static Field TE_LIST_FIELD = null;
        private static boolean TE_FIELD_CHECKED = false;

        @SuppressWarnings("unchecked")
        private static List<TileEntity> getTickableTE(World w) {
            if (!TE_FIELD_CHECKED) {
                TE_FIELD_CHECKED = true;
                String[] names = new String[] {
                    "tickableTileEntities",
                    "field_147483_b",
                    "field_72997_g",
                    "loadedTileEntityList"
                };
                for (int i = 0; i < names.length; i++) {
                    try {
                        Field f = World.class.getDeclaredField(names[i]);
                        f.setAccessible(true);
                        TE_LIST_FIELD = f;
                        break;
                    } catch (Throwable t) { }
                }
            }
            if (TE_LIST_FIELD == null) return null;
            try {
                Object o = TE_LIST_FIELD.get(w);
                if (o instanceof List) return (List<TileEntity>) o;
            } catch (Throwable t) { }
            return null;
        }

        private void doTileThrottle(World w) {
            List<TileEntity> tick = getTickableTE(w);
            if (tick == null) return;

            List players = w.playerEntities;
            if (players == null || players.isEmpty()) return;

            double limit = (double) tileDist * tileDist;

            try {
                for (int i = tick.size() - 1; i >= 0; i--) {
                    TileEntity te;
                    try { te = tick.get(i); } catch (Throwable t) { continue; }
                    if (te == null) continue;

                    double min = Double.MAX_VALUE;
                    for (int p = 0; p < players.size(); p++) {
                        Object po = players.get(p);
                        if (!(po instanceof EntityPlayer)) continue;
                        EntityPlayer pl = (EntityPlayer) po;
                        double dx = pl.posX - (te.xCoord + 0.5);
                        double dy = pl.posY - (te.yCoord + 0.5);
                        double dz = pl.posZ - (te.zCoord + 0.5);
                        double d = dx*dx + dy*dy + dz*dz;
                        if (d < min) min = d;
                    }

                    if (min > limit) {
                        tick.remove(i);
                        tilePausedCount++;
                    }
                }
            } catch (Throwable t) { }
        }

        private void doEntityHousekeeping(World w) {
            List ents = w.loadedEntityList;
            if (ents == null) return;
            List players = w.playerEntities;
            if (players == null || players.isEmpty()) return;

            double mobLimit = (double) mobDespawnDist * mobDespawnDist;

            for (int i = ents.size() - 1; i >= 0; i--) {
                Object o;
                try { o = ents.get(i); } catch (Throwable t) { continue; }
                if (!(o instanceof Entity)) continue;
                Entity en = (Entity) o;
                if (en.isDead) continue;

                double minSq = Double.MAX_VALUE;
                for (int p = 0; p < players.size(); p++) {
                    Object po = players.get(p);
                    if (!(po instanceof EntityPlayer)) continue;
                    double d = en.getDistanceSqToEntity((EntityPlayer) po);
                    if (d < minSq) minSq = d;
                }

                if (itemDespawnBoost && en instanceof EntityItem) {
                    if (minSq > mobLimit && en.ticksExisted > itemDespawnAge) en.setDead();
                } else if (xpOrbCull && en instanceof EntityXPOrb) {
                    if (minSq > mobLimit) en.setDead();
                } else if (arrowCull && en instanceof EntityArrow) {
                    if (minSq > mobLimit && en.ticksExisted > arrowDespawnAge) en.setDead();
                } else if (mobDespawnBoost && en instanceof IMob
                        && !(en instanceof IBossDisplayData)) {
                    if (minSq > mobLimit) {
                        en.setDead();
                        mobsDespawnedCount++;
                    }
                }
            }
        }

        private void doEntityCaps(World w) {
            List ents = w.loadedEntityList;
            if (ents == null) return;
            int itemCount = 0, xpCount = 0;
            for (int i = 0; i < ents.size(); i++) {
                Object o = ents.get(i);
                if (o instanceof EntityItem) itemCount++;
                else if (o instanceof EntityXPOrb) xpCount++;
            }
            if (itemCount <= itemCapPerWorld && xpCount <= xpOrbCapPerWorld) return;

            int toRemove = Math.max(0, itemCount - itemCapPerWorld);
            for (int i = 0; i < ents.size() && toRemove > 0; i++) {
                Object o = ents.get(i);
                if (o instanceof EntityItem) {
                    EntityItem ei = (EntityItem) o;
                    if (ei.isDead) continue;
                    if (ei.ticksExisted > 200) { ei.setDead(); itemsCappedCount++; toRemove--; }
                }
            }
            int xpToRemove = Math.max(0, xpCount - xpOrbCapPerWorld);
            for (int i = 0; i < ents.size() && xpToRemove > 0; i++) {
                Object o = ents.get(i);
                if (o instanceof EntityXPOrb) {
                    EntityXPOrb x = (EntityXPOrb) o;
                    if (x.isDead) continue;
                    if (x.ticksExisted > 200) { x.setDead(); xpToRemove--; }
                }
            }
        }

        private double nearestPlayerSq(Entity en) {
            List players = en.worldObj.playerEntities;
            if (players == null || players.isEmpty()) return Double.MAX_VALUE;
            double min = Double.MAX_VALUE;
            for (int i = 0; i < players.size(); i++) {
                Object o = players.get(i);
                if (!(o instanceof EntityPlayer)) continue;
                double d = en.getDistanceSqToEntity((EntityPlayer) o);
                if (d < min) min = d;
            }
            return min;
        }

        private void doMergeItems(World w) {
            List items = w.loadedEntityList;
            if (items == null || items.isEmpty()) return;
            int n = items.size();
            for (int i = 0; i < n; i++) {
                Object o1 = items.get(i);
                if (!(o1 instanceof EntityItem)) continue;
                EntityItem a = (EntityItem) o1;
                if (a.isDead || a.ticksExisted < itemMergeAge) continue;
                ItemStack sa = a.getEntityItem();
                if (sa == null) continue;
                for (int j = i + 1; j < n; j++) {
                    Object o2 = items.get(j);
                    if (!(o2 instanceof EntityItem)) continue;
                    EntityItem b = (EntityItem) o2;
                    if (b.isDead || b.ticksExisted < itemMergeAge) continue;
                    if (a.getDistanceSqToEntity(b) > (double) itemMergeDist * itemMergeDist) continue;
                    ItemStack sb = b.getEntityItem();
                    if (sb == null) continue;
                    if (!ItemStack.areItemStacksEqual(sa, sb)) continue;
                    if (!sa.isItemEqual(sb)) continue;
                    if (sa.hasTagCompound() || sb.hasTagCompound()) continue;
                    int total = sa.stackSize + sb.stackSize;
                    int max = sa.getMaxStackSize();
                    if (total <= max) { sa.stackSize = total; b.setDead(); }
                    else { sa.stackSize = max; sb.stackSize = total - max; }
                    itemsMergedCount++;
                }
            }
        }

        private static Field XP_VALUE_FIELD = null;
        private static boolean XP_FIELD_CHECKED = false;

        private static Field findXpField() {
            if (XP_FIELD_CHECKED) return XP_VALUE_FIELD;
            XP_FIELD_CHECKED = true;
            String[] names = new String[] { "xpValue", "field_70532_e" };
            for (int i = 0; i < names.length; i++) {
                try {
                    Field f = EntityXPOrb.class.getDeclaredField(names[i]);
                    f.setAccessible(true);
                    XP_VALUE_FIELD = f;
                    return f;
                } catch (Throwable t) { }
            }
            return null;
        }

        private void doMergeXP(World w) {
            Field f = findXpField();
            if (f == null) return;

            List items = w.loadedEntityList;
            if (items == null || items.isEmpty()) return;
            int n = items.size();
            for (int i = 0; i < n; i++) {
                Object o1 = items.get(i);
                if (!(o1 instanceof EntityXPOrb)) continue;
                EntityXPOrb a = (EntityXPOrb) o1;
                if (a.isDead) continue;
                for (int j = i + 1; j < n; j++) {
                    Object o2 = items.get(j);
                    if (!(o2 instanceof EntityXPOrb)) continue;
                    EntityXPOrb b = (EntityXPOrb) o2;
                    if (b.isDead) continue;
                    if (a.getDistanceSqToEntity(b) > 4.0) continue;
                    try {
                        int va = f.getInt(a);
                        int vb = f.getInt(b);
                        f.setInt(a, va + vb);
                        b.setDead();
                        xpMergedCount++;
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    // ================== CLIENTE ==================
    @SideOnly(Side.CLIENT)
    public static class ClientEvents {

        private int frames = 0;
        private long lastFps = 0;
        private long lastAdjust = 0;
        private int lowSeconds = 0;
        private int lastAppliedLevel = -1;

        private int origRender = -1;
        private boolean origFancy = true;
        private int origParticles = 0;
        private boolean origClouds = true;
        private int origAO = 2;

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onRenderLiving(RenderLivingEvent.Pre e) {
            if (level == 0 || !entityCull) return;
            if (e.entity == null) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            if (e.entity instanceof EntityPlayer) return;
            if (e.entity instanceof IBossDisplayData) return;

            double dSq = mc.thePlayer.getDistanceSqToEntity(e.entity);
            if (dSq > (double) cullDist * cullDist) {
                e.setCanceled(true);
                culledCount++;
            }
        }

        @SubscribeEvent
        public void onRenderPlayer(RenderPlayerEvent.Pre e) {
            if (level == 0 || !playerRenderCull) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || e.entityPlayer == null) return;
            if (e.entityPlayer == mc.thePlayer) return;
            double dSq = mc.thePlayer.getDistanceSqToEntity(e.entityPlayer);
            if (dSq > (double) playerCullDist * playerCullDist) e.setCanceled(true);
        }

        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            frames++;
            long now = System.currentTimeMillis();
            if (now - lastFps >= 1000) {
                fps = frames; frames = 0; lastFps = now;
                if (level > 0 && adaptiveClient) adapt(now);
            }

            if (lastAppliedLevel != level) {
                lastAppliedLevel = level;
                applyClientLevel();
            }
        }

        private void applyClientLevel() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null) return;

            if (origRender < 0) {
                origRender = mc.gameSettings.renderDistanceChunks;
                origFancy = mc.gameSettings.fancyGraphics;
                origParticles = mc.gameSettings.particleSetting;
                origClouds = mc.gameSettings.clouds;
                origAO = mc.gameSettings.ambientOcclusion;
            }

            if (level == 0) {
                if (origRender > 0) mc.gameSettings.renderDistanceChunks = origRender;
                mc.gameSettings.fancyGraphics = origFancy;
                mc.gameSettings.particleSetting = origParticles;
                mc.gameSettings.clouds = origClouds;
                mc.gameSettings.ambientOcclusion = origAO;
                mc.renderGlobal.loadRenderers();
                return;
            }

            if (level == 1) {
                return;
            }

            if (forcedRenderDist > 0) {
                mc.gameSettings.renderDistanceChunks = forcedRenderDist;
            }
            mc.gameSettings.fancyGraphics = false;
            mc.gameSettings.clouds = false;
            if (level >= 2) mc.gameSettings.particleSetting = 2;
            if (level >= 3) mc.gameSettings.ambientOcclusion = 0;

            mc.renderGlobal.loadRenderers();
            lastAdjust = System.currentTimeMillis();
        }

        private void adapt(long now) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null || mc.currentScreen != null) return;

            if (origRender < 0) {
                origRender = mc.gameSettings.renderDistanceChunks;
                origFancy = mc.gameSettings.fancyGraphics;
                origParticles = mc.gameSettings.particleSetting;
                origClouds = mc.gameSettings.clouds;
                origAO = mc.gameSettings.ambientOcclusion;
            }

            if (fps < fpsLow) lowSeconds++; else lowSeconds = 0;

            if (lowSeconds >= 3 && now - lastAdjust > 8000) {
                boolean changed = false;
                if (mc.gameSettings.fancyGraphics) {
                    mc.gameSettings.fancyGraphics = false; changed = true;
                } else if (mc.gameSettings.clouds) {
                    mc.gameSettings.clouds = false; changed = true;
                } else if (mc.gameSettings.particleSetting < 2) {
                    mc.gameSettings.particleSetting++; changed = true;
                } else if (mc.gameSettings.renderDistanceChunks > minRender) {
                    mc.gameSettings.renderDistanceChunks--; changed = true;
                } else if (mc.gameSettings.ambientOcclusion > 0) {
                    mc.gameSettings.ambientOcclusion = 0; changed = true;
                }
                if (changed) {
                    mc.renderGlobal.loadRenderers();
                    lastAdjust = now;
                    lowSeconds = 0;
                }
            } else if (fps > fpsHigh && now - lastAdjust > 30000 && level == 1) {
                boolean changed = false;
                if (mc.gameSettings.ambientOcclusion == 0 && origAO > 0) {
                    mc.gameSettings.ambientOcclusion = origAO; changed = true;
                } else if (mc.gameSettings.renderDistanceChunks < origRender) {
                    mc.gameSettings.renderDistanceChunks++; changed = true;
                } else if (mc.gameSettings.particleSetting > origParticles) {
                    mc.gameSettings.particleSetting--; changed = true;
                } else if (!mc.gameSettings.clouds && origClouds) {
                    mc.gameSettings.clouds = true; changed = true;
                } else if (!mc.gameSettings.fancyGraphics && origFancy) {
                    mc.gameSettings.fancyGraphics = true; changed = true;
                }
                if (changed) {
                    mc.renderGlobal.loadRenderers();
                    lastAdjust = now;
                }
            }
        }

        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Text e) {
            if (!metrics) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || !mc.gameSettings.showDebugInfo) return;
            e.left.add("");
            e.left.add("\u00A7b[OTM]\u00A7r nivel \u00A7e" + level
                + "\u00A7r | fps " + fps
                + " | render " + mc.gameSettings.renderDistanceChunks
                + " | mspt " + String.format("%.1f", msptAvg));
            if (level > 0) {
                e.left.add("\u00A7b[OTM]\u00A7r skip " + skippedPerSec
                    + " | cull " + culledPerSec
                    + " | TE " + tilesPaused
                    + " | mobs " + mobsDespawned
                    + " | itens " + itemsMerged
                    + " | xp " + xpMerged);
                e.left.add("\u00A7b[OTM]\u00A7r cap " + itemsCapped
                    + " | path " + pathsCleared);
            }
        }
    }

    // ================== COMANDO ==================
    public static class CmdOtm extends CommandBase {
        @Override public String getCommandName() { return "otm"; }
        @Override public String getCommandUsage(ICommandSender s) {
            return "/otm <0|1|2|3|4|5>";
        }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }

        @Override
        public void processCommand(ICommandSender s, String[] a) {
            if (a.length == 0) {
                s.addChatMessage(new ChatComponentText("\u00A7bOTM\u00A7r nivel atual: \u00A7e" + level));
                s.addChatMessage(new ChatComponentText(getCommandUsage(s)));
                return;
            }
            int lvl;
            try { lvl = Integer.parseInt(a[0].trim()); }
            catch (NumberFormatException ex) {
                s.addChatMessage(new ChatComponentText("\u00A7cUso: " + getCommandUsage(s)));
                return;
            }
            if (lvl < 0 || lvl > 5) {
                s.addChatMessage(new ChatComponentText("\u00A7cUse 0, 1, 2, 3, 4 ou 5."));
                return;
            }
            applyLevel(lvl);

            if (level == 0) {
                s.addChatMessage(new ChatComponentText("\u00A7bOTM\u00A7r otimizacao \u00A7cDESLIGADA\u00A7r."));
            } else {
                String nome;
                if (level == 1) nome = "LEVE";
                else if (level == 2) nome = "MEDIO";
                else if (level == 3) nome = "EXTREMO";
                else if (level == 4) nome = "INSANO";
                else nome = "HARDCORE";

                s.addChatMessage(new ChatComponentText("\u00A7bOTM\u00A7r nivel \u00A7e" + level + " (" + nome + ")\u00A7r aplicado."));

                if (level >= 2) {
                    s.addChatMessage(new ChatComponentText(
                        "\u00A7bOTM\u00A7r render " + forcedRenderDist
                        + " | mobs renderizam em " + cullDist + "b"
                        + " | mobs despawn " + mobDespawnDist + "b"
                        + " | TE " + tileDist + "b"));
                }
            }
        }
    }
}
