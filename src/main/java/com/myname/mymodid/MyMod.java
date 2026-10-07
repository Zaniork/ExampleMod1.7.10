package com.myname.mymodid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

@Mod(modid = MyMod.MODID, version = Tags.VERSION, name = "MyMod", acceptedMinecraftVersions = "[1.7.10]")
public class MyMod {

    public static final String MODID = "mymodid";
    public static final Logger LOG = LogManager.getLogger(MODID);

    // ================= AJUSTES (pode editar os números) =================
    // índices dos arrays = nível do Governador: 0 liso, 1 leve, 2 pesado, 3 emergência
    static final int FPS_NORMAL = 40;
    static final int FPS_EMERGENCY = 30;
    static final int BASE_CHUNKS = 5;                             // teto da distância de renderização
    static final double[] FREEZE_RADIUS = {48, 48, 24, 16};       // congela mobs vanilla além disso
    static final int[] AI_INTERVAL = {2, 3, 4, 4};                // IA dos mobs entre 24 blocos e o raio acima
    static final int[] CLEAN_INTERVAL = {600, 400, 200, 100};     // ticks entre limpezas de itens
    static final int ENTITY_CAP = 80;                             // nível 3: máximo de mobs vanilla
    static final double[] MOB_RENDER_DIST = {16, 16, 16, 8};      // D4: desenho de mobs
    static final double[] TILE_DIST_SQ = {256, 256, 256, 64};     // D4: tile entities (16 e 8 blocos ao quadrado)
    static final int[] SPAWN_EVERY = {2, 2, 4, 4};                // busca de spawn a cada N ticks (precisa dividir 400)
    static final int CROWD_LIMIT = 8;                             // 41: mobs por quadrado de 3x3 blocos
    static final double BUDGET_MS = 35.0;                         // Ω2: orçamento de tempo por tick
    static final int BURST_THRESHOLD = 16;                        // rajada: chunks carregados por segundo
    static final int BURST_TICKS = 200;                           // rajada: duração (10 s)
    static final double BURST_RADIUS = 24.0;                      // rajada: congela mobs vanilla além disso
    static final double MEM_HIGH = 0.80;                          // Ω3: memória alta
    static final double MEM_CRIT = 0.90;                          // Ω3: memória crítica

    // ================= ESTADO COMPARTILHADO =================
    static volatile int level = 0;
    static volatile double serverMs = 0;
    static volatile double frameMs = 0;
    static volatile int burstTicks = 0;
    static volatile int chunkLoads = 0;
    static volatile long tickStartNs = 0;
    static int perfLevel = 0;
    static int memFloor = 0;
    static int gcCountdown = -1;

    private static final Map<Integer, double[]> PLAYER_POS = new HashMap<Integer, double[]>();
    private static final Map<Integer, HashMap<Long, int[]>> DENSITY = new HashMap<Integer, HashMap<Long, int[]>>();

    @SidedProxy(clientSide = "com.myname.mymodid.ClientProxy", serverSide = "com.myname.mymodid.CommonProxy")
    public static CommonProxy proxy;

    // ================= PROTEÇÃO: nunca mexe em entidades de outros mods =================
    private static final Map<Class<?>, Boolean> VANILLA_CLASS = new ConcurrentHashMap<Class<?>, Boolean>();

    static boolean isVanillaClass(Class<?> c) {
        Boolean b = VANILLA_CLASS.get(c);
        if (b == null) {
            b = Boolean.valueOf(c.getName().startsWith("net.minecraft."));
            VANILLA_CLASS.put(c, b);
        }
        return b.booleanValue();
    }

    static boolean isProtected(Entity ent) {
        if (!(ent instanceof EntityLiving)) return true;          // jogadores e outros
        if (!isVanillaClass(ent.getClass())) return true;         // mobs/personagens de mods (DBC etc.)
        EntityLiving el = (EntityLiving) ent;
        if (el.hasCustomNameTag()) return true;
        if (el.getLeashed()) return true;
        if (el.riddenByEntity != null || el.ridingEntity != null) return true;
        if (el instanceof IBossDisplayData || el instanceof EntityVillager) return true;
        if (el instanceof EntityTameable && ((EntityTameable) el).isTamed()) return true;
        if (el instanceof EntityHorse && ((EntityHorse) el).isTame()) return true;
        return false;
    }

    static boolean isVanillaItem(EntityItem ei) {
        if (!isVanillaClass(ei.getClass())) return false;
        ItemStack st = ei.getEntityItem();
        if (st == null || st.getItem() == null) return false;
        String name = Item.itemRegistry.getNameForObject(st.getItem());
        return name != null && name.startsWith("minecraft:");
    }

    // ================= 40: posição dos jogadores em cache =================
    static double nearestPlayerSq(World w, Entity en) {
        double[] buf = PLAYER_POS.get(w.provider.dimensionId);
        if (buf == null || buf.length == 0) return -1;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < buf.length; i += 3) {
            double dx = en.posX - buf[i];
            double dy = en.posY - buf[i + 1];
            double dz = en.posZ - buf[i + 2];
            double d = dx * dx + dy * dy + dz * dz;
            if (d < best) best = d;
        }
        return best;
    }

    static long cellKey(Entity en) {
        long cx = (long) Math.floor(en.posX / 3.0);
        long cz = (long) Math.floor(en.posZ / 3.0);
        return (cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    static boolean isCrowded(World w, Entity en) {
        HashMap<Long, int[]> dens = DENSITY.get(w.provider.dimensionId);
        if (dens == null) return false;
        int[] c = dens.get(Long.valueOf(cellKey(en)));
        return c != null && c[0] > CROWD_LIMIT;
    }

    static boolean overBudget() {
        long s = tickStartNs;
        if (s == 0) return false;
        return (System.nanoTime() - s) > (long) (BUDGET_MS * 1.0e6);
    }

    // ================= Ω3: medida de memória (após a última limpeza do Java) =================
    static double memoryRatio() {
        try {
            long max = Runtime.getRuntime().maxMemory();
            for (MemoryPoolMXBean p : ManagementFactory.getMemoryPoolMXBeans()) {
                if (p.getType() != MemoryType.HEAP) continue;
                String n = p.getName();
                if (n.contains("Old") || n.contains("Tenured")) {
                    MemoryUsage u = p.getCollectionUsage();
                    if (u == null) u = p.getUsage();
                    long m = u.getMax() > 0 ? u.getMax() : max;
                    return (double) u.getUsed() / (double) m;
                }
            }
        } catch (Throwable t) {
            // sem acesso: usa a medida simples abaixo
        }
        Runtime rt = Runtime.getRuntime();
        return (double) (rt.totalMemory() - rt.freeMemory()) / (double) rt.maxMemory();
    }

    // ================= CICLO DE VIDA =================
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);

        FMLCommonHandler.instance().bus().register(new WorldCache());
        FMLCommonHandler.instance().bus().register(new Governor());
        FMLCommonHandler.instance().bus().register(new SpawnToggle());
        FMLCommonHandler.instance().bus().register(new ItemCleaner());
        MinecraftForge.TERRAIN_GEN_BUS.register(new NoGrass());
        MinecraftForge.EVENT_BUS.register(new EntityThrottle());
        MinecraftForge.EVENT_BUS.register(new ChunkWatch());
        MinecraftForge.EVENT_BUS.register(new WorldRules());
        if (event.getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new ClientPerf());
            MinecraftForge.EVENT_BUS.register(new MemoryTrim());
            FMLCommonHandler.instance().bus().register(new ClientSettings());
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        System.gc();
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientTileOpt.install();
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        perfLevel = 0;
        memFloor = 0;
        level = 0;
        serverMs = 0;
        burstTicks = 0;
        chunkLoads = 0;
        tickStartNs = System.nanoTime();
        proxy.serverStarting(event);
    }

    // ================= 40 e 41: cache de jogadores e densidade (servidor) =================
    public static class WorldCache {
        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent e) {
            if (e.phase != TickEvent.Phase.START || e.world.isRemote) return;
            World w = e.world;
            Integer dim = Integer.valueOf(w.provider.dimensionId);

            List pl = w.playerEntities;
            int n = pl.size();
            double[] buf = PLAYER_POS.get(dim);
            if (buf == null || buf.length != n * 3) {
                buf = new double[n * 3];
                PLAYER_POS.put(dim, buf);
            }
            for (int i = 0; i < n; i++) {
                EntityPlayer p = (EntityPlayer) pl.get(i);
                buf[i * 3] = p.posX;
                buf[i * 3 + 1] = p.posY;
                buf[i * 3 + 2] = p.posZ;
            }

            if (w.getTotalWorldTime() % 4 == 0) {
                HashMap<Long, int[]> dens = DENSITY.get(dim);
                if (dens == null) {
                    dens = new HashMap<Long, int[]>();
                    DENSITY.put(dim, dens);
                }
                dens.clear();
                List all = w.loadedEntityList;
                for (int i = 0; i < all.size(); i++) {
                    Object o = all.get(i);
                    if (o instanceof EntityLiving && !isProtected((Entity) o)) {
                        Long key = Long.valueOf(cellKey((Entity) o));
                        int[] c = dens.get(key);
                        if (c == null) dens.put(key, new int[] {1});
                        else c[0]++;
                    }
                }
            }
        }
    }

    // ================= 4 e 35: busca de spawn natural em ticks alternados =================
    public static class SpawnToggle {
        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent e) {
            if (e.phase != TickEvent.Phase.START || e.world.isRemote) return;
            World w = e.world;
            long t = w.getTotalWorldTime();
            boolean on = (t % SPAWN_EVERY[level] == 0) || (t % 400 == 0);
            boolean hostile = w.difficultySetting != EnumDifficulty.PEACEFUL;
            w.setAllowedSpawnTypes(on && hostile, on);
        }
    }

    // ================= rajada de chunks: conta carregamentos =================
    public static class ChunkWatch {
        @SubscribeEvent
        public void onChunkLoad(ChunkEvent.Load e) {
            if (!e.world.isRemote) chunkLoads++;
        }
    }

    // ================= limpador de itens (só vanilla) + teto de mobs no nível 3 =================
    public static class ItemCleaner {
        private static final int MIN_AGE = 20 * 10;

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent e) {
            if (e.phase != TickEvent.Phase.END || e.world.isRemote) return;
            int lv = level;
            long t = e.world.getTotalWorldTime();

            if (t % CLEAN_INTERVAL[lv] == 0) {
                for (Object o : new ArrayList<Object>(e.world.loadedEntityList)) {
                    if (o instanceof EntityItem) {
                        EntityItem item = (EntityItem) o;
                        if (item.age > MIN_AGE && isVanillaItem(item)) item.setDead();
                    }
                }
            }
            if (lv >= 3 && t % 100 == 0) capEntities(e.world);
        }
    }

    static class Far {
        final EntityLiving e;
        final double d;

        Far(EntityLiving e, double d) {
            this.e = e;
            this.d = d;
        }
    }

    static void capEntities(World w) {
        ArrayList<Far> list = new ArrayList<Far>();
        for (Object o : new ArrayList<Object>(w.loadedEntityList)) {
            if (o instanceof EntityLiving && !isProtected((Entity) o)) {
                EntityLiving el = (EntityLiving) o;
                double d = nearestPlayerSq(w, el);
                if (d < 0) return;
                list.add(new Far(el, d));
            }
        }
        int excess = list.size() - ENTITY_CAP;
        if (excess <= 0) return;
        Collections.sort(list, new Comparator<Far>() {
            @Override
            public int compare(Far a, Far b) {
                return Double.compare(b.d, a.d);
            }
        });
        for (int i = 0; i < excess; i++) list.get(i).e.setDead();
    }

    // ================= sem grama alta, flores e arbusto seco =================
    public static class NoGrass {
        @SubscribeEvent
        public void onDecorate(DecorateBiomeEvent.Decorate e) {
            switch (e.type) {
                case GRASS:
                case FLOWERS:
                case DEAD_BUSH:
                    e.setResult(Event.Result.DENY);
                    break;
                default:
                    break;
            }
        }
    }

    // ================= 1, 26, 41, Ω2 e rajada: controle de mobs vanilla (servidor) =================
    public static class EntityThrottle {
        @SubscribeEvent
        public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
            EntityLivingBase base = e.entityLiving;
            World w = base.worldObj;
            if (w.isRemote || isProtected(base)) return;

            double d2 = nearestPlayerSq(w, base);
            if (d2 < 0) return;

            int lv = level;
            double freeze = FREEZE_RADIUS[lv];
            if (burstTicks > 0 && freeze > BURST_RADIUS) freeze = BURST_RADIUS;
            if (d2 > freeze * freeze) {
                e.setCanceled(true);
                return;
            }

            long t = w.getTotalWorldTime();
            int id = base.getEntityId();

            if (d2 > 24.0D * 24.0D && (t + id) % AI_INTERVAL[lv] != 0) {
                e.setCanceled(true);
                return;
            }
            if ((t + id) % 2 != 0 && isCrowded(w, base)) {
                e.setCanceled(true);
                return;
            }
            if (d2 > 12.0D * 12.0D && (t + id) % 3 != 0 && overBudget()) {
                e.setCanceled(true);
            }
        }
    }

    // ================= 17: ticks aleatórios mais lentos (3 -> 2) =================
    public static class WorldRules {
        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load e) {
            if (e.world.isRemote || e.world.provider.dimensionId != 0) return;
            GameRules gr = e.world.getGameRules();
            if ("3".equals(gr.getGameRuleStringValue("randomTickSpeed"))) {
                gr.setOrCreateGameRule("randomTickSpeed", "2");
            }
        }
    }

    // ================= Governador Omega: nível por desempenho (Ω2) e por memória (Ω3) =================
    public static class Governor {
        private double avg = 20.0;
        private int sinceEval = 0;
        private int winTicks = 0;
        private int goodTicks = 0;
        private long lastGc = 0;

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                tickStartNs = System.nanoTime();
                return;
            }
            double ms = (System.nanoTime() - tickStartNs) / 1.0e6;
            avg = avg * 0.95 + ms * 0.05;
            serverMs = avg;

            if (burstTicks > 0) burstTicks--;
            if (++winTicks >= 20) {
                winTicks = 0;
                if (chunkLoads >= BURST_THRESHOLD) burstTicks = BURST_TICKS;
                chunkLoads = 0;
            }
            if (++sinceEval >= 100) {
                sinceEval = 0;
                evaluate();
            }
        }

        private void evaluate() {
            double fm = frameMs;
            boolean bad = serverMs > 42.0 || (fm > 0 && fm > 55.0);
            boolean good = serverMs < 30.0 && (fm <= 0 || fm < 38.0);
            if (bad) {
                goodTicks = 0;
                if (perfLevel < 3) perfLevel++;
            } else if (good) {
                goodTicks += 100;
                if (goodTicks >= 900 && perfLevel > 0) {
                    perfLevel--;
                    goodTicks = 0;
                }
            } else {
                goodTicks = 0;
            }

            double ratio = memoryRatio();
            if (ratio > MEM_CRIT) memFloor = 3;
            else if (ratio > MEM_HIGH) {
                if (memFloor < 2) memFloor = 2;
            } else if (ratio < 0.60) memFloor = 0;
            else if (memFloor == 3) memFloor = 2;

            int newLevel = Math.max(perfLevel, memFloor);
            if (newLevel != level) {
                level = newLevel;
                LOG.info("Omega: nivel " + newLevel + " | tick " + (int) serverMs + " ms | memoria " + (int) (ratio * 100) + "%");
            }

            long now = System.currentTimeMillis();
            if (ratio > MEM_HIGH && now - lastGc > 120000L && serverMs < 25.0 && burstTicks == 0) {
                lastGc = now;
                System.gc();
            }
        }
    }

    // ================= cliente: sem céu/nuvens/chuva e D4 (mobs vanilla perto) =================
    public static class ClientPerf {
        private static class Empty extends IRenderHandler {
            @Override
            public void render(float partialTicks, WorldClient world, Minecraft mc) {}
        }

        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load e) {
            if (e.world.isRemote) {
                e.world.provider.setSkyRenderer(new Empty());
                e.world.provider.setCloudRenderer(new Empty());
                e.world.provider.setWeatherRenderer(new Empty());
            }
        }

        @SubscribeEvent
        public void onRenderLiving(RenderLivingEvent.Pre e) {
            Entity player = Minecraft.getMinecraft().thePlayer;
            if (player == null || e.entity == player) return;
            if (isProtected(e.entity)) return;
            double max = MOB_RENDER_DIST[level];
            if (e.entity.getDistanceSqToEntity(player) > max * max) {
                e.setCanceled(true);
            }
        }
    }

    // ================= 21 e D4: tile entities vanilla distantes não são desenhadas =================
    public static class ClientTileOpt {
        public static class LimitedRenderer extends TileEntitySpecialRenderer {
            private final TileEntitySpecialRenderer inner;

            LimitedRenderer(TileEntitySpecialRenderer inner) {
                this.inner = inner;
            }

            @Override
            public void renderTileEntityAt(TileEntity te, double x, double y, double z, float pt) {
                if (isVanillaClass(te.getClass()) && (x * x + y * y + z * z) > TILE_DIST_SQ[level]) return;
                inner.renderTileEntityAt(te, x, y, z, pt);
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public static void install() {
            try {
                Map map = TileEntityRendererDispatcher.instance.mapSpecialRenderers;
                Map changes = new HashMap();
                for (Object o : map.entrySet()) {
                    Map.Entry en = (Map.Entry) o;
                    Object key = en.getKey();
                    Object val = en.getValue();
                    if (key instanceof Class && isVanillaClass((Class) key) && !(val instanceof LimitedRenderer)
                        && val instanceof TileEntitySpecialRenderer) {
                        changes.put(key, new LimitedRenderer((TileEntitySpecialRenderer) val));
                    }
                }
                map.putAll(changes);
            } catch (Throwable t) {
                LOG.warn("Nao foi possivel instalar o corte de tile entities", t);
            }
        }
    }

    // ================= cliente: limpa a memória ao entrar e ao sair de um mundo =================
    public static class MemoryTrim {
        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load e) {
            if (e.world.isRemote) gcCountdown = 20 * 15;
        }

        @SubscribeEvent
        public void onWorldUnload(WorldEvent.Unload e) {
            if (e.world.isRemote) System.gc();
        }
    }

    // ================= cliente: opções forçadas, FPS e distância pelo nível =================
    public static class ClientSettings {
        private long lastFrame = 0;
        private int userChunks = -1;
        private int appliedChunks = -1;

        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent e) {
            if (e.phase != TickEvent.Phase.START) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null) {
                lastFrame = 0;
                frameMs = 0;
                return;
            }
            long now = System.nanoTime();
            if (lastFrame != 0) {
                double ms = (now - lastFrame) / 1.0e6;
                if (ms < 1000.0) {
                    double f = frameMs;
                    frameMs = (f == 0) ? ms : f * 0.95 + ms * 0.05;
                }
            }
            lastFrame = now;
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;

            if (gcCountdown > 0 && --gcCountdown == 0) System.gc();

            Minecraft mc = Minecraft.getMinecraft();
            GameSettings gs = mc.gameSettings;
            if (gs == null) return;

            if (gs.ambientOcclusion != 0) gs.ambientOcclusion = 0; // iluminação suave off
            if (gs.particleSetting != 2) gs.particleSetting = 2;   // partículas mínimas
            if (gs.fancyGraphics) gs.fancyGraphics = false;        // 13: gráficos rápidos

            int lv = level;
            int fps = lv >= 3 ? FPS_EMERGENCY : FPS_NORMAL;       // 27
            if (gs.limitFramerate != fps) gs.limitFramerate = fps;

            if (mc.theWorld == null) return;

            int cur = gs.renderDistanceChunks;
            if (cur != appliedChunks) userChunks = cur;           // o jogador mudou a opção
            int base = Math.min(userChunks, BASE_CHUNKS);
            int target = base;
            switch (lv) {
                case 1:
                    target = base > 3 ? base - 1 : base;
                    break;
                case 2:
                    target = Math.min(base, 3);
                    break;
                case 3:
                    target = Math.min(base, 2);
                    break;
                default:
                    break;
            }
            if (target != cur) gs.renderDistanceChunks = target;
            appliedChunks = target;
        }
    }
}
