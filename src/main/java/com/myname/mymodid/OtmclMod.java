package com.myname.mymodid;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
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

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
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
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import org.lwjgl.opengl.Display;

/**
 * OtmclMod v1.2 - otimizacao drastica para Minecraft 1.7.10 (Forge, arquivo unico).
 *
 * <p>6 niveis (0 = vanilla ... 5 = maximo). Cada numero de cada nivel e editavel no config (secao "niveis").
 * Comando: /otmcl <0-5> | auto | status | limpar | ajuda
 *
 * <p>SERVIDOR: S1 ticking adaptativo de mobs, S2 TileEntities dormindo, S3 fusao/vida curta de itens, S4 fusao de XP,
 * S5 spawn reduzido + lista negra + despawn, S6 limite de mobs por chunk, S7 chunk reaper, S8 gamerules e clima,
 * S9 worldgen enxuto, S10 modo auto, S11 flechas, S12 limpeza manual,
 * S13 (NOVO) alcance de pathfinding reduzido (menos A*), S14 (NOVO) dimensoes vazias sao descarregadas.
 *
 * <p>CLIENTE: C1 pacote de graficos, C2/C3/C4 culling de entidades e tile entities, C5 teto de particulas, C6 ceu/clima,
 * C7 texturas animadas, C8 F3, C9 janela sem foco dorme,
 * C10 (NOVO) filtro de particulas na origem (por chance e por distancia),
 * C11 limite de entidades e tile entities desenhadas por frame (as mais distantes saem primeiro).
 * <p>v1.2: S15 freio de emergencia (mspt alto = spawn parado + limpeza), S16 limite de explosoes por tick,
 * S17 teto global de entidades por mundo, S18 limite de animais por chunk, S19 itens-lixo somem rapido,
 * S20 /otmcl perfil (quem esta pesando), C12 guarda de memoria (reduz render distance se a RAM esta no limite).
 */
@Mod(modid = OtmclMod.MODID, name = "Otmcl Mod", version = "1.2", acceptedMinecraftVersions = "[1.7.10]")
public class OtmclMod {

    public static final String MODID = "otmclmod";

    static final String[] NAME = {"0 vanilla", "1 leve", "2 medio", "3 forte", "4 extremo", "5 maximo"};

    // --- servidor ---
    static int[] TICK_DIST;
    static int[] TICK_LOG;
    static int[] ITEM_LIFE;
    static int[] ITEM_CAP;
    static int[] ITEM_MERGE;
    static int[] XP_CAP;
    static int[] SPAWN_DENY;
    static int[] DESPAWN_DIST;
    static int[] MOB_CAP;
    static int[] REAP_MARGIN;
    static int[] TE_SLEEP;
    static int[] FIRE_OFF;
    static int[] RTS;
    static int[] WEATHER_OFF;
    static int[] WG_TRIM;
    static int[] BLACKLIST_LV;
    static int[] ARROW_LIFE;
    static int[] FOLLOW_CAP;   // NOVO: alcance maximo de perseguicao/pathfinding dos mobs (0 = vanilla)
    static int[] JUNK_LIFE;    // NOVO: vida (ticks) dos itens-lixo no chao (0 = desliga)
    static int[] EXPL_CAP;     // NOVO: max de explosoes por tick (0 = desliga)
    static int[] ENT_CAP;      // NOVO: teto global de mobs por mundo (0 = desliga)
    static int[] ANIMAL_CAP;   // NOVO: animais por chunk (0 = desliga)
    static int emergMspt = 70;      // NOVO: mspt (curto) que dispara o freio de emergencia (0 desliga)
    static int emergSeconds = 20;   // NOVO: duracao do freio
    static int memPct = 90;         // NOVO: % de RAM "viva" que dispara a guarda de memoria (0 desliga)
    static final Set<String> JUNK = new HashSet<String>();
    static int[] DIM_UNLOAD;   // NOVO: 1 = descarrega Nether/End/etc sem jogadores
    // --- cliente ---
    static int[] CULL_LIVING;
    static int[] CULL_DYN;
    static int[] CULL_TE;
    static int[] NAMETAG;
    static int[] PART_CAP;
    static int[] PART_MODE;
    static int[] RD_CAP;
    static int[] SKY_MODE;
    static int[] ANIM_KILL;
    static int[] UNFOCUS_MS;
    static int[] PART_DROP;    // NOVO: % de particulas descartadas ao nascer
    static int[] PART_DIST;    // NOVO: particulas alem desta distancia nem nascem (0 = desliga)
    static int[] FRAME_LIVING; // NOVO: max de entidades vivas desenhadas por frame (0 = sem limite)
    static int[] FRAME_DYN;    // NOVO: max de itens/flechas/etc desenhados por frame
    static int[] FRAME_TE;     // NOVO: max de tile entities desenhadas por frame

    static String[] sleepableTE = {"TileEntityChest", "TileEntityEnderChest", "TileEntityEnchantmentTable"};
    static boolean wrapModded = false;

    static volatile int level = 0;
    static volatile boolean auto = false;
    static volatile boolean pendingApply = true;
    static volatile boolean pendingClean = false;
    static int autoMin = 0;
    static int autoMax = 5;
    static Configuration cfg;
    static final Random RND = new Random();
    static final UUID FOLLOW_ID = UUID.fromString("5f1e7c3a-9b21-4d44-8c6e-0a1b2c3d4e5f");

    static String origRts = null;
    static String origFire = null;

    // metricas
    static volatile double msptAvg = 0;
    static volatile int fps = 0;
    static int skippedTicks = 0, skippedPerSec = 0;
    static int cullLiving = 0, cullDyn = 0, cullTe = 0, cullPerSec = 0;
    static int itemsMerged = 0, orbsMerged = 0, spawnsDenied = 0, despawned = 0;
    static int mobCapKilled = 0, chunksReaped = 0, teSlept = 0, genDenied = 0, partCleared = 0;
    static int arrowsRemoved = 0, lastCleaned = 0;
    static int followCut = 0, dimsUnloaded = 0, partDropped = 0;
    static int emergencies = 0, explosionsCut = 0, memCuts = 0, entCapKilled = 0;
    static volatile long emergencyUntil = 0;
    static volatile ICommandSender profileSender = null;
    static int explThisTick = 0;
    static double msptShort = 0;
    // contadores por frame (cliente)
    static int frameLiving = 0, frameDyn = 0, frameTe = 0;

    static int[] table(String key, int[] def, int min, int max, String comment) {
        int[] r = new int[6];
        for (int i = 0; i < 6; i++) {
            r[i] = cfg.getInt(key + "_n" + i, "niveis", def[i], min, max, i == 0 ? comment : "nivel " + i + " de " + key);
        }
        return r;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        cfg = new Configuration(e.getSuggestedConfigurationFile());
        cfg.load();
        level = cfg.getInt("nivel_inicial", "geral", 0, 0, 5, "Nivel ao iniciar (0 a 5). Muda com /otmcl <0-5>");
        auto = cfg.getBoolean("auto", "geral", false, "Modo automatico: sobe/desce o nivel conforme mspt e FPS");
        autoMin = cfg.getInt("auto_minimo", "geral", 0, 0, 5, "Menor nivel que o modo auto pode usar");
        autoMax = cfg.getInt("auto_maximo", "geral", 5, 0, 5, "Maior nivel que o modo auto pode usar");
        wrapModded = cfg.getBoolean("culling_renders_de_mods", "cliente", false,
            "Se true, tambem aplica culling nos renderers de mods (pode causar bugs visuais)");
        sleepableTE = cfg.getStringList("te_dorminhocas", "servidor", sleepableTE,
            "Trechos de nome de classe das TileEntities que podem 'dormir' longe dos jogadores");
        String saved = cfg.getString("randomTickSpeed_original", "interno", "", "Guardado automaticamente");
        if (saved.length() > 0) origRts = saved;
        saved = cfg.getString("doFireTick_original", "interno", "", "Guardado automaticamente");
        if (saved.length() > 0) origFire = saved;

        TICK_DIST = table("tick_dist", new int[] {0, 48, 40, 32, 24, 16}, 0, 512, "Blocos por degrau de ticking adaptativo (0 desliga)");
        TICK_LOG = table("tick_log", new int[] {0, 1, 2, 3, 4, 5}, 0, 6, "Intervalo maximo de tick = 2^valor");
        ITEM_LIFE = table("item_vida", new int[] {6000, 6000, 3600, 2400, 1200, 600}, 100, 6000, "Vida do item no chao em ticks");
        ITEM_CAP = table("item_limite", new int[] {0, 0, 0, 48, 32, 16}, 0, 1000, "Itens por area antes do excesso sumir (0 desliga)");
        ITEM_MERGE = table("item_fusao", new int[] {0, 0, 2, 2, 3, 4}, 0, 16, "Celula de fusao de itens em blocos (0 desliga)");
        XP_CAP = table("xp_limite", new int[] {0, 0, 8, 6, 4, 2}, 0, 100, "Orbs de XP por area antes de fundir (0 desliga)");
        SPAWN_DENY = table("spawn_negado_pct", new int[] {0, 0, 15, 30, 50, 70}, 0, 100, "% de spawns naturais negados");
        DESPAWN_DIST = table("despawn_dist", new int[] {0, 0, 0, 80, 64, 48}, 0, 512, "Despawn forcado alem desta distancia (0 desliga)");
        MOB_CAP = table("mobs_por_chunk", new int[] {0, 0, 0, 64, 40, 24}, 0, 500, "Limite de mobs por chunk (0 desliga)");
        REAP_MARGIN = table("reaper_margem", new int[] {0, 0, 0, 4, 3, 2}, 0, 16, "Chunks de margem alem da view distance (0 desliga o reaper)");
        TE_SLEEP = table("te_sleep_dist", new int[] {0, 0, 0, 48, 32, 24}, 0, 512, "Distancia para TE dormirem (0 desliga)");
        FIRE_OFF = table("fogo_desligado", new int[] {0, 0, 0, 0, 1, 1}, 0, 1, "1 = doFireTick false");
        RTS = table("random_tick_speed", new int[] {-1, -1, -1, 2, 1, 1}, -1, 20, "randomTickSpeed (-1 = nao mexe)");
        WEATHER_OFF = table("clima_servidor_off", new int[] {0, 0, 0, 0, 1, 1}, 0, 1, "1 = sem chuva/trovao no servidor");
        WG_TRIM = table("worldgen_corte", new int[] {0, 0, 0, 1, 2, 3}, 0, 3, "Quanto da decoracao de worldgen e removida (0 a 3)");
        BLACKLIST_LV = table("lista_negra_mobs", new int[] {0, 0, 1, 1, 2, 2}, 0, 2, "0 nada, 1 morcegos, 2 morcegos e lulas");
        ARROW_LIFE = table("flecha_vida", new int[] {0, 0, 0, 600, 300, 100}, 0, 1200, "Flechas somem depois de N ticks (0 desliga)");
        FOLLOW_CAP = table("pathfinding_alcance", new int[] {0, 0, 0, 24, 16, 10}, 0, 64,
            "Alcance maximo de perseguicao dos mobs novos em blocos (0 desliga). Menos alcance = bem menos A*");
        DIM_UNLOAD = table("descarregar_dimensoes_vazias", new int[] {0, 0, 0, 0, 1, 1}, 0, 1,
            "1 = descarrega Nether/End/outras dimensoes sem jogadores (nao mexe no Overworld)");
        JUNK_LIFE = table("itens_lixo_vida", new int[] {0, 0, 600, 300, 200, 100}, 0, 6000, "Vida dos itens-lixo no chao em ticks (0 desliga)");
        EXPL_CAP = table("explosoes_por_tick", new int[] {0, 0, 0, 12, 6, 3}, 0, 200, "Max de explosoes por tick (0 desliga). Evita lag de TNT");
        ENT_CAP = table("teto_mobs_mundo", new int[] {0, 0, 0, 500, 350, 220}, 0, 5000, "Teto global de mobs por dimensao (0 desliga)");
        ANIMAL_CAP = table("animais_por_chunk", new int[] {0, 0, 0, 0, 16, 10}, 0, 200, "Animais por chunk (0 desliga). Segura fazendas de criacao");
        emergMspt = cfg.getInt("emergencia_mspt", "servidor", emergMspt, 0, 500, "mspt que dispara o freio de emergencia (0 desliga; so nivel 2+)");
        emergSeconds = cfg.getInt("emergencia_segundos", "servidor", emergSeconds, 5, 300, "Duracao do freio de emergencia");
        memPct = cfg.getInt("guarda_memoria_pct", "cliente", memPct, 0, 99, "% de RAM viva que reduz a render distance (0 desliga; nivel 2+)");
        String[] junk = cfg.getStringList("itens_lixo", "servidor", new String[] {"minecraft:cobblestone", "minecraft:dirt",
            "minecraft:gravel", "minecraft:netherrack", "minecraft:rotten_flesh", "minecraft:wheat_seeds", "minecraft:sapling",
            "minecraft:stone"}, "Itens que somem rapido no chao (nome de registro)");
        JUNK.clear();
        for (String j : junk) if (j != null && j.length() > 0) JUNK.add(j.trim());
        CULL_LIVING = table("cull_vivos", new int[] {0, 128, 96, 72, 48, 32}, 0, 512, "Distancia de render de entidades vivas");
        CULL_DYN = table("cull_outras", new int[] {0, 96, 72, 56, 40, 28}, 0, 512, "Distancia de render de itens/flechas/etc");
        CULL_TE = table("cull_te", new int[] {0, 0, 64, 48, 32, 24}, 0, 512, "Distancia de render de tile entities (0 desliga)");
        NAMETAG = table("nametag", new int[] {0, 0, 0, 1, 1, 2}, 0, 2, "0 normal, 1 so ate 12 blocos, 2 nunca");
        PART_CAP = table("particulas_teto", new int[] {0, 0, 400, 250, 150, 80}, 0, 4000, "Teto de particulas (0 desliga)");
        PART_MODE = table("particulas_modo", new int[] {0, 0, 1, 2, 2, 2}, 0, 2, "0 todas, 1 reduzidas, 2 minimas");
        PART_DROP = table("particulas_descarte_pct", new int[] {0, 0, 15, 30, 50, 70}, 0, 95, "% de particulas descartadas ao nascer");
        PART_DIST = table("particulas_dist", new int[] {0, 0, 0, 48, 32, 24}, 0, 256, "Particulas alem desta distancia nao nascem (0 desliga)");
        FRAME_LIVING = table("frame_vivos_max", new int[] {0, 0, 0, 80, 50, 30}, 0, 1000, "Max de entidades vivas por frame (0 desliga)");
        FRAME_DYN = table("frame_outras_max", new int[] {0, 0, 0, 100, 60, 30}, 0, 1000, "Max de itens/flechas/etc por frame (0 desliga)");
        FRAME_TE = table("frame_te_max", new int[] {0, 0, 0, 120, 80, 50}, 0, 1000, "Max de tile entities por frame (0 desliga)");
        RD_CAP = table("render_dist_max", new int[] {32, 16, 12, 8, 6, 4}, 2, 32, "Teto da render distance");
        SKY_MODE = table("ceu_modo", new int[] {0, 0, 0, 1, 1, 2}, 0, 2, "0 normal, 1 sem clima/nuvens, 2 sem ceu tambem");
        UNFOCUS_MS = table("sem_foco_ms", new int[] {0, 0, 30, 60, 100, 150}, 0, 1000, "Pausa por frame com a janela sem foco (0 desliga)");
        ANIM_KILL = table("animacoes_off", new int[] {0, 0, 0, 0, 1, 1}, 0, 1, "1 desliga texturas animadas");
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
            ClientCommandHandler.instance.registerCommand(new CmdOtmcl());
        }
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new CmdOtmcl());
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent e) {
        pendingApply = true;
    }

    // ============================================================================================
    // CONTROLE DE NIVEL
    // ============================================================================================
    static void setLevel(int n) {
        int old = level;
        level = n < 0 ? 0 : (n > 5 ? 5 : n);
        saveState();
        pendingApply = true;
        if (level >= 4 && old < level) System.gc();
    }

    static void setAuto(boolean a) {
        auto = a;
        saveState();
    }

    static void saveState() {
        if (cfg == null) return;
        cfg.get("geral", "nivel_inicial", 0).set(level);
        cfg.get("geral", "auto", false).set(auto);
        cfg.save();
    }

    static synchronized void autoStep(int dir, long cooldownMs) {
        long now = System.currentTimeMillis();
        if (now - lastAuto < cooldownMs) return;
        int n = level + dir;
        if (n < autoMin || n > autoMax || n == level) return;
        lastAuto = now;
        level = n;
        saveState();
        pendingApply = true;
    }

    static long lastAuto = 0;

    // ============================================================================================
    // SNAPSHOT DE JOGADORES
    // ============================================================================================
    static final Map<Integer, double[]> SNAP = new HashMap<Integer, double[]>();

    static void snapshot(World w) {
        List pl = w.playerEntities;
        int n = pl.size();
        double[] a = new double[n * 3];
        for (int i = 0; i < n; i++) {
            EntityPlayer p = (EntityPlayer) pl.get(i);
            a[i * 3] = p.posX;
            a[i * 3 + 1] = p.posY;
            a[i * 3 + 2] = p.posZ;
        }
        SNAP.put(Integer.valueOf(w.provider.dimensionId), a);
    }

    /** Distancia ao quadrado do jogador mais proximo. -2 = sem snapshot ainda, -1 = nenhum jogador na dimensao. */
    static double nearestSq(World w, double x, double y, double z) {
        double[] a = SNAP.get(Integer.valueOf(w.provider.dimensionId));
        if (a == null) return -2.0;
        if (a.length == 0) return -1.0;
        double m = Double.MAX_VALUE;
        for (int i = 0; i < a.length; i += 3) {
            double dx = a[i] - x;
            double dy = a[i + 1] - y;
            double dz = a[i + 2] - z;
            double d = dx * dx + dy * dy + dz * dz;
            if (d < m) m = d;
        }
        return m;
    }

    // ============================================================================================
    // SERVIDOR
    // ============================================================================================
    static final Map<Integer, List<TileEntity>> SLEEP = new HashMap<Integer, List<TileEntity>>();
    static final Map<Class<?>, Boolean> SLEEPABLE_CACHE = new HashMap<Class<?>, Boolean>();
    static final Map<Integer, Integer> IDLE_DIMS = new HashMap<Integer, Integer>();

    static List<TileEntity> sleepList(int dim) {
        List<TileEntity> l = SLEEP.get(Integer.valueOf(dim));
        if (l == null) {
            l = new ArrayList<TileEntity>();
            SLEEP.put(Integer.valueOf(dim), l);
        }
        return l;
    }

    static boolean sleepable(TileEntity te) {
        Class<?> k = te.getClass();
        Boolean b = SLEEPABLE_CACHE.get(k);
        if (b == null) {
            boolean r = false;
            String n = k.getName();
            for (String s : sleepableTE) {
                if (s != null && s.length() > 0 && n.contains(s)) r = true;
            }
            b = Boolean.valueOf(r);
            SLEEPABLE_CACHE.put(k, b);
        }
        return b.booleanValue();
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
    static void sleep(WorldServer w, int dist) {
        double dsq = (double) dist * dist;
        List list = w.loadedTileEntityList;
        Set<TileEntity> rm = new HashSet<TileEntity>();
        for (int i = 0; i < list.size(); i++) {
            TileEntity te = (TileEntity) list.get(i);
            if (!sleepable(te)) continue;
            double n = nearestSq(w, te.xCoord + 0.5, te.yCoord + 0.5, te.zCoord + 0.5);
            if (n == -2.0) return;
            if (n < 0 || n > dsq) rm.add(te);
        }
        if (rm.isEmpty()) return;
        list.removeAll(rm);
        sleepList(w.provider.dimensionId).addAll(rm);
        teSlept += rm.size();
    }

    static long cellKey(int cx, int cy, int cz) {
        return ((long) (cx & 0x1FFFFF) << 40) | ((long) (cy & 0xFF) << 32) | (cz & 0xFFFFFFFFL);
    }

    static void mergeItems(WorldServer w, int cell) {
        Map<Long, List<EntityItem>> cells = new HashMap<Long, List<EntityItem>>();
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (!(o instanceof EntityItem)) continue;
            EntityItem it = (EntityItem) o;
            if (it.isDead || it.getEntityItem() == null) continue;
            long k = cellKey((int) Math.floor(it.posX / cell), (int) Math.floor(it.posY / cell), (int) Math.floor(it.posZ / cell));
            List<EntityItem> l = cells.get(Long.valueOf(k));
            if (l == null) {
                l = new ArrayList<EntityItem>(4);
                cells.put(Long.valueOf(k), l);
            }
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

    static boolean capEligible(EntityLiving el) {
        if (el.isDead || el.getHealth() <= 0) return false;
        if (el instanceof EntityTameable || el instanceof IBossDisplayData || el instanceof EntityVillager) return false;
        if (el.hasCustomNameTag() || el.getLeashed()) return false;
        if (el.riddenByEntity != null || el.ridingEntity != null) return false;
        return true;
    }

    static void mobCap(WorldServer w, int cap, boolean animalsOnly) {
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
            if (l == null) {
                l = new ArrayList<EntityLiving>();
                by.put(Long.valueOf(k), l);
            }
            l.add(el);
        }
        for (List<EntityLiving> l : by.values()) {
            if (l.size() <= cap) continue;
            final Map<EntityLiving, Double> dm = new HashMap<EntityLiving, Double>();
            for (EntityLiving el : l) {
                double n = nearestSq(w, el.posX, el.posY, el.posZ);
                dm.put(el, Double.valueOf(n < 0 ? Double.MAX_VALUE : n));
            }
            Collections.sort(l, new Comparator<EntityLiving>() {
                @Override
                public int compare(EntityLiving a, EntityLiving b) {
                    return Double.compare(dm.get(b).doubleValue(), dm.get(a).doubleValue());
                }
            });
            int kill = l.size() - cap;
            for (int i = 0; i < kill; i++) {
                l.get(i).setDead();
                mobCapKilled++;
            }
        }
    }

    static int viewDistance() {
        int vd = 16;
        try {
            ServerConfigurationManager scm = MinecraftServer.getServer().getConfigurationManager();
            Integer v = ReflectionHelper.getPrivateValue(ServerConfigurationManager.class, scm, "viewDistance", "field_72402_d");
            if (v != null && v.intValue() > 0) vd = v.intValue();
        } catch (Throwable t) {
            // mantem o valor seguro (16)
        }
        return vd;
    }

    @SuppressWarnings("unchecked")
    static void reap(WorldServer w, int margin) {
        double[] a = SNAP.get(Integer.valueOf(w.provider.dimensionId));
        if (a == null || a.length == 0) return;
        IChunkProvider cp = w.getChunkProvider();
        if (!(cp instanceof ChunkProviderServer)) return;
        ChunkProviderServer cps = (ChunkProviderServer) cp;
        int rad = viewDistance() + margin;
        List chunks = new ArrayList(cps.loadedChunks);
        int done = 0;
        for (Object o : chunks) {
            Chunk c = (Chunk) o;
            boolean near = false;
            for (int i = 0; i < a.length; i += 3) {
                int px = ((int) Math.floor(a[i])) >> 4;
                int pz = ((int) Math.floor(a[i + 2])) >> 4;
                if (Math.abs(c.xPosition - px) <= rad && Math.abs(c.zPosition - pz) <= rad) {
                    near = true;
                    break;
                }
            }
            if (near) continue;
            if (w.getPersistentChunks().containsKey(new ChunkCoordIntPair(c.xPosition, c.zPosition))) continue;
            cps.unloadChunksIfNotNearSpawn(c.xPosition, c.zPosition);
            chunksReaped++;
            if (++done >= 64) break;
        }
    }

    static void arrowReap(WorldServer w, int maxAge) {
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (!(o instanceof EntityArrow)) continue;
            EntityArrow ar = (EntityArrow) o;
            if (!ar.isDead && ar.ticksExisted > maxAge) {
                ar.setDead();
                arrowsRemoved++;
            }
        }
    }

    static int cleanNow(WorldServer w) {
        int n = 0;
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (o instanceof EntityItem || o instanceof EntityXPOrb || o instanceof EntityArrow) {
                Entity en = (Entity) o;
                if (!en.isDead) {
                    en.setDead();
                    n++;
                }
            }
        }
        return n;
    }

    static void clearWeather(WorldServer w) {
        if (w.getWorldInfo().isRaining()) w.getWorldInfo().setRaining(false);
        if (w.getWorldInfo().isThundering()) w.getWorldInfo().setThundering(false);
    }

    /** S17: teto global de mobs por dimensao; remove os mais distantes dos jogadores. */
    static void entityCap(WorldServer w, int cap) {
        List<EntityLiving> l = new ArrayList<EntityLiving>();
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (o instanceof EntityLiving && capEligible((EntityLiving) o)) l.add((EntityLiving) o);
        }
        if (l.size() <= cap) return;
        final Map<EntityLiving, Double> dm = new HashMap<EntityLiving, Double>();
        for (EntityLiving el : l) {
            double n = nearestSq(w, el.posX, el.posY, el.posZ);
            dm.put(el, Double.valueOf(n < 0 ? Double.MAX_VALUE : n));
        }
        Collections.sort(l, new Comparator<EntityLiving>() {
            @Override
            public int compare(EntityLiving a, EntityLiving b) {
                return Double.compare(dm.get(b).doubleValue(), dm.get(a).doubleValue());
            }
        });
        int kill = l.size() - cap;
        for (int i = 0; i < kill; i++) {
            l.get(i).setDead();
            entCapKilled++;
        }
    }

    /** S15: remove itens e flechas velhos (usado pelo freio de emergencia). */
    static void trimOld(WorldServer w, int minAge) {
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (o instanceof EntityItem && ((EntityItem) o).age > minAge) ((EntityItem) o).setDead();
            else if (o instanceof EntityArrow && !((EntityArrow) o).isDead) ((EntityArrow) o).setDead();
        }
    }

    static void bump(Map<String, Integer> m, String k) {
        Integer c = m.get(k);
        m.put(k, Integer.valueOf(c == null ? 1 : c.intValue() + 1));
    }

    static String top(Map<String, Integer> m, int n) {
        List<Map.Entry<String, Integer>> l = new ArrayList<Map.Entry<String, Integer>>(m.entrySet());
        Collections.sort(l, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().intValue() - a.getValue().intValue();
            }
        });
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(n, l.size()); i++) {
            sb.append(l.get(i).getKey()).append('=').append(l.get(i).getValue()).append(' ');
        }
        return sb.length() == 0 ? "-" : sb.toString().trim();
    }

    /** S20: mostra o que esta pesando em cada dimensao (roda no thread do servidor). */
    static void profileTo(ICommandSender s) {
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null) return;
        for (WorldServer w : srv.worldServers) {
            if (w == null) continue;
            Map<String, Integer> ents = new HashMap<String, Integer>();
            Map<String, Integer> tes = new HashMap<String, Integer>();
            List all = w.loadedEntityList;
            for (int i = 0; i < all.size(); i++) {
                Entity en = (Entity) all.get(i);
                String k = EntityList.getEntityString(en);
                if (k == null) k = en.getClass().getSimpleName();
                bump(ents, k);
            }
            List tl = w.loadedTileEntityList;
            for (int i = 0; i < tl.size(); i++) bump(tes, tl.get(i).getClass().getSimpleName());
            int chunks = -1;
            if (w.getChunkProvider() instanceof ChunkProviderServer) {
                chunks = ((ChunkProviderServer) w.getChunkProvider()).getLoadedChunkCount();
            }
            s.addChatMessage(new ChatComponentText("Dim " + w.provider.dimensionId + ": chunks " + chunks + ", entidades "
                + all.size() + ", TEs " + tl.size() + ", jogadores " + w.playerEntities.size()));
            s.addChatMessage(new ChatComponentText("  entidades: " + top(ents, 6)));
            s.addChatMessage(new ChatComponentText("  tile entities: " + top(tes, 6)));
        }
    }

    /** S13: reduz o alcance de perseguicao do mob (modificador temporario, nao salvo no mundo). */
    static void capFollow(EntityLiving el, int cap) {
        IAttributeInstance a = el.getEntityAttribute(SharedMonsterAttributes.followRange);
        if (a == null || a.getModifier(FOLLOW_ID) != null) return;
        double base = a.getBaseValue();
        if (base <= cap) return;
        a.applyModifier(new AttributeModifier(FOLLOW_ID, "otmcl follow", -(1.0 - cap / base), 1).setSaved(false));
        followCut++;
    }

    /** S14: descarrega dimensoes sem jogadores e sem chunkloaders (Overworld nunca e tocado). */
    static void unloadIdleDims() {
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null) return;
        if (DIM_UNLOAD[level] <= 0) {
            IDLE_DIMS.clear();
            return;
        }
        for (WorldServer w : srv.worldServers) {
            if (w == null) continue;
            Integer d = Integer.valueOf(w.provider.dimensionId);
            if (d.intValue() == 0) continue;
            boolean idle = w.playerEntities.isEmpty() && w.getPersistentChunks().isEmpty();
            if (!idle) {
                IDLE_DIMS.remove(d);
                continue;
            }
            Integer c = IDLE_DIMS.get(d);
            int n = c == null ? 1 : c.intValue() + 1;
            if (n >= 2) {
                IDLE_DIMS.remove(d);
                DimensionManager.unloadWorld(d.intValue());
                dimsUnloaded++;
            } else {
                IDLE_DIMS.put(d, Integer.valueOf(n));
            }
        }
    }

    static void applyServer() {
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null) return;
        WorldServer w0 = srv.worldServerForDimension(0);
        if (w0 == null) return;
        GameRules gr = w0.getGameRules();
        int lv = level;
        if (origRts == null) {
            origRts = gr.getGameRuleStringValue("randomTickSpeed");
            if (origRts == null || origRts.length() == 0) origRts = "3";
            cfg.get("interno", "randomTickSpeed_original", "").set(origRts);
        }
        if (origFire == null) {
            origFire = gr.getGameRuleStringValue("doFireTick");
            if (origFire == null || origFire.length() == 0) origFire = "true";
            cfg.get("interno", "doFireTick_original", "").set(origFire);
            cfg.save();
        }
        gr.setOrCreateGameRule("randomTickSpeed", RTS[lv] < 0 ? origRts : String.valueOf(RTS[lv]));
        gr.setOrCreateGameRule("doFireTick", (lv > 0 && FIRE_OFF[lv] > 0) ? "false" : origFire);
        for (WorldServer w : srv.worldServers) {
            if (w != null) wake(w);
        }
    }

    public static class ServerEvents {
        long t0 = 0;
        int counter = 0;
        int dimTick = 0;
        int lowMs = 0;
        int highMs = 0;

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                t0 = System.nanoTime();
                explThisTick = 0;
                if (profileSender != null) {
                    ICommandSender ps = profileSender;
                    profileSender = null;
                    profileTo(ps);
                }
                if (pendingApply) {
                    pendingApply = false;
                    applyServer();
                }
                if (pendingClean) {
                    pendingClean = false;
                    MinecraftServer srv = MinecraftServer.getServer();
                    int total = 0;
                    if (srv != null) {
                        for (WorldServer ws : srv.worldServers) {
                            if (ws != null) total += cleanNow(ws);
                        }
                    }
                    lastCleaned = total;
                }
                return;
            }
            double ms = (System.nanoTime() - t0) / 1000000.0;
            msptAvg = msptAvg * 0.95 + ms * 0.05;
            msptShort = msptShort * 0.8 + ms * 0.2;
            if (emergMspt > 0 && level >= 2 && msptShort > emergMspt && System.currentTimeMillis() > emergencyUntil) {
                emergencyUntil = System.currentTimeMillis() + emergSeconds * 1000L;
                emergencies++;
                MinecraftServer es = MinecraftServer.getServer();
                if (es != null) {
                    for (WorldServer ws : es.worldServers) {
                        if (ws != null) trimOld(ws, 600);
                    }
                }
            }
            if (++dimTick >= 600) {
                dimTick = 0;
                unloadIdleDims();
            }
            if (++counter >= 20) {
                counter = 0;
                skippedPerSec = skippedTicks;
                skippedTicks = 0;
                cullPerSec = cullLiving + cullDyn + cullTe;
                if (auto) {
                    if (msptAvg > 46.0) {
                        highMs++;
                        lowMs = 0;
                    } else if (msptAvg < 28.0) {
                        lowMs++;
                        highMs = 0;
                    } else {
                        highMs = 0;
                        lowMs = 0;
                    }
                    if (highMs >= 10) {
                        autoStep(1, 15000);
                        highMs = 0;
                    }
                    if (lowMs >= 120) {
                        autoStep(-1, 60000);
                        lowMs = 0;
                    }
                }
            }
        }

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent e) {
            if (e.world.isRemote || !(e.world instanceof WorldServer)) return;
            if (e.phase != TickEvent.Phase.START) return;
            WorldServer w = (WorldServer) e.world;
            snapshot(w);
            int lv = level;
            if (lv == 0) return;
            long t = w.getTotalWorldTime();
            if (TE_SLEEP[lv] > 0) {
                if (t % 20 == 0) wake(w);
                else if (t % 20 == 1) sleep(w, TE_SLEEP[lv]);
            }
            if (ITEM_MERGE[lv] > 0 && t % 40 == 7) mergeItems(w, ITEM_MERGE[lv]);
            if (MOB_CAP[lv] > 0 && t % 100 == 13) mobCap(w, MOB_CAP[lv], false);
            if (ANIMAL_CAP[lv] > 0 && t % 100 == 17) mobCap(w, ANIMAL_CAP[lv], true);
            if (ENT_CAP[lv] > 0 && t % 200 == 21) entityCap(w, ENT_CAP[lv]);
            if (REAP_MARGIN[lv] > 0 && t % 100 == 57) reap(w, REAP_MARGIN[lv]);
            if (WEATHER_OFF[lv] > 0 && t % 100 == 3) clearWeather(w);
            if (ARROW_LIFE[lv] > 0 && t % 100 == 31) arrowReap(w, ARROW_LIFE[lv]);
        }

        @SubscribeEvent
        public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
            int lv = level;
            if (lv == 0 || TICK_DIST[lv] <= 0) return;
            EntityLivingBase en = e.entityLiving;
            if (en.worldObj == null || en.worldObj.isRemote) return;
            if (en instanceof EntityPlayer || en instanceof IBossDisplayData) return;
            if (en.riddenByEntity != null || en.ridingEntity != null) return;
            if (en.hurtTime > 0 || en.deathTime > 0 || en.getHealth() <= 0) return;
            if (en instanceof EntityLiving) {
                EntityLiving el = (EntityLiving) en;
                if (el.getAttackTarget() != null || el.getLeashed()) return;
            }
            double n = nearestSq(en.worldObj, en.posX, en.posY, en.posZ);
            if (n == -2.0) return;
            int tier;
            if (n < 0) tier = TICK_LOG[lv];
            else tier = (int) (Math.sqrt(n) / TICK_DIST[lv]);
            if (tier <= 0) return;
            int iv = 1 << Math.min(tier, TICK_LOG[lv]);
            if ((en.worldObj.getTotalWorldTime() + en.getEntityId()) % iv != 0) {
                e.setCanceled(true);
                skippedTicks++;
            }
        }

        @SubscribeEvent
        public void onJoin(EntityJoinWorldEvent e) {
            int lv = level;
            if (lv == 0 || e.world.isRemote) return;
            Entity en = e.entity;
            if (en instanceof EntityItem) {
                EntityItem it = (EntityItem) en;
                if (it.lifespan > ITEM_LIFE[lv]) it.lifespan = ITEM_LIFE[lv];
                int jl = JUNK_LIFE[lv];
                if (jl > 0 && it.getEntityItem() != null) {
                    Object nm = Item.itemRegistry.getNameForObject(it.getEntityItem().getItem());
                    if (nm != null && JUNK.contains(nm.toString())) it.lifespan = Math.min(it.lifespan, jl);
                }
                int cap = ITEM_CAP[lv];
                if (cap > 0) {
                    List l = e.world.getEntitiesWithinAABB(EntityItem.class, it.boundingBox.expand(8.0, 4.0, 8.0));
                    if (l.size() > cap) it.lifespan = Math.min(it.lifespan, 200);
                }
            } else if (en instanceof EntityXPOrb) {
                EntityXPOrb orb = (EntityXPOrb) en;
                int cap = XP_CAP[lv];
                if (cap > 0) {
                    List l = e.world.getEntitiesWithinAABB(EntityXPOrb.class, orb.boundingBox.expand(6.0, 3.0, 6.0));
                    if (l.size() >= cap) {
                        EntityXPOrb target = (EntityXPOrb) l.get(0);
                        if (target != orb && !target.isDead) {
                            target.xpValue += orb.xpValue;
                            orbsMerged++;
                            e.setCanceled(true);
                        }
                    }
                }
            } else if (en instanceof EntityBat && BLACKLIST_LV[lv] >= 1) {
                e.setCanceled(true);
                spawnsDenied++;
            } else if (en instanceof EntitySquid && BLACKLIST_LV[lv] >= 2) {
                e.setCanceled(true);
                spawnsDenied++;
            } else if (en instanceof EntityLiving && FOLLOW_CAP[lv] > 0 && !(en instanceof IBossDisplayData)) {
                capFollow((EntityLiving) en, FOLLOW_CAP[lv]);
            }
        }

        @SubscribeEvent
        public void onCheckSpawn(LivingSpawnEvent.CheckSpawn e) {
            int lv = level;
            if (lv == 0 || e.world.isRemote) return;
            if (System.currentTimeMillis() < emergencyUntil) {
                e.setResult(Event.Result.DENY);
                spawnsDenied++;
                return;
            }
            double p = SPAWN_DENY[lv] / 100.0;
            if (p <= 0) return;
            if (!(e.entityLiving instanceof IMob)) p = p / 2.0;
            if (RND.nextDouble() < p) {
                e.setResult(Event.Result.DENY);
                spawnsDenied++;
            }
        }

        /** S16: limita explosoes por tick (TNT em cadeia e canhoes). */
        @SubscribeEvent
        public void onExplosionStart(ExplosionEvent.Start e) {
            int lv = level;
            if (lv == 0 || e.world.isRemote) return;
            int cap = EXPL_CAP[lv];
            if (cap > 0 && ++explThisTick > cap) {
                e.setCanceled(true);
                explosionsCut++;
            }
        }

        @SubscribeEvent
        public void onAllowDespawn(LivingSpawnEvent.AllowDespawn e) {
            int lv = level;
            if (lv == 0 || e.world.isRemote) return;
            int dd = DESPAWN_DIST[lv];
            if (dd <= 0) return;
            if (!(e.entityLiving instanceof EntityLiving)) return;
            EntityLiving el = (EntityLiving) e.entityLiving;
            if (el.hasCustomNameTag() || el.getLeashed()) return;
            double n = nearestSq(e.world, el.posX, el.posY, el.posZ);
            if (n == -2.0) return;
            if (n < 0 || n > (double) dd * dd) {
                e.setResult(Event.Result.ALLOW);
                despawned++;
            }
        }
    }

    // ============================================================================================
    // S9: WORLDGEN ENXUTO
    // ============================================================================================
    public static class TerrainEvents {
        static final Set<String> D1 = set("LAKE");
        static final Set<String> D2 = set("DEAD_BUSH", "LILYPAD", "SHROOM", "BIG_SHROOM", "FLOWERS");
        static final Set<String> D3 = set("CLAY", "SAND", "SAND_PASS2", "PUMPKIN", "GRASS");
        static final Set<String> P1 = set("LAKE", "LAVA", "NETHER_LAVA", "NETHER_LAVA2");
        static final Set<String> P3 = set("DUNGEON", "FIRE");

        static Set<String> set(String... a) {
            Set<String> s = new HashSet<String>();
            for (String x : a) s.add(x);
            return s;
        }

        @SubscribeEvent
        public void onDecorate(DecorateBiomeEvent.Decorate e) {
            int t = WG_TRIM[level];
            if (level == 0 || t <= 0) return;
            String n = e.type.name();
            if (D1.contains(n) || (t >= 2 && D2.contains(n)) || (t >= 3 && D3.contains(n))) {
                e.setResult(Event.Result.DENY);
                genDenied++;
            }
        }

        @SubscribeEvent
        public void onPopulate(PopulateChunkEvent.Populate e) {
            int t = WG_TRIM[level];
            if (level == 0 || t <= 0) return;
            String n = e.type.name();
            if (P1.contains(n) || (t >= 3 && P3.contains(n))) {
                e.setResult(Event.Result.DENY);
                genDenied++;
            }
        }
    }

    // ============================================================================================
    // CLIENTE
    // ============================================================================================
    @SideOnly(Side.CLIENT)
    public static class CullRender extends Render {
        final Render inner;

        public CullRender(Render inner) {
            this.inner = inner;
            setRenderManager(RenderManager.instance);
        }

        boolean far(double x, double y, double z) {
            int lv = level;
            if (lv == 0) return false;
            double d = x * x + y * y + z * z;
            double r = CULL_DYN[lv];
            if (r > 0 && d > r * r) return true;
            int fc = FRAME_DYN[lv];
            return fc > 0 && ++frameDyn > fc && d > 64.0; // C11: passou do limite do frame, so as >8 blocos saem
        }

        @Override
        public void doRender(Entity e, double x, double y, double z, float yaw, float pt) {
            if (far(x, y, z)) {
                cullDyn++;
                return;
            }
            inner.doRender(e, x, y, z, yaw, pt);
        }

        @Override
        public void doRenderShadowAndFire(Entity e, double x, double y, double z, float yaw, float pt) {
            int lv = level;
            if (lv > 0) {
                double r = CULL_DYN[lv];
                if (r > 0 && x * x + y * y + z * z > r * r) return;
            }
            inner.doRenderShadowAndFire(e, x, y, z, yaw, pt);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity e) {
            return TextureMap.locationBlocksTexture;
        }
    }

    @SideOnly(Side.CLIENT)
    public static class CullTesr extends TileEntitySpecialRenderer {
        final TileEntitySpecialRenderer inner;

        public CullTesr(TileEntitySpecialRenderer inner) {
            this.inner = inner;
        }

        @Override
        public void renderTileEntityAt(TileEntity te, double x, double y, double z, float pt) {
            int lv = level;
            if (lv > 0) {
                double d = x * x + y * y + z * z;
                double r = CULL_TE[lv];
                if (r > 0 && d > r * r) {
                    cullTe++;
                    return;
                }
                int fc = FRAME_TE[lv];
                if (fc > 0 && ++frameTe > fc && d > 100.0) { // C11
                    cullTe++;
                    return;
                }
            }
            inner.renderTileEntityAt(te, x, y, z, pt);
        }
    }

    /** C10: filtra particulas na ORIGEM (antes de virarem entidades): por chance e por distancia da camera. */
    @SideOnly(Side.CLIENT)
    public static class ThrottledFx extends EffectRenderer {
        public ThrottledFx(World w, TextureManager tm) {
            super(w, tm);
        }

        @Override
        public void addEffect(EntityFX fx) {
            int lv = level;
            if (lv > 0) {
                int drop = PART_DROP[lv];
                if (drop > 0 && RND.nextInt(100) < drop) {
                    partDropped++;
                    return;
                }
                double r = PART_DIST[lv];
                if (r > 0) {
                    Entity v = Minecraft.getMinecraft().renderViewEntity;
                    if (v != null && fx.getDistanceSqToEntity(v) > r * r) {
                        partDropped++;
                        return;
                    }
                }
            }
            super.addEffect(fx);
        }
    }

    @SideOnly(Side.CLIENT)
    public static class ClientEvents {
        static final IRenderHandler NOOP = new IRenderHandler() {
            @Override
            public void render(float partialTicks, WorldClient world, Minecraft mc) {
                // nao desenha nada
            }
        };

        int frames = 0;
        long last = 0;
        int appliedLevel = -1;
        int skyLevel = -1;
        WorldProvider lastProv = null;
        boolean wrapped = false;
        boolean captured = false;
        boolean animKilled = false;
        boolean oFancy, oClouds, oBobbing, oSnooper;
        int oAO, oParticles, oRender;
        int partTick = 0;
        int lowFps = 0;
        int highFps = 0;
        int memStrikes = 0;
        long lastMemCut = 0;
        long lastGc = 0;

        @SubscribeEvent
        public void onRenderLiving(RenderLivingEvent.Pre e) {
            int lv = level;
            if (lv == 0) return;
            if (e.entity instanceof EntityPlayer || e.entity instanceof IBossDisplayData) return;
            double d = e.x * e.x + e.y * e.y + e.z * e.z;
            double r = CULL_LIVING[lv];
            if (r > 0 && d > r * r) {
                e.setCanceled(true);
                cullLiving++;
                return;
            }
            int fc = FRAME_LIVING[lv];
            if (fc > 0 && ++frameLiving > fc && d > 256.0) { // C11: acima do limite, so as >16 blocos saem
                e.setCanceled(true);
                cullLiving++;
            }
        }

        @SubscribeEvent
        public void onNameTags(RenderLivingEvent.Specials.Pre e) {
            int lv = level;
            if (lv == 0) return;
            int m = NAMETAG[lv];
            if (m == 0) return;
            double d = e.x * e.x + e.y * e.y + e.z * e.z;
            if (m >= 2 || d > 144.0) e.setCanceled(true);
        }

        @SuppressWarnings("unchecked")
        void wrapRenderers() {
            try {
                Map m = RenderManager.instance.entityRenderMap;
                for (Object o : new ArrayList(m.entrySet())) {
                    Map.Entry en = (Map.Entry) o;
                    Object v = en.getValue();
                    if (!(v instanceof Render) || v instanceof RendererLivingEntity || v instanceof CullRender) continue;
                    if (!wrapModded && !v.getClass().getName().startsWith("net.minecraft.")) continue;
                    m.put(en.getKey(), new CullRender((Render) v));
                }
            } catch (Throwable t) {
                System.out.println("[OTMCL] culling de entidades nao-vivas indisponivel: " + t);
            }
            try {
                Map m = TileEntityRendererDispatcher.instance.mapSpecialRenderers;
                for (Object o : new ArrayList(m.entrySet())) {
                    Map.Entry en = (Map.Entry) o;
                    Object v = en.getValue();
                    if (!(v instanceof TileEntitySpecialRenderer) || v instanceof CullTesr) continue;
                    if (!v.getClass().getName().startsWith("net.minecraft.")) continue;
                    m.put(en.getKey(), new CullTesr((TileEntitySpecialRenderer) v));
                }
            } catch (Throwable t) {
                System.out.println("[OTMCL] culling de tile entities indisponivel: " + t);
            }
        }

        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                frameLiving = 0; // C11: zera os contadores por frame
                frameDyn = 0;
                frameTe = 0;
                int lv = level;
                if (lv > 0 && UNFOCUS_MS[lv] > 0 && !Display.isActive()) {
                    try {
                        Thread.sleep(UNFOCUS_MS[lv]);
                    } catch (InterruptedException ex) {
                        // ignora
                    }
                }
                return;
            }
            frames++;
            long now = System.currentTimeMillis();
            if (now - last >= 1000) {
                fps = frames;
                frames = 0;
                last = now;
                cullPerSec = cullLiving + cullDyn + cullTe;
                cullLiving = 0;
                cullDyn = 0;
                cullTe = 0;
                autoClient();
                memGuard(now);
            }
        }

        /** C12: mede a RAM "viva" (depois do ultimo GC). Se ficar no limite, reduz a render distance e pede um GC. */
        void memGuard(long now) {
            if (memPct <= 0 || level < 2) return;
            int pct = -1;
            try {
                for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                    if (pool.getType() != MemoryType.HEAP) continue;
                    String n = pool.getName();
                    if (!n.contains("Old") && !n.contains("Tenured")) continue;
                    MemoryUsage u = pool.getCollectionUsage();
                    if (u != null && u.getMax() > 0) pct = (int) (u.getUsed() * 100L / u.getMax());
                }
            } catch (Throwable t) {
                return;
            }
            if (pct < 0) return;
            if (pct >= memPct) memStrikes++;
            else memStrikes = 0;
            if (memStrikes >= 5 && now - lastMemCut > 30000) {
                lastMemCut = now;
                memStrikes = 0;
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.theWorld != null && mc.gameSettings.renderDistanceChunks > 3) {
                    mc.gameSettings.renderDistanceChunks--;
                    mc.renderGlobal.loadRenderers();
                    memCuts++;
                }
                if (now - lastGc > 60000) {
                    lastGc = now;
                    System.gc();
                }
            }
        }

        void autoClient() {
            if (!auto) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.currentScreen != null) return;
            if (fps < 30) {
                lowFps++;
                highFps = 0;
            } else if (fps > 100) {
                highFps++;
                lowFps = 0;
            } else {
                lowFps = 0;
                highFps = 0;
            }
            if (lowFps >= 5) {
                autoStep(1, 15000);
                lowFps = 0;
            }
            if (highFps >= 90) {
                autoStep(-1, 60000);
                highFps = 0;
            }
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null) {
                lastProv = null;
                return;
            }
            if (!wrapped) {
                wrapped = true;
                wrapRenderers();
            }
            // C10: troca o gerenciador de particulas pelo que filtra na origem
            if (!(mc.effectRenderer instanceof ThrottledFx)) {
                mc.effectRenderer = new ThrottledFx(mc.theWorld, mc.renderEngine);
            }
            int lv = level;
            if (appliedLevel != lv) applyClient(mc, lv);
            if (lastProv != mc.theWorld.provider || skyLevel != lv) {
                applySky(mc, lv);
                lastProv = mc.theWorld.provider;
                skyLevel = lv;
            }
            if (lv >= 4) {
                mc.theWorld.setRainStrength(0.0F);
                mc.theWorld.setThunderStrength(0.0F);
            }
            int cap = PART_CAP[lv];
            if (cap > 0 && ++partTick >= 20) {
                partTick = 0;
                try {
                    int n = Integer.parseInt(mc.effectRenderer.getStatistics().trim());
                    if (n > cap) {
                        mc.effectRenderer.clearEffects(mc.theWorld);
                        partCleared++;
                    }
                } catch (Exception ex) {
                    // ignora
                }
            }
        }

        void applyClient(Minecraft mc, int lv) {
            GameSettings gs = mc.gameSettings;
            if (!captured) {
                captured = true;
                oFancy = gs.fancyGraphics;
                oClouds = gs.clouds;
                oBobbing = gs.viewBobbing;
                oSnooper = gs.snooperEnabled;
                oAO = gs.ambientOcclusion;
                oParticles = gs.particleSetting;
                oRender = gs.renderDistanceChunks;
            }
            if (lv == 0) {
                gs.fancyGraphics = oFancy;
                gs.clouds = oClouds;
                gs.viewBobbing = oBobbing;
                gs.snooperEnabled = oSnooper;
                gs.ambientOcclusion = oAO;
                gs.particleSetting = oParticles;
                gs.renderDistanceChunks = oRender;
            } else {
                gs.clouds = false;
                gs.snooperEnabled = false;
                gs.fancyGraphics = lv >= 2 ? false : oFancy;
                gs.ambientOcclusion = lv >= 3 ? 0 : (lv == 2 ? Math.min(oAO, 1) : oAO);
                gs.particleSetting = Math.max(oParticles, PART_MODE[lv]);
                gs.renderDistanceChunks = Math.max(2, Math.min(oRender, RD_CAP[lv]));
                gs.viewBobbing = lv >= 5 ? false : oBobbing;
            }
            if (lv > 0 && ANIM_KILL[lv] > 0) {
                if (!animKilled) {
                    killAnimations(mc);
                    animKilled = true;
                }
            } else if (animKilled) {
                animKilled = false;
                mc.refreshResources();
            }
            mc.renderGlobal.loadRenderers();
            appliedLevel = lv;
        }

        void killAnimations(Minecraft mc) {
            try {
                TextureMap tm = mc.getTextureMapBlocks();
                List l = ReflectionHelper.getPrivateValue(TextureMap.class, tm, "listAnimatedSprites", "field_94258_i");
                if (l != null) l.clear();
            } catch (Throwable t) {
                System.out.println("[OTMCL] nao foi possivel desligar as texturas animadas: " + t);
            }
        }

        void applySky(Minecraft mc, int lv) {
            WorldProvider p = mc.theWorld.provider;
            int mode = lv == 0 ? 0 : SKY_MODE[lv];
            if (p.getWeatherRenderer() == null || p.getWeatherRenderer() == NOOP) p.setWeatherRenderer(mode >= 1 ? NOOP : null);
            if (p.getCloudRenderer() == null || p.getCloudRenderer() == NOOP) p.setCloudRenderer(mode >= 1 ? NOOP : null);
            if (p.getSkyRenderer() == null || p.getSkyRenderer() == NOOP) p.setSkyRenderer(mode >= 2 ? NOOP : null);
        }

        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Text e) {
            Minecraft mc = Minecraft.getMinecraft();
            if (!mc.gameSettings.showDebugInfo) return;
            e.left.add("");
            e.left.add("[OTMCL] nivel " + NAME[level] + (auto ? " (auto)" : "") + " | FPS " + fps + " | render "
                + mc.gameSettings.renderDistanceChunks + " | ocultos/s " + cullPerSec);
            e.left.add("[OTMCL] mspt " + String.format("%.1f", msptAvg) + " | ticks pulados/s " + skippedPerSec
                + " | TE dormindo " + totalSleeping() + " | particulas barradas " + partDropped);
        }
    }

    static int totalSleeping() {
        int n = 0;
        for (List<TileEntity> l : SLEEP.values()) n += l.size();
        return n;
    }

    // ============================================================================================
    // COMANDO /otmcl
    // ============================================================================================
    public static class CmdOtmcl extends CommandBase {
        @Override
        public String getCommandName() {
            return "otmcl";
        }

        @Override
        public String getCommandUsage(ICommandSender s) {
            return "/otmcl <0-5> | auto | status | perfil | limpar | ajuda";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender s) {
            if (s.getEntityWorld() != null && s.getEntityWorld().isRemote) return true;
            return s.canCommandSenderUseCommand(2, "otmcl");
        }

        void say(ICommandSender s, String m) {
            s.addChatMessage(new ChatComponentText(m));
        }

        @Override
        public void processCommand(ICommandSender s, String[] a) {
            if (a.length == 0 || a[0].equalsIgnoreCase("ajuda") || a[0].equalsIgnoreCase("help")) {
                say(s, "OtmclMod: /otmcl <0-5> muda o nivel | auto liga/desliga o modo automatico | status | perfil (o que pesa) | limpar");
                say(s, "Niveis: 0 vanilla, 1 leve, 2 medio, 3 forte, 4 extremo, 5 maximo. Tudo editavel em config/otmclmod.");
                return;
            }
            if (a[0].equalsIgnoreCase("status")) {
                say(s, "Nivel " + NAME[level] + (auto ? " (auto)" : "") + " | mspt " + String.format("%.1f", msptAvg)
                    + " | FPS " + fps);
                say(s, "Servidor: ticks pulados/s " + skippedPerSec + ", TE dormindo " + totalSleeping() + ", itens fundidos "
                    + itemsMerged + ", orbs fundidos " + orbsMerged);
                say(s, "Spawns negados " + spawnsDenied + ", despawns " + despawned + ", mobs removidos (limite/chunk) "
                    + mobCapKilled + ", chunks descarregados " + chunksReaped + ", worldgen cortado " + genDenied);
                say(s, "Mobs com pathfinding reduzido " + followCut + ", dimensoes descarregadas " + dimsUnloaded
                    + ", flechas removidas " + arrowsRemoved + ", ultima limpeza " + lastCleaned + " entidades");
                say(s, "Freios de emergencia " + emergencies + ", explosoes cortadas " + explosionsCut
                    + ", mobs acima do teto removidos " + entCapKilled + ", cortes por memoria " + memCuts);
                say(s, "Cliente: ocultos/s " + cullPerSec + ", particulas barradas " + partDropped + ", limpezas de particulas "
                    + partCleared);
                return;
            }
            if (a[0].equalsIgnoreCase("perfil") || a[0].equalsIgnoreCase("profile")) {
                profileSender = s;
                say(s, "Gerando perfil no proximo tick do servidor...");
                return;
            }
            if (a[0].equalsIgnoreCase("limpar") || a[0].equalsIgnoreCase("clean")) {
                pendingClean = true;
                say(s, "Limpeza agendada: itens no chao, orbs de XP e flechas serao removidos no proximo tick do servidor.");
                return;
            }
            if (a[0].equalsIgnoreCase("auto")) {
                setAuto(!auto);
                say(s, "Modo auto " + (auto ? "LIGADO" : "desligado") + " (nivel entre " + autoMin + " e " + autoMax + ").");
                return;
            }
            int n;
            try {
                n = Integer.parseInt(a[0]);
            } catch (NumberFormatException ex) {
                say(s, "Uso: " + getCommandUsage(s));
                return;
            }
            if (n < 0 || n > 5) {
                say(s, "Nivel de 0 a 5.");
                return;
            }
            setLevel(n);
            say(s, "OtmclMod nivel " + NAME[level] + " ativado.");
        }
    }
}
