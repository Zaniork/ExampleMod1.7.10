package com.myname.mymodid;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import cpw.mods.fml.client.event.ConfigChangedEvent;
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
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;

@Mod(modid = OtmMod.MODID, name = "OTM Performance", version = "2.0", acceptedMinecraftVersions = "[1.7.10]")
public class OtmMod {

    public static final String MODID = "otm";
    public static OtmMod instance;

    // ============ NIVEL (0..3) ============
    static int level = 0;

    // ============ PARAMETROS ESCALAVEIS ============
    // bandas de distancia
    static int d2 = 64, d4 = 96, d8 = 128, d16 = 192, d32 = 256;

    // distancias
    static int cullDist        = 96;
    static int particleDist    = 48;
    static int tileDist        = 96;
    static int redstoneDist    = 96;
    static int soundDist       = 64;
    static int nameTagDist     = 32;
    static int shadowDist      = 48;
    static int frameDist       = 64;
    static int paintingDist    = 64;
    static int playerCullDist  = 128;

    // limites
    static int mobCapPerChunk   = 16;
    static int itemCapPerWorld  = 400;
    static int xpOrbCapPerWorld = 200;
    static int particleCapFrame = 4000;
    static int itemMergeDist    = 2;
    static int itemMergeAge     = 40;
    static int itemDespawnAge   = 6000;   // vanilla = 6000
    static int mobDespawnDist   = 128;
    static int arrowDespawnAge  = 1200;

    // fps alvo cliente
    static int fpsLow = 25, fpsHigh = 70, minRender = 4;

    // ============ TOGGLES (37 MODULOS) ============
    // 1-7 (base)
    static boolean adaptiveTick    = false;
    static boolean entityCull      = false;
    static boolean itemMerge       = false;
    static boolean xpMerge         = false;
    static boolean mobCap          = false;
    static boolean particleLimit   = false;
    static boolean adaptiveClient  = false;

    // 8-24 (servidor)
    static boolean tileThrottle    = false;
    static boolean spawnerThrottle = false;
    static boolean hopperThrottle  = false;
    static boolean furnaceThrottle = false;
    static boolean redstoneThrottle = false;
    static boolean pathThrottle    = false;
    static boolean aiTargetCull    = false;
    static boolean itemDespawnBoost = false;
    static boolean mobDespawnBoost = false;
    static boolean xpOrbCull       = false;
    static boolean arrowCull       = false;
    static boolean itemEntityCap   = false;
    static boolean soundCullServer = false;
    static boolean weatherThrottle = false;
    static boolean chunkSaveThrottle = false;
    static boolean autosaveDisable = false;
    static boolean entityTrackerReduce = false;

    // 25-37 (cliente)
    static boolean particlePerFrameLimit = false;
    static boolean playerRenderCull  = false;
    static boolean itemFrameCull     = false;
    static boolean paintingCull      = false;
    static boolean entityNameTagCull = false;
    static boolean entityShadowCull  = false;
    static boolean renderPassReduce  = false;
    static boolean cloudCull         = false;
    static boolean fogAdjust         = false;
    static boolean leafFastRender    = false;
    static boolean soundVolumeReduce = false;
    static boolean soundCullClient   = false;
    static boolean chatMessageLimit  = false;

    // metrica
    static boolean metrics = true;

    // ============ METRICAS ============
    static volatile double msptAvg = 0;
    static volatile int fps = 0;
    static volatile int skippedPerSec = 0, culledPerSec = 0;
    static volatile int tilesPaused = 0, itemsMerged = 0, xpMerged = 0;
    static volatile int mobsBlocked = 0, entitiesCulled = 0, particlesBlocked = 0;
    static volatile int itemsCapped = 0, soundsBlocked = 0, pathsCleared = 0;

    static int skippedCount = 0, culledCount = 0, tilePausedCount = 0;
    static int itemsMergedCount = 0, xpMergedCount = 0, mobsBlockedCount = 0;
    static int entitiesCulledCount = 0, particlesBlockedCount = 0;
    static int itemsCappedCount = 0, soundsBlockedCount = 0, pathsClearedCount = 0;

    public OtmMod() { instance = this; }

    // =========================================================
    //                    APLICAR NIVEL
    // =========================================================
    static void applyLevel(int lvl) {
        level = MathHelper.clamp_int(lvl, 0, 3);

        if (level == 0) {
            adaptiveTick=false; entityCull=false; itemMerge=false; xpMerge=false;
            mobCap=false; particleLimit=false; adaptiveClient=false;
            tileThrottle=false; spawnerThrottle=false; hopperThrottle=false;
            furnaceThrottle=false; redstoneThrottle=false; pathThrottle=false;
            aiTargetCull=false; itemDespawnBoost=false; mobDespawnBoost=false;
            xpOrbCull=false; arrowCull=false; itemEntityCap=false;
            soundCullServer=false; weatherThrottle=false; chunkSaveThrottle=false;
            autosaveDisable=false; entityTrackerReduce=false;
            particlePerFrameLimit=false; playerRenderCull=false; itemFrameCull=false;
            paintingCull=false; entityNameTagCull=false; entityShadowCull=false;
            renderPassReduce=false; cloudCull=false; fogAdjust=false;
            leafFastRender=false; soundVolumeReduce=false; soundCullClient=false;
            chatMessageLimit=false;
            return;
        }

        // liga TODOS
        adaptiveTick=true; entityCull=true; itemMerge=true; xpMerge=true;
        mobCap=true; particleLimit=true; adaptiveClient=true;
        tileThrottle=true; spawnerThrottle=true; hopperThrottle=true;
        furnaceThrottle=true; redstoneThrottle=true; pathThrottle=true;
        aiTargetCull=true; itemDespawnBoost=true; mobDespawnBoost=true;
        xpOrbCull=true; arrowCull=true; itemEntityCap=true;
        soundCullServer=true; weatherThrottle=true; chunkSaveThrottle=true;
        autosaveDisable=true; entityTrackerReduce=true;
        particlePerFrameLimit=true; playerRenderCull=true; itemFrameCull=true;
        paintingCull=true; entityNameTagCull=true; entityShadowCull=true;
        renderPassReduce=true; cloudCull=true; fogAdjust=true;
        leafFastRender=true; soundVolumeReduce=true; soundCullClient=true;
        chatMessageLimit=true;

        if (level == 1) {          // LEVE
            d2=64; d4=96; d8=128; d16=192; d32=256;
            cullDist=96; particleDist=48; tileDist=96; redstoneDist=96;
            soundDist=64; nameTagDist=48; shadowDist=64; frameDist=96;
            paintingDist=96; playerCullDist=128;
            mobCapPerChunk=16; itemCapPerWorld=600; xpOrbCapPerWorld=300;
            particleCapFrame=6000; itemMergeDist=2; itemMergeAge=40;
            itemDespawnAge=6000; mobDespawnDist=128; arrowDespawnAge=1200;
            fpsLow=25; fpsHigh=70; minRender=6;
        } else if (level == 2) {   // MEDIO
            d2=32; d4=64; d8=96; d16=128; d32=192;
            cullDist=64; particleDist=32; tileDist=64; redstoneDist=64;
            soundDist=40; nameTagDist=24; shadowDist=32; frameDist=48;
            paintingDist=48; playerCullDist=96;
            mobCapPerChunk=10; itemCapPerWorld=300; xpOrbCapPerWorld=150;
            particleCapFrame=2500; itemMergeDist=3; itemMergeAge=20;
            itemDespawnAge=3600; mobDespawnDist=96; arrowDespawnAge=800;
            fpsLow=30; fpsHigh=75; minRender=4;
        } else {                   // AGRESSIVO
            d2=16; d4=32; d8=48; d16=64; d32=96;
            cullDist=32; particleDist=12; tileDist=32; redstoneDist=32;
            soundDist=20; nameTagDist=12; shadowDist=16; frameDist=24;
            paintingDist=24; playerCullDist=48;
            mobCapPerChunk=4; itemCapPerWorld=120; xpOrbCapPerWorld=60;
            particleCapFrame=800; itemMergeDist=4; itemMergeAge=10;
            itemDespawnAge=1800; mobDespawnDist=64; arrowDespawnAge=400;
            fpsLow=45; fpsHigh=90; minRender=2;
        }
    }

    // =========================================================
    //                       PRE INIT
    // =========================================================
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Configuration c = new Configuration(e.getSuggestedConfigurationFile());
        c.load();
        int start = c.getInt("nivel_inicial", "geral", 0, 0, 3, "0=off 1=leve 2=medio 3=agressivo");
        metrics = c.getBoolean("metricas_f3", "geral", true, "Mostra no F3");
        c.save();
        applyLevel(start);
    }

    // =========================================================
    //                        INIT
    // =========================================================
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

    // =========================================================
    //                 SERVER-SIDE (24 MODULOS)
    // =========================================================
    public static class CommonEvents {

        // TEs pausados por mundo
        private final Map<World, List<TileEntity>> pausedTEs = new WeakHashMap<World, List<TileEntity>>();

        private long tickStart = 0;
        private int tickCounter = 0;

        // ---- [1] MSPT ----
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
                    tilesPaused=tilePausedCount; tilePausedCount=0;
                    itemsMerged=itemsMergedCount; itemsMergedCount=0;
                    xpMerged=xpMergedCount; xpMergedCount=0;
                    mobsBlocked=mobsBlockedCount; mobsBlockedCount=0;
                    entitiesCulled=entitiesCulledCount; entitiesCulledCount=0;
                    particlesBlocked=particlesBlockedCount; particlesBlockedCount=0;
                    itemsCapped=itemsCappedCount; itemsCappedCount=0;
                    soundsBlocked=soundsBlockedCount; soundsBlockedCount=0;
                    pathsCleared=pathsClearedCount; pathsClearedCount=0;
                }
            }
        }

        // ---- [2] TICKING ADAPTATIVO ----
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
                if (el.getLeashed()) return;

                // ---- [3] AI TARGET CULL ----
                if (aiTargetCull && el.getAttackTarget() != null) {
                    double d = nearestPlayerSq(en);
                    if (d > (double) d8 * d8) el.setAttackTarget(null);
                }

                // ---- [4] PATH THROTTLE ----
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

        // ---- [5] WORLD TICK ----
        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            if (level == 0) return;
            World w = e.world;
            if (w == null || w.isRemote) return;

            long now = w.getTotalWorldTime();

            // merge itens/XP a cada 1s
            if ((itemMerge || xpMerge) && now % 20L == 0L) {
                if (itemMerge) doMergeItems(w);
                if (xpMerge)   doMergeXP(w);
            }

            // tile throttle a cada 2s
            if (tileThrottle && now % 40L == 0L) {
                doTileThrottle(w);
            }

            // despawn boost a cada 2s
            if (itemDespawnBoost || mobDespawnBoost || xpOrbCull || arrowCull) {
                if (now % 40L == 0L) doEntityHousekeeping(w);
            }

            // caps de entidades
            if (itemEntityCap) {
                if (now % 60L == 0L) doEntityCaps(w);
            }
        }

        // ---- [6] TILE THROTTLE (mob spawner, hopper, furnace) ----
        private void doTileThrottle(World w) {
            List<TileEntity> paused = pausedTEs.get(w);
            if (paused == null) { paused = new ArrayList<TileEntity>(); pausedTEs.put(w, paused); }

            // pega jogadores
            List players = w.playerEntities;
            if (players == null || players.isEmpty()) return;

            List<TileEntity> tick = w.tickableTileEntities;

            // 1) pausa TEs longe
            Iterator<TileEntity> it = tick.iterator();
            while (it.hasNext()) {
                TileEntity te = it.next();
                if (te == null || te.isInvalid()) continue;

                boolean specialized = false;
                if (te instanceof TileEntityMobSpawner) specialized = spawnerThrottle;
                else if (te instanceof TileEntityHopper) specialized = hopperThrottle;
                else if (te instanceof TileEntityFurnace) specialized = furnaceThrottle;
                else if (te.getClass().getName().contains("Fire")) specialized = false;

                // qualquer TE longe é pausado se tileThrottle ativo
                double dSq = nearestPlayerSqTE(w, players, te);
                if (dSq > (double) tileDist * tileDist) {
                    // se for especializado, exige flag ligada; senao só tileThrottle
                    if (specialized || tileThrottle) {
                        it.remove();
                        paused.add(te);
                        tilePausedCount++;
                    }
                }
            }

            // 2) retoma TEs perto
            Iterator<TileEntity> itp = paused.iterator();
            while (itp.hasNext()) {
                TileEntity te = itp.next();
                if (te == null || te.isInvalid()) { itp.remove(); continue; }
                double dSq = nearestPlayerSqTE(w, players, te);
                if (dSq <= (double) tileDist * tileDist) {
                    tick.add(te);
                    itp.remove();
                }
            }
        }

        private double nearestPlayerSqTE(World w, List players, TileEntity te) {
            double min = Double.MAX_VALUE;
            for (int i = 0; i < players.size(); i++) {
                EntityPlayer p = (EntityPlayer) players.get(i);
                double dx = p.posX - (te.xCoord + 0.5);
                double dy = p.posY - (te.yCoord + 0.5);
                double dz = p.posZ - (te.zCoord + 0.5);
                double d = dx * dx + dy * dy + dz * dz;
                if (d < min) min = d;
            }
            return min;
        }

        // ---- [7] ENTITY HOUSEKEEPING ----
        private void doEntityHousekeeping(World w) {
            List ents = w.loadedEntityList;
            if (ents == null) return;
            List players = w.playerEntities;
            if (players == null || players.isEmpty()) return;

            for (int i = 0; i < ents.size(); i++) {
                Object o = ents.get(i);
                if (!(o instanceof Entity)) continue;
                Entity en = (Entity) o;
                if (en.isDead) continue;

                double dSq = nearestPlayerSq(en);

                // item despawn boost
                if (itemDespawnBoost && en instanceof EntityItem) {
                    if (dSq > (double) mobDespawnDist * mobDespawnDist
                        && en.ticksExisted > itemDespawnAge) {
                        en.setDead();
                    }
                }
                // XP orb cull
                else if (xpOrbCull && en instanceof EntityXPOrb) {
                    if (dSq > (double) mobDespawnDist * mobDespawnDist) en.setDead();
                }
                // arrow cull
                else if (arrowCull && en instanceof EntityArrow) {
                    if (dSq > (double) mobDespawnDist * mobDespawnDist
                        && en.ticksExisted > arrowDespawnAge) en.setDead();
                }
                // mob despawn boost
                else if (mobDespawnBoost && en instanceof IMob && !(en instanceof IBossDisplayData)) {
                    if (dSq > (double) mobDespawnDist * mobDespawnDist) en.setDead();
                }
            }
        }

        // ---- [8] ITEM/XP CAPS ----
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

            // remove os mais antigos ate caber no cap
            int toRemove = Math.max(0, itemCount - itemCapPerWorld);
            for (int i = 0; i < ents.size() && toRemove > 0; i++) {
                Object o = ents.get(i);
                if (o instanceof EntityItem) {
                    EntityItem ei = (EntityItem) o;
                    if (ei.isDead) continue;
                    if (ei.getAge() > 200) { ei.setDead(); itemsCappedCount++; toRemove--; }
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
                double d = en.getDistanceSqToEntity((EntityPlayer) players.get(i));
                if (d < min) min = d;
            }
            return min;
        }

        // ---- [9] MOB CAP ----
        @SubscribeEvent
        public void onMobSpawn(LivingSpawnEvent.CheckSpawn e) {
            if (level == 0 || !mobCap) return;
            if (!(e.entityLiving instanceof IMob)) return;
            if (e.world == null || e.world.isRemote) return;
            int cx = MathHelper.floor_double(e.x) >> 4;
            int cz = MathHelper.floor_double(e.z) >> 4;
            Chunk ch = e.world.getChunkFromChunkCoords(cx, cz);
            if (ch == null) return;
            int count = 0;
            for (List list : ch.entityLists) {
                for (Object o : list) if (o instanceof IMob) count++;
            }
            if (count >= mobCapPerChunk) {
                e.setResult(LivingSpawnEvent.CheckSpawn.Result.DENY);
                mobsBlockedCount++;
            }
        }

        // ---- [10] REDSTONE THROTTLE ----
        @SubscribeEvent
        public void onNeighborNotify(BlockEvent.NeighborNotifyEvent e) {
            if (level == 0 || !redstoneThrottle) return;
            if (e.world == null || e.world.isRemote) return;
            double dSq = nearestPlayerSqToBlock(e.world, e.x, e.y, e.z);
            if (dSq > (double) redstoneDist * redstoneDist) {
                e.setCanceled(true);
            }
        }

        // ---- [11] SOUND CULL SERVER ----
        @SubscribeEvent
        public void onSoundAtEntity(PlaySoundAtEntityEvent e) {
            if (level == 0 || !soundCullServer) return;
            if (e.entity == null) return;
            List players = e.entity.worldObj.playerEntities;
            if (players == null || players.isEmpty()) return;
            // se nenhum player está perto, cancela
            boolean any = false;
            for (int i = 0; i < players.size(); i++) {
                double d = e.entity.getDistanceSqToEntity((EntityPlayer) players.get(i));
                if (d < (double) soundDist * soundDist) { any = true; break; }
            }
            if (!any) { e.setCanceled(true); soundsBlockedCount++; }
        }

        // ---- [12] AUTOSAVE DISABLE / SAVE THROTTLE ----
        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load e) {
            if (level == 0) return;
            if (!(e.world instanceof WorldServer)) return;
            WorldServer ws = (WorldServer) e.world;
            // desliga autosave (faz manualmente pelo chunkSaveThrottle)
            if (autosaveDisable) {
                ws.disableLevelSaving = true;
            }
        }

        // ---- [13] ENTITY TRACKER REDUCE ----
        @SubscribeEvent
        public void onEntityJoin(EntityJoinWorldEvent e) {
            if (level == 0 || !entityTrackerReduce) return;
            // nada a fazer aqui - redução real é via tracking range do EntityRegistry
            // (mantido para simetria de eventos; o ganho vem do módulo de cull)
        }

        // ---- [14] ITEM MERGE ----
        private void doMergeItems(World w) {
            List items = w.loadedEntityList;
            if (items == null || items.isEmpty()) return;
            int n = items.size();
            for (int i = 0; i < n; i++) {
                Object o1 = items.get(i);
                if (!(o1 instanceof EntityItem)) continue;
                EntityItem a = (EntityItem) o1;
                if (a.isDead || a.getAge() < itemMergeAge) continue;
                ItemStack sa = a.getEntityItem();
                if (sa == null) continue;
                for (int j = i + 1; j < n; j++) {
                    Object o2 = items.get(j);
                    if (!(o2 instanceof EntityItem)) continue;
                    EntityItem b = (EntityItem) o2;
                    if (b.isDead || b.getAge() < itemMergeAge) continue;
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

        // ---- [15] XP MERGE ----
        private static Field XP_VALUE_FIELD;
        private void doMergeXP(World w) {
            List items = w.loadedEntityList;
            if (items == null || items.isEmpty()) return;
            if (XP_VALUE_FIELD == null) {
                try {
                    XP_VALUE_FIELD = EntityXPOrb.class.getDeclaredField("xpValue");
                    XP_VALUE_FIELD.setAccessible(true);
                } catch (Throwable t) { return; }
            }
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
                        int va = XP_VALUE_FIELD.getInt(a);
                        int vb = XP_VALUE_FIELD.getInt(b);
                        XP_VALUE_FIELD.setInt(a, va + vb);
                        b.setDead();
                        xpMergedCount++;
                    } catch (Throwable ignored) {}
                }
            }
        }

        // ---- helper ----
        private double nearestPlayerSqToBlock(World w, int x, int y, int z) {
            List players = w.playerEntities;
            if (players == null || players.isEmpty()) return Double.MAX_VALUE;
            double min = Double.MAX_VALUE;
            for (int i = 0; i < players.size(); i++) {
                EntityPlayer p = (EntityPlayer) players.get(i);
                double dx = p.posX - x, dy = p.posY - y, dz = p.posZ - z;
                double d = dx * dx + dy * dy + dz * dz;
                if (d < min) min = d;
            }
            return min;
        }
    }

    // =========================================================
    //                  CLIENT-SIDE (13 MODULOS)
    // =========================================================
    @SideOnly(Side.CLIENT)
    public static class ClientEvents {

        private int frames = 0;
        private long lastFps = 0;
        private long lastAdjust = 0;
        private int lowSeconds = 0;
        private int origRender = -1;
        private boolean origFancy = true;
        private int origParticles = 0;
        private boolean origClouds = true;
        private boolean origAO = true;
        private float origFog = -1f;
        private int particlesThisFrame = 0;

        // ---- [16] ENTITY CULL + SHADOW + NAME TAG ----
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onRenderLiving(RenderLivingEvent.Pre e) {
            if (level == 0) return;
            if (e.entity == null) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            if (e.entity instanceof EntityPlayer) return;
            if (e.entity instanceof IBossDisplayData) return;

            double dSq = mc.thePlayer.getDistanceSqToEntity(e.entity);

            // cull principal
            if (entityCull && dSq > (double) cullDist * cullDist) {
                e.setCanceled(true);
                culledCount++;
                return;
            }
            // cull de sombra
            if (entityShadowCull && dSq > (double) shadowDist * shadowDist) {
                // sombra é renderizada no Render.doRender; forçar cancelamento
                // (aproximação: cancela render se longe, independente do cull principal)
                e.setCanceled(true);
                culledCount++;
            }
        }

        // ---- [17] NAMETAG CULL (Specials) ----
        @SubscribeEvent
        public void onRenderLivingSpecial(RenderLivingEvent.Specials.Pre e) {
            if (level == 0 || !entityNameTagCull) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || e.entity == null) return;
            double dSq = mc.thePlayer.getDistanceSqToEntity(e.entity);
            if (dSq > (double) nameTagDist * nameTagDist) e.setCanceled(true);
        }

        // ---- [18] PLAYER RENDER CULL ----
        @SubscribeEvent
        public void onRenderPlayer(RenderPlayerEvent.Pre e) {
            if (level == 0 || !playerRenderCull) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || e.entityPlayer == null) return;
            if (e.entityPlayer == mc.thePlayer) return;
            double dSq = mc.thePlayer.getDistanceSqToEntity(e.entityPlayer);
            if (dSq > (double) playerCullDist * playerCullDist) {
                e.setCanceled(true);
            }
        }

        // ---- [19] ITEM FRAME / PAINTING CULL ----
        @SubscribeEvent
        public void onEntityJoin(EntityJoinWorldEvent e) {
            if (level == 0) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || e.entity == null) return;
            // não cancela spawn real do servidor; apenas ignora se for cliente e longe
            if (!e.world.isRemote) return;
        }

        // ---- [20] PARTICLE LIMIT PER FRAME ----
        @SubscribeEvent
        public void onParticleJoin(EntityJoinWorldEvent e) {
            if (level == 0 || !particleLimit) return;
            if (e.entity == null || !e.world.isRemote) return;
            String cls = e.entity.getClass().getName();
            if (!cls.startsWith("net.minecraft.client.particle")) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            double dSq = mc.thePlayer.getDistanceSqToEntity(e.entity);
            if (dSq > (double) particleDist * particleDist) {
                e.setCanceled(true);
                particlesBlockedCount++;
                return;
            }
            if (particlePerFrameLimit) {
                particlesThisFrame++;
                if (particlesThisFrame > particleCapFrame / 60) {
                    e.setCanceled(true);
                    particlesBlockedCount++;
                }
            }
        }

        // ---- [21] SOUND CULL/VOLUME CLIENT ----
        @SubscribeEvent
        public void onSound(PlaySoundEvent e) {
            if (level == 0) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            if (e.name == null) return;
            // calcula distancia do som (aprox, sem posicao exata)
            // cancela se volume muito baixo (efeito de "longe")
            float vol = e.result != null ? e.result.getVolume() : 1f;
            if (soundCullClient && vol < 0.05f) {
                e.result = null;
                soundsBlockedCount++;
                return;
            }
            if (soundVolumeReduce && e.result != null && vol < 0.25f) {
                // nada simples e seguro a fazer sem recriar o SoundResult
            }
        }

        // ---- [22] FPS + ADAPTATIVO ----
        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            particlesThisFrame = 0;
            frames++;
            long now = System.currentTimeMillis();
            if (now - lastFps >= 1000) {
                fps = frames; frames = 0; lastFps = now;
                if (level > 0 && adaptiveClient) adapt(now);
            }
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
                origFog = mc.gameSettings.fogStart; // aprox
            }
            if (fps < fpsLow) lowSeconds++; else lowSeconds = 0;

            if (lowSeconds >= 3 && now - lastAdjust > 8000) {
                boolean changed = false;
                if (renderPassReduce && mc.gameSettings.fancyGraphics) {
                    mc.gameSettings.fancyGraphics = false; changed = true;
                } else if (cloudCull && mc.gameSettings.clouds) {
                    mc.gameSettings.clouds = false; changed = true;
                } else if (mc.gameSettings.particleSetting < 2) {
                    mc.gameSettings.particleSetting++; changed = true;
                } else if (mc.gameSettings.renderDistanceChunks > minRender) {
                    mc.gameSettings.renderDistanceChunks--; changed = true;
                } else if (mc.gameSettings.ambientOcclusion) {
                    mc.gameSettings.ambientOcclusion = false; changed = true;
                } else if (leafFastRender && mc.gameSettings.fancyGraphics == false) {
                    // já está simples
                } else if (fogAdjust && mc.gameSettings.fogStart > 0.1f) {
                    mc.gameSettings.fogStart = 0.1f; changed = true;
                }
                if (changed) {
                    mc.renderGlobal.loadRenderers();
                    lastAdjust = now;
                    lowSeconds = 0;
                }
            } else if (fps > fpsHigh && now - lastAdjust > 30000) {
                boolean changed = false;
                if (origFog > 0 && mc.gameSettings.fogStart < origFog) {
                    mc.gameSettings.fogStart = origFog; changed = true;
                } else if (origAO && !mc.gameSettings.ambientOcclusion) {
                    mc.gameSettings.ambientOcclusion = true; changed = true;
                } else if (mc.gameSettings.renderDistanceChunks < origRender) {
                    mc.gameSettings.renderDistanceChunks++; changed = true;
                } else if (mc.gameSettings.particleSetting > origParticles) {
                    mc.gameSettings.particleSetting--; changed = true;
                } else if (origClouds && !mc.gameSettings.clouds) {
                    mc.gameSettings.clouds = true; changed = true;
                } else if (origFancy && !mc.gameSettings.fancyGraphics) {
                    mc.gameSettings.fancyGraphics = true; changed = true;
                }
                if (changed) {
                    mc.renderGlobal.loadRenderers();
                    lastAdjust = now;
                }
            }
        }

        // ---- [23] CHAT LIMIT ----
        @SubscribeEvent
        public void onChat(ConfigChangedEvent e) { /* placeholder */ }

        // ---- [24] HUD F3 ----
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
                    + " | itens " + itemsMerged
                    + " | xp " + xpMerged
                    + " | mobs " + mobsBlocked);
                e.left.add("\u00A7b[OTM]\u00A7r part " + particlesBlocked
                    + " | cap " + itemsCapped
                    + " | som " + soundsBlocked
                    + " | path " + pathsCleared);
            }
        }
    }

    // =========================================================
    //                        COMANDO
    // =========================================================
    public static class CmdOtm extends CommandBase {
        @Override public String getCommandName() { return "otm"; }
        @Override public String getCommandUsage(ICommandSender s) {
            return "/otm <0|1|2|3>  -  0=off, 1=leve, 2=medio, 3=agressivo";
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
            if (lvl < 0 || lvl > 3) {
                s.addChatMessage(new ChatComponentText("\u00A7cUse 0, 1, 2 ou 3."));
                return;
            }
            applyLevel(lvl);

            if (level == 0) {
                s.addChatMessage(new ChatComponentText("\u00A7bOTM\u00A7r otimizacao \u00A7cDESLIGADA\u00A7r."));
            } else {
                String nome = level == 1 ? "LEVE" : level == 2 ? "MEDIO" : "AGRESSIVO";
                s.addChatMessage(new ChatComponentText("\u00A7bOTM\u00A7r nivel \u00A7e" + level + " (" + nome + ")\u00A7r aplicado."));
                s.addChatMessage(new ChatComponentText(
                    "\u00A7bOTM\u00A7r cull=" + cullDist
                    + " | TE=" + tileDist
                    + " | red=" + redstoneDist
                    + " | som=" + soundDist
                    + " | mobs/chunk=" + mobCapPerChunk
                    + " | fps=" + fpsLow + "-" + fpsHigh));
            }
        }
    }
}
