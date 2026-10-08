package com.myname.mymodid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import org.lwjgl.opengl.Display;

/**
 * OtmclMod - otimizacao drastica para Minecraft 1.7.10 (Forge, arquivo unico).
 *
 * <p>Filosofia: em vez de "ajustar" o jogo, o mod REMOVE o que e inutil e CORTA trabalho que ninguem ve.
 * Tudo e controlado por 6 niveis (0 = vanilla ... 5 = maximo) e cada numero de cada nivel pode ser
 * editado no arquivo de config (secao "niveis"). Comando: /otmcl.
 *
 * <p>SERVIDOR (roda no servidor dedicado e no mundo local):
 * <ul>
 * <li>S1 Ticking adaptativo: mobs distantes do jogador tickam a cada 2/4/8/16/32 ticks.</li>
 * <li>S2 TE Sleeper: bau, bau do fim e mesa de encantamento distantes saem da lista de tick.</li>
 * <li>S3 Fusao de itens no chao por celula, vida curta e limite de itens por area.</li>
 * <li>S4 Fusao de orbs de XP (sem perder XP).</li>
 * <li>S5 Menos spawn natural, lista negra de mobs inuteis (morcego, lula) e despawn por distancia.</li>
 * <li>S6 Limite de mobs por chunk (remove os mais distantes dos jogadores).</li>
 * <li>S7 Chunk Reaper: descarrega chunks carregados longe de todos os jogadores.</li>
 * <li>S8 Gamerules: randomTickSpeed menor, doFireTick desligado, clima desligado.</li>
 * <li>S9 Worldgen enxuto: sem lagos, dungeons, argila, areia, cogumelos, flores, etc. em chunks NOVOS.</li>
 * <li>S10 Modo auto: sobe/desce o nivel sozinho conforme mspt (servidor) e FPS (cliente).</li>
 * <li>S11 Flechas: removidas depois de N ticks (as cravadas no chao acumulam em farms e arenas).</li>
 * <li>S12 /otmcl limpar: limpeza manual de itens, orbs e flechas de todos os mundos.</li>
 * </ul>
 *
 * <p>CLIENTE:
 * <ul>
 * <li>C1 Pacote de graficos (nuvens, sombras, fancy, AO, particulas, render distance, snooper).</li>
 * <li>C2 Culling de entidades vivas por distancia e nametags.</li>
 * <li>C3 Culling de entidades nao vivas (itens, flechas, XP, TNT, barcos...) por distancia.</li>
 * <li>C4 Culling de TileEntity renderers vanilla por distancia.</li>
 * <li>C5 Limite de particulas.</li>
 * <li>C6 Ceu, clima e nuvens desligados (so cor de fundo) nos niveis altos.</li>
 * <li>C7 Texturas animadas desligadas (agua, lava, fogo, portal) nos niveis altos.</li>
 * <li>C8 Metricas no F3.</li>
 * <li>C9 Janela sem foco: o jogo dorme entre frames (CPU/GPU quase zero quando voce esta em outra janela).</li>
 * </ul>
 *
 * <p>AVISO: os niveis altos mudam o comportamento do jogo (mobs congelados longe, farms mais lentas,
 * itens que somem rapido, mundo novo mais "pelado"). Use /otmcl 0 para voltar ao vanilla.
 */
@Mod(modid = OtmclMod.MODID, name = "Otmcl Mod", version = "1.0", acceptedMinecraftVersions = "[1.7.10]")
public class OtmclMod {

    public static final String MODID = "otmclmod";

    // ============================================================================================
    // TABELAS DE NIVEL (indice = nivel 0..5). Valores padrao; todos editaveis no config.
    // ============================================================================================
    static final String[] NAME = {"0 vanilla", "1 leve", "2 medio", "3 forte", "4 extremo", "5 maximo"};

    // --- servidor ---
    static int[] TICK_DIST;    // blocos por "degrau" de ticking (0 = desligado)
    static int[] TICK_LOG;     // maior degrau: intervalo maximo = 2^TICK_LOG
    static int[] ITEM_LIFE;    // vida do item no chao (ticks)
    static int[] ITEM_CAP;     // itens por area de 16x8x16 antes do excesso sumir em 10s
    static int[] ITEM_MERGE;   // tamanho (blocos) da celula de fusao de itens (0 = desligado)
    static int[] XP_CAP;       // orbs de xp por area antes de fundir
    static int[] SPAWN_DENY;   // % de spawns naturais negados
    static int[] DESPAWN_DIST; // despawn forcado de mobs nao persistentes alem disto (blocos)
    static int[] MOB_CAP;      // mobs por chunk (0 = sem limite)
    static int[] REAP_MARGIN;  // chunks alem da view distance que o reaper preserva (0 = desligado)
    static int[] TE_SLEEP;     // distancia a partir da qual TE "dorminhocas" param de tickar
    static int[] FIRE_OFF;     // 1 = doFireTick false
    static int[] RTS;          // randomTickSpeed (-1 = nao mexe)
    static int[] WEATHER_OFF;  // 1 = chuva/trovoes desligados no servidor
    static int[] WG_TRIM;      // 0..3 quanto da decoracao de worldgen e removida
    static int[] BLACKLIST_LV; // 0 nada, 1 morcegos, 2 morcegos + lulas
    static int[] ARROW_LIFE;   // flechas somem depois deste numero de ticks (0 = vanilla)
    // --- cliente ---
    static int[] CULL_LIVING;  // distancia maxima de render de entidades vivas
    static int[] CULL_DYN;     // distancia maxima de render de entidades nao vivas
    static int[] CULL_TE;      // distancia maxima de render de tile entities vanilla
    static int[] NAMETAG;      // 0 normal, 1 so ate 12 blocos, 2 nunca
    static int[] PART_CAP;     // teto de particulas (0 = sem teto)
    static int[] PART_MODE;    // 0 todas, 1 reduzidas, 2 minimas
    static int[] RD_CAP;       // teto da render distance
    static int[] SKY_MODE;     // 0 normal, 1 sem clima/nuvens, 2 sem clima/nuvens/ceu
    static int[] ANIM_KILL;    // 1 = desliga texturas animadas
    static int[] UNFOCUS_MS;   // ms de pausa por frame quando a janela esta sem foco (0 = desligado)

    // strings de config
    static String[] sleepableTE = {"TileEntityChest", "TileEntityEnderChest", "TileEntityEnchantmentTable"};
    static boolean wrapModded = false;

    // ============================================================================================
    // ESTADO GLOBAL
    // ============================================================================================
    static volatile int level = 0;
    static volatile boolean auto = false;
    static volatile boolean pendingApply = true;
    static volatile boolean pendingClean = false;
    static int autoMin = 0;
    static int autoMax = 5;
    static Configuration cfg;
    static final Random RND = new Random();

    // valores originais guardados para poder voltar ao vanilla
    static String origRts = null;
    static String origFire = null;

    // metricas (aproximadas; contadores simples)
    static volatile double msptAvg = 0;
    static volatile int fps = 0;
    static int skippedTicks = 0, skippedPerSec = 0;
    static int cullLiving = 0, cullDyn = 0, cullTe = 0, cullPerSec = 0;
    static int itemsMerged = 0, orbsMerged = 0, spawnsDenied = 0, despawned = 0;
    static int mobCapKilled = 0, chunksReaped = 0, teSlept = 0, genDenied = 0, partCleared = 0;
    static int arrowsRemoved = 0, lastCleaned = 0;

    // ============================================================================================
    // CONFIG
    // ============================================================================================
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
        CULL_LIVING = table("cull_vivos", new int[] {0, 128, 96, 72, 48, 32}, 0, 512, "Distancia de render de entidades vivas");
        CULL_DYN = table("cull_outras", new int[] {0, 96, 72, 56, 40, 28}, 0, 512, "Distancia de render de itens/flechas/etc");
        CULL_TE = table("cull_te", new int[] {0, 0, 64, 48, 32, 24}, 0, 512, "Distancia de render de tile entities (0 desliga)");
        NAMETAG = table("nametag", new int[] {0, 0, 0, 1, 1, 2}, 0, 2, "0 normal, 1 so ate 12 blocos, 2 nunca");
        PART_CAP = table("particulas_teto", new int[] {0, 0, 400, 250, 150, 80}, 0, 4000, "Teto de particulas (0 desliga)");
        PART_MODE = table("particulas_modo", new int[] {0, 0, 1, 2, 2, 2}, 0, 2, "0 todas, 1 reduzidas, 2 minimas");
        RD_CAP = table("render_dist_max", new int[] {32, 16, 12, 8, 6, 4}, 2, 32, "Teto da render distance");
        SKY_MODE = table("ceu_modo", new int[] {0, 0, 0, 1, 1, 2}, 0, 2, "0 normal, 1 sem clima/nuvens, 2 sem ceu tambem");
        ARROW_LIFE = table("flecha_vida", new int[] {0, 0, 0, 600, 300, 100}, 0, 1200, "Flechas somem depois de N ticks (0 desliga)");
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
        pendingApply = true; // o servidor aplica no proprio thread; o cliente aplica no proprio tick
        if (level >= 4 && old < level) System.gc(); // limpeza unica ao subir para nivel alto
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

    /** Usado pelo modo auto. dir = +1 sobe nivel, -1 desce. Cooldown em ms. */
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
    // SNAPSHOT DE JOGADORES (evita varrer a lista de players para cada entidade)
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

    /** Devolve as TEs dormindo para a lista de tick do mundo (seguro contra chunks descarregados). */
    @SuppressWarnings("unchecked")
    static void wake(WorldServer w) {
        List<TileEntity> sl = SLEEP.get(Integer.valueOf(w.provider.dimensionId));
        if (sl == null || sl.isEmpty()) return;
        for (TileEntity te : sl) {
            if (te.isInvalid()) continue;
            if (!w.blockExists(te.xCoord, te.yCoord, te.zCoord)) continue;
            if (w.getTileEntity(te.xCoord, te.yCoord, te.zCoord) != te) continue; // chunk recarregado: outra instancia
            w.loadedTileEntityList.add(te);
        }
        sl.clear();
    }

    /** Tira da lista de tick as TEs "dorminhocas" que estao longe de todos os jogadores. */
    @SuppressWarnings("unchecked")
    static void sleep(WorldServer w, int dist) {
        double dsq = (double) dist * dist;
        List list = w.loadedTileEntityList;
        Set<TileEntity> rm = new HashSet<TileEntity>();
        for (int i = 0; i < list.size(); i++) {
            TileEntity te = (TileEntity) list.get(i);
            if (!sleepable(te)) continue;
            double n = nearestSq(w, te.xCoord + 0.5, te.yCoord + 0.5, te.zCoord + 0.5);
            if (n == -2.0) return; // sem snapshot: nao arrisca
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

    /** Funde pilhas de itens iguais que estao na mesma celula 3D. */
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

    /** Limite de mobs por chunk: remove os mais distantes dos jogadores. */
    static void mobCap(WorldServer w, int cap) {
        Map<Long, List<EntityLiving>> by = new HashMap<Long, List<EntityLiving>>();
        List all = w.loadedEntityList;
        for (int i = 0; i < all.size(); i++) {
            Object o = all.get(i);
            if (!(o instanceof EntityLiving)) continue;
            EntityLiving el = (EntityLiving) o;
            if (!capEligible(el)) continue;
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

    /** Descarrega chunks que ninguem usa (longe de todos os jogadores, sem ticket de chunkloader). */
    @SuppressWarnings("unchecked")
    static void reap(WorldServer w, int margin) {
        double[] a = SNAP.get(Integer.valueOf(w.provider.dimensionId));
        if (a == null || a.length == 0) return; // sem jogadores: deixa o vanilla decidir
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
            if (++done >= 64) break; // espalha o custo
        }
    }

    /** S11: remove flechas velhas (as cravadas no chao ficam 1200 ticks no vanilla). */
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

    /** S12: limpeza manual. Remove itens no chao, orbs de XP e flechas do mundo. */
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

    /** Aplica gamerules e acorda TEs; SEMPRE chamado no thread do servidor. */
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
            if (w != null) wake(w); // o ciclo de sleep decide de novo no proximo tick
        }
    }

    public static class ServerEvents {
        long t0 = 0;
        int counter = 0;
        int mem = 0;
        int lowMs = 0;
        int highMs = 0;

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                t0 = System.nanoTime();
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
            if (++counter >= 20) {
                counter = 0;
                skippedPerSec = skippedTicks;
                skippedTicks = 0;
                cullPerSec = cullLiving + cullDyn + cullTe;
                // modo auto (servidor): mspt alto por 10s sobe, baixo por 2min desce
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
            if (MOB_CAP[lv] > 0 && t % 100 == 13) mobCap(w, MOB_CAP[lv]);
            if (REAP_MARGIN[lv] > 0 && t % 100 == 57) reap(w, REAP_MARGIN[lv]);
            if (WEATHER_OFF[lv] > 0 && t % 100 == 3) clearWeather(w);
            if (ARROW_LIFE[lv] > 0 && t % 100 == 31) arrowReap(w, ARROW_LIFE[lv]);
        }

        /** S1: ticking adaptativo. Quanto mais longe do jogador, menos o mob e atualizado. */
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
            if (n < 0) tier = TICK_LOG[lv]; // dimensao sem jogadores: intervalo maximo
            else tier = (int) (Math.sqrt(n) / TICK_DIST[lv]);
            if (tier <= 0) return;
            int iv = 1 << Math.min(tier, TICK_LOG[lv]);
            if ((en.worldObj.getTotalWorldTime() + en.getEntityId()) % iv != 0) {
                e.setCanceled(true);
                skippedTicks++;
            }
        }

        /** S3/S4/S5: itens, XP e mobs inuteis ao entrar no mundo. */
        @SubscribeEvent
        public void onJoin(EntityJoinWorldEvent e) {
            int lv = level;
            if (lv == 0 || e.world.isRemote) return;
            Entity en = e.entity;
            if (en instanceof EntityItem) {
                EntityItem it = (EntityItem) en;
                if (it.lifespan > ITEM_LIFE[lv]) it.lifespan = ITEM_LIFE[lv];
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
            }
        }

        /** S5: menos spawn natural. */
        @SubscribeEvent
        public void onCheckSpawn(LivingSpawnEvent.CheckSpawn e) {
            int lv = level;
            if (lv == 0 || e.world.isRemote) return;
            double p = SPAWN_DENY[lv] / 100.0;
            if (p <= 0) return;
            if (!(e.entityLiving instanceof IMob)) p = p / 2.0;
            if (RND.nextDouble() < p) {
                e.setResult(Event.Result.DENY);
                spawnsDenied++;
            }
        }

        /** S5: despawn forcado por distancia para mobs nao persistentes. */
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
    // S9: WORLDGEN ENXUTO (so afeta chunks gerados DEPOIS de ativar; chunks antigos nao mudam)
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
            double r = CULL_DYN[lv];
            return r > 0 && x * x + y * y + z * z > r * r;
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
            if (far(x, y, z)) return;
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
                double r = CULL_TE[lv];
                if (r > 0 && x * x + y * y + z * z > r * r) {
                    cullTe++;
                    return;
                }
            }
            inner.renderTileEntityAt(te, x, y, z, pt);
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
        boolean oFancy, oClouds, oShadows, oBobbing, oSnooper;
        int oAO, oParticles, oRender;
        int partTick = 0;
        int lowFps = 0;
        int highFps = 0;

        // ---------- C2: entidades vivas ----------
        @SubscribeEvent
        public void onRenderLiving(RenderLivingEvent.Pre e) {
            int lv = level;
            if (lv == 0) return;
            if (e.entity instanceof EntityPlayer || e.entity instanceof IBossDisplayData) return;
            double r = CULL_LIVING[lv];
            if (r <= 0) return;
            double d = e.x * e.x + e.y * e.y + e.z * e.z;
            if (d > r * r) {
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

        // ---------- C3/C4: embrulha renderers vanilla com culling ----------
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

        // ---------- ticks ----------
        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                // C9: janela sem foco -> dorme entre frames para quase nao usar CPU/GPU
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
            }
        }

        /** Modo auto (cliente): FPS baixo por 5s sobe, FPS alto por 90s desce. */
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
            int lv = level;
            if (appliedLevel != lv) applyClient(mc, lv);
            if (lastProv != mc.theWorld.provider || skyLevel != lv) {
                applySky(mc, lv);
                lastProv = mc.theWorld.provider;
                skyLevel = lv;
            }
            // sem chuva/neve/raios desenhados nos niveis altos
            if (lv >= 4) {
                mc.theWorld.setRainStrength(0.0F);
                mc.theWorld.setThunderStrength(0.0F);
            }
            // C5: teto de particulas
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

        // ---------- C1: pacote de graficos ----------
        void applyClient(Minecraft mc, int lv) {
            GameSettings gs = mc.gameSettings;
            if (!captured) {
                captured = true;
                oFancy = gs.fancyGraphics;
                oClouds = gs.clouds;
                oShadows = gs.entityShadows;
                oBobbing = gs.viewBobbing;
                oSnooper = gs.snooperEnabled;
                oAO = gs.ambientOcclusion;
                oParticles = gs.particleSetting;
                oRender = gs.renderDistanceChunks;
            }
            if (lv == 0) {
                gs.fancyGraphics = oFancy;
                gs.clouds = oClouds;
                gs.entityShadows = oShadows;
                gs.viewBobbing = oBobbing;
                gs.snooperEnabled = oSnooper;
                gs.ambientOcclusion = oAO;
                gs.particleSetting = oParticles;
                gs.renderDistanceChunks = oRender;
            } else {
                gs.clouds = false;
                gs.entityShadows = false;
                gs.snooperEnabled = false;
                gs.fancyGraphics = lv >= 2 ? false : oFancy;
                gs.ambientOcclusion = lv >= 3 ? 0 : (lv == 2 ? Math.min(oAO, 1) : oAO);
                gs.particleSetting = Math.max(oParticles, PART_MODE[lv]);
                gs.renderDistanceChunks = Math.max(2, Math.min(oRender, RD_CAP[lv]));
                gs.viewBobbing = lv >= 5 ? false : oBobbing;
            }
            // C7: texturas animadas
            if (lv > 0 && ANIM_KILL[lv] > 0) {
                if (!animKilled) {
                    killAnimations(mc);
                    animKilled = true;
                }
            } else if (animKilled) {
                animKilled = false;
                mc.refreshResources(); // recarrega as texturas para as animacoes voltarem
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

        // ---------- C6: ceu, clima e nuvens ----------
        void applySky(Minecraft mc, int lv) {
            WorldProvider p = mc.theWorld.provider;
            int mode = lv == 0 ? 0 : SKY_MODE[lv];
            // so mexe quando o slot esta livre (ou ja era nosso): respeita ceus de outros mods
            if (p.getWeatherRenderer() == null || p.getWeatherRenderer() == NOOP) p.setWeatherRenderer(mode >= 1 ? NOOP : null);
            if (p.getCloudRenderer() == null || p.getCloudRenderer() == NOOP) p.setCloudRenderer(mode >= 1 ? NOOP : null);
            if (p.getSkyRenderer() == null || p.getSkyRenderer() == NOOP) p.setSkyRenderer(mode >= 2 ? NOOP : null);
        }

        // ---------- C8: F3 ----------
        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Text e) {
            Minecraft mc = Minecraft.getMinecraft();
            if (!mc.gameSettings.showDebugInfo) return;
            e.left.add("");
            e.left.add("[OTMCL] nivel " + NAME[level] + (auto ? " (auto)" : "") + " | FPS " + fps + " | render "
                + mc.gameSettings.renderDistanceChunks + " | ocultos/s " + cullPerSec);
            e.left.add("[OTMCL] mspt " + String.format("%.1f", msptAvg) + " | ticks pulados/s " + skippedPerSec
                + " | TE dormindo " + totalSleeping());
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
            return "/otmcl <0-5> | auto | status | limpar | ajuda";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender s) {
            if (s.getEntityWorld() != null && s.getEntityWorld().isRemote) return true; // comando do cliente
            return s.canCommandSenderUseCommand(2, "otmcl");
        }

        void say(ICommandSender s, String m) {
            s.addChatMessage(new ChatComponentText(m));
        }

        @Override
        public void processCommand(ICommandSender s, String[] a) {
            if (a.length == 0 || a[0].equalsIgnoreCase("ajuda") || a[0].equalsIgnoreCase("help")) {
                say(s, "OtmclMod: /otmcl <0-5> muda o nivel | auto liga/desliga o modo automatico | status | limpar");
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
                say(s, "Flechas removidas " + arrowsRemoved + ", ultima limpeza manual removeu " + lastCleaned + " entidades");
                say(s, "Cliente: ocultos/s " + cullPerSec + ", limpezas de particulas " + partCleared);
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
