package com.myname.mymodid;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.opengl.Display;

/**
 * liotm v1.0 - 64 modulos de otimizacao para Minecraft 1.7.10 (Forge, arquivo unico).
 * Niveis: 0 desligado, 1 leve, 2 forte, 3 extremo. Cada modulo liga/desliga sozinho: /liotm lista, /liotm mod <n> on|off.
 */
@Mod(modid = LiotmMod.MODID, name = "liotm", version = "1.0", acceptedMinecraftVersions = "[1.7.10]")
public class LiotmMod {

    public static final String MODID = "liotm";

    // ================= REGISTRO DOS MODULOS =================
    static final String[] NAME = new String[80];
    static final int[] MIN = new int[80];
    static final boolean[] EN = new boolean[80];
    static final boolean[] SRV = new boolean[80];
    static int count = 0;

    static int reg(boolean server, String name, int min, boolean on) {
        NAME[count] = name;
        MIN[count] = min;
        EN[count] = on;
        SRV[count] = server;
        return count++;
    }

    // ---- servidor (36) ----
    static final int M_SLOW = reg(true, "Mobs distantes em camera lenta", 1, true);
    static final int M_FREEZE = reg(true, "Mobs muito longe quase congelados", 2, true);
    static final int M_ANIMSLOW = reg(true, "Animais passivos mais lentos", 2, true);
    static final int M_VILLAGER = reg(true, "Aldeoes preguicosos", 1, true);
    static final int M_TE_CHEST = reg(true, "Baus dormem longe", 2, true);
    static final int M_TE_HOPPER = reg(true, "Funis e dispensers dormem longe", 2, true);
    static final int M_TE_MODS = reg(true, "TileEntities de mods dormem longe (arriscado)", 3, false);
    static final int M_ITEM_MERGE = reg(true, "Fusao de itens no chao", 1, true);
    static final int M_ITEM_LIFE = reg(true, "Vida curta dos itens", 1, true);
    static final int M_JUNK = reg(true, "Itens-lixo somem rapido", 2, true);
    static final int M_ITEM_CAP = reg(true, "Limite de itens por area", 2, true);
    static final int M_ITEM_FAR = reg(true, "Itens longe dos jogadores somem", 2, true);
    static final int M_XP_MERGE = reg(true, "Fusao de orbs de XP", 1, true);
    static final int M_XP_FAR = reg(true, "Orbs de XP longe somem", 2, true);
    static final int M_SPAWN = reg(true, "Spawn natural reduzido", 2, true);
    static final int M_EMERG = reg(true, "Freio de emergencia (mspt alto)", 2, true);
    static final int M_BLACKLIST = reg(true, "Sem morcegos e lulas", 2, true);
    static final int M_DESPAWN = reg(true, "Despawn forcado de mobs longe", 2, true);
    static final int M_MOB_CHUNK = reg(true, "Limite de mobs por chunk", 2, true);
    static final int M_MOB_WORLD = reg(true, "Teto global de mobs", 2, true);
    static final int M_ANIMAL_CHUNK = reg(true, "Limite de animais por chunk", 2, true);
    static final int M_FOLLOW = reg(true, "Pathfinding curto dos mobs", 2, true);
    static final int M_REAPER = reg(true, "Descarrega chunks longe da view distance", 2, true);
    static final int M_DIMS = reg(true, "Dimensoes vazias descarregadas", 2, true);
    static final int M_WEATHER_S = reg(true, "Sem chuva e trovao no servidor", 3, true);
    static final int M_RTS = reg(true, "Random tick speed reduzido", 2, true);
    static final int M_FIRE = reg(true, "Fogo nao se espalha", 3, true);
    static final int M_ARROW = reg(true, "Flechas somem cedo", 2, true);
    static final int M_EXPL = reg(true, "Limite de explosoes por tick", 2, true);
    static final int M_WG_DECOR = reg(true, "Worldgen sem decoracao pesada", 2, true);
    static final int M_WG_LAKES = reg(true, "Worldgen sem lagos e lava", 2, true);
    static final int M_SLIME = reg(true, "Limite de slimes por area", 2, true);
    static final int M_TNT = reg(true, "Limite de TNT aceso por area", 2, true);
    static final int M_PROJ = reg(true, "Teto de projeteis por dimensao", 2, true);
    static final int M_AUTO = reg(true, "Modo automatico (sobe e desce o nivel)", 1, false);
    static final int M_GRIEF = reg(true, "mobGriefing desligado (creeper nao destroi blocos)", 3, false);

    // ---- cliente (28) ----
    static final int M_CULL_LIVING = reg(false, "Nao desenha mobs longe", 1, true);
    static final int M_CULL_DYN = reg(false, "Nao desenha itens, flechas e quadros longe", 1, true);
    static final int M_CULL_TE = reg(false, "Nao desenha baus e blocos especiais longe", 2, true);
    static final int M_FR_LIVING = reg(false, "Limite de mobs desenhados por frame", 2, true);
    static final int M_FR_DYN = reg(false, "Limite de itens desenhados por frame", 2, true);
    static final int M_FR_TE = reg(false, "Limite de blocos especiais por frame", 2, true);
    static final int M_NAMETAGS = reg(false, "Nomes sobre mobs so de perto", 2, true);
    static final int M_PART_DROP = reg(false, "Descarta parte das particulas ao nascer", 2, true);
    static final int M_PART_DIST = reg(false, "Particulas longe nem nascem", 2, true);
    static final int M_PART_CAP = reg(false, "Teto de particulas", 2, true);
    static final int M_PART_MIN = reg(false, "Particulas no minimo (opcao do jogo)", 2, true);
    static final int M_FASTGFX = reg(false, "Graficos rapidos (folhas opacas, sem sombras)", 1, true);
    static final int M_AO = reg(false, "Iluminacao suave desligada", 2, true);
    static final int M_CLOUDS = reg(false, "Nuvens desligadas (opcao do jogo)", 1, true);
    static final int M_BOB = reg(false, "Balanco de camera desligado", 3, true);
    static final int M_SNOOPER = reg(false, "Snooper desligado", 1, true);
    static final int M_ADV = reg(false, "Advanced OpenGL desligado", 1, true);
    static final int M_RD = reg(false, "Teto da render distance", 1, true);
    static final int M_FOV = reg(false, "Teto do FOV", 3, true);
    static final int M_SKY = reg(false, "Ceu (sol, lua, estrelas) nao e desenhado", 3, true);
    static final int M_SKYCLOUDS = reg(false, "Nuvens e chuva nao sao desenhadas", 2, true);
    static final int M_RAIN_C = reg(false, "Chuva e trovao zerados no cliente", 3, true);
    static final int M_ANIM = reg(false, "Texturas animadas desligadas", 3, true);
    static final int M_UNFOCUS = reg(false, "Jogo dorme com a janela sem foco", 2, true);
    static final int M_HAND = reg(false, "Esconde a mao e o contorno do bloco", 3, true);
    static final int M_CL_TICK = reg(false, "Mobs longe atualizam menos no cliente", 2, true);
    static final int M_MEM = reg(false, "Guarda de memoria (reduz render distance no limite)", 2, true);
    static final int M_GC = reg(false, "Limpeza de memoria ao entrar e sair do mundo", 1, true);
    static final int M_FPSCAP = reg(false, "Limite de FPS (economiza bateria; desligado por padrao)", 3, false);
    static final int M_MIP = reg(false, "Mipmaps e filtro anisotropico desligados", 3, true);

    static final String[] LVNAME = {"0 desligado", "1 leve", "2 forte", "3 extremo"};

    // ================= ESTADO =================
    static volatile int level = 0;
    static volatile boolean pendingApply = true;
    static volatile boolean pendingClean = false;
    static volatile ICommandSender profileSender = null;
    static int autoMin = 1;
    static int autoMax = 3;
    static Configuration cfg;
    static final Random RND = new Random();
    static final UUID FOLLOW_ID = UUID.fromString("0b3d5a7e-1f2c-4d6b-9e8a-7c5b3a1f0d2e");
    static final Set<String> JUNK = new HashSet<String>();
    static String origRts = null;
    static String origFire = null;
    static String origGrief = null;
    static int emergMspt = 70;
    static int emergSeconds = 20;
    static int memPct = 90;

    // valores por nivel (indice 1, 2 e 3)
    static int[] TICK_DIST, TICK_LOG, FREEZE_DIST, ANIM_DIST, ANIM_IV, VIL_DIST, VIL_IV;
    static int[] TE_CHEST, TE_HOPPER, TE_MODS, ITEM_MERGE, ITEM_LIFE, JUNK_LIFE, ITEM_CAP, ITEM_FAR, XP_CAP, XP_FAR;
    static int[] SPAWN_DENY, DESPAWN, MOB_CHUNK, MOB_WORLD, ANIMAL_CHUNK, FOLLOW, REAP, ARROW, EXPL, RTS;
    static int[] SLIME_CAP, TNT_CAP, PROJ_CAP;
    static int[] CULL_LIVING, CULL_DYN, CULL_TE, FR_LIVING, FR_DYN, FR_TE, NAME_DIST, PART_DROP, PART_DIST, PART_CAP;
    static int[] RD_MAX, FOV_MAX, UNFOCUS, CL_TICK, FPS_CAP;

    // metricas
    static volatile double msptAvg = 0;
    static double msptShort = 0;
    static volatile int fps = 0;
    static int skippedTicks = 0, skippedPerSec = 0;
    static int cullLiving = 0, cullDyn = 0, cullTe = 0, cullPerSec = 0;
    static int itemsMerged = 0, orbsMerged = 0, spawnsDenied = 0, despawned = 0, mobCapKilled = 0, chunksReaped = 0;
    static int teSlept = 0, genDenied = 0, partCleared = 0, arrowsRemoved = 0, lastCleaned = 0, followCut = 0;
    static int dimsUnloaded = 0, partDropped = 0, emergencies = 0, explosionsCut = 0, memCuts = 0, entCapKilled = 0;
    static int itemsFarKilled = 0, orbsFarKilled = 0, projKilled = 0, slimeDenied = 0, tntDenied = 0;
    static volatile long emergencyUntil = 0;
    static int explThisTick = 0;
    static int frameLiving = 0, frameDyn = 0, frameTe = 0;
    static final long[] fpsSum = new long[4];
    static final int[] fpsCnt = new int[4];
    static int statSkip = 0;
    static long lastAuto = 0;

    static boolean on(int m) {
        return level >= MIN[m] && EN[m];
    }

    static int activeCount() {
        int n = 0;
        for (int i = 0; i < count; i++) if (on(i)) n++;
        return n;
    }

    static int stateKey() {
        int k = level;
        for (int i = 0; i < count; i++) k = k * 31 + (on(i) ? 1 : 0);
        return k;
    }

    static String modKey(int i) {
        return "m" + (i < 9 ? "0" : "") + (i + 1);
    }

    static int[] P(String key, int a, int b, int c, int min, int max, String comment) {
        return new int[] {0,
            cfg.getInt(key + "_n1", "valores", a, min, max, comment + " (nivel 1)"),
            cfg.getInt(key + "_n2", "valores", b, min, max, comment + " (nivel 2)"),
            cfg.getInt(key + "_n3", "valores", c, min, max, comment + " (nivel 3)")};
    }

    // ================= CICLO DE VIDA =================
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        cfg = new Configuration(e.getSuggestedConfigurationFile());
        cfg.load();
        level = cfg.getInt("nivel_inicial", "geral", 0, 0, 3, "Nivel ao iniciar: 0 desligado, 1 leve, 2 forte, 3 extremo");
        autoMin = cfg.getInt("auto_minimo", "geral", 1, 1, 3, "Menor nivel do modo automatico");
        autoMax = cfg.getInt("auto_maximo", "geral", 3, 1, 3, "Maior nivel do modo automatico");
        emergMspt = cfg.getInt("emergencia_mspt", "geral", emergMspt, 20, 500, "mspt que dispara o freio de emergencia");
        emergSeconds = cfg.getInt("emergencia_segundos", "geral", emergSeconds, 5, 300, "Duracao do freio de emergencia");
        memPct = cfg.getInt("guarda_memoria_pct", "geral", memPct, 50, 99, "% de RAM viva que dispara a guarda de memoria");
        String saved = cfg.getString("randomTickSpeed_original", "interno", "", "Guardado automaticamente");
        if (saved.length() > 0) origRts = saved;
        saved = cfg.getString("doFireTick_original", "interno", "", "Guardado automaticamente");
        if (saved.length() > 0) origFire = saved;
        saved = cfg.getString("mobGriefing_original", "interno", "", "Guardado automaticamente");
        if (saved.length() > 0) origGrief = saved;

        for (int i = 0; i < count; i++) {
            EN[i] = cfg.getBoolean(modKey(i), "modulos", EN[i], (i + 1) + " - " + NAME[i] + (SRV[i] ? " [servidor]" : " [cliente]"));
            MIN[i] = cfg.getInt(modKey(i) + "_nivel", "modulos", MIN[i], 1, 3, "Nivel minimo do modulo " + (i + 1));
        }

        TICK_DIST = P("tick_dist", 48, 32, 20, 4, 512, "Blocos por degrau de ticking dos mobs");
        TICK_LOG = P("tick_log", 2, 3, 4, 1, 6, "Intervalo maximo de tick = 2^valor");
        FREEZE_DIST = P("congelar_dist", 160, 112, 80, 32, 1000, "Mobs alem disso quase param");
        ANIM_DIST = P("animais_lentos_dist", 16, 12, 8, 2, 128, "Animais alem disso ficam lentos");
        ANIM_IV = P("animais_lentos_iv", 2, 2, 3, 2, 10, "Animais passivos atualizam 1 a cada N ticks");
        VIL_DIST = P("aldeoes_dist", 32, 24, 16, 2, 128, "Aldeoes alem disso ficam lentos");
        VIL_IV = P("aldeoes_iv", 2, 3, 4, 2, 10, "Aldeoes atualizam 1 a cada N ticks");
        TE_CHEST = P("te_baus_dist", 64, 40, 24, 8, 512, "Baus dormem alem disso");
        TE_HOPPER = P("te_funis_dist", 64, 40, 24, 8, 512, "Funis e dispensers dormem alem disso");
        TE_MODS = P("te_mods_dist", 96, 64, 48, 8, 512, "TEs de mods dormem alem disso");
        ITEM_MERGE = P("item_fusao_celula", 2, 3, 4, 1, 16, "Celula de fusao de itens em blocos");
        ITEM_LIFE = P("item_vida", 4800, 2400, 1000, 100, 6000, "Vida do item no chao em ticks");
        JUNK_LIFE = P("lixo_vida", 600, 300, 100, 20, 6000, "Vida dos itens-lixo em ticks");
        ITEM_CAP = P("item_limite", 64, 40, 20, 4, 1000, "Itens por area antes do excesso sumir");
        ITEM_FAR = P("item_longe_dist", 160, 112, 80, 32, 1000, "Itens alem disso dos jogadores somem");
        XP_CAP = P("xp_limite", 8, 5, 3, 2, 100, "Orbs de XP por area antes de fundir");
        XP_FAR = P("xp_longe_dist", 128, 96, 64, 32, 1000, "Orbs alem disso dos jogadores somem");
        SPAWN_DENY = P("spawn_negado_pct", 15, 40, 70, 1, 100, "% de spawns naturais negados");
        DESPAWN = P("despawn_dist", 80, 64, 48, 16, 512, "Despawn forcado alem desta distancia");
        MOB_CHUNK = P("mobs_por_chunk", 64, 32, 16, 4, 500, "Limite de mobs por chunk");
        MOB_WORLD = P("teto_mobs_mundo", 500, 300, 160, 20, 5000, "Teto global de mobs por dimensao");
        ANIMAL_CHUNK = P("animais_por_chunk", 20, 12, 8, 2, 200, "Animais por chunk");
        FOLLOW = P("pathfinding_alcance", 24, 16, 10, 4, 64, "Alcance de perseguicao dos mobs novos");
        REAP = P("reaper_margem", 4, 2, 1, 1, 16, "Chunks de margem alem da view distance");
        ARROW = P("flecha_vida", 600, 200, 100, 20, 1200, "Flechas somem depois de N ticks");
        EXPL = P("explosoes_por_tick", 12, 6, 3, 1, 200, "Max de explosoes por tick");
        RTS = P("random_tick_speed", 2, 1, 1, 0, 20, "randomTickSpeed");
        SLIME_CAP = P("slimes_area", 16, 10, 6, 2, 200, "Slimes por area (excesso nao nasce)");
        TNT_CAP = P("tnt_area", 40, 24, 12, 2, 500, "TNT aceso por area (excesso some)");
        PROJ_CAP = P("projeteis_teto", 80, 40, 20, 5, 1000, "Projeteis por dimensao");

        CULL_LIVING = P("cull_vivos", 128, 64, 32, 4, 512, "Mobs so desenhados ate aqui");
        CULL_DYN = P("cull_outras", 96, 48, 24, 4, 512, "Itens e flechas so desenhados ate aqui");
        CULL_TE = P("cull_te", 64, 32, 16, 4, 512, "Blocos especiais so desenhados ate aqui");
        FR_LIVING = P("frame_vivos", 80, 40, 20, 5, 1000, "Max de mobs desenhados por frame");
        FR_DYN = P("frame_outras", 100, 50, 25, 5, 1000, "Max de itens desenhados por frame");
        FR_TE = P("frame_te", 120, 60, 30, 5, 1000, "Max de blocos especiais por frame");
        NAME_DIST = P("nametag_dist", 24, 12, 0, 0, 128, "Nome de mobs so ate aqui (0 = nunca)");
        PART_DROP = P("particulas_descarte_pct", 15, 40, 75, 1, 95, "% de particulas descartadas ao nascer");
        PART_DIST = P("particulas_dist", 48, 32, 20, 4, 256, "Particulas alem disso nao nascem");
        PART_CAP = P("particulas_teto", 400, 200, 80, 20, 4000, "Teto de particulas");
        RD_MAX = P("render_dist_max", 10, 6, 4, 2, 32, "Teto da render distance");
        FOV_MAX = P("fov_max", 80, 70, 60, 30, 110, "Teto do FOV em graus");
        UNFOCUS = P("sem_foco_ms", 30, 80, 150, 5, 1000, "Pausa por frame com a janela sem foco");
        CL_TICK = P("cliente_tick_dist", 64, 40, 24, 8, 256, "Mobs alem disso atualizam 1 a cada 4 ticks no cliente");
        FPS_CAP = P("fps_limite", 60, 50, 40, 15, 260, "Limite de FPS (se o modulo estiver ligado)");

        String[] junk = cfg.getStringList("itens_lixo", "geral", new String[] {"minecraft:cobblestone", "minecraft:dirt",
            "minecraft:gravel", "minecraft:netherrack", "minecraft:rotten_flesh", "minecraft:wheat_seeds", "minecraft:sapling",
            "minecraft:stone"}, "Itens que somem rapido no chao (nome de registro)");
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
            ClientCommandHandler.instance.registerCommand(new CmdLiotm());
        }
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new CmdLiotm());
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent e) {
        pendingApply = true;
    }

    static void saveState() {
        if (cfg == null) return;
        cfg.get("geral", "nivel_inicial", 0).set(level);
        for (int i = 0; i < count; i++) cfg.get("modulos", modKey(i), EN[i]).set(EN[i]);
        cfg.save();
    }

    static void setLevel(int n) {
        int old = level;
        level = n < 0 ? 0 : (n > 3 ? 3 : n);
        saveState();
        pendingApply = true;
        if (level >= 3 && old < level) System.gc();
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

    // ================= SNAPSHOT DE JOGADORES =================
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

    static double sq(double v) {
        return v * v;
    }

    // ================= SERVIDOR: TILE ENTITIES DORMINDO =================
    static final Map<Integer, List<TileEntity>> SLEEP = new HashMap<Integer, List<TileEntity>>();
    static final Map<Class<?>, Integer> TE_GROUP = new HashMap<Class<?>, Integer>();

    static List<TileEntity> sleepList(int dim) {
        List<TileEntity> l = SLEEP.get(Integer.valueOf(dim));
        if (l == null) {
            l = new ArrayList<TileEntity>();
            SLEEP.put(Integer.valueOf(dim), l);
        }
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
    static void sleepTEs(WorldServer w, int lv) {
        List list = w.loadedTileEntityList;
        Set<TileEntity> rm = new HashSet<TileEntity>();
        for (int i = 0; i < list.size(); i++) {
            TileEntity te = (TileEntity) list.get(i);
            int g = teGroup(te);
            int dist = 0;
            if (g == 1 && on(M_TE_CHEST)) dist = TE_CHEST[lv];
            else if (g == 2 && on(M_TE_HOPPER)) dist = TE_HOPPER[lv];
            else if (g == 3 && on(M_TE_MODS)) dist = TE_MODS[lv];
            if (dist <= 0) continue;
            double n = nearestSq(w, te.xCoord + 0.5, te.yCoord + 0.5, te.zCoord + 0.5);
            if (n == -2.0) return;
            if (n < 0 || n > sq(dist)) rm.add(te);
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

    // ================= SERVIDOR: ITENS, XP, MOBS =================
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

    static void itemsFar(WorldServer w, int dist) {
        double dsq = sq(dist);
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (!(o instanceof EntityItem)) continue;
            EntityItem it = (EntityItem) o;
            if (it.isDead || it.age < 600) continue;
            double n = nearestSq(w, it.posX, it.posY, it.posZ);
            if (n == -2.0) return;
            if (n >= 0 && n > dsq) {
                it.setDead();
                itemsFarKilled++;
            }
        }
    }

    static void orbsFar(WorldServer w, int dist) {
        double dsq = sq(dist);
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (!(o instanceof EntityXPOrb)) continue;
            EntityXPOrb orb = (EntityXPOrb) o;
            if (orb.isDead) continue;
            double n = nearestSq(w, orb.posX, orb.posY, orb.posZ);
            if (n == -2.0) return;
            if (n >= 0 && n > dsq) {
                orb.setDead();
                orbsFarKilled++;
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

    static void projCap(WorldServer w, int cap) {
        List<Entity> l = new ArrayList<Entity>();
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if ((o instanceof EntityThrowable || o instanceof EntityFireball) && !((Entity) o).isDead) l.add((Entity) o);
        }
        int ex = l.size() - cap;
        if (ex <= 0) return;
        Collections.sort(l, new Comparator<Entity>() {
            @Override
            public int compare(Entity a, Entity b) {
                return b.ticksExisted - a.ticksExisted;
            }
        });
        for (int i = 0; i < ex; i++) {
            l.get(i).setDead();
            projKilled++;
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

    static void trimOld(WorldServer w, int minAge) {
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (o instanceof EntityItem && ((EntityItem) o).age > minAge) ((EntityItem) o).setDead();
            else if (o instanceof EntityArrow && !((EntityArrow) o).isDead) ((EntityArrow) o).setDead();
        }
    }

    static void capFollow(EntityLiving el, int cap) {
        IAttributeInstance a = el.getEntityAttribute(SharedMonsterAttributes.followRange);
        if (a == null || a.getModifier(FOLLOW_ID) != null) return;
        double base = a.getBaseValue();
        if (base <= cap) return;
        a.applyModifier(new AttributeModifier(FOLLOW_ID, "liotm follow", -(1.0 - cap / base), 1).setSaved(false));
        followCut++;
    }

    static final Map<Integer, Integer> IDLE_DIMS = new HashMap<Integer, Integer>();

    static void unloadIdleDims() {
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null) return;
        if (!on(M_DIMS)) {
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

    static void applyServer() {
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null) return;
        WorldServer w0 = srv.worldServerForDimension(0);
        if (w0 == null) return;
        GameRules gr = w0.getGameRules();
        boolean save = false;
        if (origRts == null) {
            origRts = gr.getGameRuleStringValue("randomTickSpeed");
            if (origRts == null || origRts.length() == 0) origRts = "3";
            cfg.get("interno", "randomTickSpeed_original", "").set(origRts);
            save = true;
        }
        if (origFire == null) {
            origFire = gr.getGameRuleStringValue("doFireTick");
            if (origFire == null || origFire.length() == 0) origFire = "true";
            cfg.get("interno", "doFireTick_original", "").set(origFire);
            save = true;
        }
        if (origGrief == null) {
            origGrief = gr.getGameRuleStringValue("mobGriefing");
            if (origGrief == null || origGrief.length() == 0) origGrief = "true";
            cfg.get("interno", "mobGriefing_original", "").set(origGrief);
            save = true;
        }
        if (save) cfg.save();
        gr.setOrCreateGameRule("randomTickSpeed", on(M_RTS) ? String.valueOf(RTS[level]) : origRts);
        gr.setOrCreateGameRule("doFireTick", on(M_FIRE) ? "false" : origFire);
        gr.setOrCreateGameRule("mobGriefing", on(M_GRIEF) ? "false" : origGrief);
        for (WorldServer w : srv.worldServers) {
            if (w != null) wake(w);
        }
    }

    // ================= SERVIDOR: EVENTOS =================
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
            if (on(M_EMERG) && msptShort > emergMspt && System.currentTimeMillis() > emergencyUntil) {
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
                if (EN[M_AUTO]) {
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
            if (on(M_TE_CHEST) || on(M_TE_HOPPER) || on(M_TE_MODS)) {
                if (t % 20 == 0) wake(w);
                else if (t % 20 == 1) sleepTEs(w, lv);
            }
            if (on(M_ITEM_MERGE) && t % 40 == 7) mergeItems(w, ITEM_MERGE[lv]);
            if (on(M_ITEM_FAR) && t % 100 == 9) itemsFar(w, ITEM_FAR[lv]);
            if (on(M_XP_FAR) && t % 100 == 11) orbsFar(w, XP_FAR[lv]);
            if (on(M_MOB_CHUNK) && t % 100 == 13) mobCap(w, MOB_CHUNK[lv], false);
            if (on(M_ANIMAL_CHUNK) && t % 100 == 17) mobCap(w, ANIMAL_CHUNK[lv], true);
            if (on(M_MOB_WORLD) && t % 200 == 21) entityCap(w, MOB_WORLD[lv]);
            if (on(M_REAPER) && t % 100 == 57) reap(w, REAP[lv]);
            if (on(M_WEATHER_S) && t % 100 == 3) clearWeather(w);
            if (on(M_ARROW) && t % 100 == 31) arrowReap(w, ARROW[lv]);
            if (on(M_PROJ) && t % 100 == 41) projCap(w, PROJ_CAP[lv]);
        }

        @SubscribeEvent
        public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
            int lv = level;
            if (lv == 0) return;
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
            int iv = 1;
            if (on(M_FREEZE) && (n < 0 || n > sq(FREEZE_DIST[lv]))) {
                iv = 16;
            } else {
                if (on(M_SLOW)) {
                    int tlog = TICK_LOG[lv];
                    int tier = n < 0 ? tlog : (int) (Math.sqrt(n) / TICK_DIST[lv]);
                    if (tier > 0) iv = 1 << Math.min(tier, tlog);
                }
                if (en instanceof EntityVillager) {
                    if (on(M_VILLAGER) && (n < 0 || n > sq(VIL_DIST[lv]))) iv = Math.max(iv, VIL_IV[lv]);
                } else if (en instanceof EntityAnimal) {
                    if (on(M_ANIMSLOW) && (n < 0 || n > sq(ANIM_DIST[lv]))) iv = Math.max(iv, ANIM_IV[lv]);
                }
            }
            if (iv > 1 && (en.worldObj.getTotalWorldTime() + en.getEntityId()) % iv != 0) {
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
                if (on(M_ITEM_LIFE) && it.lifespan > ITEM_LIFE[lv]) it.lifespan = ITEM_LIFE[lv];
                if (on(M_JUNK) && it.getEntityItem() != null) {
                    Object nm = Item.itemRegistry.getNameForObject(it.getEntityItem().getItem());
                    if (nm != null && JUNK.contains(nm.toString())) it.lifespan = Math.min(it.lifespan, JUNK_LIFE[lv]);
                }
                if (on(M_ITEM_CAP)) {
                    List l = e.world.getEntitiesWithinAABB(EntityItem.class, it.boundingBox.expand(8.0, 4.0, 8.0));
                    if (l.size() > ITEM_CAP[lv]) it.lifespan = Math.min(it.lifespan, 200);
                }
            } else if (en instanceof EntityXPOrb) {
                if (on(M_XP_MERGE)) {
                    EntityXPOrb orb = (EntityXPOrb) en;
                    List l = e.world.getEntitiesWithinAABB(EntityXPOrb.class, orb.boundingBox.expand(6.0, 3.0, 6.0));
                    if (l.size() >= XP_CAP[lv]) {
                        EntityXPOrb target = (EntityXPOrb) l.get(0);
                        if (target != orb && !target.isDead) {
                            target.xpValue += orb.xpValue;
                            orbsMerged++;
                            e.setCanceled(true);
                        }
                    }
                }
            } else if (en instanceof EntityBat || en instanceof EntitySquid) {
                if (on(M_BLACKLIST)) {
                    e.setCanceled(true);
                    spawnsDenied++;
                }
            } else if (en instanceof EntitySlime) {
                if (on(M_SLIME)) {
                    List l = e.world.getEntitiesWithinAABB(EntitySlime.class, en.boundingBox.expand(16.0, 8.0, 16.0));
                    if (l.size() > SLIME_CAP[lv]) {
                        e.setCanceled(true);
                        slimeDenied++;
                    }
                }
            } else if (en instanceof EntityTNTPrimed) {
                if (on(M_TNT)) {
                    List l = e.world.getEntitiesWithinAABB(EntityTNTPrimed.class, en.boundingBox.expand(12.0, 8.0, 12.0));
                    if (l.size() > TNT_CAP[lv]) {
                        e.setCanceled(true);
                        tntDenied++;
                    }
                }
            } else if (en instanceof EntityLiving && !(en instanceof IBossDisplayData)) {
                if (on(M_FOLLOW)) capFollow((EntityLiving) en, FOLLOW[lv]);
            }
        }

        @SubscribeEvent
        public void onCheckSpawn(LivingSpawnEvent.CheckSpawn e) {
            int lv = level;
            if (lv == 0 || e.world.isRemote) return;
            if (on(M_EMERG) && System.currentTimeMillis() < emergencyUntil) {
                e.setResult(Event.Result.DENY);
                spawnsDenied++;
                return;
            }
            if (!on(M_SPAWN)) return;
            double p = SPAWN_DENY[lv] / 100.0;
            if (!(e.entityLiving instanceof IMob)) p = p / 2.0;
            if (RND.nextDouble() < p) {
                e.setResult(Event.Result.DENY);
                spawnsDenied++;
            }
        }

        @SubscribeEvent
        public void onExplosionStart(ExplosionEvent.Start e) {
            int lv = level;
            if (lv == 0 || e.world.isRemote || !on(M_EXPL)) return;
            if (++explThisTick > EXPL[lv]) {
                e.setCanceled(true);
                explosionsCut++;
            }
        }

        @SubscribeEvent
        public void onAllowDespawn(LivingSpawnEvent.AllowDespawn e) {
            int lv = level;
            if (lv == 0 || e.world.isRemote || !on(M_DESPAWN)) return;
            if (!(e.entityLiving instanceof EntityLiving)) return;
            EntityLiving el = (EntityLiving) e.entityLiving;
            if (el.hasCustomNameTag() || el.getLeashed()) return;
            double n = nearestSq(e.world, el.posX, el.posY, el.posZ);
            if (n == -2.0) return;
            if (n < 0 || n > sq(DESPAWN[lv])) {
                e.setResult(Event.Result.ALLOW);
                despawned++;
            }
        }
    }

    // ================= WORLDGEN ENXUTO =================
    public static class TerrainEvents {
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
            int lv = level;
            if (lv == 0) return;
            String n = e.type.name();
            boolean deny = false;
            if (on(M_WG_LAKES) && n.equals("LAKE")) deny = true;
            if (on(M_WG_DECOR) && ((lv >= 2 && D2.contains(n)) || (lv >= 3 && D3.contains(n)))) deny = true;
            if (deny) {
                e.setResult(Event.Result.DENY);
                genDenied++;
            }
        }

        @SubscribeEvent
        public void onPopulate(PopulateChunkEvent.Populate e) {
            int lv = level;
            if (lv == 0 || !on(M_WG_LAKES)) return;
            String n = e.type.name();
            if (P1.contains(n) || (lv >= 3 && P3.contains(n))) {
                e.setResult(Event.Result.DENY);
                genDenied++;
            }
        }
    }

    // ================= CLIENTE =================
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
            if (on(M_CULL_DYN) && d > sq(CULL_DYN[lv])) return true;
            return on(M_FR_DYN) && ++frameDyn > FR_DYN[lv] && d > 36.0;
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
            if (level > 0 && on(M_CULL_DYN) && x * x + y * y + z * z > sq(CULL_DYN[level])) return;
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
                if (on(M_CULL_TE) && d > sq(CULL_TE[lv])) {
                    cullTe++;
                    return;
                }
                if (on(M_FR_TE) && ++frameTe > FR_TE[lv] && d > 36.0) {
                    cullTe++;
                    return;
                }
            }
            inner.renderTileEntityAt(te, x, y, z, pt);
        }
    }

    /** Filtra particulas na origem: por chance e por distancia da camera. */
    @SideOnly(Side.CLIENT)
    public static class ThrottledFx extends EffectRenderer {
        public ThrottledFx(World w, TextureManager tm) {
            super(w, tm);
        }

        @Override
        public void addEffect(EntityFX fx) {
            int lv = level;
            if (lv > 0) {
                if (on(M_PART_DROP) && RND.nextInt(100) < PART_DROP[lv]) {
                    partDropped++;
                    return;
                }
                if (on(M_PART_DIST)) {
                    Entity v = Minecraft.getMinecraft().renderViewEntity;
                    if (v != null && fx.getDistanceSqToEntity(v) > sq(PART_DIST[lv])) {
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
        int appliedKey = 0;
        boolean firstApply = true;
        WorldProvider lastProv = null;
        boolean wrapped = false;
        boolean captured = false;
        boolean animKilled = false;
        boolean oFancy, oClouds, oBobbing, oSnooper, oAdv;
        float oFov;
        int oAO, oParticles, oRender, oMip, oAni, oLimit;
        int partTick = 0, lowFps = 0, highFps = 0, memStrikes = 0, gcCountdown = -1;
        long lastMemCut = 0, lastGc = 0;

        @SubscribeEvent
        public void onRenderLiving(RenderLivingEvent.Pre e) {
            int lv = level;
            if (lv == 0) return;
            if (e.entity instanceof EntityPlayer || e.entity instanceof IBossDisplayData) return;
            double d = e.x * e.x + e.y * e.y + e.z * e.z;
            if (on(M_CULL_LIVING) && d > sq(CULL_LIVING[lv])) {
                e.setCanceled(true);
                cullLiving++;
                return;
            }
            if (on(M_FR_LIVING) && ++frameLiving > FR_LIVING[lv] && d > 100.0) {
                e.setCanceled(true);
                cullLiving++;
            }
        }

        @SubscribeEvent
        public void onClientLiving(LivingEvent.LivingUpdateEvent e) {
            if (!on(M_CL_TICK)) return;
            EntityLivingBase en = e.entityLiving;
            if (en.worldObj == null || !en.worldObj.isRemote) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || en instanceof EntityPlayer || en instanceof IBossDisplayData) return;
            if (en.getDistanceSqToEntity(mc.thePlayer) > sq(CL_TICK[level])
                && (en.worldObj.getTotalWorldTime() + en.getEntityId()) % 4 != 0) {
                e.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void onHand(RenderHandEvent e) {
            if (on(M_HAND)) e.setCanceled(true);
        }

        @SubscribeEvent
        public void onHighlight(DrawBlockHighlightEvent e) {
            if (on(M_HAND)) e.setCanceled(true);
        }

        @SubscribeEvent
        public void onNameTags(RenderLivingEvent.Specials.Pre e) {
            if (!on(M_NAMETAGS)) return;
            int nd = NAME_DIST[level];
            double d = e.x * e.x + e.y * e.y + e.z * e.z;
            if (nd <= 0 || d > sq(nd)) e.setCanceled(true);
        }

        @SuppressWarnings("unchecked")
        void wrapRenderers() {
            try {
                Map m = RenderManager.instance.entityRenderMap;
                for (Object o : new ArrayList(m.entrySet())) {
                    Map.Entry en = (Map.Entry) o;
                    Object v = en.getValue();
                    if (!(v instanceof Render) || v instanceof RendererLivingEntity || v instanceof CullRender) continue;
                    if (!v.getClass().getName().startsWith("net.minecraft.")) continue;
                    m.put(en.getKey(), new CullRender((Render) v));
                }
            } catch (Throwable t) {
                System.out.println("[LIOTM] culling de entidades nao-vivas indisponivel: " + t);
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
                System.out.println("[LIOTM] culling de tile entities indisponivel: " + t);
            }
        }

        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                frameLiving = 0;
                frameDyn = 0;
                frameTe = 0;
                if (on(M_UNFOCUS) && !Display.isActive()) {
                    try {
                        Thread.sleep(UNFOCUS[level]);
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
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.theWorld != null && mc.currentScreen == null) {
                    if (statSkip > 0) statSkip--;
                    else {
                        fpsSum[level] += fps;
                        fpsCnt[level]++;
                    }
                }
                autoClient();
                memGuard(now);
            }
        }

        void memGuard(long now) {
            if (!on(M_MEM)) return;
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
            if (!EN[M_AUTO]) return;
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
        public void onWorldLoad(WorldEvent.Load e) {
            if (e.world.isRemote && on(M_GC)) gcCountdown = 300;
        }

        @SubscribeEvent
        public void onWorldUnload(WorldEvent.Unload e) {
            if (e.world.isRemote && on(M_GC)) System.gc();
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
            if (!(mc.effectRenderer instanceof ThrottledFx)) {
                mc.effectRenderer = new ThrottledFx(mc.theWorld, mc.renderEngine);
            }
            if (gcCountdown > 0 && --gcCountdown == 0) System.gc();

            int key = stateKey();
            if (firstApply || key != appliedKey) {
                firstApply = false;
                appliedKey = key;
                statSkip = 4;
                applyClient(mc);
                applySky(mc);
                lastProv = mc.theWorld.provider;
            } else if (lastProv != mc.theWorld.provider) {
                applySky(mc);
                lastProv = mc.theWorld.provider;
            }
            if (on(M_RAIN_C)) {
                mc.theWorld.setRainStrength(0.0F);
                mc.theWorld.setThunderStrength(0.0F);
            }
            if (on(M_PART_CAP) && ++partTick >= 20) {
                partTick = 0;
                try {
                    int n = Integer.parseInt(mc.effectRenderer.getStatistics().trim());
                    if (n > PART_CAP[level]) {
                        mc.effectRenderer.clearEffects(mc.theWorld);
                        partCleared++;
                    }
                } catch (Exception ex) {
                    // ignora
                }
            }
        }

        void applyClient(Minecraft mc) {
            GameSettings gs = mc.gameSettings;
            int lv = level;
            if (!captured) {
                captured = true;
                oFancy = gs.fancyGraphics;
                oClouds = gs.clouds;
                oBobbing = gs.viewBobbing;
                oSnooper = gs.snooperEnabled;
                oAO = gs.ambientOcclusion;
                oParticles = gs.particleSetting;
                oRender = gs.renderDistanceChunks;
                oAdv = gs.advancedOpengl;
                oFov = gs.fovSetting;
                oMip = gs.mipmapLevels;
                oAni = gs.anisotropicFiltering;
                oLimit = gs.limitFramerate;
            }
            gs.fancyGraphics = on(M_FASTGFX) ? false : oFancy;
            gs.ambientOcclusion = on(M_AO) ? 0 : oAO;
            gs.clouds = on(M_CLOUDS) ? false : oClouds;
            gs.viewBobbing = on(M_BOB) ? false : oBobbing;
            gs.snooperEnabled = on(M_SNOOPER) ? false : oSnooper;
            gs.advancedOpengl = on(M_ADV) ? false : oAdv;
            gs.particleSetting = on(M_PART_MIN) ? Math.max(oParticles, 2) : oParticles;
            gs.renderDistanceChunks = on(M_RD) ? Math.max(2, Math.min(oRender, RD_MAX[lv])) : oRender;
            gs.fovSetting = on(M_FOV) ? Math.min(oFov, (float) FOV_MAX[lv]) : oFov;
            gs.limitFramerate = on(M_FPSCAP) ? FPS_CAP[lv] : oLimit;

            boolean refresh = false;
            int wantMip = on(M_MIP) ? 0 : oMip;
            int wantAni = on(M_MIP) ? 1 : oAni;
            if (gs.mipmapLevels != wantMip) {
                gs.mipmapLevels = wantMip;
                refresh = true;
            }
            if (gs.anisotropicFiltering != wantAni) {
                gs.anisotropicFiltering = wantAni;
                refresh = true;
            }
            boolean wantAnimOff = on(M_ANIM);
            if (!wantAnimOff && animKilled) {
                animKilled = false;
                refresh = true;
            }
            if (refresh) mc.refreshResources();
            if (wantAnimOff && (!animKilled || refresh)) {
                killAnimations(mc);
                animKilled = true;
            }
            mc.renderGlobal.loadRenderers();
        }

        void killAnimations(Minecraft mc) {
            try {
                TextureMap tm = mc.getTextureMapBlocks();
                List l = ReflectionHelper.getPrivateValue(TextureMap.class, tm, "listAnimatedSprites", "field_94258_i");
                if (l != null) l.clear();
            } catch (Throwable t) {
                System.out.println("[LIOTM] nao foi possivel desligar as texturas animadas: " + t);
            }
        }

        void applySky(Minecraft mc) {
            WorldProvider p = mc.theWorld.provider;
            boolean cl = on(M_SKYCLOUDS);
            boolean sk = on(M_SKY);
            if (p.getWeatherRenderer() == null || p.getWeatherRenderer() == NOOP) p.setWeatherRenderer(cl ? NOOP : null);
            if (p.getCloudRenderer() == null || p.getCloudRenderer() == NOOP) p.setCloudRenderer(cl ? NOOP : null);
            if (p.getSkyRenderer() == null || p.getSkyRenderer() == NOOP) p.setSkyRenderer(sk ? NOOP : null);
        }

        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Text e) {
            Minecraft mc = Minecraft.getMinecraft();
            if (!mc.gameSettings.showDebugInfo) return;
            e.left.add("");
            e.left.add("[LIOTM] nivel " + LVNAME[level] + (EN[M_AUTO] ? " (auto)" : "") + " | modulos ativos " + activeCount() + "/" + count
                + " | FPS " + fps + " | render " + mc.gameSettings.renderDistanceChunks + " | ocultos/s " + cullPerSec);
            e.left.add("[LIOTM] mspt " + String.format("%.1f", msptAvg) + " | ticks pulados/s " + skippedPerSec
                + " | TE dormindo " + totalSleeping() + " | particulas barradas " + partDropped);
        }
    }

    // ================= COMANDO /liotm (e /lm) =================
    public static class CmdLiotm extends CommandBase {
        @Override
        public String getCommandName() {
            return "liotm";
        }

        @Override
        public List getCommandAliases() {
            return Arrays.asList("lm");
        }

        @Override
        public String getCommandUsage(ICommandSender s) {
            return "/liotm <0-3> | lista [pagina] | mod <n> on|off | auto | status | perfil | limpar | ajuda";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender s) {
            if (s.getEntityWorld() != null && s.getEntityWorld().isRemote) return true;
            return s.canCommandSenderUseCommand(2, "liotm");
        }

        void say(ICommandSender s, String m) {
            s.addChatMessage(new ChatComponentText(m));
        }

        double avg(int lv) {
            return fpsCnt[lv] > 0 ? (double) fpsSum[lv] / fpsCnt[lv] : -1.0;
        }

        @Override
        public void processCommand(ICommandSender s, String[] a) {
            if (a.length == 0 || a[0].equalsIgnoreCase("ajuda") || a[0].equalsIgnoreCase("help")) {
                say(s, "liotm: /liotm <0-3> muda o nivel (0 desligado, 1 leve, 2 forte, 3 extremo)");
                say(s, "/liotm lista [pagina] mostra os " + count + " modulos | /liotm mod <n> on|off | auto | status | perfil | limpar");
                say(s, "Para medir o ganho: jogue 30 s no nivel 0, depois 30 s no nivel 3, e use /liotm status.");
                return;
            }
            if (a[0].equalsIgnoreCase("lista") || a[0].equalsIgnoreCase("list")) {
                int page = 1;
                if (a.length > 1) {
                    try {
                        page = Integer.parseInt(a[1]);
                    } catch (NumberFormatException ex) {
                        page = 1;
                    }
                }
                int pages = (count + 11) / 12;
                if (page < 1) page = 1;
                if (page > pages) page = pages;
                say(s, "Modulos (pagina " + page + " de " + pages + "), nivel atual " + LVNAME[level] + ":");
                for (int i = (page - 1) * 12; i < Math.min(count, page * 12); i++) {
                    String st = !EN[i] ? "[off]" : (level >= MIN[i] ? "[ATIVO]" : "[aguarda nv" + MIN[i] + "]");
                    say(s, (i + 1) + " " + st + " " + NAME[i] + (SRV[i] ? " (S)" : " (C)"));
                }
                return;
            }
            if (a[0].equalsIgnoreCase("mod")) {
                if (a.length < 3) {
                    say(s, "Uso: /liotm mod <numero> on|off");
                    return;
                }
                int n;
                try {
                    n = Integer.parseInt(a[1]);
                } catch (NumberFormatException ex) {
                    say(s, "Numero invalido.");
                    return;
                }
                if (n < 1 || n > count) {
                    say(s, "Modulo de 1 a " + count + ".");
                    return;
                }
                boolean v = a[2].equalsIgnoreCase("on") || a[2].equalsIgnoreCase("true") || a[2].equals("1");
                EN[n - 1] = v;
                saveState();
                pendingApply = true;
                say(s, "Modulo " + n + " (" + NAME[n - 1] + ") " + (v ? "ligado" : "desligado") + ".");
                return;
            }
            if (a[0].equalsIgnoreCase("auto")) {
                EN[M_AUTO] = !EN[M_AUTO];
                saveState();
                say(s, "Modo automatico " + (EN[M_AUTO] ? "LIGADO" : "desligado") + " (nivel entre " + autoMin + " e " + autoMax + ").");
                return;
            }
            if (a[0].equalsIgnoreCase("status")) {
                say(s, "liotm nivel " + LVNAME[level] + (EN[M_AUTO] ? " (auto)" : "") + " | modulos ativos " + activeCount() + "/"
                    + count + " | mspt " + String.format("%.1f", msptAvg) + " | FPS " + fps);
                StringBuilder sb = new StringBuilder("FPS medio por nivel: ");
                for (int lv = 0; lv < 4; lv++) {
                    sb.append(lv).append('=').append(avg(lv) < 0 ? "-" : String.format("%.0f", avg(lv))).append("  ");
                }
                say(s, sb.toString().trim());
                if (avg(0) > 0 && level > 0 && avg(level) > 0) {
                    say(s, "Ganho do nivel " + level + " sobre o nivel 0: " + String.format("%+.0f", avg(level) - avg(0)) + " FPS");
                }
                say(s, "Servidor: ticks pulados/s " + skippedPerSec + ", TE dormindo " + totalSleeping() + ", itens fundidos "
                    + itemsMerged + ", orbs fundidos " + orbsMerged + ", itens longe " + itemsFarKilled + ", orbs longe " + orbsFarKilled);
                say(s, "Spawns negados " + spawnsDenied + ", despawns " + despawned + ", mobs removidos " + (mobCapKilled + entCapKilled)
                    + ", chunks descarregados " + chunksReaped + ", worldgen cortado " + genDenied + ", pathfinding reduzido "
                    + followCut);
                say(s, "Projeteis removidos " + projKilled + ", slimes barrados " + slimeDenied + ", TNT barrado " + tntDenied
                    + ", explosoes cortadas " + explosionsCut + ", freios de emergencia " + emergencies + ", dimensoes descarregadas "
                    + dimsUnloaded);
                say(s, "Cliente: ocultos/s " + cullPerSec + ", particulas barradas " + partDropped + ", cortes por memoria " + memCuts
                    + ", ultima limpeza " + lastCleaned + " entidades");
                return;
            }
            if (a[0].equalsIgnoreCase("perfil") || a[0].equalsIgnoreCase("profile")) {
                profileSender = s;
                say(s, "Gerando perfil no proximo tick do servidor...");
                return;
            }
            if (a[0].equalsIgnoreCase("limpar") || a[0].equalsIgnoreCase("clean")) {
                pendingClean = true;
                say(s, "Limpeza agendada: itens no chao, orbs de XP e flechas somem no proximo tick.");
                return;
            }
            int n;
            try {
                n = Integer.parseInt(a[0]);
            } catch (NumberFormatException ex) {
                say(s, "Uso: " + getCommandUsage(s));
                return;
            }
            if (n < 0 || n > 3) {
                say(s, "Nivel de 0 a 3.");
                return;
            }
            setLevel(n);
            say(s, "liotm nivel " + LVNAME[level] + " ativado (" + activeCount() + " modulos agindo).");
        }
    }
}
