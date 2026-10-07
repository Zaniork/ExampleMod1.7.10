package com.myname.mymodid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.world.WorldEvent;

@Mod(modid = MyMod.MODID, version = Tags.VERSION, name = "MyMod", acceptedMinecraftVersions = "[1.7.10]")
public class MyMod {

    public static final String MODID = "mymodid";
    public static final Logger LOG = LogManager.getLogger(MODID);

    // ---------- ajustes (pode editar os números) ----------
    static final int FPS_NORMAL = 40;                          // 27
    static final int FPS_EMERGENCY = 30;
    static final int BASE_CHUNKS = 5;                          // 6: teto de distância (cliente e servidor interno)
    static final double[] FREEZE_RADIUS = {48, 48, 24, 16};    // 1: congela mobs vanilla além disso (por nível)
    static final int[] AI_INTERVAL = {2, 3, 4, 4};             // 26: mobs entre 24 blocos e o raio de congelar
    static final int[] CLEAN_INTERVAL = {600, 400, 200, 100};  // ticks entre limpezas de itens (por nível)
    static final int ENTITY_CAP = 80;                          // nível 3: máximo de mobs vanilla
    static final double TILE_RENDER_DIST_SQ = 256.0;           // 21: 16 blocos ao quadrado

    // nível do Governador Omega (0 = liso ... 3 = emergência)
    static volatile int level = 0;
    static volatile double serverMs = 0;
    static volatile double frameMs = 0;
    static int gcCountdown = -1;

    @SidedProxy(clientSide = "com.myname.mymodid.ClientProxy", serverSide = "com.myname.mymodid.CommonProxy")
    public static CommonProxy proxy;

    // ---------- proteção: nunca mexe em entidades de outros mods ----------
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
        if (!(ent instanceof EntityLiving)) return true; // jogadores e outros
        if (!isVanillaClass(ent.getClass())) return true; // mobs/personagens de mods (DBC etc.)
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

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);

        FMLCommonHandler.instance().bus().register(new ItemCleaner());
        FMLCommonHandler.instance().bus().register(new Governor());
        MinecraftForge.TERRAIN_GEN_BUS.register(new NoGrass());
        MinecraftForge.EVENT_BUS.register(new SpawnLimiter());
        MinecraftForge.EVENT_BUS.register(new EntityThrottle());
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

    // jogo terminou de carregar: limpa a memória e instala o corte de tile entities
    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        System.gc();
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientTileOpt.install();
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        level = 0;
        serverMs = 0;
        proxy.serverStarting(event);
    }

    // ---------- limpador de itens (só itens vanilla) + teto de mobs no nível 3 ----------
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
                EntityPlayer p = w.getClosestPlayerToEntity(el, -1.0D);
                if (p == null) return;
                list.add(new Far(el, el.getDistanceSqToEntity(p)));
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

    // ---------- sem grama alta, flores e arbusto seco ----------
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

    // ---------- metade dos mobs vanilla naturais não nasce ----------
    public static class SpawnLimiter {
        @SubscribeEvent
        public void onCheckSpawn(LivingSpawnEvent.CheckSpawn e) {
            if (!isVanillaClass(e.entityLiving.getClass())) return;
            if (e.world.rand.nextInt(2) == 0) {
                e.setResult(Event.Result.DENY);
            }
        }
    }

    // ---------- congela / desacelera mobs vanilla distantes (servidor) ----------
    public static class EntityThrottle {
        @SubscribeEvent
        public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
            EntityLivingBase base = e.entityLiving;
            World w = base.worldObj;
            if (w.isRemote || isProtected(base)) return;

            EntityPlayer p = w.getClosestPlayerToEntity(base, -1.0D);
            if (p == null) return;

            int lv = level;
            double d2 = base.getDistanceSqToEntity(p);
            double freeze = FREEZE_RADIUS[lv];
            if (d2 > freeze * freeze) {
                e.setCanceled(true);
                return;
            }
            if (d2 > 24.0D * 24.0D) {
                int interval = AI_INTERVAL[lv];
                if ((w.getTotalWorldTime() + base.getEntityId()) % interval != 0) {
                    e.setCanceled(true);
                }
            }
        }
    }

    // ---------- ticks aleatórios mais lentos (3 -> 2) ----------
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

    // ---------- Governador Omega: sobe e desce o nível conforme o desempenho ----------
    public static class Governor {
        private long start;
        private double avg = 20.0;
        private int sinceEval = 0;
        private int goodTicks = 0;

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                start = System.nanoTime();
                return;
            }
            double ms = (System.nanoTime() - start) / 1.0e6;
            avg = avg * 0.95 + ms * 0.05;
            serverMs = avg;
            sinceEval++;
            if (sinceEval >= 100) { // a cada ~5 s
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
                if (level < 3) {
                    level++;
                    LOG.info("Omega: nivel " + level);
                }
            } else if (good) {
                goodTicks += 100;
                if (goodTicks >= 900 && level > 0) { // ~45 s liso para descer
                    level--;
                    goodTicks = 0;
                    LOG.info("Omega: nivel " + level);
                }
            } else {
                goodTicks = 0;
            }
        }
    }

    // ---------- cliente: sem céu/nuvens/chuva, mobs vanilla a 16 blocos ----------
    public static class ClientPerf {
        private static final double MAX_DIST = 16.0;

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
            if (e.entity.getDistanceSqToEntity(player) > MAX_DIST * MAX_DIST) {
                e.setCanceled(true);
            }
        }
    }

    // ---------- cliente: não desenha tile entities vanilla distantes ----------
    public static class ClientTileOpt {
        public static class LimitedRenderer extends TileEntitySpecialRenderer {
            private final TileEntitySpecialRenderer inner;

            LimitedRenderer(TileEntitySpecialRenderer inner) {
                this.inner = inner;
            }

            @Override
            public void renderTileEntityAt(TileEntity te, double x, double y, double z, float pt) {
                if (isVanillaClass(te.getClass()) && (x * x + y * y + z * z) > TILE_RENDER_DIST_SQ) return;
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

    // ---------- cliente: limpa a memória ao entrar e ao sair de um mundo ----------
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

    // ---------- cliente: opções forçadas, FPS e distância pelo nível ----------
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
            if (gs.fancyGraphics) gs.fancyGraphics = false;        // gráficos rápidos

            int lv = level;
            int fps = lv >= 3 ? FPS_EMERGENCY : FPS_NORMAL;
            if (gs.limitFramerate != fps) gs.limitFramerate = fps;

            if (mc.theWorld == null) return;

            int cur = gs.renderDistanceChunks;
            if (cur != appliedChunks) userChunks = cur; // o jogador mudou a opção
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
