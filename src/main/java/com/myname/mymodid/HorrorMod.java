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
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.OpenGlHelper;
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
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
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

@Mod(modid = HorrorMod.MODID, name = "Floresta do Cervo", version = "1.1", acceptedMinecraftVersions = "[1.7.10]")
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
        EntityRegistry.registerModEntity(EntityFireOrb.class, "horror_fireorb", 3, instance, 64, 5, true);
        EntityRegistry.registerModEntity(EntityGuard.class, "horror_guard", 4, instance, 64, 3, false);
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
        public static volatile int pendingGui = 0;   // 1 menu, 2 fim, 3 bancada, 4 jumpscare
        public static volatile boolean startRequested = false;
        public static volatile boolean craftRequested = false;
        public static volatile boolean fireRequested = false;
        public static volatile long elapsed = 0;
        public static volatile int day = 1;
        public static volatile int fireCharges = 3;

        // ---- ajustes (pode editar) ----
        static final int DAY_TICKS = 4800;           // 4 minutos por dia
        static final double NIGHT_FRAC = 0.6;        // 60% do dia e noite
        static final int TOTAL_DAYS = 10;
        static final int TASK_BONUS = 1200;          // task comum: tira 1 minuto
        static final int PUZZLE_BONUS = 2400;        // quebra-cabeca: tira 2 minutos
        static final int PUZZLE_BLOCK = 1;           // a task do quebra-cabeca vem nos dias 3 e 4
        static final int STUN_TICKS = 40;            // cervo paralisado ao levar fogo (0 = desliga)
        static final int[] CABIN_COST = {10, 6, 4, 3, 0};
        public static final String[] RES_NAMES = {"Madeira", "Folha", "Pedra", "Corda", "Fruta"};
        public static final String[] COLOR_NAMES = {"VERMELHO", "VERDE", "AZUL", "AMARELO"};
        static final int[][] TASKS = {
            {6, 0, 0, 0, 0}, {7, 4, 0, 0, 0}, {0, 0, 5, 0, 0}, {0, 0, 0, 3, 4},
            {5, 0, 3, 0, 0}, {0, 6, 0, 2, 0}, {0, 0, 0, 0, 6}, {4, 5, 4, 0, 0}};

        public static final int[] res = new int[5];
        public static final int[] need = new int[5];
        public static final int[] have = new int[5];
        public static final int[] puzzleSeq = new int[4];
        public static volatile int puzzleProgress = 0;
        public static volatile boolean puzzleTask = false;
        public static volatile boolean puzzleSolved = false;
        public static volatile boolean taskDone = false;
        static int taskIndex = -1;
        static int taskBlock = -1;
        static int guardTalks = 0;

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
            fireRequested = false;
            elapsed = 0;
            day = 1;
            fireCharges = 3;
            arenaBuilt = false;
            cabinBuilt = false;
            taskBlock = -1;
            taskIndex = -1;
            taskDone = false;
            puzzleTask = false;
            puzzleSolved = false;
            puzzleProgress = 0;
            NOISES.clear();
            for (int i = 0; i < 5; i++) {
                res[i] = 0;
                need[i] = 0;
                have[i] = 0;
            }
        }

        public static boolean isNight() {
            return phase == 1 && (elapsed % DAY_TICKS) < (long) (DAY_TICKS * NIGHT_FRAC);
        }

        public static String taskText() {
            if (puzzleTask) {
                return "Quebra-cabeca 3D: " + (puzzleSolved ? "resolvido"
                    : "aperte os 4 botoes na ordem do bilhete (" + puzzleProgress + "/4)");
            }
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
                if (o instanceof EntityProp || o instanceof EntityDeer || o instanceof EntityGuard
                    || o instanceof EntityFireOrb) {
                    ((Entity) o).setDead();
                }
            }
        }

        // remove todos os mobs comuns (animais, monstros, aldeoes...)
        static void purge(World w) {
            for (Object o : new ArrayList<Object>(w.loadedEntityList)) {
                if (o instanceof EntityLiving && !(o instanceof EntityDeer) && !(o instanceof EntityGuard)) {
                    ((Entity) o).setDead();
                }
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
            purge(w);

            elapsed = 0;
            day = 1;
            taskBlock = -1;
            taskIndex = -1;
            taskDone = false;
            puzzleTask = false;
            puzzleSolved = false;
            puzzleProgress = 0;
            fireCharges = 3;
            guardTalks = 0;
            cabinBuilt = false;
            NOISES.clear();
            for (int i = 0; i < 5; i++) {
                res[i] = 0;
                need[i] = 0;
                have[i] = 0;
            }
            for (int i = 0; i < 4; i++) puzzleSeq[i] = i;
            for (int i = 3; i > 0; i--) {
                int j = RND.nextInt(i + 1);
                int t = puzzleSeq[i];
                puzzleSeq[i] = puzzleSeq[j];
                puzzleSeq[j] = t;
            }
            Arena.populate(w, cx, cz, g);

            GameRules gr = w.getGameRules();
            if (prevDaylight == null) {
                prevDaylight = gr.getGameRuleStringValue("doDaylightCycle");
                prevMobs = gr.getGameRuleStringValue("doMobSpawning");
            }
            gr.setOrCreateGameRule("doDaylightCycle", "false");
            gr.setOrCreateGameRule("doMobSpawning", "false");

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
            msg(p, "Dica: Shift + botao direito lanca uma bola de fogo (3 usos). Fale com o guarda da floresta.");
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
            pendingGui = 4;
            finish(p);
            p.worldObj.playSoundEffect(p.posX, p.posY, p.posZ, "mob.ghast.scream", 8.0F, 0.5F);
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

        // ---------- tasks ----------
        static void newTask(EntityPlayer p, int block) {
            taskBlock = block;
            taskDone = false;
            puzzleProgress = 0;
            for (int i = 0; i < 5; i++) {
                have[i] = 0;
                need[i] = 0;
            }
            if (block == PUZZLE_BLOCK) {
                puzzleTask = true;
                msg(p, "NOVA TASK: resolva o quebra-cabeca 3D (painel de 4 botoes). A ordem esta num bilhete, em uma torre.");
                return;
            }
            puzzleTask = false;
            int idx;
            do {
                idx = RND.nextInt(TASKS.length);
            } while (idx == taskIndex);
            taskIndex = idx;
            for (int i = 0; i < 5; i++) need[i] = TASKS[idx][i];
            msg(p, "NOVA TASK: " + taskText());
        }

        static void checkTask(EntityPlayer p) {
            if (taskDone || puzzleTask) return;
            for (int i = 0; i < 5; i++) {
                if (have[i] < need[i]) return;
            }
            taskDone = true;
            elapsed += TASK_BONUS;
            msg(p, "Task concluida! O tempo do jogo diminuiu 1 minuto.");
        }

        // ---------- laco principal (servidor) ----------
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
            if (phase != 1) {
                fireRequested = false;
                return;
            }

            elapsed++;
            day = (int) Math.min((long) TOTAL_DAYS, elapsed / DAY_TICKS + 1);
            double pr = (elapsed % DAY_TICKS) / (double) DAY_TICKS;
            long mc = pr < NIGHT_FRAC
                ? 13000L + (long) (pr / NIGHT_FRAC * 11000.0)
                : (long) ((pr - NIGHT_FRAC) / (1.0 - NIGHT_FRAC) * 13000.0);
            w.setWorldTime(mc);

            int block = (day - 1) / 2;
            if (block != taskBlock) newTask(p, block);

            if (fireRequested) {
                fireRequested = false;
                shoot(p);
            }
            if (elapsed % 20 == 0 && p.isSprinting()) noise(w, p.posX, p.posY, p.posZ, 14.0);
            if (elapsed % 100 == 0) {
                p.getFoodStats().addStats(1, 0.0F);
                purge(w);
            }
            if (elapsed % 10 == 0) heartbeat(w, p);
            if (elapsed >= (long) TOTAL_DAYS * DAY_TICKS) win(p);
        }

        static void heartbeat(World w, EntityPlayer p) {
            double best = 1.0E9;
            for (Object o : w.loadedEntityList) {
                if (o instanceof EntityDeer) {
                    double d = ((Entity) o).getDistanceSqToEntity(p);
                    if (d < best) best = d;
                }
            }
            double dist = Math.sqrt(best);
            if (dist > 22.0) return;
            int every = dist < 8.0 ? 10 : (dist < 14.0 ? 20 : 30);
            if (elapsed % every == 0) w.playSoundAtEntity(p, "note.bd", 1.2F, 0.5F);
        }

        static void shoot(EntityPlayer p) {
            if (fireCharges <= 0) {
                msg(p, "Sem bolas de fogo.");
                return;
            }
            fireCharges--;
            World w = p.worldObj;
            w.spawnEntityInWorld(new EntityFireOrb(w, p));
            w.playSoundAtEntity(p, "mob.ghast.fireball", 1.0F, 1.0F);
            noise(w, p.posX, p.posY, p.posZ, 25.0);
            msg(p, "Bola de fogo! Restam " + fireCharges + ".");
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
            } else if (k >= 14 && k <= 17) {
                pressButton(e, p, k - 14);
            } else if (k == 18) {
                msg(p, "Painel com 4 botoes coloridos. Aperte na ordem do bilhete.");
            } else if (k == 19) {
                StringBuilder sb = new StringBuilder("Bilhete rasgado - ordem dos botoes: ");
                for (int i = 0; i < 4; i++) {
                    sb.append(COLOR_NAMES[puzzleSeq[i]]);
                    if (i < 3) sb.append(", ");
                }
                msg(p, sb.toString());
                noise(w, e.posX, e.posY, e.posZ, 10.0);
            }
        }

        static void pressButton(EntityProp e, EntityPlayer p, int color) {
            World w = e.worldObj;
            if (!puzzleTask || taskDone) {
                msg(p, "O painel esta travado... ainda nao e hora.");
                return;
            }
            w.playSoundAtEntity(p, "random.click", 1.0F, 0.6F + color * 0.2F);
            if (puzzleSeq[puzzleProgress] == color) {
                puzzleProgress++;
                noise(w, e.posX, e.posY, e.posZ, 16.0);
                if (puzzleProgress >= 4) {
                    puzzleSolved = true;
                    taskDone = true;
                    elapsed += PUZZLE_BONUS;
                    w.playSoundAtEntity(p, "random.levelup", 1.0F, 0.8F);
                    msg(p, "Quebra-cabeca resolvido! O tempo do jogo diminuiu 2 minutos.");
                }
            } else {
                puzzleProgress = 0;
                w.playSoundEffect(e.posX, e.posY, e.posZ, "note.bass", 3.0F, 0.5F);
                noise(w, e.posX, e.posY, e.posZ, 35.0);
                msg(p, "ERRADO! O painel apitou alto... recomece.");
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

        // torre: 2 aberturas na grade (entrada junto da escada no canto NE e saida/pulo no lado sul)
        static void tower(World w, int tx, int tz, int g) {
            int top = g + 11;
            // postes (o canto NE fica livre para a entrada)
            for (int y = g; y <= top + 5; y++) {
                for (int sx = -4; sx <= 4; sx += 8) {
                    for (int sz = -4; sz <= 4; sz += 8) {
                        if (sx == 4 && sz == -4) continue;
                        set(w, tx + sx, y, tz + sz, Blocks.log, 0);
                    }
                }
            }
            // piso e teto
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    set(w, tx + dx, top, tz + dz, Blocks.planks, 0);
                    set(w, tx + dx, top + 5, tz + dz, Blocks.planks, 0);
                }
            }
            // vigas sob o teto
            for (int d = -3; d <= 3; d++) {
                set(w, tx + d, top + 4, tz - 4, Blocks.log, 4);
                set(w, tx + d, top + 4, tz + 4, Blocks.log, 4);
                set(w, tx - 4, top + 4, tz + d, Blocks.log, 8);
                set(w, tx + 4, top + 4, tz + d, Blocks.log, 8);
            }
            // grade lateral
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                    boolean post = Math.abs(dx) == 4 && Math.abs(dz) == 4;
                    boolean entry = dx == 4 && dz == -4;
                    boolean jump = dz == 4 && Math.abs(dx) <= 1;
                    if (!edge || post || entry || jump) continue;
                    boolean wallSide = dx == -4 || dz == -4;
                    boolean window = Math.abs(dx == -4 ? dz : dx) <= 1;
                    if (wallSide && !window) {
                        set(w, tx + dx, top + 1, tz + dz, Blocks.planks, 0);
                        set(w, tx + dx, top + 2, tz + dz, Blocks.planks, 0);
                    } else {
                        set(w, tx + dx, top + 1, tz + dz, Blocks.fence, 0);
                    }
                }
            }
            // detalhes: mesa, estante, fardos, caldeirao, teias e "sangue"
            set(w, tx - 2, top + 1, tz + 2, Blocks.fence, 0);
            set(w, tx - 2, top + 2, tz + 2, Blocks.wooden_pressure_plate, 0);
            set(w, tx - 3, top + 1, tz - 3, Blocks.bookshelf, 0);
            set(w, tx - 3, top + 2, tz - 3, Blocks.bookshelf, 0);
            set(w, tx - 3, top + 1, tz - 2, Blocks.bookshelf, 0);
            set(w, tx + 3, top + 1, tz + 3, Blocks.hay_block, 0);
            set(w, tx + 2, top + 1, tz + 3, Blocks.hay_block, 0);
            set(w, tx - 3, top + 1, tz + 3, Blocks.cauldron, 0);
            set(w, tx + 3, top + 4, tz + 3, Blocks.web, 0);
            set(w, tx - 3, top + 4, tz - 3, Blocks.web, 0);
            set(w, tx + 3, top + 4, tz - 3, Blocks.web, 0);
            set(w, tx, top + 4, tz, Blocks.iron_bars, 0);
            set(w, tx, top + 3, tz, Blocks.iron_bars, 0);
            set(w, tx, top + 2, tz, Blocks.iron_bars, 0);
            set(w, tx, top + 1, tz + 1, Blocks.carpet, 14);
            set(w, tx + 1, top + 1, tz + 1, Blocks.carpet, 14);
            set(w, tx + 1, top + 1, tz + 2, Blocks.carpet, 14);
            set(w, tx - 1, top + 1, tz + 1, Blocks.carpet, 14);

            // escada: sobe pelo lado norte e dobra para a entrada no canto NE
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
            // bancada
            prop(w, 13, cx + 0.5, g, cz + 5.5);
            // painel do quebra-cabeca: 4 botoes sobre o pedestal
            double px = cx + 1.5;
            double pz = cz - 12.5;
            prop(w, 18, px, g, pz);
            for (int i = 0; i < 4; i++) prop(w, 14 + i, px - 0.6 + i * 0.4, g + 0.9, pz);

            // torres: radio, TV, lanterna (2 torres) e o bilhete (1 torre)
            double topY = g + 12;
            for (int i = 0; i < 4; i++) {
                double tx = cx + TOWERS[i][0] + 0.5;
                double tz = cz + TOWERS[i][1] + 0.5;
                prop(w, 10, tx - 2.5, topY, tz + 2.5);
                prop(w, 11, tx + 2.5, topY, tz + 2.5);
                if (i == 0 || i == 3) prop(w, 12, tx, topY, tz - 2.5);
                if (i == 2) prop(w, 19, tx + 1.5, topY, tz - 1.5);
            }

            // guarda da floresta: no chao, ao lado da torre 0
            EntityGuard gd = new EntityGuard(w);
            gd.setLocationAndAngles(cx + TOWERS[0][0] - 7.5, g, cz + TOWERS[0][1] + 0.5, 90.0F, 0.0F);
            w.spawnEntityInWorld(gd);

            // o cervo
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

    // ================= ENTIDADE: objetos coletaveis, radio, TV, bancada, painel =================
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
            else if (k == 18) setSize(1.7F, 0.95F);
            else if (k == 11) setSize(0.8F, 0.8F);
            else if (k >= 14 && k <= 17) setSize(0.3F, 0.25F);
            else if (k == 19) setSize(0.5F, 0.3F);
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

    // ================= ENTIDADE: bola de fogo do jogador =================
    public static class EntityFireOrb extends EntityThrowable {
        public EntityFireOrb(World w) {
            super(w);
        }

        public EntityFireOrb(World w, EntityLivingBase thrower) {
            super(w, thrower);
        }

        @Override
        protected float getGravityVelocity() {
            return 0.0F;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (worldObj.isRemote) worldObj.spawnParticle("flame", posX, posY, posZ, 0.0D, 0.0D, 0.0D);
            if (ticksExisted > 80) setDead();
        }

        @Override
        protected void onImpact(MovingObjectPosition mop) {
            if (worldObj.isRemote) return;
            if (mop.entityHit instanceof EntityDeer) ((EntityDeer) mop.entityHit).hitByFire();
            worldObj.playSoundEffect(posX, posY, posZ, "random.fizz", 1.0F, 1.0F);
            setDead();
        }
    }

    // ================= ENTIDADE: guarda da floresta =================
    public static class EntityGuard extends EntityCreature {
        static final String[] STORY = {
            "Guarda da Floresta: Voce nao devia estar aqui, forasteiro...",
            "Guarda da Floresta: Este lugar foi dominado por um cervo assustador. Ele nao ve. So escuta.",
            "Guarda da Floresta: E continua assim ate os dias de hoje. Ninguem saiu daqui.",
            "Guarda da Floresta: Nao faca barulho. Sobreviva 10 dias e talvez voce escape com vida."};
        static final String[] HINTS = {
            "Guarda da Floresta: Os radios das torres gritam como ele. Evite mexer neles.",
            "Guarda da Floresta: Um bilhete numa das torres guarda a ordem dos botoes do painel.",
            "Guarda da Floresta: O fogo o faz recuar... mas so tres vezes. Shift e botao direito.",
            "Guarda da Floresta: Dentro de uma cabana ele nao escuta seus passos."};
        int talks = 0;

        public EntityGuard(World w) {
            super(w);
            setSize(0.6F, 1.8F);
            tasks.addTask(1, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(20.0D);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.0D);
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

        @Override
        protected String getLivingSound() {
            return null;
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
        public boolean interact(EntityPlayer p) {
            if (!worldObj.isRemote) {
                if (talks == 0) {
                    for (String s : STORY) HorrorMod.msg(p, s);
                } else {
                    HorrorMod.msg(p, HINTS[(talks - 1) % HINTS.length]);
                }
                talks++;
            }
            return true;
        }
    }

    // ================= ENTIDADE: o cervo =================
    public static class EntityDeer extends EntityCreature {
        int stun = 0;

        public EntityDeer(World w) {
            super(w);
            setSize(0.9F, 1.9F);
            stepHeight = 1.1F;
            tasks.addTask(1, new AIGoToNoise(this));
            tasks.addTask(2, new EntityAILookIdle(this));
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataWatcher.addObject(21, Integer.valueOf(0));
        }

        public int getHits() {
            return dataWatcher.getWatchableObjectInt(21);
        }

        // bola de fogo: o cervo fica mais deformado (ate 3 vezes)
        public void hitByFire() {
            int h = Math.min(3, getHits() + 1);
            dataWatcher.updateObject(21, Integer.valueOf(h));
            stun = Game.STUN_TICKS;
            worldObj.playSoundEffect(posX, posY, posZ, "mob.ghast.scream", 3.0F, 0.4F);
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(200.0D);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.26D);
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
        public int getTalkInterval() {
            return Game.isNight() ? 50 : 120;
        }

        @Override
        protected String getLivingSound() {
            return "mob.horse.zombie.idle";
        }

        @Override
        protected float getSoundPitch() {
            return 0.55F;
        }

        @Override
        protected float getSoundVolume() {
            return 2.5F;
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
            if (worldObj.isRemote) return;
            if (ticksExisted % 20 == 0) {
                getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(Game.isNight() ? 0.33D : 0.26D);
            }
            if (stun > 0) {
                stun--;
                motionX = 0.0D;
                motionZ = 0.0D;
                getNavigator().clearPathEntity();
            } else if (Game.phase == 1) {
                Game.checkKill(this);
            }
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
            if (Game.phase != 1 || d.stun > 0) return false;
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
            return Game.phase == 1 && d.stun <= 0 && timer > 0 && (dx * dx + dz * dz) > 4.0;
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
            RenderingRegistry.registerEntityRenderingHandler(EntityFireOrb.class, new RenderFireOrb());
            RenderingRegistry.registerEntityRenderingHandler(EntityGuard.class, new RenderGuard());
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

    // abre as telas pedidas pelo servidor e detecta Shift + botao direito (mundo solo: mesmo programa)
    @SideOnly(Side.CLIENT)
    public static class ClientBus {
        boolean prevUse = false;

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
                else if (gui == 4) mc.displayGuiScreen(new GuiJumpscare());
                else mc.displayGuiScreen(new GuiEnd());
            }
            if ((Game.phase == 2 || Game.phase == 3) && !Game.startRequested
                && !(mc.currentScreen instanceof GuiEnd) && !(mc.currentScreen instanceof GuiJumpscare)) {
                mc.displayGuiScreen(new GuiEnd());
            }

            boolean down = mc.gameSettings.keyBindUseItem.getIsKeyPressed();
            if (down && !prevUse && Game.phase == 1 && mc.currentScreen == null && mc.thePlayer.isSneaking()) {
                Entity hit = mc.objectMouseOver != null ? mc.objectMouseOver.entityHit : null;
                if (!(hit instanceof EntityProp) && !(hit instanceof EntityGuard)) Game.fireRequested = true;
            }
            prevUse = down;
        }
    }

    @SideOnly(Side.CLIENT)
    public static class ClientForge {
        static float fogDensity() {
            Minecraft mc = Minecraft.getMinecraft();
            ItemStack held = mc.thePlayer != null ? mc.thePlayer.getCurrentEquippedItem() : null;
            boolean light = held != null && held.getItem() == flashlightItem;
            return light ? 0.30F : 1.0F; // sem lanterna: ~3 blocos; com lanterna: ~10 blocos
        }

        @SubscribeEvent
        public void onFogDensity(EntityViewRenderEvent.FogDensity e) {
            if (Game.phase != 1) return;
            e.density = fogDensity();
            e.setCanceled(true);
        }

        // reforca o nevoeiro direto no OpenGL (caso o jogo ignore o evento acima)
        @SubscribeEvent
        public void onRenderFog(EntityViewRenderEvent.RenderFogEvent e) {
            if (Game.phase != 1) return;
            GL11.glEnable(GL11.GL_FOG);
            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
            GL11.glFogf(GL11.GL_FOG_DENSITY, fogDensity());
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
            e.left.add("Bolas de fogo: " + Game.fireCharges + "  (Shift + botao direito)");
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

    // jumpscare: rosto do cervo com dentes (desenhado por codigo); depois abre a tela de fim
    @SideOnly(Side.CLIENT)
    public static class GuiJumpscare extends GuiHorror {
        final Random r = new Random();
        boolean sounded = false;

        @Override
        protected void keyTyped(char c, int key) {}

        @Override
        protected void mouseClicked(int mx, int my, int btn) {}

        @Override
        public void updateScreen() {
            tick++;
            if (!sounded) {
                sounded = true;
                if (mc.thePlayer != null) mc.thePlayer.playSound("mob.ghast.scream", 4.0F, 0.5F);
            }
            if (tick > 70) mc.displayGuiScreen(new GuiEnd());
        }

        // elipse desenhada em linhas
        void blob(int cx, int cy, double hw, double hh, double s, int color) {
            for (int y = (int) -hh; y < hh; y += 2) {
                double k = 1.0 - (y * y) / (hh * hh);
                if (k <= 0.0) continue;
                int w2 = (int) (hw * Math.sqrt(k) * s);
                drawRect(cx - w2, cy + (int) (y * s), cx + w2, cy + (int) ((y + 2) * s), color);
            }
        }

        void line(int x1, int y1, int x2, int y2, int th, int color) {
            int n = Math.max(1, Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)));
            for (int i = 0; i <= n; i += 2) {
                int x = x1 + (x2 - x1) * i / n;
                int y = y1 + (y2 - y1) * i / n;
                drawRect(x - th, y - th, x + th, y + th, color);
            }
        }

        @Override
        public void drawScreen(int mx, int my, float pt) {
            boolean flash = tick % 6 < 2;
            drawRect(0, 0, width, height, flash ? 0xFFAA0000 : 0xFF000000);
            double s = Math.min(width, height) / 300.0 * (1.0 + Math.min(tick, 20) * 0.03);
            int cx = width / 2 + r.nextInt(9) - 4;
            int cy = height / 2 + r.nextInt(9) - 4;
            int bone = 0xFFD8D0B8;
            int th = Math.max(2, (int) (4 * s));

            // chifres
            line(cx - (int) (45 * s), cy - (int) (95 * s), cx - (int) (95 * s), cy - (int) (200 * s), th, bone);
            line(cx - (int) (95 * s), cy - (int) (200 * s), cx - (int) (60 * s), cy - (int) (260 * s), th, bone);
            line(cx - (int) (80 * s), cy - (int) (160 * s), cx - (int) (140 * s), cy - (int) (190 * s), th, bone);
            line(cx - (int) (70 * s), cy - (int) (130 * s), cx - (int) (125 * s), cy - (int) (120 * s), th, bone);
            line(cx + (int) (45 * s), cy - (int) (95 * s), cx + (int) (95 * s), cy - (int) (200 * s), th, bone);
            line(cx + (int) (95 * s), cy - (int) (200 * s), cx + (int) (60 * s), cy - (int) (260 * s), th, bone);
            line(cx + (int) (80 * s), cy - (int) (160 * s), cx + (int) (140 * s), cy - (int) (190 * s), th, bone);
            line(cx + (int) (70 * s), cy - (int) (130 * s), cx + (int) (125 * s), cy - (int) (120 * s), th, bone);

            // cabeca e focinho
            blob(cx, cy, 85, 135, s, 0xFF1C120C);
            blob(cx, cy + (int) (50 * s), 55, 80, s, 0xFF2B1B12);
            // olhos que brilham
            blob(cx - (int) (38 * s), cy - (int) (45 * s), 24, 18, s, 0xFFFFFFFF);
            blob(cx + (int) (38 * s), cy - (int) (45 * s), 24, 18, s, 0xFFFFFFFF);
            int pj = r.nextInt(5) - 2;
            blob(cx - (int) (38 * s) + pj, cy - (int) (45 * s), 9, 15, s, 0xFFCC0000);
            blob(cx + (int) (38 * s) + pj, cy - (int) (45 * s), 9, 15, s, 0xFFCC0000);
            drawRect(cx - (int) (38 * s) + pj - 1, cy - (int) (58 * s), cx - (int) (38 * s) + pj + 1, cy - (int) (32 * s), 0xFF000000);
            drawRect(cx + (int) (38 * s) + pj - 1, cy - (int) (58 * s), cx + (int) (38 * s) + pj + 1, cy - (int) (32 * s), 0xFF000000);

            // boca aberta com dentes
            double open = 14 + Math.min(tick, 15) * 1.6;
            int my0 = cy + (int) (92 * s);
            blob(cx, my0, 58, open, s, 0xFF000000);
            int top = my0 - (int) (open * s);
            int bot = my0 + (int) (open * s);
            int tooth = 0xFFE8E0C8;
            int len = (int) (20 * s);
            for (int i = -5; i <= 5; i++) {
                int x = cx + (int) (i * 10 * s);
                for (int k = 0; k < len; k += 2) {
                    int hw = (int) (5 * s * (1.0 - (double) k / len));
                    drawRect(x - hw, top + k, x + hw, top + k + 2, tooth);
                    drawRect(x - hw, bot - k - 2, x + hw, bot - k, tooth);
                }
            }
            // sangue escorrendo
            for (int i = -4; i <= 4; i++) {
                int x = cx + (int) (i * 11 * s) + 3;
                int lenb = (int) ((30 + ((i * 37 + 91) % 40)) * s * Math.min(1.0, tick / 25.0));
                drawRect(x, bot, x + Math.max(2, (int) (3 * s)), bot + lenb, 0xFF8A0000);
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
        public static ResourceLocation WHITE, WOOD, LEAF, ROCK, ROPE, STATIC, DEER, GUARD;

        static int clamp(int v) {
            return v < 0 ? 0 : (v > 255 ? 255 : v);
        }

        static int rgb(int r, int g, int b) {
            return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
        }

        static void fill(BufferedImage im, int x1, int y1, int x2, int y2, int col) {
            for (int y = y1; y <= y2; y++) for (int x = x1; x <= x2; x++) im.setRGB(x, y, col);
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
            fill(im, 0, 0, 15, 15, 0xFFFFFFFF);
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

            // cervo 64x64: pelo, chifres (48,40), olhos (24,48), dentes (36,40)
            im = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    int n = (x * 13 + y * 7 + x * y * 3) % 9;
                    im.setRGB(x, y, rgb(88 + n * 3, 58 + n * 2, 36 + n * 2));
                }
            }
            fill(im, 48, 40, 59, 59, rgb(215, 200, 165));
            fill(im, 36, 40, 43, 45, rgb(240, 235, 215));
            fill(im, 24, 47, 37, 51, rgb(255, 30, 30));
            DEER = make("deer", im);

            // guarda 64x32 (pele padrao do ModelBiped)
            im = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            fill(im, 0, 0, 63, 31, rgb(52, 92, 52));
            fill(im, 0, 0, 31, 15, rgb(205, 165, 125));
            fill(im, 8, 0, 23, 7, rgb(70, 45, 25));
            fill(im, 0, 8, 31, 9, rgb(70, 45, 25));
            fill(im, 10, 11, 11, 11, rgb(30, 20, 20));
            fill(im, 13, 11, 14, 11, rgb(30, 20, 20));
            fill(im, 10, 13, 13, 15, rgb(120, 110, 100));
            fill(im, 0, 16, 15, 31, rgb(85, 65, 45));
            fill(im, 40, 28, 55, 31, rgb(205, 165, 125));
            GUARD = make("guard", im);
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

        static void drawOrb() {
            br = FULL;
            begin();
            Tex.init();
            box(Tex.WHITE, -0.12, -0.12, -0.12, 0.12, 0.12, 0.12, 1.0f, 0.55f, 0.10f, 1f);
            box(Tex.WHITE, -0.07, -0.07, -0.07, 0.07, 0.07, 0.07, 1.0f, 0.90f, 0.30f, 1f);
            box(Tex.WHITE, -0.20, -0.20, -0.20, 0.20, 0.20, 0.20, 1.0f, 0.40f, 0.05f, 0.30f);
            end();
            br = -1;
        }

        // 0 madeira, 1 folha, 2 pedra, 3 corda, 4 fruta, 10 radio, 11 TV, 12 lanterna, 13 bancada,
        // 14-17 botoes (vermelho, verde, azul, amarelo), 18 painel, 19 bilhete, 20 kit de cabana
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
                case 14:
                case 15:
                case 16:
                case 17: {
                    float[][] cc = {{0.90f, 0.10f, 0.10f}, {0.10f, 0.80f, 0.20f}, {0.15f, 0.30f, 0.95f}, {0.95f, 0.85f, 0.10f}};
                    float[] c = cc[kind - 14];
                    box(Tex.WHITE, -0.12, 0.00, -0.12, 0.12, 0.08, 0.12, 0.20f, 0.20f, 0.22f, 1f);
                    br = FULL;
                    box(Tex.WHITE, -0.09, 0.08, -0.09, 0.09, 0.15, 0.09, c[0], c[1], c[2], 1f);
                    br = light;
                    break;
                }
                case 18:
                    box(Tex.WOOD, -0.80, 0.00, -0.35, 0.80, 0.80, 0.35, 0.9f, 0.9f, 0.9f, 1f);
                    box(Tex.WHITE, -0.85, 0.80, -0.40, 0.85, 0.90, 0.40, 0.15f, 0.15f, 0.17f, 1f);
                    box(Tex.ROPE, -0.70, 0.10, 0.35, 0.70, 0.70, 0.37, 0.5f, 0.5f, 0.5f, 1f);
                    break;
                case 19:
                    box(Tex.WHITE, -0.12, 0.00, -0.16, 0.12, 0.015, 0.16, 0.90f, 0.88f, 0.75f, 1f);
                    box(Tex.WHITE, -0.08, 0.015, -0.10, 0.08, 0.02, -0.08, 0.30f, 0.05f, 0.05f, 1f);
                    box(Tex.WHITE, -0.08, 0.015, -0.04, 0.05, 0.02, -0.02, 0.30f, 0.05f, 0.05f, 1f);
                    box(Tex.WHITE, -0.08, 0.015, 0.02, 0.08, 0.02, 0.04, 0.30f, 0.05f, 0.05f, 1f);
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
        ModelRenderer body, neck, head, jaw, eyes, legFR, legFL, legBR, legBL, antL, antR, tineL, tineR;

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

            // mandibula e dentes
            jaw = new ModelRenderer(this, 16, 40);
            jaw.addBox(-2F, 0F, -6F, 4, 1, 6);
            jaw.setRotationPoint(0F, 2.5F, 0.5F);
            head.addChild(jaw);
            for (int i = 0; i < 4; i++) {
                float x = -1.5F + i;
                ModelRenderer low = new ModelRenderer(this, 36, 40);
                low.addBox(-0.5F, -2F, -0.5F, 1, 2, 1);
                low.setRotationPoint(x, 0F, -5.5F);
                jaw.addChild(low);
                ModelRenderer up = new ModelRenderer(this, 36, 40);
                up.addBox(-0.5F, 0F, -0.5F, 1, 2, 1);
                up.setRotationPoint(x, 3F, -6.5F);
                head.addChild(up);
            }

            antL = new ModelRenderer(this, 48, 40);
            antL.addBox(-0.5F, -9F, -0.5F, 1, 9, 1);
            antL.setRotationPoint(-2F, -3F, -1F);
            antR = new ModelRenderer(this, 48, 40);
            antR.addBox(-0.5F, -9F, -0.5F, 1, 9, 1);
            antR.setRotationPoint(2F, -3F, -1F);
            tineL = new ModelRenderer(this, 48, 52);
            tineL.addBox(-0.5F, -5F, -0.5F, 1, 5, 1);
            tineL.setRotationPoint(-3.5F, -8F, -1F);
            tineR = new ModelRenderer(this, 48, 52);
            tineR.addBox(-0.5F, -5F, -0.5F, 1, 5, 1);
            tineR.setRotationPoint(3.5F, -8F, -1F);
            head.addChild(antL);
            head.addChild(antR);
            head.addChild(tineL);
            head.addChild(tineR);

            // olhos (desenhados brilhando, em uma etapa separada)
            eyes = new ModelRenderer(this, 24, 48);
            eyes.addBox(-2.5F, -1.5F, -7.7F, 5, 1, 1);
            eyes.setRotationPoint(0F, -4F, -7F);

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

            eyes.rotateAngleX = head.rotateAngleX;
            eyes.rotateAngleY = head.rotateAngleY;
            eyes.rotateAngleZ = head.rotateAngleZ;
            GL11.glDisable(GL11.GL_LIGHTING);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            eyes.render(f5);
            int light = e.getBrightnessForRender(0.0F);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
            GL11.glEnable(GL11.GL_LIGHTING);
        }

        @Override
        public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity e) {
            int h = (e instanceof EntityDeer) ? ((EntityDeer) e).getHits() : 0;
            head.rotateAngleY = f3 / 57.29578F;
            head.rotateAngleX = f4 / 57.29578F;
            head.rotateAngleZ = 0.28F * h;
            neck.rotateAngleX = -0.25F * h;
            body.rotateAngleZ = 0.08F * h;
            jaw.rotateAngleX = 0.35F + 0.25F * h;
            antL.rotateAngleZ = -0.35F - 0.25F * h;
            antR.rotateAngleZ = 0.35F + 0.10F * h;
            tineL.rotateAngleZ = -0.9F - 0.30F * h;
            tineR.rotateAngleZ = 0.9F - 0.40F * h;
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

        // mais alto e esticado a cada bola de fogo; maior a noite
        protected void preRenderCallback(EntityLivingBase e, float pt) {
            int h = (e instanceof EntityDeer) ? ((EntityDeer) e).getHits() : 0;
            float n = Game.isNight() ? 1.12F : 1.0F;
            GL11.glScalef(n * (1.0F - 0.05F * h), n * (1.0F + 0.15F * h), n * (1.0F - 0.05F * h));
        }

        // fica avermelhado conforme e queimado
        protected int getColorMultiplier(EntityLivingBase e, float brightness, float pt) {
            int h = (e instanceof EntityDeer) ? ((EntityDeer) e).getHits() : 0;
            return h > 0 ? (((40 * h) << 24) | 0x600000) : 0;
        }
    }

    @SideOnly(Side.CLIENT)
    public static class RenderGuard extends RenderLiving {
        public RenderGuard() {
            super(makeModel(), 0.5F);
        }

        static ModelBiped makeModel() {
            ModelBiped m = new ModelBiped();
            m.bipedHeadwear.showModel = false;
            return m;
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity e) {
            Tex.init();
            return Tex.GUARD;
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

    @SideOnly(Side.CLIENT)
    public static class RenderFireOrb extends Render {
        @Override
        public void doRender(Entity e, double x, double y, double z, float yaw, float pt) {
            GL11.glPushMatrix();
            GL11.glTranslated(x, y + 0.1, z);
            GL11.glRotatef(e.ticksExisted * 25.0F, 0.3F, 1.0F, 0.2F);
            Models.drawOrb();
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
