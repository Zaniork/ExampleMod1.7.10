package com.myname.mymodid;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

@Mod(modid = HorrorMod.MODID, name = "Floresta do Cervo", version = "1.0", acceptedMinecraftVersions = "[1.7.10]")
public class HorrorMod {

    public static final String MODID = "horrormod";

    @Mod.Instance(MODID)
    public static HorrorMod instance;

    public static Item flashlightItem;
    public static Item cabinItem;

    // ================= CICLO DE VIDA =================
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        flashlightItem = new ItemHorror(0);
        cabinItem = new ItemHorror(1);
        GameRegistry.registerItem(flashlightItem, "horror_flashlight");
        GameRegistry.registerItem(cabinItem, "horror_cabin");
        EntityRegistry.registerModEntity(EntityDeer.class, "horror_deer", 1, instance, 80, 3, true);
        EntityRegistry.registerModEntity(EntityProp.class, "horror_prop", 2, instance, 80, 20, false);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        LanguageRegistry lang = LanguageRegistry.instance();
        lang.addStringLocalization("item.horror_flashlight.name", "Lanterna");
        lang.addStringLocalization("item.horror_cabin.name", "Kit de Cabana");
        FMLCommonHandler.instance().bus().register(new ServerEvents());
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientSide.init();
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        Game.reset();
        e.registerServerCommand(new CommandPlay());
    }

    static void msg(EntityPlayer p, String s) {
        p.addChatMessage(new ChatComponentText(s));
    }

    static void giveOrDrop(EntityPlayer p, ItemStack st) {
        if (!p.inventory.addItemStackToInventory(st)) {
            p.worldObj.spawnEntityInWorld(new EntityItem(p.worldObj, p.posX, p.posY + 0.5, p.posZ, st));
        }
    }

    // ================= COMANDO /play =================
    public static class CommandPlay extends CommandBase {
        @Override
        public String getCommandName() {
            return "play";
        }

        @Override
        public String getCommandUsage(ICommandSender s) {
            return "/play";
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
            if (s instanceof EntityPlayer) {
                if (Game.phase == 1) msg((EntityPlayer) s, "O jogo ja esta em andamento.");
                else Game.pendingGui = 1;
            }
        }
    }

    // ================= EVENTOS =================
    public static class ServerEvents {
        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent e) {
            if (e.phase == TickEvent.Phase.END) Game.tick();
        }
    }

    public static class ForgeEvents {
        @SubscribeEvent
        public void onInteract(PlayerInteractEvent e) {
            if (Game.phase == 1 && e.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) e.setCanceled(true);
        }

        @SubscribeEvent
        public void onBreak(BlockEvent.BreakEvent e) {
            if (Game.phase == 1) e.setCanceled(true);
        }
    }

    // ================= ESTADO DO JOGO (so funciona em mundo solo) =================
    public static class Game {
        public static volatile int phase = 0;        // 0 espera, 1 jogando, 2 venceu, 3 perdeu
        public static volatile int pendingGui = 0;   // 1 menu inicial, 2 fim, 3 bancada
        public static volatile boolean startRequested = false;
        public static volatile boolean craftRequested = false;
        public static volatile long elapsed = 0;
        public static volatile int day = 1;

        static final int DAY_TICKS = 6000;           // 5 minutos
        static final int TOTAL_DAYS = 10;
        static final int TASK_BONUS = 1200;          // cada task concluida tira 1 minuto do jogo
        static final int[] CABIN_COST = {10, 6, 4, 3, 0};
        public static final String[] RES_NAMES = {"Madeira", "Folha", "Pedra", "Corda", "Fruta"};
        static final int[][] TASKS = {
            {6, 0, 0, 0, 0}, {7, 4, 0, 0, 0}, {0, 0, 5, 0, 0}, {0, 0, 0, 3, 4},
            {5, 0, 3, 0, 0}, {0, 6, 0, 2, 0}, {0, 0, 0, 0, 6}, {4, 5, 4, 0, 0}};

        public static final int[] res = new int[5];
        public static final int[] need = new int[5];
        public static final int[] have = new int[5];
        public static volatile boolean taskDone = false;
        static int taskIndex = -1;
        static int taskBlock = -1;

        static boolean arenaBuilt = false;
        static int cx, cz, g;
        static boolean cabinBuilt = false;
        static int cabX, cabY, cabZ;
        static String prevDaylight = null;
        static String prevMobs = null;
        static final ArrayList<double[]> NOISES = new ArrayList<double[]>();
        static final Random RND = new Random();

        static void reset() {
            phase = 0;
            pendingGui = 0;
            startRequested = false;
            craftRequested = false;
            elapsed = 0;
            day = 1;
            arenaBuilt = false;
            cabinBuilt = false;
            taskBlock = -1;
            taskIndex = -1;
            taskDone = false;
            NOISES.clear();
            for (int i = 0; i < 5; i++) {
                res[i] = 0;
                need[i] = 0;
                have[i] = 0;
            }
        }

        public static String taskText() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                if (need[i] > 0) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(RES_NAMES[i]).append(' ').append(Math.min(have[i], need[i])).append('/').append(need[i]);
                }
            }
            return sb.toString();
        }

        // ---------- barulho ----------
        static boolean insideCabin(double x, double y, double z) {
            return cabinBuilt && Math.abs(x - cabX) < 2.6 && Math.abs(z - cabZ) < 2.6 && y >= cabY - 1 && y <= cabY + 4;
        }

        static void noise(World w, double x, double y, double z, double radius) {
            if (phase != 1 || insideCabin(x, y, z)) return;
            NOISES.add(new double[] {x, y, z, radius, w.getTotalWorldTime()});
            if (NOISES.size() > 8) NOISES.remove(0);
        }

        static double[] bestNoise(World w, double px, double py, double pz) {
            double[] best = null;
            long now = w.getTotalWorldTime();
            for (double[] n : NOISES) {
                if (now - (long) n[4] > 600) continue;
                double dx = n[0] - px;
                double dy = n[1] - py;
                double dz = n[2] - pz;
                if (Math.sqrt(dx * dx + dy * dy + dz * dz) > n[3]) continue;
                if (best == null || n[4] > best[4]) best = n;
            }
            return best;
        }

        // ---------- inicio ----------
        static void clearEntities(World w) {
            for (Object o : new ArrayList<Object>(w.loadedEntityList)) {
                if (o instanceof EntityProp || o instanceof EntityDeer) ((Entity) o).setDead();
            }
        }

        static void start(EntityPlayer p) {
            World w = p.worldObj;
            if (w.provider.dimensionId != 0) {
                msg(p, "Use /play no Overworld (de preferencia em um mundo Superflat).");
                return;
            }
            if (!arenaBuilt) {
                cx = MathHelper.floor_double(p.posX);
                cz = MathHelper.floor_double(p.posZ);
                g = w.getTopSolidOrLiquidBlock(cx, cz);
                msg(p, "Preparando a floresta... (pode travar alguns segundos)");
                Arena.build(w, cx, cz, g);
                arenaBuilt = true;
            }
            clearEntities(w);
            Arena.populate(w, cx, cz, g);

            elapsed = 0;
            day = 1;
            taskBlock = -1;
            taskIndex = -1;
            taskDone = false;
            cabinBuilt = false;
            NOISES.clear();
            for (int i = 0; i < 5; i++) {
                res[i] = 0;
                need[i] = 0;
                have[i] = 0;
            }

            GameRules gr = w.getGameRules();
            if (prevDaylight == null) {
                prevDaylight = gr.getGameRuleStringValue("doDaylightCycle");
                prevMobs = gr.getGameRuleStringValue("doMobSpawning");
            }
            gr.setOrCreateGameRule("doDaylightCycle", "false");
            gr.setOrCreateGameRule("doMobSpawning", "false");
            for (Object o : new ArrayList<Object>(w.loadedEntityList)) {
                if (o instanceof EntityMob) ((Entity) o).setDead();
            }

            p.inventory.clearInventory(null, -1);
            p.setPositionAndUpdate(cx + 0.5, g, cz + 0.5);
            p.capabilities.disableDamage = true;
            p.sendPlayerAbilities();
            p.setHealth(p.getMaxHealth());
            w.setWorldTime(13000L);

            startRequested = false;
            pendingGui = 0;
            phase = 1;
            msg(p, "A noite caiu. O cervo escuta tudo. Nao faca barulho...");
        }

        static void finish(EntityPlayer p) {
            World w = p.worldObj;
            GameRules gr = w.getGameRules();
            if (prevDaylight != null) gr.setOrCreateGameRule("doDaylightCycle", prevDaylight);
            if (prevMobs != null) gr.setOrCreateGameRule("doMobSpawning", prevMobs);
            prevDaylight = null;
            prevMobs = null;
            p.capabilities.disableDamage = false;
            p.sendPlayerAbilities();
        }

        static void lose(EntityPlayer p, String reason) {
            if (phase != 1) return;
            phase = 3;
            pendingGui = 2;
            finish(p);
            msg(p, reason);
        }

        static void win(EntityPlayer p) {
            if (phase != 1) return;
            phase = 2;
            pendingGui = 2;
            finish(p);
            msg(p, "O sol nasceu pela decima vez. Voce sobreviveu!");
        }

        static void checkKill(EntityDeer d) {
            List players = d.worldObj.playerEntities;
            for (int i = 0; i < players.size(); i++) {
                EntityPlayer p = (EntityPlayer) players.get(i);
                if (d.getDistanceSqToEntity(p) < 6.0D) {
                    lose(p, "O cervo te encontrou.");
                    return;
                }
            }
        }

        // ---------- tempo e tasks ----------
        static void newTask(EntityPlayer p, int block) {
            taskBlock = block;
            int idx;
            do {
                idx = RND.nextInt(TASKS.length);
            } while (idx == taskIndex);
            taskIndex = idx;
            for (int i = 0; i < 5; i++) {
                need[i] = TASKS[idx][i];
                have[i] = 0;
            }
            taskDone = false;
            msg(p, "NOVA TASK: " + taskText());
        }

        static void checkTask(EntityPlayer p) {
            if (taskDone) return;
            for (int i = 0; i < 5; i++) {
                if (have[i] < need[i]) return;
            }
            taskDone = true;
            elapsed += TASK_BONUS;
            msg(p, "Task concluida! O tempo do jogo diminuiu 1 minuto.");
        }

        static void tick() {
            World w = DimensionManager.getWorld(0);
            if (w == null || w.playerEntities.isEmpty()) return;
            EntityPlayer p = (EntityPlayer) w.playerEntities.get(0);

            if (startRequested) {
                startRequested = false;
                if (phase != 1) {
                    start(p);
                    return;
                }
            }
            if (craftRequested) {
                craftRequested = false;
                craft(p);
            }
            if (phase != 1) return;

            elapsed++;
            day = (int) Math.min((long) TOTAL_DAYS, elapsed / DAY_TICKS + 1);
            double pr = (elapsed % DAY_TICKS) / (double) DAY_TICKS;
            long mc = pr < 0.6 ? 13000L + (long) (pr / 0.6 * 11000.0) : (long) ((pr - 0.6) / 0.4 * 13000.0);
            w.setWorldTime(mc);

            int block = (day - 1) / 2;
            if (block != taskBlock) newTask(p, block);
            if (elapsed % 20 == 0 && p.isSprinting()) noise(w, p.posX, p.posY, p.posZ, 14.0);
            if (elapsed % 100 == 0) p.getFoodStats().addStats(1, 0.0F);
            if (elapsed >= (long) TOTAL_DAYS * DAY_TICKS) win(p);
        }

        // ---------- interacao com objetos ----------
        static void interact(EntityProp e, EntityPlayer p) {
            if (phase != 1) return;
            World w = e.worldObj;
            int k = e.kind();
            if (k < 5) {
                res[k]++;
                if (!taskDone && have[k] < need[k]) have[k]++;
                w.playSoundAtEntity(p, "step.grass", 1.0F, 0.9F);
                noise(w, e.posX, e.posY, e.posZ, 24.0);
                e.setDead();
                checkTask(p);
            } else if (k == 10) {
                w.playSoundEffect(e.posX, e.posY, e.posZ, "mob.horse.zombie.idle", 4.0F, 0.5F);
                noise(w, e.posX, e.posY, e.posZ, 45.0);
                msg(p, "O radio solta um berro de cervo... ele ouviu.");
            } else if (k == 11) {
                w.playSoundEffect(e.posX, e.posY, e.posZ, "random.fizz", 2.0F, 0.5F);
                noise(w, e.posX, e.posY, e.posZ, 32.0);
                msg(p, "A TV chia alto...");
            } else if (k == 12) {
                giveOrDrop(p, new ItemStack(flashlightItem));
                w.playSoundAtEntity(p, "random.click", 1.0F, 0.7F);
                noise(w, e.posX, e.posY, e.posZ, 26.0);
                e.setDead();
                msg(p, "Voce pegou a lanterna. Segure ela na mao para enxergar mais longe.");
            } else if (k == 13) {
                pendingGui = 3;
            }
        }

        static void craft(EntityPlayer p) {
            if (phase != 1) return;
            for (int i = 0; i < 5; i++) {
                if (res[i] < CABIN_COST[i]) {
                    msg(p, "Faltam recursos para a cabana.");
                    return;
                }
            }
            for (int i = 0; i < 5; i++) res[i] -= CABIN_COST[i];
            giveOrDrop(p, new ItemStack(cabinItem));
            noise(p.worldObj, p.posX, p.posY, p.posZ, 12.0);
            msg(p, "Voce montou o kit de cabana! Use no chao para construir.");
        }

        static void buildCabin(World w, int x, int y, int z, EntityPlayer p) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Arena.set(w, x + dx, y, z + dz, Blocks.planks, 0);
                    for (int h = 1; h <= 3; h++) {
                        boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                        boolean door = dz == 2 && dx == 0 && h <= 2;
                        if (wall && !door) Arena.set(w, x + dx, y + h, z + dz, Blocks.planks, 0);
                        else Arena.set(w, x + dx, y + h, z + dz, Blocks.air, 0);
                    }
                    Arena.set(w, x + dx, y + 4, z + dz, Blocks.planks, 0);
                }
            }
            Arena.set(w, x + 1, y + 1, z - 1, Blocks.torch, 5);
            cabinBuilt = true;
            cabX = x;
            cabY = y;
            cabZ = z;
            noise(w, x, y, z, 30.0);
            msg(p, "Cabana pronta! Barulho feito la dentro o cervo nao escuta.");
        }
    }

    // ================= CENARIO: floresta, cerca de ferro e torres =================
    public static class Arena {
        static final int R = 36;
        static final int[][] TOWERS = {{-24, -24}, {24, -24}, {-24, 24}, {24, 24}};

        static void set(World w, int x, int y, int z, Block b, int meta) {
            w.setBlock(x, y, z, b, meta, 2);
        }

        static void build(World w, int cx, int cz, int g) {
            for (int i = -R; i <= R; i++) {
                for (int h = 0; h < 6; h++) {
                    set(w, cx + i, g + h, cz - R, Blocks.iron_bars, 0);
                    set(w, cx + i, g + h, cz + R, Blocks.iron_bars, 0);
                    set(w, cx - R, g + h, cz + i, Blocks.iron_bars, 0);
                    set(w, cx + R, g + h, cz + i, Blocks.iron_bars, 0);
                }
            }
            for (int[] t : TOWERS) tower(w, cx + t[0], cz + t[1], g);

            Random r = new Random(cx * 31L + cz * 17L + g);
            for (int ix = -30; ix <= 30; ix += 7) {
                for (int iz = -30; iz <= 30; iz += 7) {
                    if (Math.abs(ix) < 8 && Math.abs(iz) < 8) continue;
                    boolean near = false;
                    for (int[] t : TOWERS) {
                        if (Math.abs(ix - t[0]) < 9 && Math.abs(iz - t[1]) < 9) near = true;
                    }
                    if (near) continue;
                    tree(w, cx + ix + r.nextInt(3) - 1, g, cz + iz + r.nextInt(3) - 1, 5 + r.nextInt(3));
                }
            }
        }

        static void tree(World w, int x, int g, int z, int h) {
            for (int y = 0; y < h; y++) set(w, x, g + y, z, Blocks.log, 0);
            for (int yy = h - 2; yy <= h + 1; yy++) {
                int rad = yy >= h ? 1 : 2;
                for (int dx = -rad; dx <= rad; dx++) {
                    for (int dz = -rad; dz <= rad; dz++) {
                        if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                        if (w.isAirBlock(x + dx, g + yy, z + dz)) set(w, x + dx, g + yy, z + dz, Blocks.leaves, 4);
                    }
                }
            }
        }

        static void tower(World w, int tx, int tz, int g) {
            int top = g + 11;
            for (int y = g; y <= top + 5; y++) {
                for (int sx = -4; sx <= 4; sx += 8) {
                    for (int sz = -4; sz <= 4; sz += 8) set(w, tx + sx, y, tz + sz, Blocks.log, 0);
                }
            }
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    set(w, tx + dx, top, tz + dz, Blocks.planks, 0);
                    set(w, tx + dx, top + 5, tz + dz, Blocks.planks, 0);
                    boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                    boolean entry = dx == 4 && dz == -4;
                    boolean post = Math.abs(dx) == 4 && Math.abs(dz) == 4;
                    if (edge && !entry && !post) set(w, tx + dx, top + 1, tz + dz, Blocks.fence, 0);
                }
            }
            // escada: sobe pelo lado norte e dobra para a plataforma
            for (int i = 0; i <= 11; i++) {
                int sx = i <= 10 ? -5 + i : 5;
                int sz = i <= 10 ? -5 : -4;
                int y = g + i;
                for (int yy = g; yy < y; yy++) set(w, tx + sx, yy, tz + sz, Blocks.planks, 0);
                set(w, tx + sx, y, tz + sz, Blocks.oak_stairs, i <= 10 ? 0 : 2);
            }
        }

        static void prop(World w, int kind, double x, double y, double z) {
            EntityProp e = new EntityProp(w);
            e.setKind(kind);
            e.setLocationAndAngles(x, y, z, Game.RND.nextFloat() * 360.0F, 0.0F);
            w.spawnEntityInWorld(e);
        }

        static void populate(World w, int cx, int cz, int g) {
            Random r = Game.RND;
            int[] counts = {30, 24, 20, 14, 20};
            for (int k = 0; k < 5; k++) {
                for (int i = 0; i < counts[k]; i++) {
                    for (int tries = 0; tries < 30; tries++) {
                        int x = cx + r.nextInt(2 * R - 8) - (R - 4);
                        int z = cz + r.nextInt(2 * R - 8) - (R - 4);
                        if (Math.abs(x - cx) < 3 && Math.abs(z - cz) < 6) continue;
                        boolean nearTower = false;
                        for (int[] t : TOWERS) {
                            if (Math.abs(x - (cx + t[0])) < 9 && Math.abs(z - (cz + t[1])) < 9) nearTower = true;
                        }
                        if (nearTower) continue;
                        if (!w.isAirBlock(x, g, z) || !w.isAirBlock(x, g + 1, z)) continue;
                        prop(w, k, x + 0.5, g, z + 0.5);
                        break;
                    }
                }
            }
            prop(w, 13, cx + 0.5, g, cz + 5.5);
            double topY = g + 12;
            for (int i = 0; i < 4; i++) {
                double tx = cx + TOWERS[i][0] + 0.5;
                double tz = cz + TOWERS[i][1] + 0.5;
                prop(w, 10, tx - 2.5, topY, tz + 2.5);
                prop(w, 11, tx + 2.5, topY, tz + 2.5);
                if (i == 0 || i == 3) prop(w, 12, tx, topY, tz - 2.5);
            }
            for (int tries = 0; tries < 30; tries++) {
                int x = cx + (r.nextBoolean() ? 1 : -1) * (18 + r.nextInt(10));
                int z = cz + (r.nextBoolean() ? 1 : -1) * (18 + r.nextInt(10));
                if (!w.isAirBlock(x, g, z)) continue;
                EntityDeer d = new EntityDeer(w);
                d.setLocationAndAngles(x + 0.5, g, z + 0.5, r.nextFloat() * 360.0F, 0.0F);
                w.spawnEntityInWorld(d);
                break;
            }
        }
    }

    // ================= ITENS (lanterna e kit de cabana) =================
    public static class ItemHorror extends Item {
        final int kind;

        public ItemHorror(int kind) {
            this.kind = kind;
            setUnlocalizedName(kind == 0 ? "horror_flashlight" : "horror_cabin");
            setMaxStackSize(1);
            setCreativeTab(CreativeTabs.tabMisc);
        }

        @Override
        public void registerIcons(IIconRegister r) {}

        @Override
        public IIcon getIconFromDamage(int d) {
            return Items.iron_ingot.getIconFromDamage(0);
        }

        @Override
        public boolean onItemUse(ItemStack s, EntityPlayer p, World w, int x, int y, int z, int side, float hx, float hy, float hz) {
            if (kind == 1 && !w.isRemote && Game.phase == 1) {
                Game.buildCabin(w, x, y, z, p);
                s.stackSize--;
                return true;
            }
            return kind == 1;
        }
    }

    // ================= ENTIDADE: objetos coletaveis, radio, TV, bancada =================
    public static class EntityProp extends Entity {
        int sizedFor = -1;

        public EntityProp(World w) {
            super(w);
            setSize(0.7F, 0.45F);
        }

        @Override
        protected void entityInit() {
            dataWatcher.addObject(20, Integer.valueOf(0));
        }

        public int kind() {
            return dataWatcher.getWatchableObjectInt(20);
        }

        public void setKind(int k) {
            dataWatcher.updateObject(20, Integer.valueOf(k));
            applySize(k);
        }

        void applySize(int k) {
            sizedFor = k;
            if (k == 13) setSize(1.4F, 1.0F);
            else if (k == 11) setSize(0.8F, 0.8F);
            else if (k >= 10) setSize(0.5F, 0.5F);
            else setSize(0.7F, 0.45F);
        }

        @Override
        public void onUpdate() {
            int k = kind();
            if (k != sizedFor) applySize(k);
        }

        @Override
        public boolean canBeCollidedWith() {
            return true;
        }

        @Override
        public boolean canBePushed() {
            return false;
        }

        @Override
        public boolean attackEntityFrom(DamageSource s, float a) {
            return false;
        }

        @Override
        public boolean interactFirst(EntityPlayer p) {
            if (!worldObj.isRemote) Game.interact(this, p);
            return true;
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound t) {
            setKind(t.getInteger("kind"));
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound t) {
            t.setInteger("kind", kind());
        }
    }

    // ================= ENTIDADE: o cervo =================
    public static class EntityDeer extends EntityCreature {
        public EntityDeer(World w) {
            super(w);
            setSize(0.9F, 1.9F);
            stepHeight = 1.1F;
            tasks.addTask(1, new AIGoToNoise(this));
            tasks.addTask(2, new EntityAILookIdle(this));
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(200.0D);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.27D);
            getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(64.0D);
        }

        @Override
        public boolean isAIEnabled() {
            return true;
        }

        @Override
        protected boolean canDespawn() {
            return false;
        }

        @Override
        public boolean getCanSpawnHere() {
            return false;
        }

        // sobe paredes, arvores e torres como uma aranha
        @Override
        public boolean isOnLadder() {
            return isCollidedHorizontally;
        }

        @Override
        protected String getLivingSound() {
            return "mob.horse.zombie.idle";
        }

        @Override
        protected float getSoundPitch() {
            return 0.6F;
        }

        @Override
        protected float getSoundVolume() {
            return 2.0F;
        }

        @Override
        protected String getHurtSound() {
            return null;
        }

        @Override
        protected String getDeathSound() {
            return null;
        }

        @Override
        public boolean attackEntityFrom(DamageSource s, float a) {
            return s == DamageSource.outOfWorld && super.attackEntityFrom(s, a);
        }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            if (!worldObj.isRemote && Game.phase == 1) Game.checkKill(this);
        }
    }

    // o cervo so reage a barulho: vai ate perto da direcao do som mais recente
    public static class AIGoToNoise extends EntityAIBase {
        final EntityDeer d;
        double tx, ty, tz;
        int timer;

        public AIGoToNoise(EntityDeer d) {
            this.d = d;
            setMutexBits(1);
        }

        boolean pick() {
            if (Game.phase != 1) return false;
            double[] n = Game.bestNoise(d.worldObj, d.posX, d.posY, d.posZ);
            if (n == null) return false;
            double dx = n[0] - d.posX;
            double dz = n[2] - d.posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 2.5) return false;
            double step = dist > 12.0 ? dist * 0.6 : dist;
            tx = d.posX + dx / dist * step;
            tz = d.posZ + dz / dist * step;
            ty = (n[1] > d.posY + 2.0 && dist < 25.0) ? n[1] : d.posY;
            timer = 120;
            return true;
        }

        void move() {
            if (!d.getNavigator().tryMoveToXYZ(tx, ty, tz, 1.0D)) {
                d.getMoveHelper().setMoveTo(tx, ty, tz, 1.0D);
            }
        }

        @Override
        public boolean shouldExecute() {
            return pick();
        }

        @Override
        public void startExecuting() {
            move();
        }

        @Override
        public boolean continueExecuting() {
            double dx = tx - d.posX;
            double dz = tz - d.posZ;
            return Game.phase == 1 && timer > 0 && (dx * dx + dz * dz) > 4.0;
        }

        @Override
        public void updateTask() {
            timer--;
            if (timer % 10 == 0) move();
        }

        @Override
        public void resetTask() {
            d.getNavigator().clearPathEntity();
        }
    }

    // ================= CLIENTE =================
    @SideOnly(Side.CLIENT)
    public static class ClientSide {
        public static void init() {
            RenderingRegistry.registerEntityRenderingHandler(EntityDeer.class, new RenderDeer());
            RenderingRegistry.registerEntityRenderingHandler(EntityProp.class, new RenderProp());
            MinecraftForgeClient.registerItemRenderer(flashlightItem, new PropItemRenderer(12));
            MinecraftForgeClient.registerItemRenderer(cabinItem, new PropItemRenderer(20));
            FMLCommonHandler.instance().bus().register(new ClientBus());
            MinecraftForge.EVENT_BUS.register(new ClientForge());
        }

        public static void leaveWorld() {
            Minecraft mc = Minecraft.getMinecraft();
            Game.reset();
            if (mc.theWorld != null) mc.theWorld.sendQuittingDisconnectingPacket();
            mc.loadWorld((WorldClient) null);
            mc.displayGuiScreen(new GuiMainMenu());
        }
    }

    // abre as telas pedidas pelo servidor (mundo solo: mesmo programa)
    @SideOnly(Side.CLIENT)
    public static class ClientBus {
        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.thePlayer == null) return;
            int gui = Game.pendingGui;
            if (gui != 0) {
                Game.pendingGui = 0;
                if (gui == 1) mc.displayGuiScreen(new GuiStartMenu());
                else if (gui == 3) mc.displayGuiScreen(new GuiBench());
                else mc.displayGuiScreen(new GuiEnd());
            }
            if ((Game.phase == 2 || Game.phase == 3) && !Game.startRequested && !(mc.currentScreen instanceof GuiEnd)) {
                mc.displayGuiScreen(new GuiEnd());
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static class ClientForge {
        // visao curta (6 blocos); com a lanterna na mao fica bem maior
        @SubscribeEvent
        public void onFogDensity(EntityViewRenderEvent.FogDensity e) {
            if (Game.phase != 1) return;
            Minecraft mc = Minecraft.getMinecraft();
            ItemStack held = mc.thePlayer != null ? mc.thePlayer.getCurrentEquippedItem() : null;
            boolean light = held != null && held.getItem() == flashlightItem;
            e.density = light ? 0.17F : 0.5F;
            e.setCanceled(true);
        }

        @SubscribeEvent
        public void onFogColor(EntityViewRenderEvent.FogColors e) {
            if (Game.phase != 1) return;
            e.red = 0.02F;
            e.green = 0.02F;
            e.blue = 0.03F;
        }

        @SubscribeEvent
        public void onText(RenderGameOverlayEvent.Text e) {
            if (Game.phase != 1) return;
            long left = (long) Game.TOTAL_DAYS * Game.DAY_TICKS - Game.elapsed;
            int s = (int) Math.max(0L, left / 20L);
            e.left.add("Dia " + Game.day + " de " + Game.TOTAL_DAYS + "   Tempo restante: " + (s / 60) + ":"
                + (s % 60 < 10 ? "0" : "") + (s % 60));
            e.left.add(Game.taskDone ? "Task concluida! Outra task a cada 2 dias." : "Task: " + Game.taskText());
            StringBuilder sb = new StringBuilder("Recursos: ");
            for (int i = 0; i < 5; i++) {
                sb.append(Game.RES_NAMES[i]).append(' ').append(Game.res[i]);
                if (i < 4) sb.append(" | ");
            }
            e.left.add(sb.toString());
        }
    }

    // ---------- telas de terror ----------
    @SideOnly(Side.CLIENT)
    public static abstract class GuiHorror extends GuiScreen {
        int tick = 0;

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void updateScreen() {
            tick++;
        }

        void backdrop() {
            drawRect(0, 0, width, height, 0xF0060000);
            for (int i = 0; i < 28; i++) {
                int col = ((28 - i) * 4 << 24) | 0x700000;
                drawRect(0, i, width, i + 1, col);
                drawRect(0, height - i - 1, width, height - i, col);
                drawRect(i, 0, i + 1, height, col);
                drawRect(width - i - 1, 0, width - i, height, col);
            }
        }

        void title(String text, int y) {
            int jx = (tick % 23 < 3) ? (tick % 3) - 1 : 0;
            int col = (tick % 40 < 34) ? 0xFFB00000 : 0xFF600000;
            GL11.glPushMatrix();
            GL11.glScalef(2.0F, 2.0F, 1.0F);
            drawCenteredString(fontRendererObj, text, width / 4 + jx, y / 2, col);
            GL11.glPopMatrix();
        }

        static boolean in(int px, int py, int cx, int y, int w, int h) {
            return px >= cx - w / 2 && px <= cx + w / 2 && py >= y && py <= y + h;
        }

        void button(int cx, int y, int w, int h, String label, int mx, int my, boolean enabled) {
            boolean hover = enabled && in(mx, my, cx, y, w, h);
            drawRect(cx - w / 2 - 1, y - 1, cx + w / 2 + 1, y + h + 1, enabled ? 0xFF700000 : 0xFF303030);
            drawRect(cx - w / 2, y, cx + w / 2, y + h, hover ? 0xFF5A0000 : 0xFF260000);
            drawCenteredString(fontRendererObj, label, cx, y + (h - 8) / 2,
                !enabled ? 0xFF666666 : (hover ? 0xFFFFFFFF : 0xFFCC9999));
        }
    }

    @SideOnly(Side.CLIENT)
    public static class GuiStartMenu extends GuiHorror {
        @Override
        public void drawScreen(int mx, int my, float pt) {
            backdrop();
            title("NAO FACA BARULHO", height / 2 - 80);
            drawCenteredString(fontRendererObj, "Ele nao ve. Ele so escuta.", width / 2, height / 2 - 44, 0xFF994444);
            drawCenteredString(fontRendererObj, "Sobreviva 10 dias na floresta.", width / 2, height / 2 - 30, 0xFF994444);
            button(width / 2, height / 2, 160, 24, "JOGAR", mx, my, true);
            button(width / 2, height / 2 + 32, 160, 24, "SAIR DO MUNDO", mx, my, true);
            super.drawScreen(mx, my, pt);
        }

        @Override
        protected void mouseClicked(int mx, int my, int btn) {
            if (btn != 0) return;
            if (in(mx, my, width / 2, height / 2, 160, 24)) {
                Game.startRequested = true;
                mc.displayGuiScreen(null);
            } else if (in(mx, my, width / 2, height / 2 + 32, 160, 24)) {
                ClientSide.leaveWorld();
            }
        }
    }

    // tela de fim: nao fecha com ESC
    @SideOnly(Side.CLIENT)
    public static class GuiEnd extends GuiHorror {
        @Override
        protected void keyTyped(char c, int key) {}

        @Override
        public void drawScreen(int mx, int my, float pt) {
            backdrop();
            boolean win = Game.phase == 2;
            title(win ? "VOCE SOBREVIVEU" : "O CERVO TE ENCONTROU", height / 2 - 80);
            drawCenteredString(fontRendererObj, win ? "O sol nasceu... por enquanto." : "Voce fez barulho demais.",
                width / 2, height / 2 - 44, 0xFF994444);
            button(width / 2, height / 2, 160, 24, "JOGAR DE NOVO", mx, my, true);
            button(width / 2, height / 2 + 32, 160, 24, "SAIR DO MUNDO", mx, my, true);
            super.drawScreen(mx, my, pt);
        }

        @Override
        protected void mouseClicked(int mx, int my, int btn) {
            if (btn != 0) return;
            if (in(mx, my, width / 2, height / 2, 160, 24)) {
                Game.startRequested = true;
                mc.displayGuiScreen(null);
            } else if (in(mx, my, width / 2, height / 2 + 32, 160, 24)) {
                ClientSide.leaveWorld();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static class GuiBench extends GuiHorror {
        boolean can() {
            for (int i = 0; i < 5; i++) {
                if (Game.res[i] < Game.CABIN_COST[i]) return false;
            }
            return true;
        }

        @Override
        public void drawScreen(int mx, int my, float pt) {
            backdrop();
            title("BANCADA DE IMPROVISO", height / 2 - 90);
            drawCenteredString(fontRendererObj, "Cabana: junte os recursos", width / 2, height / 2 - 56, 0xFF994444);
            int y = height / 2 - 40;
            for (int i = 0; i < 5; i++) {
                if (Game.CABIN_COST[i] > 0) {
                    boolean ok = Game.res[i] >= Game.CABIN_COST[i];
                    drawCenteredString(fontRendererObj, Game.RES_NAMES[i] + ": " + Game.res[i] + " / " + Game.CABIN_COST[i],
                        width / 2, y, ok ? 0xFF88CC88 : 0xFFCC8888);
                    y += 12;
                }
            }
            button(width / 2, height / 2 + 30, 180, 24, "CONSTRUIR KIT DE CABANA", mx, my, can());
            button(width / 2, height / 2 + 62, 180, 24, "VOLTAR", mx, my, true);
            super.drawScreen(mx, my, pt);
        }

        @Override
        protected void mouseClicked(int mx, int my, int btn) {
            if (btn != 0) return;
            if (can() && in(mx, my, width / 2, height / 2 + 30, 180, 24)) {
                Game.craftRequested = true;
                mc.displayGuiScreen(null);
            } else if (in(mx, my, width / 2, height / 2 + 62, 180, 24)) {
                mc.displayGuiScreen(null);
            }
        }
    }

    // ---------- texturas feitas por codigo ----------
    @SideOnly(Side.CLIENT)
    public static class Tex {
        static boolean ready = false;
        public static ResourceLocation WHITE, WOOD, LEAF, ROCK, ROPE, STATIC, DEER;

        static int clamp(int v) {
            return v < 0 ? 0 : (v > 255 ? 255 : v);
        }

        static int rgb(int r, int g, int b) {
            return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
        }

        static ResourceLocation make(String name, BufferedImage im) {
            ResourceLocation rl = new ResourceLocation(MODID, "dyn_" + name);
            Minecraft.getMinecraft().getTextureManager().loadTexture(rl, new DynamicTexture(im));
            return rl;
        }

        public static void bind(ResourceLocation rl) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(rl);
        }

        public static void init() {
            if (ready) return;
            ready = true;
            BufferedImage im;

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) im.setRGB(x, y, 0xFFFFFFFF);
            WHITE = make("white", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int n = ((x * 3 + (y / 2) * 5) % 4) * 8;
                    im.setRGB(x, y, rgb(95 + n, 62 + n / 2, 34 + n / 3));
                }
            }
            WOOD = make("wood", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int n = ((x * 7 + y * 11 + x * y) % 5) * 9;
                    boolean vein = x == 7 || x == 8;
                    im.setRGB(x, y, vein ? rgb(110, 170, 60) : rgb(35 + n, 100 + n, 30 + n / 2));
                }
            }
            LEAF = make("leaf", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int n = ((x * 5 + y * 9 + x * y * 3) % 7) * 7;
                    im.setRGB(x, y, rgb(95 + n, 95 + n, 100 + n));
                }
            }
            ROCK = make("rock", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    boolean band = ((x + y) % 4) < 2;
                    im.setRGB(x, y, band ? rgb(190, 160, 100) : rgb(140, 115, 70));
                }
            }
            ROPE = make("rope", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Random r = new Random(7L);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int v = 60 + r.nextInt(170);
                    im.setRGB(x, y, rgb(v, v, v));
                }
            }
            STATIC = make("static", im);

            im = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    int n = (x * 13 + y * 7 + x * y * 3) % 9;
                    int col = rgb(100 + n * 3, 68 + n * 2, 40 + n * 2);
                    if (x >= 36 && x < 52 && y >= 36 && y < 56) col = rgb(215, 200, 165);
                    if (x >= 56 && y < 4) col = rgb(255, 30, 30);
                    im.setRGB(x, y, col);
                }
            }
            DEER = make("deer", im);
        }
    }

    // ---------- modelos 3D feitos por codigo ----------
    @SideOnly(Side.CLIENT)
    public static class Models {
        static final int FULL = 15728880;
        static int br = -1;
        static boolean beam = false;

        static void begin() {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        static void end() {
            GL11.glPopAttrib();
        }

        static void box(ResourceLocation tex, double x1, double y1, double z1, double x2, double y2, double z2,
            float r, float g, float b, float a) {
            Tex.bind(tex);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            if (br >= 0) t.setBrightness(br);
            t.setColorRGBA_F(r, g, b, a);
            t.addVertexWithUV(x1, y2, z1, 0, 0);
            t.addVertexWithUV(x1, y2, z2, 0, 1);
            t.addVertexWithUV(x2, y2, z2, 1, 1);
            t.addVertexWithUV(x2, y2, z1, 1, 0);
            t.setColorRGBA_F(r * 0.5f, g * 0.5f, b * 0.5f, a);
            t.addVertexWithUV(x1, y1, z1, 0, 0);
            t.addVertexWithUV(x2, y1, z1, 1, 0);
            t.addVertexWithUV(x2, y1, z2, 1, 1);
            t.addVertexWithUV(x1, y1, z2, 0, 1);
            t.setColorRGBA_F(r * 0.8f, g * 0.8f, b * 0.8f, a);
            t.addVertexWithUV(x1, y1, z1, 0, 1);
            t.addVertexWithUV(x1, y2, z1, 0, 0);
            t.addVertexWithUV(x2, y2, z1, 1, 0);
            t.addVertexWithUV(x2, y1, z1, 1, 1);
            t.addVertexWithUV(x1, y1, z2, 0, 1);
            t.addVertexWithUV(x2, y1, z2, 1, 1);
            t.addVertexWithUV(x2, y2, z2, 1, 0);
            t.addVertexWithUV(x1, y2, z2, 0, 0);
            t.setColorRGBA_F(r * 0.65f, g * 0.65f, b * 0.65f, a);
            t.addVertexWithUV(x1, y1, z1, 0, 1);
            t.addVertexWithUV(x1, y1, z2, 1, 1);
            t.addVertexWithUV(x1, y2, z2, 1, 0);
            t.addVertexWithUV(x1, y2, z1, 0, 0);
            t.addVertexWithUV(x2, y1, z1, 0, 1);
            t.addVertexWithUV(x2, y2, z1, 0, 0);
            t.addVertexWithUV(x2, y2, z2, 1, 0);
            t.addVertexWithUV(x2, y1, z2, 1, 1);
            t.draw();
        }

        // 0 madeira, 1 folha, 2 pedra, 3 corda, 4 fruta, 10 radio, 11 TV, 12 lanterna, 13 bancada, 20 kit de cabana
        static void drawProp(int kind, int light) {
            br = light;
            begin();
            Tex.init();
            switch (kind) {
                case 0:
                    box(Tex.WOOD, -0.30, 0.00, -0.10, 0.30, 0.20, 0.10, 1f, 1f, 1f, 1f);
                    box(Tex.WOOD, -0.22, 0.20, -0.08, 0.26, 0.34, 0.08, 0.9f, 0.9f, 0.9f, 1f);
                    break;
                case 1:
                    for (int i = 0; i < 3; i++) {
                        GL11.glPushMatrix();
                        GL11.glRotatef(i * 50.0F, 0.0F, 1.0F, 0.0F);
                        box(Tex.LEAF, -0.22, 0.02 + 0.02 * i, -0.12, 0.22, 0.05 + 0.02 * i, 0.12, 1f, 1f, 1f, 1f);
                        GL11.glPopMatrix();
                    }
                    break;
                case 2:
                    box(Tex.ROCK, -0.22, 0.00, -0.18, 0.20, 0.22, 0.16, 1f, 1f, 1f, 1f);
                    box(Tex.ROCK, -0.10, 0.22, -0.08, 0.14, 0.34, 0.10, 0.9f, 0.9f, 0.9f, 1f);
                    break;
                case 3:
                    box(Tex.ROPE, -0.20, 0.00, -0.20, 0.20, 0.06, 0.20, 1f, 1f, 1f, 1f);
                    box(Tex.ROPE, -0.17, 0.06, -0.17, 0.17, 0.12, 0.17, 1f, 1f, 1f, 1f);
                    box(Tex.ROPE, -0.14, 0.12, -0.14, 0.14, 0.18, 0.14, 1f, 1f, 1f, 1f);
                    break;
                case 4:
                    box(Tex.WHITE, -0.16, 0.00, -0.08, -0.04, 0.12, 0.04, 0.85f, 0.10f, 0.15f, 1f);
                    box(Tex.WHITE, 0.02, 0.00, -0.04, 0.14, 0.12, 0.08, 0.85f, 0.10f, 0.15f, 1f);
                    box(Tex.WHITE, -0.07, 0.00, 0.05, 0.05, 0.12, 0.17, 0.80f, 0.10f, 0.20f, 1f);
                    box(Tex.WHITE, -0.02, 0.12, 0.00, 0.02, 0.18, 0.04, 0.20f, 0.55f, 0.20f, 1f);
                    break;
                case 10:
                    box(Tex.WHITE, -0.22, 0.00, -0.09, 0.22, 0.26, 0.09, 0.15f, 0.15f, 0.17f, 1f);
                    box(Tex.WHITE, -0.18, 0.05, 0.09, 0.00, 0.21, 0.095, 0.05f, 0.05f, 0.05f, 1f);
                    box(Tex.WHITE, 0.04, 0.14, 0.09, 0.18, 0.21, 0.095, 0.70f, 0.65f, 0.30f, 1f);
                    box(Tex.WHITE, 0.16, 0.26, -0.02, 0.18, 0.55, 0.00, 0.70f, 0.70f, 0.70f, 1f);
                    break;
                case 11:
                    box(Tex.WHITE, -0.38, 0.12, -0.25, 0.38, 0.62, 0.22, 0.12f, 0.12f, 0.14f, 1f);
                    box(Tex.WHITE, -0.30, 0.00, -0.15, -0.20, 0.12, 0.10, 0.10f, 0.10f, 0.12f, 1f);
                    box(Tex.WHITE, 0.20, 0.00, -0.15, 0.30, 0.12, 0.10, 0.10f, 0.10f, 0.12f, 1f);
                    br = FULL;
                    box(Tex.STATIC, -0.32, 0.18, 0.22, 0.32, 0.56, 0.225, 1f, 1f, 1f, 1f);
                    break;
                case 12:
                    box(Tex.WHITE, -0.20, 0.00, -0.04, 0.10, 0.07, 0.04, 0.25f, 0.25f, 0.28f, 1f);
                    box(Tex.WHITE, 0.10, -0.01, -0.06, 0.22, 0.09, 0.06, 0.65f, 0.65f, 0.70f, 1f);
                    box(Tex.WHITE, -0.24, 0.00, -0.045, -0.20, 0.07, 0.045, 0.15f, 0.15f, 0.17f, 1f);
                    br = FULL;
                    box(Tex.WHITE, 0.22, 0.00, -0.045, 0.225, 0.08, 0.045, 1.0f, 0.95f, 0.70f, 1f);
                    if (beam) {
                        box(Tex.WHITE, 0.23, 0.01, -0.04, 0.70, 0.07, 0.04, 1.0f, 0.95f, 0.70f, 0.12f);
                        box(Tex.WHITE, 0.70, -0.04, -0.10, 1.40, 0.12, 0.10, 1.0f, 0.95f, 0.70f, 0.07f);
                    }
                    break;
                case 13:
                    box(Tex.WOOD, -0.70, 0.78, -0.40, 0.70, 0.88, 0.40, 1f, 1f, 1f, 1f);
                    box(Tex.WOOD, -0.66, 0.00, -0.36, -0.58, 0.78, -0.28, 0.9f, 0.9f, 0.9f, 1f);
                    box(Tex.WOOD, 0.58, 0.00, -0.36, 0.66, 0.78, -0.28, 0.9f, 0.9f, 0.9f, 1f);
                    box(Tex.WOOD, -0.66, 0.00, 0.28, -0.58, 0.78, 0.36, 0.9f, 0.9f, 0.9f, 1f);
                    box(Tex.WOOD, 0.58, 0.00, 0.28, 0.66, 0.78, 0.36, 0.9f, 0.9f, 0.9f, 1f);
                    box(Tex.WOOD, -0.62, 0.30, -0.32, 0.62, 0.36, 0.32, 0.8f, 0.8f, 0.8f, 1f);
                    box(Tex.WOOD, -0.40, 0.88, -0.05, -0.05, 0.92, 0.03, 0.9f, 0.9f, 0.9f, 1f);
                    box(Tex.ROCK, -0.12, 0.88, -0.09, 0.00, 0.98, 0.07, 1f, 1f, 1f, 1f);
                    break;
                default:
                    box(Tex.WOOD, -0.30, 0.00, -0.30, 0.30, 0.30, 0.30, 1f, 1f, 1f, 1f);
                    box(Tex.WOOD, -0.36, 0.30, -0.36, 0.36, 0.36, 0.36, 0.6f, 0.3f, 0.25f, 1f);
                    box(Tex.WOOD, -0.26, 0.36, -0.26, 0.26, 0.44, 0.26, 0.6f, 0.3f, 0.25f, 1f);
                    box(Tex.WHITE, -0.06, 0.00, 0.30, 0.06, 0.18, 0.305, 0.30f, 0.20f, 0.10f, 1f);
                    break;
            }
            br = -1;
            end();
        }
    }

    // ---------- modelo e render do cervo ----------
    @SideOnly(Side.CLIENT)
    public static class ModelDeer extends ModelBase {
        ModelRenderer body, neck, head, legFR, legFL, legBR, legBL, antL, antR, tineL, tineR, eyeL, eyeR;

        public ModelDeer() {
            textureWidth = 64;
            textureHeight = 64;

            body = new ModelRenderer(this, 0, 0);
            body.addBox(-4F, -4F, -8F, 8, 8, 16);
            body.setRotationPoint(0F, 8F, 0F);

            neck = new ModelRenderer(this, 0, 24);
            neck.addBox(-2F, -10F, -2F, 4, 10, 4);
            neck.setRotationPoint(0F, 6F, -7F);

            head = new ModelRenderer(this, 16, 24);
            head.addBox(-3F, -3F, -7F, 6, 6, 8);
            head.setRotationPoint(0F, -4F, -7F);

            antL = new ModelRenderer(this, 40, 36);
            antL.addBox(-0.5F, -9F, -0.5F, 1, 9, 1);
            antL.setRotationPoint(-2F, -3F, -1F);
            antL.rotateAngleZ = -0.35F;
            antR = new ModelRenderer(this, 40, 36);
            antR.addBox(-0.5F, -9F, -0.5F, 1, 9, 1);
            antR.setRotationPoint(2F, -3F, -1F);
            antR.rotateAngleZ = 0.35F;
            tineL = new ModelRenderer(this, 40, 36);
            tineL.addBox(-0.5F, -5F, -0.5F, 1, 5, 1);
            tineL.setRotationPoint(-3.5F, -8F, -1F);
            tineL.rotateAngleZ = -0.9F;
            tineR = new ModelRenderer(this, 40, 36);
            tineR.addBox(-0.5F, -5F, -0.5F, 1, 5, 1);
            tineR.setRotationPoint(3.5F, -8F, -1F);
            tineR.rotateAngleZ = 0.9F;
            eyeL = new ModelRenderer(this, 56, 0);
            eyeL.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1);
            eyeL.setRotationPoint(-2F, -1F, -7.2F);
            eyeR = new ModelRenderer(this, 56, 0);
            eyeR.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1);
            eyeR.setRotationPoint(2F, -1F, -7.2F);
            head.addChild(antL);
            head.addChild(antR);
            head.addChild(tineL);
            head.addChild(tineR);
            head.addChild(eyeL);
            head.addChild(eyeR);

            legFR = leg(-2.5F, -6F);
            legFL = leg(2.5F, -6F);
            legBR = leg(-2.5F, 6F);
            legBL = leg(2.5F, 6F);
        }

        ModelRenderer leg(float x, float z) {
            ModelRenderer l = new ModelRenderer(this, 0, 40);
            l.addBox(-1.5F, 0F, -1.5F, 3, 12, 3);
            l.setRotationPoint(x, 12F, z);
            return l;
        }

        @Override
        public void render(Entity e, float f, float f1, float f2, float f3, float f4, float f5) {
            setRotationAngles(f, f1, f2, f3, f4, f5, e);
            body.render(f5);
            neck.render(f5);
            head.render(f5);
            legFR.render(f5);
            legFL.render(f5);
            legBR.render(f5);
            legBL.render(f5);
        }

        @Override
        public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity e) {
            head.rotateAngleY = f3 / 57.29578F;
            head.rotateAngleX = f4 / 57.29578F;
            legFR.rotateAngleX = MathHelper.cos(f * 0.6662F) * 1.2F * f1;
            legBL.rotateAngleX = MathHelper.cos(f * 0.6662F) * 1.2F * f1;
            legFL.rotateAngleX = MathHelper.cos(f * 0.6662F + 3.1415927F) * 1.2F * f1;
            legBR.rotateAngleX = MathHelper.cos(f * 0.6662F + 3.1415927F) * 1.2F * f1;
        }
    }

    @SideOnly(Side.CLIENT)
    public static class RenderDeer extends RenderLiving {
        public RenderDeer() {
            super(new ModelDeer(), 0.6F);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity e) {
            Tex.init();
            return Tex.DEER;
        }
    }

    @SideOnly(Side.CLIENT)
    public static class RenderProp extends Render {
        @Override
        public void doRender(Entity e, double x, double y, double z, float yaw, float pt) {
            if (!(e instanceof EntityProp)) return;
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(-e.rotationYaw, 0.0F, 1.0F, 0.0F);
            Models.drawProp(((EntityProp) e).kind(), e.getBrightnessForRender(pt));
            GL11.glPopMatrix();
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity e) {
            Tex.init();
            return Tex.WHITE;
        }
    }

    // ---------- itens na mao e no inventario ----------
    @SideOnly(Side.CLIENT)
    public static class PropItemRenderer implements IItemRenderer {
        private final int kind;

        public PropItemRenderer(int kind) {
            this.kind = kind;
        }

        @Override
        public boolean handleRenderType(ItemStack item, ItemRenderType type) {
            return type != ItemRenderType.FIRST_PERSON_MAP;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
            return helper == ItemRendererHelper.INVENTORY_BLOCK || helper == ItemRendererHelper.ENTITY_ROTATION
                || helper == ItemRendererHelper.ENTITY_BOBBING;
        }

        @Override
        public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
            boolean held = type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
            GL11.glPushMatrix();
            switch (type) {
                case INVENTORY:
                    GL11.glTranslatef(0.0F, -0.3F, 0.0F);
                    GL11.glScalef(1.5F, 1.5F, 1.5F);
                    if (kind == 12) GL11.glRotatef(-35.0F, 0.0F, 0.0F, 1.0F);
                    break;
                case ENTITY:
                    GL11.glScalef(1.2F, 1.2F, 1.2F);
                    break;
                default:
                    GL11.glTranslatef(0.5F, 0.3F, 0.0F);
                    GL11.glScalef(1.6F, 1.6F, 1.6F);
                    if (kind == 12) GL11.glRotatef(45.0F, 0.0F, 0.0F, 1.0F);
                    break;
            }
            Models.beam = held && kind == 12;
            Models.drawProp(kind, -1);
            Models.beam = false;
            GL11.glPopMatrix();
        }
    }
}
