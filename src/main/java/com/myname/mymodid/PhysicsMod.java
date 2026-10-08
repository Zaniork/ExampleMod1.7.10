package com.myname.mymodid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.material.Material;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Facing;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;

@Mod(modid = PhysicsMod.MODID, name = "Physics Mod", version = "1.0", acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*")
public class PhysicsMod {

    public static final String MODID = "physicsmod";
    public static PhysicsMod INSTANCE;

    // ---------- configuracao (config/physicsmod.cfg) ----------
    static boolean enabled = true; // liga/desliga tudo
    static boolean structural = true; // estruturas soltas do chao desabam
    static boolean strictGravity = false; // quebrou um bloco: a coluna de cima cai (cuidado em minas!)
    static boolean explosions = true; // explosoes causam desabamento
    static boolean damage = true; // blocos caindo machucam
    static int bfsLimit = 600; // acima disso a estrutura e considerada presa ao chao
    static int spawnPerTick = 30; // quantos blocos viram "caindo" por tick
    static int maxDirtyPerTick = 120; // quantas posicoes verificar por tick

    static final Map<Integer, State> STATES = new HashMap<Integer, State>();

    // ================= CICLO DE VIDA =================
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        INSTANCE = this;
        Configuration cfg = new Configuration(e.getSuggestedConfigurationFile());
        cfg.load();
        enabled = cfg.getBoolean("enabled", "fisica", enabled, "Liga a fisica");
        structural = cfg.getBoolean("structural", "fisica", structural,
            "Estruturas desconectadas do chao desabam (torres, pontes, arvores soltas)");
        strictGravity = cfg.getBoolean("strictGravity", "fisica", strictGravity,
            "Quebrou um bloco: ate 16 blocos acima caem (pode causar desabamento em minas)");
        explosions = cfg.getBoolean("explosions", "fisica", explosions, "Explosoes causam desabamento");
        damage = cfg.getBoolean("damage", "fisica", damage, "Blocos caindo causam dano");
        bfsLimit = cfg.getInt("bfsLimit", "desempenho", bfsLimit, 50, 5000,
            "Estruturas maiores que isso sao consideradas presas ao chao");
        spawnPerTick = cfg.getInt("spawnPerTick", "desempenho", spawnPerTick, 1, 500,
            "Blocos que comecam a cair por tick");
        maxDirtyPerTick = cfg.getInt("maxDirtyPerTick", "desempenho", maxDirtyPerTick, 10, 2000,
            "Posicoes verificadas por tick");
        if (cfg.hasChanged()) cfg.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        Events ev = new Events();
        MinecraftForge.EVENT_BUS.register(ev);
        FMLCommonHandler.instance().bus().register(ev);
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new CmdPhysics());
    }

    @Mod.EventHandler
    public void serverStop(FMLServerStoppingEvent e) {
        STATES.clear();
    }

    static State state(World w) {
        State s = STATES.get(w.provider.dimensionId);
        if (s == null) {
            s = new State();
            STATES.put(w.provider.dimensionId, s);
        }
        return s;
    }

    static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    // ================= REGRAS =================
    // bloco "fisico" = pode cair
    static boolean physical(World w, int x, int y, int z, Block b) {
        if (b == null || b == Blocks.air) return false;
        Material m = b.getMaterial();
        if (m.isLiquid() || !m.isSolid()) return false;
        if (b.getBlockHardness(w, x, y, z) < 0) return false;
        if (b.hasTileEntity(w.getBlockMetadata(x, y, z))) return false;
        if (b instanceof BlockDoor || b instanceof BlockBed || b instanceof BlockPistonBase
            || b instanceof BlockPistonExtension || b instanceof BlockPistonMoving) return false;
        return true;
    }

    // bloco solido que nao cai (bedrock, bau, porta...) segura as estruturas
    static boolean anchor(World w, int x, int y, int z, Block b) {
        if (b == null || b == Blocks.air) return false;
        Material m = b.getMaterial();
        if (m.isLiquid() || !m.isSolid()) return false;
        return !physical(w, x, y, z, b);
    }

    // acha o grupo de blocos conectado. Retorna true se esta preso ao chao.
    static boolean flood(World w, int sx, int sy, int sz, HashSet<Long> grounded, List<int[]> comp) {
        HashSet<Long> seen = new HashSet<Long>();
        ArrayDeque<int[]> q = new ArrayDeque<int[]>();
        q.add(new int[] {sx, sy, sz});
        seen.add(key(sx, sy, sz));
        boolean g = false;
        while (!q.isEmpty()) {
            int[] c = q.poll();
            comp.add(c);
            if (comp.size() >= bfsLimit) {
                g = true;
                break;
            }
            for (int d = 0; d < 6; d++) {
                int nx = c[0] + Facing.offsetsXForSide[d];
                int ny = c[1] + Facing.offsetsYForSide[d];
                int nz = c[2] + Facing.offsetsZForSide[d];
                if (ny < 0) {
                    g = true;
                    continue;
                }
                if (ny > 255) continue;
                if (!w.blockExists(nx, ny, nz)) {
                    g = true;
                    continue;
                }
                long k = key(nx, ny, nz);
                if (seen.contains(k)) continue;
                if (grounded.contains(k)) {
                    g = true;
                    continue;
                }
                Block b = w.getBlock(nx, ny, nz);
                if (anchor(w, nx, ny, nz, b)) {
                    g = true;
                    continue;
                }
                if (!physical(w, nx, ny, nz, b)) continue;
                seen.add(k);
                q.add(new int[] {nx, ny, nz});
            }
        }
        return g;
    }

    // ================= ESTADO POR DIMENSAO =================
    static class Tracked {
        EntityFallingBlock e;
        int id, meta;

        Tracked(EntityFallingBlock e, int id, int meta) {
            this.e = e;
            this.id = id;
            this.meta = meta;
        }
    }

    static class State {
        ArrayDeque<int[]> dirty = new ArrayDeque<int[]>();
        PriorityQueue<int[]> falls = new PriorityQueue<int[]>(64, new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                return a[1] - b[1]; // de baixo para cima
            }
        });
        HashSet<Long> queued = new HashSet<Long>();
        List<Tracked> tracked = new ArrayList<Tracked>();

        void enqueueFall(int x, int y, int z) {
            if (queued.add(key(x, y, z))) falls.add(new int[] {x, y, z});
        }

        void process(World w) {
            processDirty(w);
            processFalls(w);
            processTracked(w);
        }

        void processDirty(World w) {
            HashSet<Long> grounded = new HashSet<Long>();
            HashSet<Long> done = new HashSet<Long>();
            int n = 0;
            while (!dirty.isEmpty() && n++ < maxDirtyPerTick) {
                int[] p = dirty.poll();
                if (strictGravity && w.isAirBlock(p[0], p[1], p[2])) {
                    for (int k = 1; k <= 16; k++) {
                        int y = p[1] + k;
                        if (y > 255) break;
                        Block b = w.getBlock(p[0], y, p[2]);
                        if (!physical(w, p[0], y, p[2], b)) break;
                        enqueueFall(p[0], y, p[2]);
                    }
                }
                if (!structural) continue;
                for (int d = 0; d < 6; d++) {
                    int x = p[0] + Facing.offsetsXForSide[d];
                    int y = p[1] + Facing.offsetsYForSide[d];
                    int z = p[2] + Facing.offsetsZForSide[d];
                    if (y < 0 || y > 255) continue;
                    long k = key(x, y, z);
                    if (done.contains(k) || grounded.contains(k) || queued.contains(k)) continue;
                    if (!w.blockExists(x, y, z)) continue;
                    Block b = w.getBlock(x, y, z);
                    if (!physical(w, x, y, z, b)) continue;
                    List<int[]> comp = new ArrayList<int[]>();
                    boolean g = flood(w, x, y, z, grounded, comp);
                    for (int[] c : comp) {
                        if (g) grounded.add(key(c[0], c[1], c[2]));
                        else done.add(key(c[0], c[1], c[2]));
                    }
                    if (!g) for (int[] c : comp) enqueueFall(c[0], c[1], c[2]);
                }
            }
        }

        void processFalls(World w) {
            int c = 0;
            while (!falls.isEmpty() && c < spawnPerTick) {
                int[] p = falls.poll();
                queued.remove(key(p[0], p[1], p[2]));
                if (!w.blockExists(p[0], p[1], p[2])) continue;
                Block b = w.getBlock(p[0], p[1], p[2]);
                if (!physical(w, p[0], p[1], p[2], b)) continue;
                int meta = w.getBlockMetadata(p[0], p[1], p[2]);
                w.setBlock(p[0], p[1], p[2], Blocks.air, 0, 3);
                EntityFallingBlock fb = new EntityFallingBlock(w, p[0] + 0.5, p[1] + 0.5, p[2] + 0.5, b, meta);
                w.spawnEntityInWorld(fb);
                tracked.add(new Tracked(fb, Block.getIdFromBlock(b), meta));
                c++;
            }
        }

        void processTracked(World w) {
            Iterator<Tracked> it = tracked.iterator();
            while (it.hasNext()) {
                Tracked t = it.next();
                if (t.e.isDead) {
                    // som e particulas de impacto
                    w.playAuxSFX(2001, (int) Math.floor(t.e.posX), (int) Math.floor(t.e.posY),
                        (int) Math.floor(t.e.posZ), t.id + (t.meta << 12));
                    it.remove();
                    continue;
                }
                if (damage && t.e.motionY < -0.25) {
                    List l = w.getEntitiesWithinAABBExcludingEntity(t.e, t.e.boundingBox.expand(0.05, 0.2, 0.05));
                    for (Object o : l) {
                        if (o instanceof EntityLivingBase) {
                            float dmg = (float) Math.min(20.0, -t.e.motionY * 8.0);
                            ((EntityLivingBase) o).attackEntityFrom(DamageSource.anvil, dmg);
                        }
                    }
                }
            }
        }
    }

    // ================= EVENTOS =================
    public static class Events {
        @SubscribeEvent
        public void onBreak(BlockEvent.BreakEvent e) {
            if (!enabled || e.world.isRemote) return;
            state(e.world).dirty.add(new int[] {e.x, e.y, e.z});
        }

        @SubscribeEvent
        public void onExplosion(ExplosionEvent.Detonate e) {
            if (!enabled || !explosions || e.world.isRemote) return;
            State s = state(e.world);
            for (Object o : e.explosion.affectedBlockPositions) {
                ChunkPosition cp = (ChunkPosition) o;
                s.dirty.add(new int[] {cp.chunkPosX, cp.chunkPosY, cp.chunkPosZ});
            }
        }

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent e) {
            if (e.phase != TickEvent.Phase.END || e.world.isRemote || !enabled) return;
            State s = STATES.get(e.world.provider.dimensionId);
            if (s == null) return;
            s.process(e.world);
        }
    }

    // ================= COMANDO =================
    public static class CmdPhysics extends CommandBase {
        @Override
        public String getCommandName() {
            return "physics";
        }

        @Override
        public String getCommandUsage(ICommandSender s) {
            return "/physics on|off|status|gravity|structural";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void processCommand(ICommandSender s, String[] a) {
            String m;
            if (a.length == 0 || a[0].equalsIgnoreCase("status")) {
                m = "Fisica: " + (enabled ? "ON" : "OFF") + " | estrutural: " + structural + " | gravidade total: "
                    + strictGravity + " | explosoes: " + explosions;
            } else if (a[0].equalsIgnoreCase("on")) {
                enabled = true;
                m = "Fisica ligada.";
            } else if (a[0].equalsIgnoreCase("off")) {
                enabled = false;
                m = "Fisica desligada.";
            } else if (a[0].equalsIgnoreCase("gravity")) {
                strictGravity = !strictGravity;
                m = "Gravidade total (coluna cai): " + strictGravity;
            } else if (a[0].equalsIgnoreCase("structural")) {
                structural = !structural;
                m = "Desabamento estrutural: " + structural;
            } else {
                m = getCommandUsage(s);
            }
            s.addChatMessage(new ChatComponentText(m));
        }
    }
}
