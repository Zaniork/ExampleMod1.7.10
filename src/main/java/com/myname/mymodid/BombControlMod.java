package com.myname.mymodid;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Cylinder;
import org.lwjgl.util.glu.Disk;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Facing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Controle de Bomba Nuclear (Minecraft 1.7.10 / Forge). Arquivo unico: basta colocar em src/main/java/com/myname/mymodid/.
 * Comando: /controlebomba  (te da o Controle de Bomba e a Nave Destruidora).
 * Tudo e desenhado por codigo (sem texturas externas).
 */
@Mod(modid = BombControlMod.MODID, name = "Controle de Bomba Nuclear", version = "1.0", acceptedMinecraftVersions = "[1.7.10]")
public class BombControlMod {

    public static final String MODID = "bombcontrol";

    @Mod.Instance(MODID)
    public static BombControlMod instance;

    public static SimpleNetworkWrapper NET;
    public static Item controller;
    public static Item shipItem;

    public static final String[] BOMB_NAMES = {"Bomba Convencional", "Bomba Incendiaria", "Bomba Termobarica", "Bomba Nuclear",
        "Nave Destruidora", "Nuclear Personalizada"};
    public static final String[] BOMB_INFO = {"Explosao forte, cratera media", "Napalm: cratera e fogo por todo lado",
        "Onda de pressao enorme", "Cogumelo atomico e radiacao", "Destroi o planeta (cutscene)", "Voce monta a sua (aba Bomba 6)"};
    public static final int[] BOMB_RADIUS = {11, 15, 24, 42, 64, 40};
    // bits: 1 = fogo, 2 = radiacao, 4 = cogumelo
    public static final int[] BOMB_FLAGS = {0, 1, 4, 7, 7, 7};

    static final Random RAND = new Random();
    static final ConcurrentLinkedQueue<Runnable> SERVER_TASKS = new ConcurrentLinkedQueue<Runnable>();
    static final ArrayList<Delayed> DELAYED = new ArrayList<Delayed>();
    static final ArrayList<Job> JOBS = new ArrayList<Job>();
    static final int CUTSCENE_SECONDS = 22;

    // ============================================================================================
    // CICLO DE VIDA
    // ============================================================================================
    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        NET = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        NET.registerMessage(HandLaunch.class, MsgLaunch.class, 0, Side.SERVER);
        NET.registerMessage(HandFx.class, MsgFx.class, 1, Side.CLIENT);
        NET.registerMessage(HandState.class, MsgState.class, 2, Side.CLIENT);

        controller = new ItemController();
        GameRegistry.registerItem(controller, "bomb_controller");
        shipItem = new ItemShip();
        GameRegistry.registerItem(shipItem, "ship_item");

        EntityRegistry.registerModEntity(EntityBomb.class, "BombEntity", 1, instance, 256, 1, true);
        EntityRegistry.registerModEntity(EntityShip.class, "ShipEntity", 2, instance, 256, 5, false);

        GameRegistry.addRecipe(new ItemStack(controller), "IRI", "IBI", "IGI", 'I', Items.iron_ingot, 'R', Items.redstone, 'B',
            Blocks.stone_button, 'G', Items.gold_ingot);
        GameRegistry.addRecipe(new ItemStack(shipItem), "DDD", "DND", "OOO", 'D', Blocks.diamond_block, 'N', Items.nether_star, 'O',
            Blocks.obsidian);

        FMLCommonHandler.instance().bus().register(new ServerTicker());
        if (FMLCommonHandler.instance().getSide().isClient()) {
            Client.init();
        }
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new CmdGive());
    }

    public static class CmdGive extends CommandBase {
        @Override
        public String getCommandName() {
            return "controlebomba";
        }

        @Override
        public String getCommandUsage(ICommandSender s) {
            return "/controlebomba";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender s) {
            return true;
        }

        @Override
        public void processCommand(ICommandSender s, String[] a) {
            EntityPlayerMP p = getCommandSenderAsPlayer(s);
            giveOrDrop(p, new ItemStack(controller));
            giveOrDrop(p, new ItemStack(shipItem));
            tell(p, "Voce recebeu o Controle de Bomba e a Nave Destruidora.");
        }
    }

    static void giveOrDrop(EntityPlayer p, ItemStack st) {
        if (!p.inventory.addItemStackToInventory(st)) {
            p.dropPlayerItemWithRandomChoice(st, false);
        }
    }

    static void tell(EntityPlayer p, String s) {
        p.addChatMessage(new ChatComponentText(s));
    }

    // ============================================================================================
    // REDE
    // ============================================================================================
    public static class MsgLaunch implements IMessage {
        public int type, mode, radius, flags, hue, height;
        public double x, y, z;

        public MsgLaunch() {
        }

        @Override
        public void toBytes(ByteBuf b) {
            b.writeInt(type);
            b.writeInt(mode);
            b.writeInt(radius);
            b.writeInt(flags);
            b.writeInt(hue);
            b.writeInt(height);
            b.writeDouble(x);
            b.writeDouble(y);
            b.writeDouble(z);
        }

        @Override
        public void fromBytes(ByteBuf b) {
            type = b.readInt();
            mode = b.readInt();
            radius = b.readInt();
            flags = b.readInt();
            hue = b.readInt();
            height = b.readInt();
            x = b.readDouble();
            y = b.readDouble();
            z = b.readDouble();
        }
    }

    public static class HandLaunch implements IMessageHandler<MsgLaunch, IMessage> {
        @Override
        public IMessage onMessage(final MsgLaunch m, MessageContext ctx) {
            final EntityPlayerMP pl = ctx.getServerHandler().playerEntity;
            SERVER_TASKS.add(new Runnable() {
                @Override
                public void run() {
                    handleLaunch(pl, m);
                }
            });
            return null;
        }
    }

    // kind: 0 = explosao, 1 = cutscene da nave, 2 = explosao do Sol, 3 = explosao da Lua
    public static class MsgFx implements IMessage {
        public int kind, type, radius, flags, hue, ticks;
        public double x, y, z;

        public MsgFx() {
        }

        @Override
        public void toBytes(ByteBuf b) {
            b.writeInt(kind);
            b.writeInt(type);
            b.writeInt(radius);
            b.writeInt(flags);
            b.writeInt(hue);
            b.writeInt(ticks);
            b.writeDouble(x);
            b.writeDouble(y);
            b.writeDouble(z);
        }

        @Override
        public void fromBytes(ByteBuf b) {
            kind = b.readInt();
            type = b.readInt();
            radius = b.readInt();
            flags = b.readInt();
            hue = b.readInt();
            ticks = b.readInt();
            x = b.readDouble();
            y = b.readDouble();
            z = b.readDouble();
        }
    }

    public static class HandFx implements IMessageHandler<MsgFx, IMessage> {
        @Override
        public IMessage onMessage(final MsgFx m, MessageContext ctx) {
            Client.TASKS.add(new Runnable() {
                @Override
                public void run() {
                    Client.onFx(m);
                }
            });
            return null;
        }
    }

    public static class MsgState implements IMessage {
        public boolean sun, moon;

        public MsgState() {
        }

        @Override
        public void toBytes(ByteBuf b) {
            b.writeBoolean(sun);
            b.writeBoolean(moon);
        }

        @Override
        public void fromBytes(ByteBuf b) {
            sun = b.readBoolean();
            moon = b.readBoolean();
        }
    }

    public static class HandState implements IMessageHandler<MsgState, IMessage> {
        @Override
        public IMessage onMessage(final MsgState m, MessageContext ctx) {
            Client.TASKS.add(new Runnable() {
                @Override
                public void run() {
                    Client.sunDead = m.sun;
                    Client.moonDead = m.moon;
                }
            });
            return null;
        }
    }

    static void sendFx(World w, int kind, int type, double x, double y, double z, int radius, int flags, int hue, int ticks, boolean wholeDimension) {
        MsgFx m = new MsgFx();
        m.kind = kind;
        m.type = type;
        m.x = x;
        m.y = y;
        m.z = z;
        m.radius = radius;
        m.flags = flags;
        m.hue = hue;
        m.ticks = ticks;
        if (wholeDimension) {
            NET.sendToDimension(m, w.provider.dimensionId);
        } else {
            NET.sendToAllAround(m, new NetworkRegistry.TargetPoint(w.provider.dimensionId, x, y, z, 700.0D));
        }
    }

    static void broadcastState(Data d) {
        MsgState m = new MsgState();
        m.sun = d.sunDead;
        m.moon = d.moonDead;
        NET.sendToAll(m);
    }

    // ============================================================================================
    // ESTADO SALVO NO MUNDO (Sol / Lua destruidos)
    // ============================================================================================
    public static class Data extends WorldSavedData {
        public static final String ID = "bombcontrol_state";
        public boolean sunDead, moonDead;

        public Data() {
            super(ID);
        }

        public Data(String s) {
            super(s);
        }

        @Override
        public void readFromNBT(NBTTagCompound n) {
            sunDead = n.getBoolean("sun");
            moonDead = n.getBoolean("moon");
        }

        @Override
        public void writeToNBT(NBTTagCompound n) {
            n.setBoolean("sun", sunDead);
            n.setBoolean("moon", moonDead);
        }

        public static Data get(World w) {
            Data d = (Data) w.mapStorage.loadData(Data.class, ID);
            if (d == null) {
                d = new Data();
                w.mapStorage.setData(ID, d);
            }
            return d;
        }
    }

    // ============================================================================================
    // TICK DO SERVIDOR (tarefas, trabalhos de destruicao, noite eterna)
    // ============================================================================================
    static class Delayed {
        int ticks;
        Runnable r;

        Delayed(int t, Runnable r) {
            this.ticks = t;
            this.r = r;
        }
    }

    static void later(int ticks, Runnable r) {
        DELAYED.add(new Delayed(ticks, r));
    }

    public static abstract class Job {
        public boolean done;

        public abstract int step(int budget);
    }

    public static class ServerTicker {
        int counter = 0;

        @SubscribeEvent
        public void onTick(TickEvent.ServerTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Runnable r;
            while ((r = SERVER_TASKS.poll()) != null) {
                try {
                    r.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            for (int i = DELAYED.size() - 1; i >= 0; i--) {
                if (i >= DELAYED.size()) continue;
                Delayed d = DELAYED.get(i);
                d.ticks--;
                if (d.ticks <= 0) {
                    DELAYED.remove(i);
                    try {
                        d.r.run();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
            int budget = 1600;
            for (Iterator<Job> it = JOBS.iterator(); it.hasNext() && budget > 0;) {
                Job j = it.next();
                try {
                    budget -= Math.max(1, j.step(budget));
                } catch (Throwable t) {
                    t.printStackTrace();
                    j.done = true;
                }
                if (j.done) it.remove();
            }
            counter++;
            MinecraftServer srv = MinecraftServer.getServer();
            if (srv == null) return;
            WorldServer w0 = srv.worldServerForDimension(0);
            if (w0 == null) return;
            Data d = Data.get(w0);
            if (d.sunDead) {
                long t = w0.getWorldTime();
                w0.setWorldTime((t / 24000L) * 24000L + 18000L);
            }
            if (counter % 100 == 0) {
                broadcastState(d);
            }
        }
    }

    // ============================================================================================
    // LOGICA DO SERVIDOR
    // ============================================================================================
    static int clamp(int v, int a, int b) {
        return v < a ? a : (v > b ? b : v);
    }

    static void handleLaunch(EntityPlayerMP pl, MsgLaunch m) {
        World w = pl.worldObj;
        if (m.mode == 1) {
            explodeBody(pl, true);
            return;
        }
        if (m.mode == 2) {
            explodeBody(pl, false);
            return;
        }
        if (m.mode == 3) {
            detonateNearestShip(pl);
            return;
        }
        if (m.mode == 4) {
            restoreSky(pl);
            return;
        }
        int type = clamp(m.type, 0, 5);
        int bx = MathHelper.floor_double(m.x);
        int bz = MathHelper.floor_double(m.z);
        boolean loaded = w.getChunkProvider().chunkExists(bx >> 4, bz >> 4);
        int top = w.getHeightValue(bx, bz);
        if (top < 1) top = 1;
        if (type == 4) {
            EntityShip s = new EntityShip(w);
            s.setLocationAndAngles(bx + 0.5D, top, bz + 0.5D, RAND.nextFloat() * 360.0F, 0.0F);
            w.spawnEntityInWorld(s);
            tell(pl, "Nave Destruidora posicionada. Use 'Detonar nave' no controle (ou clique nela com o controle).");
            return;
        }
        int radius = BOMB_RADIUS[type];
        int flags = BOMB_FLAGS[type];
        int hue = 0;
        if (type == 5) {
            radius = clamp(m.radius, 8, 70);
            flags = m.flags & 7;
            hue = clamp(m.hue, 0, 359);
        }
        if (!loaded) {
            detonateAtSurface(w, type, bx + 0.5D, bz + 0.5D, radius, flags, hue);
            tell(pl, "Alvo fora da area carregada: a bomba explodiu na hora.");
            return;
        }
        int height = clamp(m.height, 40, 230);
        EntityBomb b = new EntityBomb(w);
        b.setBomb(type, radius, flags, hue);
        b.setPosition(bx + 0.5D, Math.min(250, top + height), bz + 0.5D);
        w.spawnEntityInWorld(b);
        tell(pl, BOMB_NAMES[type] + " lancada!");
    }

    static void detonateAtSurface(World w, int type, double x, double z, int radius, int flags, int hue) {
        int ix = MathHelper.floor_double(x);
        int iz = MathHelper.floor_double(z);
        int gy = w.getHeightValue(ix, iz) - 1;
        if (gy < 1) gy = 1;
        double y = gy + 1.0D;
        sendFx(w, 0, type, x, y, z, radius, flags, hue, 0, false);
        shock(w, x, y, z, radius, (flags & 2) != 0);
        JOBS.add(new CraterJob(w, ix, gy, iz, radius, type, (flags & 1) != 0));
    }

    static void shock(World w, double x, double y, double z, int R, boolean radiation) {
        double reach = R * 2.2D;
        AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(x - reach, y - reach, z - reach, x + reach, y + reach, z + reach);
        List list = w.getEntitiesWithinAABB(Entity.class, bb);
        for (int i = 0; i < list.size(); i++) {
            Entity e = (Entity) list.get(i);
            if (e instanceof EntityBomb || e instanceof EntityShip) continue;
            double dx = e.posX - x;
            double dy = e.posY + e.height * 0.5D - y;
            double dz = e.posZ - z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > reach) continue;
            double f = 1.0D - dist / reach;
            float dmg = (float) (f * f * R * 2.4D);
            if (e instanceof EntityLivingBase) {
                EntityLivingBase le = (EntityLivingBase) e;
                if (dmg > 0.5F) le.attackEntityFrom(DamageSource.generic, dmg);
                if (radiation && dist < R * 1.8D) {
                    le.addPotionEffect(new PotionEffect(Potion.wither.id, 20 * 20, 1));
                    le.addPotionEffect(new PotionEffect(Potion.hunger.id, 20 * 30, 1));
                }
            }
            if (dist > 0.01D) {
                double k = f * 2.4D;
                e.motionX += dx / dist * k;
                e.motionY += 0.3D + f * 1.1D;
                e.motionZ += dz / dist * k;
                e.velocityChanged = true;
            }
            if (f > 0.45D) e.setFire(8);
        }
    }

    // ---- cratera progressiva ----
    public static class CraterJob extends Job {
        final World w;
        final int cx, gy, cz, R, Rb, type;
        final boolean fire;
        final long[] order;
        int idx = 0;
        final Random rnd = new Random();

        public CraterJob(World w, int cx, int gy, int cz, int R, int type, boolean fire) {
            this.w = w;
            this.cx = cx;
            this.gy = gy;
            this.cz = cz;
            this.R = R;
            this.type = type;
            this.fire = fire;
            this.Rb = (int) (R * 1.7D) + 2;
            long[] tmp = new long[(2 * Rb + 1) * (2 * Rb + 1)];
            int n = 0;
            for (int dx = -Rb; dx <= Rb; dx++) {
                for (int dz = -Rb; dz <= Rb; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > Rb * Rb) continue;
                    tmp[n++] = (((long) d2) << 20) | ((long) (dx + 256) << 10) | (long) (dz + 256);
                }
            }
            this.order = Arrays.copyOf(tmp, n);
            Arrays.sort(this.order);
        }

        Block floorBlock(double s) {
            double r = rnd.nextDouble();
            switch (type) {
                case 0:
                    return r < 0.5D ? Blocks.gravel : null;
                case 1:
                    return Blocks.netherrack;
                case 2:
                    return r < 0.3D ? Blocks.stone : null;
                case 4:
                    if (s < 0.12D) return Blocks.lava;
                    return s < 0.5D ? Blocks.obsidian : Blocks.netherrack;
                default:
                    if (s < 0.35D) return Blocks.obsidian;
                    return r < 0.5D ? Blocks.netherrack : Blocks.soul_sand;
            }
        }

        @Override
        public int step(int budget) {
            int used = 0;
            while (idx < order.length && used < budget) {
                long k = order[idx++];
                int dz = (int) (k & 1023L) - 256;
                int dx = (int) ((k >> 10) & 1023L) - 256;
                int d2 = (int) (k >> 20);
                used++;
                int x = cx + dx;
                int z = cz + dz;
                if (!w.getChunkProvider().chunkExists(x >> 4, z >> 4)) continue;
                double d = Math.sqrt(d2);
                int sy = w.getHeightValue(x, z) - 1;
                if (sy < 1) continue;
                if (d < R) {
                    double s = d / R;
                    int dep = (int) Math.round(R * 0.36D * (1.0D - s * s));
                    int fy = gy - dep;
                    if (fy < 1) fy = 1;
                    int y0 = sy;
                    int guard = 0;
                    while (y0 < 250 && guard < 40 && w.getBlock(x, y0 + 1, z) != Blocks.air) {
                        y0++;
                        guard++;
                    }
                    for (int y = y0; y > fy; y--) {
                        Block b = w.getBlock(x, y, z);
                        if (b == Blocks.air) continue;
                        if (b == Blocks.bedrock) break;
                        w.setBlock(x, y, z, Blocks.air, 0, 2);
                        used++;
                    }
                    Block fb = w.getBlock(x, fy, z);
                    if (fb != Blocks.air && fb != Blocks.bedrock && !fb.getMaterial().isLiquid()) {
                        Block nb = floorBlock(s);
                        if (nb != null) {
                            w.setBlock(x, fy, z, nb, 0, 2);
                            used++;
                        }
                    }
                    if (fire && rnd.nextInt(9) == 0 && w.getBlock(x, fy + 1, z) == Blocks.air) {
                        w.setBlock(x, fy + 1, z, Blocks.fire, 0, 2);
                        used++;
                    }
                } else {
                    double p = 1.0D - (d - R) / (double) (Rb - R);
                    if (p > 0.0D && rnd.nextDouble() < p) {
                        int y = sy;
                        int cnt = 0;
                        while (y > 1 && cnt < 40) {
                            Block b = w.getBlock(x, y, z);
                            Material m = b.getMaterial();
                            if (b == Blocks.air) {
                                y--;
                                cnt++;
                                continue;
                            }
                            if (m == Material.leaves || m == Material.plants || m == Material.vine || m == Material.glass
                                || m == Material.wood || m == Material.cloth || m == Material.web || m == Material.snow
                                || m == Material.craftedSnow) {
                                w.setBlock(x, y, z, Blocks.air, 0, 2);
                                used++;
                                y--;
                                cnt++;
                                continue;
                            }
                            break;
                        }
                        if (fire && rnd.nextInt(16) == 0) {
                            int ty = w.getHeightValue(x, z);
                            if (ty > 1 && w.getBlock(x, ty, z) == Blocks.air && w.getBlock(x, ty - 1, z).getMaterial().isSolid()) {
                                w.setBlock(x, ty, z, Blocks.fire, 0, 2);
                                used++;
                            }
                        }
                    }
                }
            }
            if (idx >= order.length) done = true;
            return used;
        }
    }

    // ---- rachadura gigante (a Terra partindo ao meio) ----
    public static class ChasmJob extends Job {
        final World w;
        final int cx, cz, L;
        final double dx, dz;
        int s;
        final Random rnd = new Random();

        public ChasmJob(World w, int cx, int cz, int L) {
            this.w = w;
            this.cx = cx;
            this.cz = cz;
            this.L = L;
            double a = rnd.nextDouble() * Math.PI;
            this.dx = Math.cos(a);
            this.dz = Math.sin(a);
            this.s = -L;
        }

        @Override
        public int step(int budget) {
            int used = 0;
            while (s <= L && used < budget) {
                double wob = Math.sin(s * 0.045D) * 6.0D + Math.sin(s * 0.11D + 2.0D) * 2.5D;
                double hw = 2.5D + 3.5D * (1.0D - Math.abs(s) / (double) L) + rnd.nextDouble() * 0.8D;
                int ihw = (int) Math.ceil(hw);
                for (int k = -ihw; k <= ihw; k++) {
                    double lat = k + wob;
                    int x = (int) Math.round(cx + dx * s - dz * lat);
                    int z = (int) Math.round(cz + dz * s + dx * lat);
                    used++;
                    if (!w.getChunkProvider().chunkExists(x >> 4, z >> 4)) continue;
                    int sy = w.getHeightValue(x, z) - 1;
                    for (int y = sy; y >= 6; y--) {
                        if (y <= 9) {
                            w.setBlock(x, y, z, Blocks.lava, 0, 2);
                            used++;
                        } else {
                            if (w.getBlock(x, y, z) == Blocks.air) continue;
                            w.setBlock(x, y, z, Blocks.air, 0, 2);
                            used++;
                        }
                    }
                }
                s++;
            }
            if (s > L) done = true;
            return used;
        }
    }

    // ---- nave destruidora: cutscene + destruicao ----
    static void detonateShip(World w, EntityShip ship) {
        final World fw = w;
        final double x = ship.posX;
        final double z = ship.posZ;
        final int ix = MathHelper.floor_double(x);
        final int iz = MathHelper.floor_double(z);
        final int gy = Math.max(1, w.getHeightValue(ix, iz) - 1);
        final int dur = CUTSCENE_SECONDS * 20;
        ship.setDead();
        for (int i = 0; i < w.playerEntities.size(); i++) {
            EntityPlayer p = (EntityPlayer) w.playerEntities.get(i);
            p.addPotionEffect(new PotionEffect(Potion.resistance.id, dur + 1000, 4));
            p.addPotionEffect(new PotionEffect(Potion.fireResistance.id, dur + 1000, 0));
        }
        sendFx(w, 1, 4, x, gy + 1.0D, z, 64, 7, 0, dur, true);
        JOBS.add(new CraterJob(w, ix, gy, iz, 64, 4, true));
        JOBS.add(new ChasmJob(w, ix, iz, 230));
        later(dur - 40, new Runnable() {
            @Override
            public void run() {
                sendFx(fw, 0, 4, x, gy + 1.0D, z, 64, 7, 0, 0, false);
                shock(fw, x, gy + 1.0D, z, 64, true);
            }
        });
        later(dur, new Runnable() {
            @Override
            public void run() {
                rescuePlayers(fw, x, z, 64);
                fw.getWorldInfo().setRaining(true);
                fw.getWorldInfo().setThundering(true);
                fw.getWorldInfo().setRainTime(12000);
                fw.getWorldInfo().setThunderTime(12000);
            }
        });
    }

    static void rescuePlayers(World w, double x, double z, int R) {
        for (int i = 0; i < w.playerEntities.size(); i++) {
            EntityPlayer p = (EntityPlayer) w.playerEntities.get(i);
            double dx = p.posX - x;
            double dz = p.posZ - z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > R * 1.1D) continue;
            double nx = d < 0.5D ? 1.0D : dx / d;
            double nz = d < 0.5D ? 0.0D : dz / d;
            double tx = x + nx * (R * 1.25D);
            double tz = z + nz * (R * 1.25D);
            int sy = w.getHeightValue(MathHelper.floor_double(tx), MathHelper.floor_double(tz));
            p.setPositionAndUpdate(tx, sy + 1.0D, tz);
        }
    }

    static void detonateNearestShip(EntityPlayerMP pl) {
        World w = pl.worldObj;
        EntityShip best = null;
        double bd = 1.0E30D;
        for (int i = 0; i < w.loadedEntityList.size(); i++) {
            Object o = w.loadedEntityList.get(i);
            if (o instanceof EntityShip) {
                EntityShip s = (EntityShip) o;
                double d = s.getDistanceSqToEntity(pl);
                if (d < bd) {
                    bd = d;
                    best = s;
                }
            }
        }
        if (best == null) {
            tell(pl, "Nenhuma Nave Destruidora no mundo. Posicione uma primeiro.");
            return;
        }
        detonateShip(w, best);
    }

    // ---- Sol e Lua ----
    static void explodeBody(EntityPlayerMP pl, final boolean sun) {
        MinecraftServer srv = MinecraftServer.getServer();
        final WorldServer w0 = srv.worldServerForDimension(0);
        if (pl.worldObj.provider.dimensionId != 0) {
            tell(pl, "So funciona no Overworld.");
            return;
        }
        final Data d = Data.get(w0);
        if (sun ? d.sunDead : d.moonDead) {
            tell(pl, sun ? "O Sol ja foi destruido." : "A Lua ja foi destruida.");
            return;
        }
        long t = w0.getWorldTime();
        w0.setWorldTime((t / 24000L) * 24000L + (sun ? 6000L : 18000L));
        sendFx(w0, sun ? 2 : 3, 0, pl.posX, pl.posY, pl.posZ, 0, 0, 0, 0, true);
        later(60, new Runnable() {
            @Override
            public void run() {
                if (sun) {
                    d.sunDead = true;
                } else {
                    d.moonDead = true;
                }
                d.markDirty();
                broadcastState(d);
                if (!sun) meteors(w0, 40);
            }
        });
    }

    static void meteors(final World w, final int remaining) {
        if (remaining <= 0) return;
        for (int i = 0; i < w.playerEntities.size(); i++) {
            EntityPlayer p = (EntityPlayer) w.playerEntities.get(i);
            for (int k = 0; k < 2; k++) {
                double x = p.posX + (RAND.nextDouble() - 0.5D) * 90.0D;
                double z = p.posZ + (RAND.nextDouble() - 0.5D) * 90.0D;
                double y = Math.min(250.0D, p.posY + 70.0D + RAND.nextDouble() * 40.0D);
                EntitySmallFireball f = new EntitySmallFireball(w, x, y, z, (RAND.nextDouble() - 0.5D) * 0.4D, -1.0D,
                    (RAND.nextDouble() - 0.5D) * 0.4D);
                w.spawnEntityInWorld(f);
            }
        }
        later(6, new Runnable() {
            @Override
            public void run() {
                meteors(w, remaining - 1);
            }
        });
    }

    static void restoreSky(EntityPlayerMP pl) {
        MinecraftServer srv = MinecraftServer.getServer();
        WorldServer w0 = srv.worldServerForDimension(0);
        Data d = Data.get(w0);
        d.sunDead = false;
        d.moonDead = false;
        d.markDirty();
        broadcastState(d);
        tell(pl, "Sol e Lua restaurados.");
    }

    // ============================================================================================
    // ITENS
    // ============================================================================================
    public static class ItemController extends Item {
        public ItemController() {
            setMaxStackSize(1);
            setUnlocalizedName("bombcontrol.controller");
            setTextureName("stick");
            setCreativeTab(CreativeTabs.tabMisc);
            setFull3D();
        }

        @Override
        public String getItemStackDisplayName(ItemStack s) {
            return "Controle de Bomba Nuclear";
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack s, EntityPlayer p, List l, boolean adv) {
            l.add("Botao direito ou esquerdo: abre o painel");
            l.add("Modo Mira: direito confirma, esquerdo volta ao painel");
        }

        @Override
        public ItemStack onItemRightClick(ItemStack s, World w, EntityPlayer p) {
            if (w.isRemote) Client.useController(true);
            return s;
        }

        @Override
        public boolean onEntitySwing(EntityLivingBase e, ItemStack s) {
            if (e.worldObj.isRemote) Client.swing(e);
            return true;
        }

        @Override
        public boolean onBlockStartBreak(ItemStack s, int x, int y, int z, EntityPlayer p) {
            return true;
        }

        @Override
        public boolean onLeftClickEntity(ItemStack s, EntityPlayer p, Entity e) {
            return true;
        }
    }

    public static class ItemShip extends Item {
        public ItemShip() {
            setMaxStackSize(16);
            setUnlocalizedName("bombcontrol.ship");
            setTextureName("stick");
            setCreativeTab(CreativeTabs.tabMisc);
        }

        @Override
        public String getItemStackDisplayName(ItemStack s) {
            return "Nave Destruidora";
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack s, EntityPlayer p, List l, boolean adv) {
            l.add("Clique no chao para colocar a nave");
            l.add("Detone com o Controle de Bomba");
        }

        @Override
        public boolean onItemUse(ItemStack s, EntityPlayer p, World w, int x, int y, int z, int side, float hx, float hy, float hz) {
            if (w.isRemote) return true;
            double px = x + 0.5D + Facing.offsetsXForSide[side];
            double py = y + Facing.offsetsYForSide[side];
            double pz = z + 0.5D + Facing.offsetsZForSide[side];
            EntityShip e = new EntityShip(w);
            e.setLocationAndAngles(px, py, pz, p.rotationYaw, 0.0F);
            w.spawnEntityInWorld(e);
            if (!p.capabilities.isCreativeMode) s.stackSize--;
            return true;
        }
    }

    // ============================================================================================
    // ENTIDADES
    // ============================================================================================
    public static class EntityBomb extends Entity {
        int life = 0;

        public EntityBomb(World w) {
            super(w);
            setSize(0.8F, 0.8F);
            ignoreFrustumCheck = true;
        }

        @Override
        protected void entityInit() {
            dataWatcher.addObject(20, Integer.valueOf(0));
            dataWatcher.addObject(21, Integer.valueOf(20));
            dataWatcher.addObject(22, Integer.valueOf(0));
            dataWatcher.addObject(23, Integer.valueOf(0));
        }

        public void setBomb(int type, int radius, int flags, int hue) {
            dataWatcher.updateObject(20, Integer.valueOf(type));
            dataWatcher.updateObject(21, Integer.valueOf(radius));
            dataWatcher.updateObject(22, Integer.valueOf(flags));
            dataWatcher.updateObject(23, Integer.valueOf(hue));
        }

        public int bType() {
            return dataWatcher.getWatchableObjectInt(20);
        }

        public int bRadius() {
            return dataWatcher.getWatchableObjectInt(21);
        }

        public int bFlags() {
            return dataWatcher.getWatchableObjectInt(22);
        }

        public int bHue() {
            return dataWatcher.getWatchableObjectInt(23);
        }

        @Override
        public boolean isInRangeToRenderDist(double d) {
            return true;
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound n) {
            setBomb(n.getInteger("bt"), n.getInteger("br"), n.getInteger("bf"), n.getInteger("bh"));
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound n) {
            n.setInteger("bt", bType());
            n.setInteger("br", bRadius());
            n.setInteger("bf", bFlags());
            n.setInteger("bh", bHue());
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            life++;
            motionY -= 0.06D;
            if (motionY < -3.2D) motionY = -3.2D;
            moveEntity(motionX, motionY, motionZ);
            if (worldObj.isRemote) {
                if (life == 2) worldObj.playSound(posX, posY, posZ, "fireworks.launch", 6.0F, 0.5F, false);
                worldObj.spawnParticle("smoke", posX, posY + 1.2D, posZ, 0.0D, 0.0D, 0.0D);
                return;
            }
            if (onGround || isCollidedVertically || posY < 2.0D || life > 1500) {
                setDead();
                detonateAtSurface(worldObj, bType(), posX, posZ, bRadius(), bFlags(), bHue());
            }
        }
    }

    public static class EntityShip extends Entity {
        public EntityShip(World w) {
            super(w);
            setSize(3.2F, 7.5F);
            ignoreFrustumCheck = true;
        }

        @Override
        protected void entityInit() {
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound n) {
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound n) {
        }

        @Override
        public boolean isInRangeToRenderDist(double d) {
            return true;
        }

        @Override
        public boolean canBeCollidedWith() {
            return true;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            motionY -= 0.04D;
            moveEntity(0.0D, motionY, 0.0D);
            if (onGround) motionY = 0.0D;
        }

        @Override
        public boolean interactFirst(EntityPlayer p) {
            ItemStack s = p.getHeldItem();
            if (s != null && s.getItem() == controller) {
                if (!worldObj.isRemote) detonateShip(worldObj, this);
                return true;
            }
            return false;
        }

        @Override
        public boolean attackEntityFrom(DamageSource src, float amt) {
            if (!worldObj.isRemote && !isDead && src.getEntity() instanceof EntityPlayer
                && ((EntityPlayer) src.getEntity()).isSneaking()) {
                entityDropItem(new ItemStack(shipItem), 0.5F);
                setDead();
                return true;
            }
            return false;
        }
    }

    // ============================================================================================
    // CLIENTE: estado, eventos, efeitos de explosao
    // ============================================================================================
    @SideOnly(Side.CLIENT)
    public static class Client {
        static final ConcurrentLinkedQueue<Runnable> TASKS = new ConcurrentLinkedQueue<Runnable>();
        static boolean sunDead = false, moonDead = false;
        static boolean aiming = false;
        static int selBomb = 3, mode = 0, zoom = 1;
        static double tgtX = Double.NaN, tgtZ = Double.NaN;
        static int cRadius = 40, cHue = 20, cHeight = 150;
        static boolean cFire = true, cRad = true, cMush = true;
        static final ArrayList<ExFx> FX = new ArrayList<ExFx>();
        static SkyFx skyFx = null;
        static final SkyRenderer SKY = new SkyRenderer();
        static float shake = 0.0F;

        static void init() {
            RenderingRegistry.registerEntityRenderingHandler(EntityBomb.class, new RenderBomb());
            RenderingRegistry.registerEntityRenderingHandler(EntityShip.class, new RenderShip());
            MinecraftForgeClient.registerItemRenderer(controller, new ControllerRenderer());
            MinecraftForgeClient.registerItemRenderer(shipItem, new ShipItemRenderer());
            Events ev = new Events();
            MinecraftForge.EVENT_BUS.register(ev);
            FMLCommonHandler.instance().bus().register(ev);
        }

        static void onFx(MsgFx m) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null) return;
            if (m.kind == 0) {
                FX.add(new ExFx(m.x, m.y, m.z, m.radius, m.type, m.flags, m.hue));
                while (FX.size() > 6) FX.remove(0);
            } else if (m.kind == 1) {
                mc.displayGuiScreen(new GuiCutscene(m.ticks / 20.0F));
            } else if (m.kind == 2 || m.kind == 3) {
                skyFx = new SkyFx(m.kind == 2);
            }
        }

        static void chat(String s) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(s));
        }

        static void sendLaunch(int type, int mode, double x, double y, double z) {
            MsgLaunch m = new MsgLaunch();
            m.type = type;
            m.mode = mode;
            m.x = x;
            m.y = y;
            m.z = z;
            m.radius = cRadius;
            m.flags = (cFire ? 1 : 0) | (cRad ? 2 : 0) | (cMush ? 4 : 0);
            m.hue = cHue;
            m.height = cHeight;
            NET.sendToServer(m);
        }

        static void swing(EntityLivingBase e) {
            if (e == Minecraft.getMinecraft().thePlayer) useController(false);
        }

        static void useController(boolean right) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen != null || mc.thePlayer == null) return;
            if (right && aiming) {
                confirmAim();
                return;
            }
            aiming = false;
            mc.displayGuiScreen(new GuiControl());
        }

        static void confirmAim() {
            Minecraft mc = Minecraft.getMinecraft();
            MovingObjectPosition mop = mc.thePlayer.rayTrace(400.0D, 1.0F);
            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                chat("Mire em um bloco para confirmar o alvo.");
                return;
            }
            sendLaunch(selBomb, 0, mop.blockX + 0.5D, mop.blockY, mop.blockZ + 0.5D);
            aiming = false;
            chat("Alvo confirmado!");
        }

        static void updateSky(Minecraft mc) {
            if (mc.theWorld.provider.dimensionId != 0) return;
            IRenderHandler cur = mc.theWorld.provider.getSkyRenderer();
            boolean want = sunDead || moonDead;
            if (want && cur != SKY) {
                mc.theWorld.provider.setSkyRenderer(SKY);
            } else if (!want && cur == SKY) {
                mc.theWorld.provider.setSkyRenderer(null);
            }
        }

        public static class Events {
            @SubscribeEvent
            public void onClientTick(TickEvent.ClientTickEvent e) {
                if (e.phase != TickEvent.Phase.END) return;
                Runnable r;
                while ((r = TASKS.poll()) != null) {
                    try {
                        r.run();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.theWorld == null || mc.thePlayer == null) {
                    FX.clear();
                    aiming = false;
                    skyFx = null;
                    return;
                }
                ItemStack held = mc.thePlayer.getHeldItem();
                if (aiming && (held == null || held.getItem() != controller)) aiming = false;
                updateSky(mc);
                long now = System.currentTimeMillis();
                for (Iterator<ExFx> it = FX.iterator(); it.hasNext();) {
                    ExFx f = it.next();
                    f.tick(mc, now);
                    if (f.dead(now)) it.remove();
                }
                if (skyFx != null) {
                    skyFx.tick(mc, now);
                    if (skyFx.dead(now)) skyFx = null;
                }
                if (shake > 0.01F) {
                    mc.thePlayer.rotationYaw += (RAND.nextFloat() - 0.5F) * shake;
                    float np = mc.thePlayer.rotationPitch + (RAND.nextFloat() - 0.5F) * shake;
                    mc.thePlayer.rotationPitch = Math.max(-90.0F, Math.min(90.0F, np));
                    shake *= 0.9F;
                }
            }

            @SubscribeEvent
            public void onRenderLast(RenderWorldLastEvent e) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.theWorld == null || mc.renderViewEntity == null) return;
                if (FX.isEmpty() && skyFx == null && !aiming) return;
                float pt = e.partialTicks;
                Entity v = mc.renderViewEntity;
                double ix = v.lastTickPosX + (v.posX - v.lastTickPosX) * pt;
                double iy = v.lastTickPosY + (v.posY - v.lastTickPosY) * pt;
                double iz = v.lastTickPosZ + (v.posZ - v.lastTickPosZ) * pt;
                long now = System.currentTimeMillis();
                Gfx.fxBegin();
                for (int i = 0; i < FX.size(); i++) FX.get(i).render(ix, iy, iz, now);
                if (skyFx != null) skyFx.render(now, mc);
                if (aiming) renderAim(mc, ix, iy, iz, now);
                Gfx.fxEnd();
            }

            @SubscribeEvent
            public void onOverlay(RenderGameOverlayEvent.Post e) {
                if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer == null || mc.theWorld == null) return;
                int w = e.resolution.getScaledWidth();
                int h = e.resolution.getScaledHeight();
                long now = System.currentTimeMillis();
                float fl = 0.0F;
                for (int i = 0; i < FX.size(); i++) fl = Math.max(fl, FX.get(i).flash(mc, now));
                if (skyFx != null) fl = Math.max(fl, skyFx.flash(now));
                if (fl > 0.01F) {
                    int a = (int) (Math.min(1.0F, fl) * 255.0F);
                    Gui.drawRect(0, 0, w, h, (a << 24) | 0xFFFFFF);
                }
                if (aiming) {
                    MovingObjectPosition mop = mc.thePlayer.rayTrace(400.0D, 1.0F);
                    String l1 = "MODO MIRA - " + BOMB_NAMES[selBomb];
                    String l2;
                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                        double dx = mop.blockX + 0.5D - mc.thePlayer.posX;
                        double dz = mop.blockZ + 0.5D - mc.thePlayer.posZ;
                        l2 = "Alvo: " + mop.blockX + ", " + mop.blockY + ", " + mop.blockZ + "  (" + (int) Math.sqrt(dx * dx + dz * dz) + " m)";
                    } else {
                        l2 = "Mire em um bloco";
                    }
                    String l3 = "Direito: CONFIRMAR   Esquerdo: abrir painel";
                    int y = h / 2 + 24;
                    mc.fontRenderer.drawStringWithShadow(l1, (w - mc.fontRenderer.getStringWidth(l1)) / 2, y, 0xFF5544);
                    mc.fontRenderer.drawStringWithShadow(l2, (w - mc.fontRenderer.getStringWidth(l2)) / 2, y + 11, 0xFFFFFF);
                    mc.fontRenderer.drawStringWithShadow(l3, (w - mc.fontRenderer.getStringWidth(l3)) / 2, y + 22, 0xFFDD66);
                }
            }
        }

        static void renderAim(Minecraft mc, double ix, double iy, double iz, long now) {
            MovingObjectPosition mop = mc.thePlayer.rayTrace(400.0D, 1.0F);
            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
            float R = selBomb == 5 ? cRadius : BOMB_RADIUS[selBomb];
            float pulse = 0.5F + 0.5F * (float) Math.sin(now / 150.0D);
            GL11.glPushMatrix();
            GL11.glTranslated(mop.blockX + 0.5D - ix, mop.blockY + 1.05D - iy, mop.blockZ + 0.5D - iz);
            GL11.glPushMatrix();
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            Gfx.col(1.0F, 0.15F, 0.1F, 0.30F + 0.25F * pulse);
            Gfx.DSK.draw(R - 0.7F, R, 64, 1);
            Gfx.col(1.0F, 0.3F, 0.1F, 0.10F);
            Gfx.DSK.draw(0.0F, R, 64, 1);
            GL11.glPopMatrix();
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glPushMatrix();
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            Gfx.col(1.0F, 0.2F, 0.2F, 0.55F);
            Gfx.CYL.draw(0.15F, 0.15F, 120.0F, 8, 1);
            GL11.glPopMatrix();
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glPopMatrix();
        }

        // ---------------- explosao (visual) ----------------
        static class ExFx {
            final double x, y, z;
            final int R, type, flags, hue;
            final long start;
            boolean sndDone = false, snd2 = false, shkDone = false;
            float[] tint;

            ExFx(double x, double y, double z, int R, int type, int flags, int hue) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.R = R;
                this.type = type;
                this.flags = flags;
                this.hue = hue;
                this.start = System.currentTimeMillis();
                if (type == 5) {
                    tint = Gfx.hsv(hue / 360.0F, 0.85F, 1.0F);
                } else if (type == 4) {
                    tint = new float[] {0.75F, 0.25F, 1.0F};
                }
            }

            float age(long now) {
                return (now - start) / 1000.0F;
            }

            boolean dead(long now) {
                return age(now) > 26.0F;
            }

            double dist(Minecraft mc) {
                double dx = mc.thePlayer.posX - x;
                double dy = mc.thePlayer.posY - y;
                double dz = mc.thePlayer.posZ - z;
                return Math.sqrt(dx * dx + dy * dy + dz * dz);
            }

            float flash(Minecraft mc, long now) {
                float t = age(now);
                if (t < 0.0F || t > 2.6F) return 0.0F;
                float base = t < 0.12F ? t / 0.12F : Math.max(0.0F, 1.0F - (t - 0.12F) / 2.4F);
                double d = dist(mc);
                float f = (float) Math.max(0.0D, 1.0D - d / (R * 35.0D + 60.0D));
                return base * f;
            }

            void tick(Minecraft mc, long now) {
                World w = mc.theWorld;
                if (w == null) return;
                float t = age(now);
                double d = dist(mc);
                float vol = (float) Math.max(0.4D, 8.0D * (1.0D - d / (R * 40.0D + 80.0D)));
                if (!sndDone && t >= d / 90.0D) {
                    sndDone = true;
                    w.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.explode", vol, 0.5F, false);
                }
                if (!snd2 && t >= d / 90.0D + 0.6D) {
                    snd2 = true;
                    w.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "ambient.weather.thunder", vol, 0.6F, false);
                }
                if (!shkDone && t >= d / (R * 1.3D + 1.0D)) {
                    shkDone = true;
                    float k = (float) Math.max(0.0D, 1.0D - d / (R * 22.0D + 40.0D));
                    shake = Math.max(shake, 7.0F * k);
                }
                if (t < 3.0F) {
                    double rr = R * 0.85D * (1.0D - Math.exp(-t * 2.2D)) + 1.0D;
                    for (int i = 0; i < 5; i++) {
                        double rx = (RAND.nextDouble() - 0.5D) * rr * 1.2D;
                        double ry = (RAND.nextDouble() - 0.1D) * rr * 0.9D;
                        double rz = (RAND.nextDouble() - 0.5D) * rr * 1.2D;
                        w.spawnParticle(i % 2 == 0 ? "explode" : "largesmoke", x + rx, y + ry, z + rz, (RAND.nextDouble() - 0.5D) * 0.3D,
                            0.1D + RAND.nextDouble() * 0.3D, (RAND.nextDouble() - 0.5D) * 0.3D);
                    }
                }
                if (t < 2.6F) {
                    double sw = t * R * 1.3D;
                    for (int i = 0; i < 12; i++) {
                        double a = RAND.nextDouble() * Math.PI * 2.0D;
                        w.spawnParticle("largesmoke", x + Math.cos(a) * sw, y + 0.5D, z + Math.sin(a) * sw, Math.cos(a) * 0.15D, 0.05D,
                            Math.sin(a) * 0.15D);
                    }
                }
                if ((flags & 1) != 0 && t < 12.0F) {
                    for (int i = 0; i < 6; i++) {
                        double a = RAND.nextDouble() * Math.PI * 2.0D;
                        double r = Math.sqrt(RAND.nextDouble()) * R;
                        w.spawnParticle("flame", x + Math.cos(a) * r, y + 0.2D, z + Math.sin(a) * r, 0.0D, 0.05D + RAND.nextDouble() * 0.1D,
                            0.0D);
                    }
                }
                if ((flags & 4) != 0 && t > 0.8F && t < 14.0F) {
                    double grow = Math.min(1.0D, (t - 0.8D) / 7.0D);
                    double h = R * 1.5D * grow;
                    for (int i = 0; i < 3; i++) {
                        w.spawnParticle("largesmoke", x + (RAND.nextDouble() - 0.5D) * R * 0.25D, y + h, z + (RAND.nextDouble() - 0.5D) * R * 0.25D,
                            0.0D, 0.02D, 0.0D);
                    }
                }
            }

            void render(double ix, double iy, double iz, long now) {
                float t = age(now);
                float Rf = R;
                GL11.glPushMatrix();
                GL11.glTranslated(x - ix, y - iy, z - iz);
                float life = 5.0F + Rf * 0.06F;
                if (t < life) {
                    float p = t / life;
                    float rr = Rf * 0.85F * (1.0F - (float) Math.exp(-t * 2.2F)) + 0.5F;
                    float cyb = rr * 0.6F + Rf * 0.9F * p;
                    float[] fc = Gfx.fireColor(p);
                    if (tint != null) {
                        fc[0] = fc[0] * 0.4F + tint[0] * 0.6F;
                        fc[1] = fc[1] * 0.4F + tint[1] * 0.6F;
                        fc[2] = fc[2] * 0.4F + tint[2] * 0.6F;
                    }
                    float al = (float) Math.pow(1.0F - p, 0.7F);
                    GL11.glPushMatrix();
                    GL11.glTranslatef(0.0F, cyb, 0.0F);
                    Gfx.col(fc[0], fc[1], fc[2], 0.85F * al);
                    Gfx.sph(rr, 24);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                    Gfx.col(1.0F, 0.9F - 0.5F * p, 0.5F - 0.4F * p, 0.5F * al);
                    Gfx.sph(rr * 0.7F, 20);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GL11.glPopMatrix();
                }
                float swMax = Rf * 3.2F;
                float sw = t * Rf * 1.3F;
                if (sw < swMax && sw > 0.1F) {
                    float a = 1.0F - sw / swMax;
                    Gfx.col(1.0F, 0.92F, 0.8F, 0.10F * a);
                    Gfx.sph(sw, 28);
                    GL11.glPushMatrix();
                    GL11.glTranslatef(0.0F, 0.4F, 0.0F);
                    GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                    Gfx.col(1.0F, 0.85F, 0.6F, 0.45F * a);
                    Gfx.DSK.draw(Math.max(0.0F, sw - 2.0F - Rf * 0.04F), sw, 64, 1);
                    GL11.glPopMatrix();
                }
                if ((flags & 4) != 0 && t > 0.8F && t < 24.0F) {
                    float grow = Math.min(1.0F, (t - 0.8F) / 7.0F);
                    float al = t < 16.0F ? 0.9F : 0.9F * (1.0F - (t - 16.0F) / 8.0F);
                    float stemH = Rf * 1.5F * grow;
                    float stemR = Rf * 0.11F;
                    GL11.glPushMatrix();
                    GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                    Gfx.col(0.30F, 0.25F, 0.22F, al);
                    Gfx.CYL.draw(stemR * 1.4F, stemR * 0.8F, stemH, 14, 1);
                    GL11.glPopMatrix();
                    float capR = Rf * 0.55F * grow;
                    GL11.glPushMatrix();
                    GL11.glTranslatef(0.0F, stemH, 0.0F);
                    GL11.glScalef(1.0F, 0.55F, 1.0F);
                    Gfx.col(0.33F, 0.28F, 0.25F, al);
                    Gfx.sph(capR, 22);
                    if (t < 12.0F) {
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                        Gfx.col(1.0F, 0.45F, 0.1F, 0.35F * (1.0F - t / 12.0F));
                        Gfx.sph(capR * 0.82F, 18);
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    }
                    GL11.glPopMatrix();
                    GL11.glPushMatrix();
                    GL11.glTranslatef(0.0F, stemH - capR * 0.12F, 0.0F);
                    GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                    Gfx.col(0.40F, 0.34F, 0.30F, al * 0.6F);
                    Gfx.DSK.draw(capR * 0.3F, capR * 1.12F, 28, 1);
                    GL11.glPopMatrix();
                    GL11.glPushMatrix();
                    GL11.glScalef(1.0F, 0.35F, 1.0F);
                    Gfx.col(0.45F, 0.38F, 0.32F, 0.35F * al);
                    Gfx.sph(Rf * 0.75F * grow + 1.0F, 20);
                    GL11.glPopMatrix();
                }
                GL11.glPopMatrix();
            }
        }

        // ---------------- explosao do Sol / Lua (visual) ----------------
        static class SkyFx {
            final boolean sun;
            final long start;
            boolean snd = false;

            SkyFx(boolean sun) {
                this.sun = sun;
                this.start = System.currentTimeMillis();
            }

            float age(long now) {
                return (now - start) / 1000.0F;
            }

            boolean dead(long now) {
                return age(now) > 7.5F;
            }

            float[] dir(Minecraft mc) {
                float th = mc.theWorld.getCelestialAngle(1.0F) * 6.2831855F;
                float dx = -(float) Math.sin(th);
                float dy = (float) Math.cos(th);
                if (!sun) {
                    dx = -dx;
                    dy = -dy;
                }
                return new float[] {dx, dy, 0.0F};
            }

            float flash(long now) {
                float u = age(now) - 1.6F;
                if (u < 0.0F) return 0.0F;
                if (u < 0.2F) return u / 0.2F;
                return Math.max(0.0F, 1.0F - (u - 0.2F) / 1.6F);
            }

            void tick(Minecraft mc, long now) {
                float t = age(now);
                if (t < 1.8F) {
                    float[] d = dir(mc);
                    float tyaw = (float) (Math.atan2(-d[0], d[2]) * 180.0D / Math.PI);
                    float tpitch = (float) (-Math.asin(Math.max(-1.0F, Math.min(1.0F, d[1]))) * 180.0D / Math.PI);
                    tpitch = Math.max(-90.0F, Math.min(90.0F, tpitch));
                    mc.thePlayer.rotationYaw += MathHelper.wrapAngleTo180_float(tyaw - mc.thePlayer.rotationYaw) * 0.08F;
                    mc.thePlayer.rotationPitch += (tpitch - mc.thePlayer.rotationPitch) * 0.08F;
                }
                if (!snd && t >= 1.6F) {
                    snd = true;
                    mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.explode", 8.0F, 0.4F, false);
                    mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "mob.wither.spawn", 3.0F, 0.6F, false);
                    shake = Math.max(shake, 8.0F);
                }
            }

            void render(long now, Minecraft mc) {
                float t = age(now);
                float[] d = dir(mc);
                float dist = 60.0F;
                float cr = sun ? 1.0F : 0.8F;
                float cg = sun ? 0.85F : 0.9F;
                float cb = sun ? 0.4F : 1.0F;
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                if (t < 1.6F) {
                    float u = t / 1.6F;
                    float hx = d[0];
                    float hz = d[2];
                    float hl = (float) Math.sqrt(hx * hx + hz * hz);
                    float sx, sz;
                    if (hl < 0.05F) {
                        sx = 0.0F;
                        sz = 1.0F;
                    } else {
                        sx = hx / hl;
                        sz = hz / hl;
                    }
                    float sy = 0.05F;
                    for (int i = 0; i < 10; i++) {
                        float uu = u - i * 0.03F;
                        if (uu < 0.0F) break;
                        float px = sx + (d[0] - sx) * uu;
                        float py = sy + (d[1] - sy) * uu;
                        float pz = sz + (d[2] - sz) * uu;
                        float pl = (float) Math.sqrt(px * px + py * py + pz * pz);
                        if (pl < 0.001F) continue;
                        GL11.glPushMatrix();
                        GL11.glTranslatef(px / pl * dist, py / pl * dist, pz / pl * dist);
                        Gfx.col(1.0F, 0.8F, 0.4F, 0.85F - i * 0.07F);
                        Gfx.sph(1.3F - i * 0.08F, 10);
                        GL11.glPopMatrix();
                    }
                } else {
                    float u = t - 1.6F;
                    float r = 1.0F + 48.0F * (1.0F - (float) Math.exp(-u * 2.2D));
                    float a = u < 3.0F ? 1.0F : Math.max(0.0F, 1.0F - (u - 3.0F) / 2.9F);
                    GL11.glPushMatrix();
                    GL11.glTranslatef(d[0] * dist, d[1] * dist, d[2] * dist);
                    Gfx.col(1.0F, 1.0F, 1.0F, 0.9F * a);
                    Gfx.sph(r * 0.45F, 20);
                    Gfx.col(cr, cg, cb, 0.6F * a);
                    Gfx.sph(r * 0.75F, 20);
                    Gfx.col(cr, cg * 0.6F, cb * 0.4F, 0.35F * a);
                    Gfx.sph(r, 20);
                    float sr = u * 30.0F;
                    if (sr < 90.0F) {
                        Gfx.col(1.0F, 1.0F, 1.0F, 0.18F * (1.0F - sr / 90.0F));
                        Gfx.sph(sr, 24);
                    }
                    GL11.glPopMatrix();
                }
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
    }

    // ============================================================================================
    // GRAFICOS 3D (modelos feitos por codigo)
    // ============================================================================================
    @SideOnly(Side.CLIENT)
    public static class Gfx {
        static final Sphere SPH = new Sphere();
        static final Sphere SPH_T = new Sphere();
        static final Cylinder CYL = new Cylinder();
        static final Disk DSK = new Disk();
        static boolean fogWas;

        static {
            SPH_T.setTextureFlag(true);
        }

        static void col(float r, float g, float b) {
            GL11.glColor3f(r, g, b);
        }

        static void col(float r, float g, float b, float a) {
            GL11.glColor4f(r, g, b, a);
        }

        static void sph(float r, int q) {
            SPH.draw(r, q, Math.max(4, q / 2));
        }

        static float lerp(float a, float b, float u) {
            return a + (b - a) * u;
        }

        static float[] hsv(float h, float s, float v) {
            float r, g, b;
            float hh = (h - (float) Math.floor(h)) * 6.0F;
            int i = (int) hh;
            float f = hh - i;
            float p = v * (1.0F - s);
            float q = v * (1.0F - s * f);
            float t = v * (1.0F - s * (1.0F - f));
            switch (i % 6) {
                case 0:
                    r = v;
                    g = t;
                    b = p;
                    break;
                case 1:
                    r = q;
                    g = v;
                    b = p;
                    break;
                case 2:
                    r = p;
                    g = v;
                    b = t;
                    break;
                case 3:
                    r = p;
                    g = q;
                    b = v;
                    break;
                case 4:
                    r = t;
                    g = p;
                    b = v;
                    break;
                default:
                    r = v;
                    g = p;
                    b = q;
                    break;
            }
            return new float[] {r, g, b};
        }

        static float[] fireColor(float p) {
            float[][] k = {{1.0F, 1.0F, 0.9F}, {1.0F, 0.75F, 0.2F}, {1.0F, 0.38F, 0.05F}, {0.35F, 0.08F, 0.03F}};
            float[] pos = {0.0F, 0.12F, 0.45F, 1.0F};
            for (int i = 0; i < 3; i++) {
                if (p <= pos[i + 1]) {
                    float u = Math.max(0.0F, (p - pos[i]) / (pos[i + 1] - pos[i]));
                    return new float[] {lerp(k[i][0], k[i + 1][0], u), lerp(k[i][1], k[i + 1][1], u), lerp(k[i][2], k[i + 1][2], u)};
                }
            }
            return new float[] {k[3][0], k[3][1], k[3][2]};
        }

        static void fxBegin() {
            fogWas = GL11.glIsEnabled(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);
        }

        static void fxEnd() {
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            if (fogWas) GL11.glEnable(GL11.GL_FOG);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }

        static void box(double x0, double y0, double z0, double x1, double y1, double z1) {
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.setNormal(0.0F, 0.0F, -1.0F);
            t.addVertex(x0, y0, z0);
            t.addVertex(x0, y1, z0);
            t.addVertex(x1, y1, z0);
            t.addVertex(x1, y0, z0);
            t.setNormal(0.0F, 0.0F, 1.0F);
            t.addVertex(x0, y0, z1);
            t.addVertex(x1, y0, z1);
            t.addVertex(x1, y1, z1);
            t.addVertex(x0, y1, z1);
            t.setNormal(-1.0F, 0.0F, 0.0F);
            t.addVertex(x0, y0, z0);
            t.addVertex(x0, y0, z1);
            t.addVertex(x0, y1, z1);
            t.addVertex(x0, y1, z0);
            t.setNormal(1.0F, 0.0F, 0.0F);
            t.addVertex(x1, y0, z0);
            t.addVertex(x1, y1, z0);
            t.addVertex(x1, y1, z1);
            t.addVertex(x1, y0, z1);
            t.setNormal(0.0F, -1.0F, 0.0F);
            t.addVertex(x0, y0, z0);
            t.addVertex(x1, y0, z0);
            t.addVertex(x1, y0, z1);
            t.addVertex(x0, y0, z1);
            t.setNormal(0.0F, 1.0F, 0.0F);
            t.addVertex(x0, y1, z0);
            t.addVertex(x0, y1, z1);
            t.addVertex(x1, y1, z1);
            t.addVertex(x1, y1, z0);
            t.draw();
        }

        static void cylZ(float r1, float r2, float z0, float h) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 0.0F, z0);
            CYL.draw(r1, r2, h, 18, 1);
            GL11.glPopMatrix();
        }

        static void fins(float x0, float x1, float z0, float z1, float th) {
            for (int i = 0; i < 4; i++) {
                GL11.glPushMatrix();
                GL11.glRotatef(i * 90.0F, 0.0F, 0.0F, 1.0F);
                box(x0, -th, z0, x1, th, z1);
                GL11.glPopMatrix();
            }
        }

        static void modelBegin() {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        }

        // Bombas: nariz em +Z, comprimento em torno de 2.4
        static void drawBomb(int type, int hue, float time) {
            if (type == 4) {
                GL11.glPushMatrix();
                GL11.glScalef(0.2F, 0.2F, 0.2F);
                ship(0.5F, 0.35F, time, false);
                GL11.glPopMatrix();
                return;
            }
            modelBegin();
            GL11.glEnable(GL11.GL_LIGHTING);
            if (type == 0) {
                col(0.30F, 0.34F, 0.18F);
                cylZ(0.28F, 0.28F, -0.8F, 1.2F);
                cylZ(0.28F, 0.04F, 0.4F, 0.7F);
                cylZ(0.2F, 0.28F, -1.1F, 0.3F);
                col(0.9F, 0.8F, 0.1F);
                cylZ(0.285F, 0.285F, 0.05F, 0.08F);
                cylZ(0.285F, 0.285F, -0.5F, 0.08F);
                col(0.25F, 0.27F, 0.15F);
                fins(0.18F, 0.55F, -1.15F, -0.78F, 0.015F);
            } else if (type == 1) {
                col(0.75F, 0.77F, 0.8F);
                cylZ(0.3F, 0.3F, -0.9F, 1.6F);
                GL11.glPushMatrix();
                GL11.glTranslatef(0.0F, 0.0F, -0.9F);
                sph(0.3F, 16);
                GL11.glTranslatef(0.0F, 0.0F, 1.6F);
                sph(0.3F, 16);
                GL11.glPopMatrix();
                col(0.8F, 0.15F, 0.1F);
                cylZ(0.305F, 0.305F, 0.2F, 0.1F);
                cylZ(0.305F, 0.305F, -0.4F, 0.1F);
                col(1.0F, 0.5F, 0.1F);
                cylZ(0.305F, 0.305F, -0.1F, 0.06F);
                col(0.45F, 0.45F, 0.5F);
                fins(0.2F, 0.5F, -1.2F, -0.85F, 0.015F);
            } else if (type == 2) {
                col(0.2F, 0.28F, 0.25F);
                cylZ(0.42F, 0.42F, -0.6F, 1.1F);
                GL11.glPushMatrix();
                GL11.glTranslatef(0.0F, 0.0F, 0.5F);
                GL11.glScalef(1.0F, 1.0F, 1.3F);
                sph(0.42F, 18);
                GL11.glPopMatrix();
                cylZ(0.22F, 0.42F, -1.1F, 0.5F);
                col(0.95F, 0.8F, 0.1F);
                cylZ(0.425F, 0.425F, -0.2F, 0.25F);
                col(0.15F, 0.2F, 0.18F);
                fins(0.2F, 0.75F, -1.2F, -0.7F, 0.02F);
            } else if (type == 3) {
                col(0.85F, 0.65F, 0.2F);
                GL11.glPushMatrix();
                GL11.glScalef(0.55F, 0.55F, 0.8F);
                sph(1.0F, 22);
                GL11.glPopMatrix();
                col(0.7F, 0.12F, 0.1F);
                cylZ(0.56F, 0.56F, -0.05F, 0.08F);
                col(0.5F, 0.4F, 0.15F);
                cylZ(0.25F, 0.25F, -0.9F, 0.2F);
                col(0.75F, 0.55F, 0.18F);
                box(-0.42, -0.42, -1.25, 0.42, 0.42, -0.75);
                box(-0.62, -0.02, -1.25, 0.62, 0.02, -0.8);
                box(-0.02, -0.62, -1.25, 0.02, 0.62, -0.8);
                col(0.2F, 0.2F, 0.2F);
                GL11.glPushMatrix();
                GL11.glTranslatef(0.0F, 0.0F, 0.78F);
                sph(0.1F, 10);
                GL11.glPopMatrix();
            } else {
                float[] hc = hsv(hue / 360.0F, 0.9F, 1.0F);
                col(0.12F, 0.12F, 0.14F);
                cylZ(0.3F, 0.3F, -1.0F, 1.9F);
                cylZ(0.3F, 0.05F, 0.9F, 0.6F);
                cylZ(0.22F, 0.3F, -1.3F, 0.3F);
                col(0.18F, 0.18F, 0.2F);
                fins(0.2F, 0.6F, -1.3F, -0.8F, 0.02F);
                GL11.glDisable(GL11.GL_LIGHTING);
                col(hc[0], hc[1], hc[2]);
                cylZ(0.31F, 0.31F, 0.3F, 0.1F);
                cylZ(0.31F, 0.31F, -0.3F, 0.1F);
                cylZ(0.31F, 0.31F, -0.7F, 0.06F);
                for (int i = 0; i < 3; i++) {
                    GL11.glPushMatrix();
                    GL11.glRotatef(i * 120.0F, 0.0F, 0.0F, 1.0F);
                    box(0.29, -0.04, -0.15, 0.32, 0.04, 0.2);
                    GL11.glPopMatrix();
                }
                for (int i = 0; i < 4; i++) {
                    GL11.glPushMatrix();
                    GL11.glRotatef(i * 90.0F, 0.0F, 0.0F, 1.0F);
                    box(0.55, -0.025, -1.25, 0.6, 0.025, -0.85);
                    GL11.glPopMatrix();
                }
                GL11.glEnable(GL11.GL_LIGHTING);
            }
            GL11.glDisable(GL11.GL_NORMALIZE);
        }

        static float[] glowCol(float charge) {
            if (charge < 0.5F) {
                float k = charge * 2.0F;
                return new float[] {0.1F + 0.85F * k, 0.9F - 0.7F * k, 1.0F};
            }
            float k = (charge - 0.5F) * 2.0F;
            return new float[] {0.95F + 0.05F * k, 0.2F - 0.1F * k, 1.0F - 0.9F * k};
        }

        // Nave alienigena: nariz em +Z, comprimento ~10.7 (de -5.7 a +5)
        static void ship(float open, float charge, float t, boolean flame) {
            float[] gc = glowCol(charge);
            modelBegin();
            GL11.glEnable(GL11.GL_LIGHTING);
            col(0.20F, 0.14F, 0.32F);
            GL11.glPushMatrix();
            GL11.glScalef(1.3F, 1.3F, 5.0F);
            SPH.draw(1.0F, 28, 20);
            GL11.glPopMatrix();
            col(0.12F, 0.10F, 0.20F);
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 0.0F, -5.4F);
            CYL.draw(0.9F, 1.15F, 1.8F, 20, 1);
            GL11.glPopMatrix();
            col(0.30F, 0.22F, 0.45F);
            box(-0.18, 1.18, -3.0, 0.18, 1.5, 2.2);
            box(-0.18, -1.5, -3.0, 0.18, -1.18, 2.2);
            for (int i = 0; i < 6; i++) {
                GL11.glPushMatrix();
                GL11.glRotatef(i * 60.0F + 30.0F, 0.0F, 0.0F, 1.0F);
                GL11.glTranslatef(1.22F, 0.0F, 0.8F);
                GL11.glRotatef(open * 62.0F, 0.0F, 1.0F, 0.0F);
                col(0.16F, 0.12F, 0.26F);
                box(0.0, -0.55, 0.0, 0.12, 0.55, 4.0);
                GL11.glDisable(GL11.GL_LIGHTING);
                col(gc[0], gc[1], gc[2]);
                box(0.12, -0.04, 0.2, 0.17, 0.04, 3.9);
                box(0.12, -0.5, 3.8, 0.17, 0.5, 3.95);
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glPopMatrix();
            }
            GL11.glDisable(GL11.GL_LIGHTING);
            col(gc[0], gc[1], gc[2]);
            float[] zs = {-2.5F, -0.5F, 1.5F};
            for (int i = 0; i < zs.length; i++) {
                float z = zs[i];
                float r = 1.3F * (float) Math.sqrt(1.0F - (z / 5.0F) * (z / 5.0F)) + 0.03F;
                GL11.glPushMatrix();
                GL11.glTranslatef(0.0F, 0.0F, z);
                CYL.draw(r, r, 0.14F, 24, 1);
                GL11.glPopMatrix();
            }
            float cr = 0.3F + 0.35F * charge;
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 0.0F, 4.5F);
            col(gc[0], gc[1], gc[2]);
            SPH.draw(cr, 16, 12);
            col(1.0F, 1.0F, 1.0F);
            SPH.draw(cr * 0.5F, 12, 8);
            GL11.glPopMatrix();
            for (int k = 0; k < 4; k++) {
                float a = t * 2.5F + k * 1.5708F;
                float rad = 1.1F + charge * 0.8F;
                GL11.glPushMatrix();
                GL11.glTranslatef((float) Math.cos(a) * rad, (float) Math.sin(a) * rad, 3.6F);
                col(gc[0], gc[1], gc[2]);
                SPH.draw(0.1F + 0.1F * charge, 8, 6);
                GL11.glPopMatrix();
            }
            col(1.0F, 0.7F, 0.3F);
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 0.0F, -5.7F);
            SPH.draw(0.6F, 12, 8);
            GL11.glPopMatrix();
            if (flame) {
                boolean bl = GL11.glIsEnabled(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                float fl = 4.0F + (float) Math.sin(t * 40.0F) * 0.6F;
                GL11.glPushMatrix();
                GL11.glTranslatef(0.0F, 0.0F, -5.8F);
                GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
                col(gc[0], gc[1], gc[2], 0.45F);
                CYL.draw(0.7F, 0.0F, fl, 14, 1);
                col(1.0F, 1.0F, 1.0F, 0.5F);
                CYL.draw(0.35F, 0.0F, fl * 0.6F, 10, 1);
                GL11.glPopMatrix();
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                if (!bl) GL11.glDisable(GL11.GL_BLEND);
            }
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_NORMALIZE);
        }

        // Controle remoto: cabe em um cubo de +-0.5 centrado na origem
        static void controller() {
            modelBegin();
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, -0.08F, 0.0F);
            col(0.17F, 0.18F, 0.21F);
            box(-0.22, -0.42, -0.06, 0.22, 0.30, 0.06);
            col(0.35F, 0.36F, 0.40F);
            box(-0.24, -0.44, -0.05, 0.24, -0.40, 0.05);
            box(-0.24, 0.28, -0.05, 0.24, 0.32, 0.05);
            box(-0.25, -0.40, -0.05, -0.22, 0.28, 0.05);
            box(0.22, -0.40, -0.05, 0.25, 0.28, 0.05);
            col(0.6F, 0.6F, 0.65F);
            GL11.glPushMatrix();
            GL11.glTranslatef(0.15F, 0.30F, 0.0F);
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            CYL.draw(0.02F, 0.02F, 0.28F, 8, 1);
            GL11.glPopMatrix();
            GL11.glDisable(GL11.GL_LIGHTING);
            col(0.2F, 0.9F, 1.0F);
            box(-0.16, 0.03, 0.06, 0.16, 0.24, 0.075);
            col(1.0F, 0.2F, 0.2F);
            box(-0.12, 0.12, 0.075, 0.12, 0.14, 0.08);
            col(0.95F, 0.1F, 0.1F);
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, -0.14F, 0.06F);
            CYL.draw(0.11F, 0.09F, 0.05F, 16, 1);
            GL11.glPopMatrix();
            col(1.0F, 0.9F, 0.1F);
            box(-0.17, -0.35, 0.06, -0.09, -0.29, 0.075);
            col(0.2F, 1.0F, 0.3F);
            box(-0.04, -0.35, 0.06, 0.04, -0.29, 0.075);
            col(0.3F, 0.5F, 1.0F);
            box(0.09, -0.35, 0.06, 0.17, -0.29, 0.075);
            col(1.0F, 0.1F, 0.1F);
            GL11.glPushMatrix();
            GL11.glTranslatef(0.15F, 0.60F, 0.0F);
            SPH.draw(0.035F, 8, 6);
            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glPopMatrix();
            GL11.glDisable(GL11.GL_NORMALIZE);
        }

        static void orient(float nx, float ny, float nz) {
            float ax = -ny;
            float ay = nx;
            float len = (float) Math.sqrt(ax * ax + ay * ay);
            if (len < 1.0E-4F) {
                if (nz < 0.0F) GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                return;
            }
            float ang = (float) Math.toDegrees(Math.acos(Math.max(-1.0F, Math.min(1.0F, nz))));
            GL11.glRotatef(ang, ax / len, ay / len, 0.0F);
        }
    }

    // ============================================================================================
    // RENDERIZADORES
    // ============================================================================================
    @SideOnly(Side.CLIENT)
    public static class RenderBomb extends Render {
        @Override
        public void doRender(Entity e, double x, double y, double z, float yaw, float pt) {
            EntityBomb b = (EntityBomb) e;
            int type = b.bType();
            float tm = (b.ticksExisted + pt);
            GL11.glPushMatrix();
            GL11.glTranslated(x, y + 0.9D, z);
            GL11.glRotatef(tm * 9.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            float s = type == 3 ? 2.2F : 1.8F;
            GL11.glScalef(s, s, s);
            boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_CULL_FACE);
            Gfx.drawBomb(type, b.bHue(), tm / 20.0F);
            if (cull) GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity e) {
            return null;
        }
    }

    @SideOnly(Side.CLIENT)
    public static class RenderShip extends Render {
        @Override
        public void doRender(Entity e, double x, double y, double z, float yaw, float pt) {
            float tm = (e.ticksExisted + pt) / 20.0F;
            GL11.glPushMatrix();
            GL11.glTranslated(x, y + 4.1D, z);
            GL11.glRotatef(e.rotationYaw, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(0.72F, 0.72F, 0.72F);
            boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_CULL_FACE);
            Gfx.ship(0.15F, 0.3F + 0.15F * (float) Math.sin(tm * 3.0F), tm, false);
            if (cull) GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity e) {
            return null;
        }
    }

    @SideOnly(Side.CLIENT)
    public static class ControllerRenderer implements IItemRenderer {
        @Override
        public boolean handleRenderType(ItemStack item, ItemRenderType type) {
            return type != ItemRenderType.FIRST_PERSON_MAP;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
            return true;
        }

        @Override
        public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
            GL11.glPushMatrix();
            if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(-45.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(1.4F, 1.4F, 1.4F);
            }
            boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_CULL_FACE);
            Gfx.controller();
            if (cull) GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @SideOnly(Side.CLIENT)
    public static class ShipItemRenderer implements IItemRenderer {
        @Override
        public boolean handleRenderType(ItemStack item, ItemRenderType type) {
            return type != ItemRenderType.FIRST_PERSON_MAP;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
            return true;
        }

        @Override
        public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
            GL11.glPushMatrix();
            if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(-45.0F, 0.0F, 1.0F, 0.0F);
            }
            GL11.glScalef(0.085F, 0.085F, 0.085F);
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_CULL_FACE);
            Gfx.ship(0.3F, 0.3F, (System.currentTimeMillis() % 100000L) / 1000.0F, false);
            if (cull) GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    // ============================================================================================
    // PAINEL DO CONTROLE (GUI com mouse)
    // ============================================================================================
    @SideOnly(Side.CLIENT)
    public static class GuiControl extends GuiScreen {
        static DynamicTexture MAP_TEX;
        static ResourceLocation MAP_LOC;
        static final int[] ZOOMS = {1, 2, 4, 8};
        static final String[] LABELS = {"Raio da explosao", "Fogo", "Radiacao", "Cogumelo atomico", "Cor do brilho", "Altura da queda"};

        int tab = 0;
        int cardX, cardW, cardH, cardY0, rx0, rw, mapX, mapY, mapS, rcx, rcw;
        int originX, originZ;
        GuiTextField fx, fz;
        String info = "";
        boolean mapDirty = true;

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void initGui() {
            Keyboard.enableRepeatEvents(true);
            buttonList.clear();
            if (MAP_TEX == null) {
                MAP_TEX = new DynamicTexture(64, 64);
                MAP_LOC = mc.getTextureManager().getDynamicTextureLocation("bombmap", MAP_TEX);
            }
            if (Double.isNaN(Client.tgtX)) {
                Client.tgtX = mc.thePlayer.posX;
                Client.tgtZ = mc.thePlayer.posZ;
            }
            cardX = 6;
            cardW = Math.min(124, width / 3);
            cardY0 = 30;
            cardH = Math.max(26, (height - 40) / 6);
            rx0 = cardX + cardW + 8;
            rw = width - rx0 - 6;
            mapX = rx0;
            mapY = 30;
            mapS = Math.max(60, Math.min(height - 44, rw * 6 / 10));
            rcx = mapX + mapS + 6;
            rcw = Math.max(60, width - rcx - 6);
            int tw = (rw - 8) / 3;
            buttonList.add(new GuiButton(1, rx0, 6, tw, 18, "Alvo"));
            buttonList.add(new GuiButton(2, rx0 + tw + 4, 6, tw, 18, "Bomba 6"));
            buttonList.add(new GuiButton(3, rx0 + 2 * (tw + 4), 6, tw, 18, "Sol / Lua"));
            fx = null;
            fz = null;
            if (tab == 0) {
                int half = (rcw - 4) / 2;
                buttonList.add(new GuiButton(10, rcx, 30, half, 18, "Zoom -"));
                buttonList.add(new GuiButton(11, rcx + half + 4, 30, half, 18, "Zoom +"));
                buttonList.add(new GuiButton(12, rcx, 52, rcw, 18, Client.mode == 0 ? "Modo: MAPA" : "Modo: MIRA"));
                buttonList.add(new GuiButton(13, rcx, 74, rcw, 18, "Minha posicao"));
                fx = new GuiTextField(fontRendererObj, rcx + 12, 98, rcw - 12, 14);
                fz = new GuiTextField(fontRendererObj, rcx + 12, 116, rcw - 12, 14);
                fx.setMaxStringLength(9);
                fz.setMaxStringLength(9);
                fx.setText(String.valueOf((int) Math.floor(Client.tgtX)));
                fz.setText(String.valueOf((int) Math.floor(Client.tgtZ)));
                String lb;
                if (Client.mode == 1) {
                    lb = "MIRAR";
                } else {
                    lb = Client.selBomb == 4 ? "POSICIONAR NAVE" : "LANCAR BOMBA";
                }
                buttonList.add(new GuiButton(14, rcx, 138, rcw, 22, lb));
                if (Client.selBomb == 4) {
                    buttonList.add(new GuiButton(15, rcx, 164, rcw, 22, "DETONAR NAVE"));
                }
            } else if (tab == 1) {
                int bx = rx0 + rw - 90;
                buttonList.add(new GuiButton(20, bx, 34, 20, 18, "-"));
                buttonList.add(new GuiButton(21, bx + 66, 34, 20, 18, "+"));
                buttonList.add(new GuiButton(22, bx, 58, 86, 18, Client.cFire ? "SIM" : "NAO"));
                buttonList.add(new GuiButton(23, bx, 82, 86, 18, Client.cRad ? "SIM" : "NAO"));
                buttonList.add(new GuiButton(24, bx, 106, 86, 18, Client.cMush ? "SIM" : "NAO"));
                buttonList.add(new GuiButton(25, bx, 130, 20, 18, "-"));
                buttonList.add(new GuiButton(26, bx + 66, 130, 20, 18, "+"));
                buttonList.add(new GuiButton(27, bx, 154, 20, 18, "-"));
                buttonList.add(new GuiButton(28, bx + 66, 154, 20, 18, "+"));
            } else {
                int bw = Math.min(230, rw);
                int bx = rx0 + (rw - bw) / 2;
                buttonList.add(new GuiButton(30, bx, 44, bw, 22, "EXPLODIR O SOL"));
                buttonList.add(new GuiButton(31, bx, 72, bw, 22, "EXPLODIR A LUA"));
                buttonList.add(new GuiButton(32, bx, 100, bw, 22, "Restaurar Sol e Lua"));
            }
            mapDirty = true;
        }

        @Override
        public void onGuiClosed() {
            Keyboard.enableRepeatEvents(false);
        }

        @Override
        public void updateScreen() {
            if (fx != null) fx.updateCursorCounter();
            if (fz != null) fz.updateCursorCounter();
        }

        void parseFields() {
            if (fx == null || fz == null) return;
            try {
                Client.tgtX = Double.parseDouble(fx.getText().trim());
            } catch (NumberFormatException ex) {
                // ignora
            }
            try {
                Client.tgtZ = Double.parseDouble(fz.getText().trim());
            } catch (NumberFormatException ex) {
                // ignora
            }
        }

        @Override
        protected void actionPerformed(GuiButton b) {
            int id = b.id;
            if (id >= 1 && id <= 3) {
                parseFields();
                tab = id - 1;
                initGui();
                return;
            }
            if (id == 10) {
                Client.zoom = Math.max(0, Client.zoom - 1);
                mapDirty = true;
            } else if (id == 11) {
                Client.zoom = Math.min(3, Client.zoom + 1);
                mapDirty = true;
            } else if (id == 12) {
                parseFields();
                Client.mode = 1 - Client.mode;
                initGui();
            } else if (id == 13) {
                Client.tgtX = mc.thePlayer.posX;
                Client.tgtZ = mc.thePlayer.posZ;
                initGui();
            } else if (id == 14) {
                parseFields();
                if (Client.mode == 0) {
                    Client.sendLaunch(Client.selBomb, 0, Client.tgtX, mc.thePlayer.posY, Client.tgtZ);
                    info = "Enviado: " + BOMB_NAMES[Client.selBomb] + " em " + (int) Client.tgtX + ", " + (int) Client.tgtZ;
                } else {
                    Client.aiming = true;
                    mc.displayGuiScreen(null);
                }
            } else if (id == 15) {
                Client.sendLaunch(4, 3, 0.0D, 0.0D, 0.0D);
                mc.displayGuiScreen(null);
            } else if (id == 20) {
                Client.cRadius = Math.max(8, Client.cRadius - 4);
                initGui();
            } else if (id == 21) {
                Client.cRadius = Math.min(70, Client.cRadius + 4);
                initGui();
            } else if (id == 22) {
                Client.cFire = !Client.cFire;
                initGui();
            } else if (id == 23) {
                Client.cRad = !Client.cRad;
                initGui();
            } else if (id == 24) {
                Client.cMush = !Client.cMush;
                initGui();
            } else if (id == 25) {
                Client.cHue = (Client.cHue + 340) % 360;
                initGui();
            } else if (id == 26) {
                Client.cHue = (Client.cHue + 20) % 360;
                initGui();
            } else if (id == 27) {
                Client.cHeight = Math.max(40, Client.cHeight - 20);
                initGui();
            } else if (id == 28) {
                Client.cHeight = Math.min(230, Client.cHeight + 20);
                initGui();
            } else if (id == 30) {
                Client.sendLaunch(0, 1, 0.0D, 0.0D, 0.0D);
                mc.displayGuiScreen(null);
            } else if (id == 31) {
                Client.sendLaunch(0, 2, 0.0D, 0.0D, 0.0D);
                mc.displayGuiScreen(null);
            } else if (id == 32) {
                Client.sendLaunch(0, 4, 0.0D, 0.0D, 0.0D);
                info = "Pedido de restauracao enviado.";
            }
        }

        @Override
        protected void keyTyped(char c, int k) {
            if (fx != null && fz != null && (fx.isFocused() || fz.isFocused()) && k != Keyboard.KEY_ESCAPE) {
                fx.textboxKeyTyped(c, k);
                fz.textboxKeyTyped(c, k);
                if (k == Keyboard.KEY_RETURN) {
                    parseFields();
                    fx.setFocused(false);
                    fz.setFocused(false);
                }
                return;
            }
            super.keyTyped(c, k);
        }

        @Override
        protected void mouseClicked(int mx, int my, int btn) {
            super.mouseClicked(mx, my, btn);
            if (btn != 0) return;
            for (int i = 0; i < 6; i++) {
                int y = cardY0 + i * cardH;
                if (mx >= cardX && mx < cardX + cardW && my >= y && my < y + cardH - 2) {
                    parseFields();
                    Client.selBomb = i;
                    info = BOMB_NAMES[i] + ": " + BOMB_INFO[i];
                    initGui();
                    return;
                }
            }
            if (tab == 0 && fx != null && fz != null) {
                fx.mouseClicked(mx, my, btn);
                fz.mouseClicked(mx, my, btn);
                if (mx >= mapX && mx < mapX + mapS && my >= mapY && my < mapY + mapS) {
                    int sc = ZOOMS[Client.zoom];
                    Client.tgtX = originX + (mx - mapX) * (64.0D * sc) / mapS;
                    Client.tgtZ = originZ + (my - mapY) * (64.0D * sc) / mapS;
                    fx.setText(String.valueOf((int) Math.floor(Client.tgtX)));
                    fz.setText(String.valueOf((int) Math.floor(Client.tgtZ)));
                    info = "Alvo definido: " + (int) Client.tgtX + ", " + (int) Client.tgtZ;
                }
            }
        }

        void rebuildMap() {
            World w = mc.theWorld;
            int S = 64;
            int sc = ZOOMS[Client.zoom];
            originX = MathHelper.floor_double(mc.thePlayer.posX) - (S / 2) * sc;
            originZ = MathHelper.floor_double(mc.thePlayer.posZ) - (S / 2) * sc;
            int[] data = MAP_TEX.getTextureData();
            for (int cz = 0; cz < S; cz++) {
                for (int cx = 0; cx < S; cx++) {
                    int wx = originX + cx * sc;
                    int wz = originZ + cz * sc;
                    int col;
                    if (!w.blockExists(wx, 64, wz)) {
                        col = (((cx + cz) & 1) == 0) ? 0xFF1E1E22 : 0xFF26262B;
                    } else {
                        int h = w.getHeightValue(wx, wz);
                        int by = Math.max(0, h - 1);
                        Block b = w.getBlock(wx, by, wz);
                        int meta = w.getBlockMetadata(wx, by, wz);
                        int rgb = b.getMapColor(meta).colorValue;
                        int nh = h;
                        if (w.blockExists(wx - sc, 64, wz - sc)) nh = w.getHeightValue(wx - sc, wz - sc);
                        float sh = 0.9F + (h - nh) * 0.05F;
                        sh = Math.max(0.55F, Math.min(1.2F, sh));
                        int r = clamp((int) (((rgb >> 16) & 255) * sh), 0, 255);
                        int g = clamp((int) (((rgb >> 8) & 255) * sh), 0, 255);
                        int bl = clamp((int) ((rgb & 255) * sh), 0, 255);
                        col = 0xFF000000 | (r << 16) | (g << 8) | bl;
                    }
                    data[cz * S + cx] = col;
                }
            }
            MAP_TEX.updateDynamicTexture();
            mapDirty = false;
        }

        void drawMap(int mx, int my) {
            if (mapDirty) rebuildMap();
            int sc = ZOOMS[Client.zoom];
            double span = 64.0D * sc;
            drawRect(mapX - 2, mapY - 2, mapX + mapS + 2, mapY + mapS + 2, 0xFF000000);
            mc.getTextureManager().bindTexture(MAP_LOC);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.addVertexWithUV(mapX, mapY + mapS, 0.0D, 0.0D, 1.0D);
            t.addVertexWithUV(mapX + mapS, mapY + mapS, 0.0D, 1.0D, 1.0D);
            t.addVertexWithUV(mapX + mapS, mapY, 0.0D, 1.0D, 0.0D);
            t.addVertexWithUV(mapX, mapY, 0.0D, 0.0D, 0.0D);
            t.draw();
            // jogador
            int px = mapX + (int) ((mc.thePlayer.posX - originX) / span * mapS);
            int pz = mapY + (int) ((mc.thePlayer.posZ - originZ) / span * mapS);
            if (px >= mapX && px < mapX + mapS && pz >= mapY && pz < mapY + mapS) {
                drawRect(px - 2, pz - 2, px + 3, pz + 3, 0xFF000000);
                drawRect(px - 1, pz - 1, px + 2, pz + 2, 0xFF55FF55);
            }
            // alvo e raio
            int tx = mapX + (int) ((Client.tgtX - originX) / span * mapS);
            int tz = mapY + (int) ((Client.tgtZ - originZ) / span * mapS);
            double rad = (Client.selBomb == 5 ? Client.cRadius : BOMB_RADIUS[Client.selBomb]) / span * mapS;
            for (int i = 0; i < 40; i++) {
                double a = i * Math.PI * 2.0D / 40.0D;
                int cx = tx + (int) (Math.cos(a) * rad);
                int cy = tz + (int) (Math.sin(a) * rad);
                if (cx >= mapX && cx < mapX + mapS && cy >= mapY && cy < mapY + mapS) drawRect(cx, cy, cx + 2, cy + 2, 0xFFFF3333);
            }
            if (tx >= mapX && tx < mapX + mapS && tz >= mapY && tz < mapY + mapS) {
                drawRect(tx - 6, tz, tx + 7, tz + 1, 0xFFFF2222);
                drawRect(tx, tz - 6, tx + 1, tz + 7, 0xFFFF2222);
            }
            if (mx >= mapX && mx < mapX + mapS && my >= mapY && my < mapY + mapS) {
                int hx = (int) (originX + (mx - mapX) * span / mapS);
                int hz = (int) (originZ + (my - mapY) * span / mapS);
                fontRendererObj.drawStringWithShadow(hx + ", " + hz, mapX + 3, mapY + mapS - 11, 0xFFFFFF);
            }
        }

        void drawPreview(int type, int cx, int cy, float scale, long now) {
            boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glDisable(GL11.GL_CULL_FACE);
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glTranslatef(cx, cy, 100.0F);
            GL11.glScalef(scale, -scale, scale);
            GL11.glRotatef(18.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef((now % 6000L) * 0.06F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            Gfx.drawBomb(type, Client.cHue, (now % 100000L) / 1000.0F);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (cull) GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }

        @Override
        public void drawScreen(int mx, int my, float pt) {
            drawDefaultBackground();
            fontRendererObj.drawStringWithShadow("CONTROLE NUCLEAR", cardX, 10, 0xFF5544);
            long now = System.currentTimeMillis();
            for (int i = 0; i < 6; i++) {
                int y = cardY0 + i * cardH;
                boolean sel = i == Client.selBomb;
                boolean hov = mx >= cardX && mx < cardX + cardW && my >= y && my < y + cardH - 2;
                drawRect(cardX, y, cardX + cardW, y + cardH - 2, sel ? 0xCC803030 : (hov ? 0xAA404050 : 0xAA202028));
                if (sel) {
                    drawRect(cardX, y, cardX + cardW, y + 1, 0xFFFF5555);
                    drawRect(cardX, y + cardH - 3, cardX + cardW, y + cardH - 2, 0xFFFF5555);
                    drawRect(cardX, y, cardX + 1, y + cardH - 2, 0xFFFF5555);
                    drawRect(cardX + cardW - 1, y, cardX + cardW, y + cardH - 2, 0xFFFF5555);
                }
                drawPreview(i, cardX + cardH / 2, y + (cardH - 2) / 2, (cardH - 2) * 0.30F, now);
                int tx = cardX + cardH + 1;
                fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth((i + 1) + ". " + BOMB_NAMES[i], cardW - cardH - 2), tx, y + 4, 0xFFFFFF);
                if (cardH >= 34) {
                    fontRendererObj.drawString(fontRendererObj.trimStringToWidth(BOMB_INFO[i], cardW - cardH - 2), tx, y + 15, 0xAAAAAA);
                }
            }
            if (tab == 0) {
                drawMap(mx, my);
                fontRendererObj.drawStringWithShadow("X", rcx, 101, 0xFFFFFF);
                fontRendererObj.drawStringWithShadow("Z", rcx, 119, 0xFFFFFF);
                if (fx != null) fx.drawTextBox();
                if (fz != null) fz.drawTextBox();
                String hint = Client.mode == 0 ? "Clique no mapa para escolher o alvo" : "Modo mira: aponte e confirme com o botao direito";
                fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(hint, rw), rx0, mapY + mapS + 6, 0xCCCCCC);
            } else if (tab == 1) {
                int bx = rx0 + rw - 90;
                for (int i = 0; i < 6; i++) {
                    int y = 34 + i * 24;
                    fontRendererObj.drawStringWithShadow(LABELS[i], rx0, y + 5, 0xFFFFFF);
                }
                fontRendererObj.drawStringWithShadow(String.valueOf(Client.cRadius), bx + 20 + (46 - fontRendererObj.getStringWidth(String.valueOf(Client.cRadius))) / 2, 39, 0xFFDD66);
                float[] c = Gfx.hsv(Client.cHue / 360.0F, 0.9F, 1.0F);
                int rgb = ((int) (c[0] * 255) << 16) | ((int) (c[1] * 255) << 8) | (int) (c[2] * 255);
                drawRect(bx + 24, 132, bx + 62, 146, 0xFF000000 | rgb);
                fontRendererObj.drawStringWithShadow(String.valueOf(Client.cHeight), bx + 20 + (46 - fontRendererObj.getStringWidth(String.valueOf(Client.cHeight))) / 2, 159, 0xFFDD66);
                fontRendererObj.drawString(fontRendererObj.trimStringToWidth("Escolha a bomba 6 na esquerda e lance na aba Alvo.", rw), rx0, 182, 0xAAAAAA);
            } else {
                fontRendererObj.drawString(fontRendererObj.trimStringToWidth("Destroi o astro com uma animacao no ceu.", rw), rx0, 132, 0xCCCCCC);
                fontRendererObj.drawString(fontRendererObj.trimStringToWidth("Sem Sol: noite eterna. Sem Lua: chuva de meteoros.", rw), rx0, 144, 0xCCCCCC);
                fontRendererObj.drawString("So funciona no Overworld.", rx0, 156, 0xCCCCCC);
            }
            if (info.length() > 0) {
                fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(info, width - rx0 - 6), rx0, height - 12, 0xFFFF88);
            }
            super.drawScreen(mx, my, pt);
        }
    }

    // ============================================================================================
    // CUTSCENE DA NAVE (3D, estilo animacao): Terra -> nave decola -> carrega -> laser -> Terra parte ao meio
    // ============================================================================================
    @SideOnly(Side.CLIENT)
    public static class GuiCutscene extends GuiScreen {
        static final float ER = 3.0F;
        static final float SS = 0.55F;
        static final float[] U = norm(0.75F, 0.35F, 0.55F);
        static final float[] W = norm(-U[2], 0.0F, U[0]);
        static final DoubleBuffer CLIP = BufferUtils.createDoubleBuffer(4);
        static DynamicTexture EARTH, CLOUD;
        static ResourceLocation EARTH_LOC, CLOUD_LOC;
        static float[][] STARS, DEB;
        static final float[][] NEB = {{-90.0F, 40.0F, -120.0F, 70.0F, 0.5F, 0.2F, 0.9F}, {100.0F, -30.0F, -140.0F, 60.0F, 0.1F, 0.5F, 0.9F},
            {20.0F, 70.0F, -170.0F, 80.0F, 0.9F, 0.2F, 0.5F}, {-60.0F, -60.0F, 110.0F, 55.0F, 0.2F, 0.4F, 0.8F}};

        final float D;
        long start = 0L;
        final boolean[] cue = new boolean[8];

        public GuiCutscene(float seconds) {
            this.D = Math.max(6.0F, seconds);
        }

        static float[] norm(float x, float y, float z) {
            float l = (float) Math.sqrt(x * x + y * y + z * z);
            return new float[] {x / l, y / l, z / l};
        }

        static float smooth(float x) {
            float k = Math.max(0.0F, Math.min(1.0F, x));
            return k * k * (3.0F - 2.0F * k);
        }

        static float easeOut(float x) {
            float k = Math.max(0.0F, Math.min(1.0F, x));
            float q = 1.0F - k;
            return 1.0F - q * q * q;
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void initGui() {
            if (start == 0L) start = System.currentTimeMillis();
            ensureTextures();
        }

        @Override
        protected void keyTyped(char c, int k) {
            if (k == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(null);
        }

        @Override
        protected void mouseClicked(int x, int y, int b) {
        }

        void ensureTextures() {
            if (EARTH == null) {
                int w = 256;
                int h = 128;
                EARTH = new DynamicTexture(w, h);
                CLOUD = new DynamicTexture(w, h);
                int[] e = EARTH.getTextureData();
                int[] c = CLOUD.getTextureData();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        double lon = x / (double) w * Math.PI * 2.0D;
                        double lat = (y / (double) h - 0.5D) * Math.PI;
                        double f = Math.sin(lon * 3.0D + 1.1D * Math.sin(lat * 4.0D)) + 0.7D * Math.sin(lat * 5.0D + 2.0D * Math.cos(lon * 2.0D))
                            + 0.4D * Math.sin(lon * 7.0D + lat * 3.0D) + 0.25D * Math.sin(lon * 13.0D + lat * 9.0D);
                        int r, g, b;
                        if (Math.abs(lat) > 1.25D) {
                            r = 235;
                            g = 240;
                            b = 250;
                        } else if (f > 0.55D) {
                            double k = Math.min(1.0D, (f - 0.55D) / 1.2D);
                            r = (int) (45 + 110 * k);
                            g = (int) (130 - 30 * k);
                            b = (int) (45 + 10 * k);
                        } else {
                            double d = Math.max(0.0D, Math.min(1.0D, (f + 1.5D) / 2.0D));
                            r = (int) (8 + 20 * d);
                            g = (int) (40 + 60 * d);
                            b = (int) (120 + 70 * d);
                        }
                        e[y * w + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
                        double cf = Math.sin(lon * 5.0D + 2.0D * Math.sin(lat * 6.0D)) + Math.sin(lat * 9.0D + lon * 2.3D)
                            + 0.6D * Math.sin(lon * 11.0D - lat * 4.0D);
                        int a = cf > 0.9D ? (int) Math.min(200.0D, (cf - 0.9D) * 220.0D) : 0;
                        c[y * w + x] = (a << 24) | 0xFFFFFF;
                    }
                }
                EARTH.updateDynamicTexture();
                CLOUD.updateDynamicTexture();
                EARTH_LOC = mc.getTextureManager().getDynamicTextureLocation("bombearth", EARTH);
                CLOUD_LOC = mc.getTextureManager().getDynamicTextureLocation("bombcloud", CLOUD);
                Random rn = new Random(42L);
                STARS = new float[700][4];
                for (int i = 0; i < STARS.length; i++) {
                    float[] d = norm(rn.nextFloat() - 0.5F, rn.nextFloat() - 0.5F, rn.nextFloat() - 0.5F);
                    float rad = 120.0F + rn.nextFloat() * 40.0F;
                    STARS[i][0] = d[0] * rad;
                    STARS[i][1] = d[1] * rad;
                    STARS[i][2] = d[2] * rad;
                    STARS[i][3] = 0.4F + rn.nextFloat() * 0.6F;
                }
                DEB = new float[70][5];
                for (int i = 0; i < DEB.length; i++) {
                    float[] d = norm(rn.nextFloat() - 0.5F, rn.nextFloat() - 0.5F, rn.nextFloat() - 0.5F);
                    DEB[i][0] = d[0];
                    DEB[i][1] = d[1];
                    DEB[i][2] = d[2];
                    DEB[i][3] = 1.0F + rn.nextFloat() * 4.0F;
                    DEB[i][4] = 0.06F + rn.nextFloat() * 0.2F;
                }
            }
        }

        void playCues(float tt) {
            float[] at = {0.1F, 4.0F, 9.4F, 11.5F, 14.0F, 16.5F, 18.0F, 18.2F};
            String[] snd = {"portal.travel", "fireworks.launch", "mob.endermen.portal", "mob.wither.spawn", "mob.enderdragon.growl",
                "fireworks.launch", "random.explode", "ambient.weather.thunder"};
            float[] vol = {1.0F, 3.0F, 2.0F, 2.0F, 3.0F, 4.0F, 8.0F, 6.0F};
            float[] pit = {0.8F, 0.7F, 0.6F, 0.6F, 0.5F, 0.4F, 0.4F, 0.5F};
            for (int i = 0; i < at.length; i++) {
                if (!cue[i] && tt >= at[i]) {
                    cue[i] = true;
                    mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, snd[i], vol[i], pit[i], false);
                }
            }
        }

        @Override
        public void drawScreen(int mx, int my, float pt) {
            long now = System.currentTimeMillis();
            if (start == 0L) start = now;
            float t = (now - start) / 1000.0F;
            if (t >= D) {
                mc.displayGuiScreen(null);
                return;
            }
            float tt = t * 22.0F / D;
            playCues(tt);
            drawRect(0, 0, width, height, 0xFF000000);
            scene(tt, t);
            overlay(tt);
        }

        float[] camera(float tt, float spx, float spy, float spz, float nx, float ny, float nz) {
            float ex, ey, ez, tx, ty, tz, fov;
            if (tt < 4.0F) {
                float s = tt / 4.0F;
                ex = 2.5F * s;
                ey = 1.2F;
                ez = 15.0F - 4.0F * s;
                tx = 0.0F;
                ty = 0.0F;
                tz = 0.0F;
                fov = 50.0F;
            } else if (tt < 9.0F) {
                ex = 11.0F + (tt - 4.0F) * 0.3F;
                ey = 4.0F;
                ez = 12.0F;
                tx = spx * 0.5F;
                ty = spy * 0.5F;
                tz = spz * 0.5F;
                fov = 55.0F;
            } else if (tt < 14.0F) {
                float k = tt - 9.0F;
                float a = 0.6F + k * 0.5F;
                float r = 12.0F - k * 0.6F;
                ex = spx + (float) Math.cos(a) * r;
                ey = spy + 3.0F - k * 0.2F;
                ez = spz + (float) Math.sin(a) * r;
                tx = spx + nx;
                ty = spy + ny;
                tz = spz + nz;
                fov = 55.0F - k * 2.0F;
            } else if (tt < 16.5F) {
                ex = spx - nx * 9.0F + W[0] * 3.0F;
                ey = spy - ny * 9.0F + W[1] * 3.0F + 2.5F;
                ez = spz - nz * 9.0F + W[2] * 3.0F;
                tx = spx + nx * 14.0F;
                ty = spy + ny * 14.0F;
                tz = spz + nz * 14.0F;
                fov = 40.0F;
            } else if (tt < 18.0F) {
                ex = U[0] * 12.0F + W[0] * 14.0F;
                ey = U[1] * 12.0F + W[1] * 14.0F + 3.0F;
                ez = U[2] * 12.0F + W[2] * 14.0F;
                tx = U[0] * 12.0F;
                ty = U[1] * 12.0F;
                tz = U[2] * 12.0F;
                fov = 60.0F;
            } else {
                float k = tt - 18.0F;
                ex = 0.0F;
                ey = 1.5F + k * 0.1F;
                ez = 13.5F - k * 0.3F;
                tx = 0.0F;
                ty = 0.0F;
                tz = 0.0F;
                fov = 50.0F;
            }
            float sh = 0.0F;
            if (tt >= 18.0F && tt < 20.0F) {
                sh = (1.0F - (tt - 18.0F) / 2.0F) * 0.25F;
            } else if (tt >= 14.0F && tt < 16.5F) {
                sh = 0.04F;
            } else if (tt >= 11.0F && tt < 14.0F) {
                sh = 0.02F;
            }
            ex += (float) Math.sin(tt * 91.0F) * sh;
            ey += (float) Math.cos(tt * 77.0F) * sh;
            return new float[] {ex, ey, ez, tx, ty, tz, fov};
        }

        void scene(float tt, float t) {
            float dI = ER + 5.0F * SS;
            boolean shipVis = tt >= 4.0F && tt < 18.0F;
            float dShip;
            if (tt < 8.2F) {
                dShip = ER + 1.6F + (24.0F - ER - 1.6F) * easeOut((tt - 4.0F) / 4.2F);
            } else if (tt < 16.5F) {
                dShip = 24.0F;
            } else {
                float q = Math.min(1.0F, (tt - 16.5F) / 1.5F);
                dShip = 24.0F - (24.0F - dI) * q * q * q;
            }
            float flip = smooth((tt - 7.0F) / 2.0F);
            float cf = (float) Math.cos(Math.PI * flip);
            float sf = (float) Math.sin(Math.PI * flip);
            float nx = U[0] * cf + W[0] * sf;
            float ny = U[1] * cf + W[1] * sf;
            float nz = U[2] * cf + W[2] * sf;
            float spx = U[0] * dShip;
            float spy = U[1] * dShip;
            float spz = U[2] * dShip;
            float open = smooth((tt - 9.3F) / 2.5F);
            float charge = Math.max(0.0F, Math.min(1.0F, (tt - 9.0F) / 5.0F));
            float[] c = camera(tt, spx, spy, spz, nx, ny, nz);

            boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GLU.gluPerspective(c[6], (float) mc.displayWidth / (float) mc.displayHeight, 0.1F, 400.0F);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GLU.gluLookAt(c[0], c[1], c[2], c[3], c[4], c[5], 0.0F, 1.0F, 0.0F);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

            drawSpace(c);

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LIGHTING);
            RenderHelper.enableStandardItemLighting();
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            drawEarth(tt);
            if (shipVis) {
                GL11.glPushMatrix();
                GL11.glTranslatef(spx, spy, spz);
                Gfx.orient(nx, ny, nz);
                GL11.glScalef(SS, SS, SS);
                Gfx.ship(open, charge, t, tt < 9.0F || tt >= 16.5F);
                GL11.glPopMatrix();
            }

            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glDepthMask(false);
            Gfx.col(0.3F, 0.6F, 1.0F, 0.10F);
            Gfx.SPH.draw(ER + 0.12F, 32, 20);
            Gfx.col(0.3F, 0.6F, 1.0F, 0.06F);
            Gfx.SPH.draw(ER + 0.3F, 32, 20);
            Gfx.col(0.3F, 0.6F, 1.0F, 0.035F);
            Gfx.SPH.draw(ER + 0.55F, 32, 20);
            if (shipVis && tt >= 14.0F && tt < 16.5F) {
                float grow = smooth((tt - 14.0F) / 0.5F);
                float len = dShip - ER - 5.2F * SS;
                float[] gc = Gfx.glowCol(1.0F);
                GL11.glPushMatrix();
                GL11.glTranslatef(spx, spy, spz);
                Gfx.orient(nx, ny, nz);
                GL11.glTranslatef(0.0F, 0.0F, 5.2F * SS);
                Gfx.col(1.0F, 1.0F, 1.0F, 0.9F);
                Gfx.CYL.draw(0.12F * grow, 0.12F * grow, len, 10, 1);
                Gfx.col(gc[0], gc[1], gc[2], 0.45F);
                Gfx.CYL.draw(0.3F * grow, 0.3F * grow, len, 12, 1);
                Gfx.col(gc[0], gc[1], gc[2], 0.18F);
                Gfx.CYL.draw(0.6F * grow, 0.6F * grow, len, 12, 1);
                Gfx.col(1.0F, 0.6F, 0.5F, 0.6F);
                Gfx.SPH.draw(0.8F * grow, 12, 8);
                GL11.glPopMatrix();
            }
            if (tt >= 14.3F && tt < 18.0F) {
                float g = 0.2F + 0.9F * smooth((tt - 14.3F) / 3.7F);
                GL11.glPushMatrix();
                GL11.glTranslatef(U[0] * ER, U[1] * ER, U[2] * ER);
                Gfx.col(1.0F, 0.3F, 0.1F, 0.25F);
                Gfx.SPH.draw(g * 1.6F, 14, 10);
                Gfx.col(1.0F, 0.6F, 0.3F, 0.5F);
                Gfx.SPH.draw(g, 14, 10);
                Gfx.col(1.0F, 1.0F, 0.9F, 0.8F);
                Gfx.SPH.draw(g * 0.5F, 12, 8);
                GL11.glPopMatrix();
            }
            if (tt >= 18.0F) {
                float u = tt - 18.0F;
                GL11.glPushMatrix();
                GL11.glTranslatef(U[0] * ER, U[1] * ER, U[2] * ER);
                float fa = Math.max(0.0F, 1.0F - u / 1.6F);
                if (fa > 0.0F) {
                    float fr = 1.5F + u * 4.0F;
                    Gfx.col(1.0F, 0.5F, 0.15F, 0.35F * fa);
                    Gfx.SPH.draw(fr, 20, 14);
                    Gfx.col(1.0F, 0.95F, 0.8F, 0.7F * fa);
                    Gfx.SPH.draw(fr * 0.55F, 16, 12);
                }
                if (u < 3.5F) {
                    Gfx.col(1.0F, 0.85F, 0.6F, 0.45F * (1.0F - u / 3.5F));
                    Gfx.SPH.draw(u * 9.0F + 0.01F, 28, 18);
                    Gfx.col(0.6F, 0.8F, 1.0F, 0.30F * (1.0F - u / 3.5F));
                    Gfx.SPH.draw(u * 5.5F + 0.01F, 24, 16);
                }
                for (int i = 0; i < DEB.length; i++) {
                    float[] d = DEB[i];
                    float dd = d[3] * u;
                    GL11.glPushMatrix();
                    GL11.glTranslatef(d[0] * dd, d[1] * dd, d[2] * dd);
                    GL11.glRotatef(u * 200.0F * d[4], d[0], d[1], d[2]);
                    float al = Math.max(0.0F, 1.0F - u / 3.8F);
                    Gfx.col(1.0F, 0.45F + 0.4F * d[4], 0.1F, 0.9F * al);
                    Gfx.box(-d[4], -d[4], -d[4], d[4], d[4], d[4]);
                    GL11.glPopMatrix();
                }
                GL11.glPopMatrix();
            }
            GL11.glDepthMask(true);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CLIP_PLANE0);
            RenderHelper.disableStandardItemLighting();

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (cull) GL11.glEnable(GL11.GL_CULL_FACE);
        }

        void drawSpace(float[] c) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            for (int i = 0; i < NEB.length; i++) {
                float[] n = NEB[i];
                GL11.glPushMatrix();
                GL11.glTranslatef(n[0], n[1], n[2]);
                Gfx.col(n[4], n[5], n[6], 0.07F);
                Gfx.SPH.draw(n[3], 18, 12);
                Gfx.col(n[4], n[5], n[6], 0.07F);
                Gfx.SPH.draw(n[3] * 0.6F, 16, 10);
                GL11.glPopMatrix();
            }
            GL11.glPointSize(2.0F);
            GL11.glBegin(GL11.GL_POINTS);
            for (int i = 0; i < STARS.length; i++) {
                float[] s = STARS[i];
                GL11.glColor4f(s[3], s[3], Math.min(1.0F, s[3] * 1.1F), 1.0F);
                GL11.glVertex3f(c[0] + s[0], c[1] + s[1], c[2] + s[2]);
            }
            GL11.glEnd();
            GL11.glDepthMask(true);
        }

        void drawEarth(float tt) {
            float sep = tt < 18.1F ? 0.0F : 0.14F * (float) Math.pow(tt - 18.1F, 1.8F);
            float rot = tt * 5.0F;
            float crack = smooth((tt - 14.2F) / 3.8F);
            if (sep <= 0.001F) {
                earthSphere(rot, crack);
            } else {
                for (int s = -1; s <= 1; s += 2) {
                    GL11.glPushMatrix();
                    GL11.glTranslatef(s * sep, 0.0F, 0.0F);
                    CLIP.clear();
                    CLIP.put((double) s).put(0.0D).put(0.0D).put(0.0D);
                    CLIP.flip();
                    GL11.glClipPlane(GL11.GL_CLIP_PLANE0, CLIP);
                    GL11.glEnable(GL11.GL_CLIP_PLANE0);
                    earthSphere(rot, crack);
                    GL11.glDisable(GL11.GL_CLIP_PLANE0);
                    cutFace(tt);
                    GL11.glPopMatrix();
                }
            }
        }

        void earthSphere(float rot, float crack) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            mc.getTextureManager().bindTexture(EARTH_LOC);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPushMatrix();
            GL11.glRotatef(rot, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            Gfx.SPH_T.draw(ER, 48, 32);
            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            mc.getTextureManager().bindTexture(CLOUD_LOC);
            GL11.glPushMatrix();
            GL11.glRotatef(rot * 1.4F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            Gfx.SPH_T.draw(ER + 0.05F, 40, 26);
            GL11.glPopMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            if (crack > 0.01F) {
                GL11.glDisable(GL11.GL_LIGHTING);
                Gfx.col(1.0F, 0.25F, 0.05F, 0.38F * crack);
                Gfx.SPH.draw(ER + 0.03F, 32, 20);
                GL11.glEnable(GL11.GL_LIGHTING);
            }
            GL11.glDisable(GL11.GL_BLEND);
        }

        void cutFace(float tt) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glPushMatrix();
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            float fl = 0.85F + 0.15F * (float) Math.sin(tt * 30.0F);
            Gfx.col(0.30F * fl, 0.16F, 0.08F);
            Gfx.DSK.draw(2.7F, 3.0F, 48, 1);
            Gfx.col(fl, 0.42F * fl, 0.08F);
            Gfx.DSK.draw(1.2F, 2.72F, 48, 1);
            Gfx.col(1.0F, 0.92F * fl, 0.5F);
            Gfx.DSK.draw(0.0F, 1.22F, 40, 1);
            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_LIGHTING);
        }

        void txt(String s, float cy, float scale, int rgb, float alpha) {
            int a = (int) (alpha * 255.0F);
            if (a < 10) return;
            GL11.glPushMatrix();
            GL11.glScalef(scale, scale, 1.0F);
            int w = fontRendererObj.getStringWidth(s);
            fontRendererObj.drawStringWithShadow(s, (int) (width / 2.0F / scale - w / 2.0F), (int) (cy / scale), (a << 24) | rgb);
            GL11.glPopMatrix();
        }

        static float win(float tt, float a, float b) {
            return Math.max(0.0F, Math.min(1.0F, Math.min((tt - a) / 0.5F, (b - tt) / 0.5F)));
        }

        void overlay(float tt) {
            GL11.glDisable(GL11.GL_LIGHTING);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            int bar = (int) (height * 0.11F);
            float sl = 0.0F;
            if (tt >= 16.5F && tt < 18.0F) {
                sl = 1.0F;
            } else if (tt >= 18.0F && tt < 19.0F) {
                sl = 1.0F - (tt - 18.0F);
            }
            if (sl > 0.0F) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glLineWidth(2.0F);
                Random r = new Random((long) (tt * 40.0F));
                GL11.glBegin(GL11.GL_LINES);
                for (int i = 0; i < 40; i++) {
                    float a = r.nextFloat() * 6.2832F;
                    float r0 = (0.25F + 0.2F * r.nextFloat()) * height;
                    float r1 = r0 + (0.25F + 0.4F * r.nextFloat()) * height;
                    float cx = width / 2.0F;
                    float cy = height / 2.0F;
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.5F * sl);
                    GL11.glVertex2f(cx + (float) Math.cos(a) * r0, cy + (float) Math.sin(a) * r0);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.0F);
                    GL11.glVertex2f(cx + (float) Math.cos(a) * r1, cy + (float) Math.sin(a) * r1);
                }
                GL11.glEnd();
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }
            drawRect(0, 0, width, bar, 0xFF000000);
            drawRect(0, height - bar, width, height, 0xFF000000);
            drawGradientRect(0, bar, width, bar + 26, 0x99000000, 0x00000000);
            drawGradientRect(0, height - bar - 26, width, height - bar, 0x00000000, 0x99000000);

            float ty = height - bar - 44;
            txt("T E R R A", ty, 3.0F, 0x66CCFF, win(tt, 0.3F, 3.7F));
            txt("Ultimo sinal de vida detectado...", ty + 30.0F, 1.1F, 0xFFFFFF, win(tt, 0.8F, 3.7F));
            txt("A NAVE DECOLA", ty, 3.0F, 0xCC88FF, win(tt, 4.4F, 8.6F));
            txt("Rumo ao alvo...", ty + 30.0F, 1.1F, 0xFFFFFF, win(tt, 5.0F, 8.6F));
            txt("MODO DE DESTRUICAO", ty, 2.6F, 0xFF4488, win(tt, 9.2F, 14.0F));
            txt("FOGO!", ty - 6.0F, 4.0F, 0xFF4040, win(tt, 14.2F, 16.4F));
            txt("O FIM DO MUNDO", ty, 3.0F, 0xFFAA44, win(tt, 18.4F, 21.0F));
            txt("Nao ha para onde fugir.", ty + 30.0F, 1.1F, 0xFFFFFF, win(tt, 19.0F, 21.0F));
            if (tt >= 9.0F && tt < 14.2F) {
                float ch = Math.min(1.0F, (tt - 9.0F) / 5.0F);
                int bw = Math.min(200, width - 40);
                int bx = (width - bw) / 2;
                int by = height - bar - 16;
                float[] gc = Gfx.glowCol(ch);
                int rgb = ((int) (gc[0] * 255.0F) << 16) | ((int) (gc[1] * 255.0F) << 8) | (int) (gc[2] * 255.0F);
                drawRect(bx - 1, by - 1, bx + bw + 1, by + 9, 0xFF000000);
                drawRect(bx, by, bx + (int) (bw * ch), by + 8, 0xFF000000 | rgb);
                String pc = (int) (ch * 100.0F) + "%";
                fontRendererObj.drawStringWithShadow(pc, (width - fontRendererObj.getStringWidth(pc)) / 2, by - 11, 0xFFFFFF);
            }
            float wf = 0.0F;
            float[] cuts = {4.0F, 9.0F, 14.0F, 16.5F};
            for (int i = 0; i < cuts.length; i++) {
                float dd = Math.abs(tt - cuts[i]);
                if (dd < 0.1F) wf = Math.max(wf, 0.8F * (1.0F - dd / 0.1F));
            }
            if (tt >= 18.0F && tt < 19.5F) {
                wf = Math.max(wf, tt < 18.1F ? 1.0F : 1.0F - (tt - 18.1F) / 1.4F);
            }
            if (tt > 20.8F) wf = Math.max(wf, Math.min(1.0F, (tt - 20.8F) / 1.2F));
            if (wf > 0.02F) {
                drawRect(0, 0, width, height, ((int) (Math.min(1.0F, wf) * 255.0F) << 24) | 0xFFFFFF);
            }
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    // ============================================================================================
    // CEU CUSTOMIZADO (usado quando o Sol e/ou a Lua foram destruidos)
    // ============================================================================================
    @SideOnly(Side.CLIENT)
    public static class SkyRenderer extends IRenderHandler {
        static final ResourceLocation SUN = new ResourceLocation("textures/environment/sun.png");
        static final ResourceLocation MOON = new ResourceLocation("textures/environment/moon_phases.png");
        static float[][] STARS;

        static void vtx(Tessellator t, double el, double az, float m, float[] fog, float[] sky) {
            float r = fog[0] + (sky[0] - fog[0]) * m;
            float g = fog[1] + (sky[1] - fog[1]) * m;
            float b = fog[2] + (sky[2] - fog[2]) * m;
            t.setColorOpaque_F(r, g, b);
            double R = 100.0D;
            t.addVertex(R * Math.cos(el) * Math.cos(az), R * Math.sin(el), R * Math.cos(el) * Math.sin(az));
        }

        @Override
        public void render(float pt, WorldClient world, Minecraft mc) {
            if (STARS == null) {
                Random rn = new Random(10842L);
                STARS = new float[1500][3];
                for (int i = 0; i < STARS.length; i++) {
                    double x = rn.nextDouble() * 2.0D - 1.0D;
                    double y = rn.nextDouble() * 2.0D - 1.0D;
                    double z = rn.nextDouble() * 2.0D - 1.0D;
                    double l = Math.sqrt(x * x + y * y + z * z);
                    if (l < 0.01D || l > 1.0D) {
                        i--;
                        continue;
                    }
                    STARS[i][0] = (float) (x / l * 90.0D);
                    STARS[i][1] = (float) (y / l * 90.0D);
                    STARS[i][2] = (float) (z / l * 90.0D);
                }
            }
            Tessellator t = Tessellator.instance;
            Vec3 sk = world.getSkyColor(mc.renderViewEntity, pt);
            Vec3 fg = world.getFogColor(pt);
            float[] sky = {(float) sk.xCoord, (float) sk.yCoord, (float) sk.zCoord};
            float[] fog = {(float) fg.xCoord, (float) fg.yCoord, (float) fg.zCoord};
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(false);
            RenderHelper.disableStandardItemLighting();
            float[] els = {-35.0F, 0.0F, 12.0F, 28.0F, 55.0F, 90.0F};
            float[] mix = {0.0F, 0.0F, 0.45F, 0.75F, 1.0F, 1.0F};
            int seg = 16;
            for (int i = 0; i < els.length - 1; i++) {
                for (int s = 0; s < seg; s++) {
                    double a0 = s * Math.PI * 2.0D / seg;
                    double a1 = (s + 1) * Math.PI * 2.0D / seg;
                    double e0 = Math.toRadians(els[i]);
                    double e1 = Math.toRadians(els[i + 1]);
                    t.startDrawingQuads();
                    vtx(t, e0, a0, mix[i], fog, sky);
                    vtx(t, e0, a1, mix[i], fog, sky);
                    vtx(t, e1, a1, mix[i + 1], fog, sky);
                    vtx(t, e1, a0, mix[i + 1], fog, sky);
                    t.draw();
                }
            }
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glPushMatrix();
            GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(world.getCelestialAngle(pt) * 360.0F, 1.0F, 0.0F, 0.0F);
            float sb = world.getStarBrightness(pt);
            if (sb > 0.0F) {
                GL11.glPointSize(2.0F);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, sb);
                GL11.glBegin(GL11.GL_POINTS);
                for (int i = 0; i < STARS.length; i++) {
                    GL11.glVertex3f(STARS[i][0], STARS[i][1], STARS[i][2]);
                }
                GL11.glEnd();
            }
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            float rain = 1.0F - world.getRainStrength(pt);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, rain);
            if (!Client.sunDead) {
                mc.getTextureManager().bindTexture(SUN);
                float f = 30.0F;
                t.startDrawingQuads();
                t.addVertexWithUV(-f, 100.0D, -f, 0.0D, 0.0D);
                t.addVertexWithUV(f, 100.0D, -f, 1.0D, 0.0D);
                t.addVertexWithUV(f, 100.0D, f, 1.0D, 1.0D);
                t.addVertexWithUV(-f, 100.0D, f, 0.0D, 1.0D);
                t.draw();
            }
            if (!Client.moonDead) {
                mc.getTextureManager().bindTexture(MOON);
                float f = 20.0F;
                int k = world.getMoonPhase();
                int l = k % 4;
                int i1 = k / 4 % 2;
                float u0 = l / 4.0F;
                float v0 = i1 / 2.0F;
                float u1 = (l + 1) / 4.0F;
                float v1 = (i1 + 1) / 2.0F;
                t.startDrawingQuads();
                t.addVertexWithUV(-f, -100.0D, f, u1, v1);
                t.addVertexWithUV(f, -100.0D, f, u0, v1);
                t.addVertexWithUV(f, -100.0D, -f, u0, v0);
                t.addVertexWithUV(-f, -100.0D, -f, u1, v0);
                t.draw();
            }
            GL11.glPopMatrix();
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_FOG);
            GL11.glDepthMask(true);
        }
    }
}
