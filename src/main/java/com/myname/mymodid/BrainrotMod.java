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
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.ScoreDummyCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

@Mod(modid = BrainrotMod.MODID, name = "Brainrot Mod", version = "1.0", acceptedMinecraftVersions = "[1.7.10]")
public class BrainrotMod {

    public static final String MODID = "brainrotmod";
    public static BrainrotMod INSTANCE;

    // ---------- os 6 brainrots ----------
    static final String[] NAMES = {"Tung Tung Sahur", "Tralalero Tralala", "Lirili Larila", "Cappuccino Assassino",
        "Ballerina Cappuccina", "Bombardiro Crocodilo"};
    static final int[] PRICE = {50, 150, 400, 1000, 3000, 10000};
    static final int[] INCOME = {2, 6, 15, 35, 100, 350};
    static final int[] WEIGHT = {30, 25, 20, 13, 8, 4};
    static final int FULL = 15728880;
    static final Random RND = new Random();

    static int cv(int d) {
        return d < 0 ? 0 : (d > 5 ? 5 : d);
    }

    public static Item batItem;
    public static Item lockItem;

    public static final CreativeTabs TAB = new CreativeTabs("brainrotmod") {
        @Override
        public Item getTabIconItem() {
            return batItem;
        }
    };

    // ================= CICLO DE VIDA =================
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        INSTANCE = this;
        batItem = new ItemBat();
        lockItem = new ItemLock();
        GameRegistry.registerItem(batItem, "brainrot_bat");
        GameRegistry.registerItem(lockItem, "brainrot_lock");
        EntityRegistry.registerModEntity(EntityBrainrot.class, "Brainrot", 1, this, 80, 1, true);
        EntityRegistry.registerModEntity(EntityBotPlayer.class, "BrainrotBot", 2, this, 80, 3, true);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        LanguageRegistry lang = LanguageRegistry.instance();
        lang.addStringLocalization("itemGroup.brainrotmod", "Brainrot Mod");
        lang.addStringLocalization("item.brainrot_bat.name", "Bastao de Madeira");
        lang.addStringLocalization("item.brainrot_lock.name", "Cadeado da Base");
        GameRegistry.addRecipe(new ItemStack(batItem), "  P", " P ", "S  ", 'P', Blocks.planks, 'S', Items.stick);
        GameRegistry.addRecipe(new ItemStack(lockItem), " I ", "IBI", 'I', Items.iron_ingot, 'B', Blocks.iron_block);
        Events ev = new Events();
        MinecraftForge.EVENT_BUS.register(ev);
        FMLCommonHandler.instance().bus().register(ev);
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientSide.init();
        }
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new CmdBrainrot());
    }

    static void msg(EntityPlayer p, String s) {
        p.addChatMessage(new ChatComponentText(s));
    }

    // ================= ITENS =================
    public static class ItemBat extends ItemSword {
        public ItemBat() {
            super(Item.ToolMaterial.WOOD);
            setMaxDamage(0);
            setCreativeTab(TAB);
            setUnlocalizedName("brainrot_bat");
        }

        @Override
        public boolean hitEntity(ItemStack st, EntityLivingBase target, EntityLivingBase attacker) {
            double dx = target.posX - attacker.posX;
            double dz = target.posZ - attacker.posZ;
            double l = Math.sqrt(dx * dx + dz * dz) + 0.0001;
            target.addVelocity(dx / l * 0.8, 0.3, dz / l * 0.8);
            if (target instanceof EntityBotPlayer) {
                target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 60, 3));
            }
            return true;
        }

        @Override
        public void registerIcons(IIconRegister r) {}

        @Override
        public IIcon getIconFromDamage(int d) {
            return Items.stick.getIconFromDamage(0);
        }
    }

    public static class ItemLock extends Item {
        public ItemLock() {
            setMaxStackSize(1);
            setCreativeTab(TAB);
            setUnlocalizedName("brainrot_lock");
        }

        @Override
        public ItemStack onItemRightClick(ItemStack st, World w, EntityPlayer p) {
            if (!w.isRemote) Game.toggleLock(p);
            return st;
        }

        @Override
        public void registerIcons(IIconRegister r) {}

        @Override
        public IIcon getIconFromDamage(int d) {
            return Items.iron_ingot.getIconFromDamage(0);
        }
    }

    // ================= DADOS DO JOGO (salvos no mundo) =================
    public static class GameData extends WorldSavedData {
        public static final String NAME = "brainrot_game";
        public boolean active = false;
        public int dim = 0, x0 = 0, y0 = 0, z0 = 0;
        public String owner = "";
        public int money = 0;
        public int[] ps = {-1, -1, -1, -1, -1, -1};
        public int[] bs = {-1, -1, -1, -1, -1, -1};
        public long pLock = 0, bLock = 0, lockCd = 0;
        public int carrying = -1, carryFrom = -1, botCarry = -1, botCarryFrom = -1;

        public GameData(String n) {
            super(n);
        }

        @Override
        public void readFromNBT(NBTTagCompound t) {
            active = t.getBoolean("active");
            dim = t.getInteger("dim");
            x0 = t.getInteger("x0");
            y0 = t.getInteger("y0");
            z0 = t.getInteger("z0");
            owner = t.getString("owner");
            money = t.getInteger("money");
            int[] a = t.getIntArray("ps");
            int[] b = t.getIntArray("bs");
            if (a.length == 6) ps = a;
            if (b.length == 6) bs = b;
            pLock = t.getLong("pLock");
            bLock = t.getLong("bLock");
            lockCd = t.getLong("lockCd");
            carrying = t.getInteger("carrying");
            carryFrom = t.getInteger("carryFrom");
            botCarry = t.getInteger("botCarry");
            botCarryFrom = t.getInteger("botCarryFrom");
        }

        @Override
        public void writeToNBT(NBTTagCompound t) {
            t.setBoolean("active", active);
            t.setInteger("dim", dim);
            t.setInteger("x0", x0);
            t.setInteger("y0", y0);
            t.setInteger("z0", z0);
            t.setString("owner", owner);
            t.setInteger("money", money);
            t.setIntArray("ps", ps);
            t.setIntArray("bs", bs);
            t.setLong("pLock", pLock);
            t.setLong("bLock", bLock);
            t.setLong("lockCd", lockCd);
            t.setInteger("carrying", carrying);
            t.setInteger("carryFrom", carryFrom);
            t.setInteger("botCarry", botCarry);
            t.setInteger("botCarryFrom", botCarryFrom);
        }
    }

    // ================= ENTIDADES =================
    // estado: 0 esteira, 1 slot do jogador, 2 slot do bot, 3 carregado por alguem
    public static class EntityBrainrot extends Entity {
        public EntityBrainrot(World w) {
            super(w);
            setSize(0.8F, 1.1F);
            noClip = true;
        }

        @Override
        protected void entityInit() {
            dataWatcher.addObject(20, Integer.valueOf(0));
            dataWatcher.addObject(21, Integer.valueOf(0));
            dataWatcher.addObject(22, Integer.valueOf(0));
            dataWatcher.addObject(23, Integer.valueOf(0));
        }

        public int getVariant() {
            return dataWatcher.getWatchableObjectInt(20);
        }

        public int getState() {
            return dataWatcher.getWatchableObjectInt(21);
        }

        public int getOwnerId() {
            return dataWatcher.getWatchableObjectInt(22);
        }

        public int getSlot() {
            return dataWatcher.getWatchableObjectInt(23);
        }

        public void setVariant(int v) {
            dataWatcher.updateObject(20, Integer.valueOf(v));
        }

        public void setState(int v) {
            dataWatcher.updateObject(21, Integer.valueOf(v));
        }

        public void setOwnerId(int v) {
            dataWatcher.updateObject(22, Integer.valueOf(v));
        }

        public void setSlot(int v) {
            dataWatcher.updateObject(23, Integer.valueOf(v));
        }

        @Override
        public void onUpdate() {
            int st = getState();
            if (st == 0) {
                setPosition(posX, posY, posZ + 0.05);
                if (!worldObj.isRemote) {
                    GameData g = Game.get(worldObj);
                    if (!g.active || posZ > g.z0 + 16.6) setDead();
                }
            } else if (st == 3) {
                Entity o = worldObj.getEntityByID(getOwnerId());
                if (o != null) {
                    setPosition(o.posX, o.boundingBox.minY + o.height + 0.1, o.posZ);
                    rotationYaw = o.rotationYaw;
                } else if (!worldObj.isRemote) {
                    setDead();
                }
            }
            ticksExisted++;
        }

        @Override
        public boolean canBeCollidedWith() {
            return true;
        }

        @Override
        public boolean interactFirst(EntityPlayer p) {
            if (!worldObj.isRemote) Game.interact(p, this);
            return true;
        }

        @Override
        public boolean writeToNBTOptional(NBTTagCompound tag) {
            return false; // nao salva: o jogo recria pelos dados do mundo
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound tag) {}

        @Override
        protected void writeEntityToNBT(NBTTagCompound tag) {}
    }

    public static class EntityBotPlayer extends EntityCreature {
        public EntityBotPlayer(World w) {
            super(w);
            setSize(0.6F, 1.8F);
            getNavigator().setAvoidsWater(true);
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(1, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.3D, false));
            tasks.addTask(2, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
            setCurrentItemOrArmor(0, new ItemStack(batItem));
            setCustomNameTag("Bot");
            setAlwaysRenderNameTag(true);
        }

        @Override
        public boolean isAIEnabled() {
            return true;
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(40.0D);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.32D);
            getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(2.0D);
        }

        @Override
        protected boolean canDespawn() {
            return false;
        }

        public boolean attackEntityAsMob(Entity t) {
            swingItem();
            float d = (float) getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
            final Entity self = this;
            DamageSource src = new EntityDamageSource("mob", self) {
                @Override
                public boolean isDifficultyScaled() {
                    return false;
                }
            };
            t.attackEntityFrom(src, d);
            double dx = t.posX - posX;
            double dz = t.posZ - posZ;
            double l = Math.sqrt(dx * dx + dz * dz) + 0.0001;
            t.addVelocity(dx / l * 0.6, 0.3, dz / l * 0.6);
            t.velocityChanged = true;
            if (t instanceof EntityPlayer && !worldObj.isRemote) Game.botHitPlayer((EntityPlayer) t);
            return true;
        }
    }

    // ================= LOGICA DO JOGO =================
    public static class Game {
        static int raidState = 0, raidSlot = -1;
        static long nextRaid = 0;

        static GameData get(World w) {
            GameData g = (GameData) w.mapStorage.loadData(GameData.class, GameData.NAME);
            if (g == null) {
                g = new GameData(GameData.NAME);
                w.mapStorage.setData(GameData.NAME, g);
            }
            return g;
        }

        static AxisAlignedBB arenaBox(GameData g) {
            return AxisAlignedBB.getBoundingBox(g.x0 - 1, g.y0 - 3, g.z0 - 1, g.x0 + 42, g.y0 + 25, g.z0 + 18);
        }

        static boolean inside(GameData g, double x, double y, double z) {
            return x >= g.x0 && x < g.x0 + 41 && z >= g.z0 && z < g.z0 + 17 && y >= g.y0 - 2 && y < g.y0 + 20;
        }

        static boolean inBlocks(GameData g, int x, int y, int z) {
            return x >= g.x0 && x <= g.x0 + 40 && z >= g.z0 && z <= g.z0 + 16 && y >= g.y0 && y <= g.y0 + 10;
        }

        static double[] slotPos(GameData g, boolean bot, int i) {
            int col = i % 2;
            int row = i / 2;
            return new double[] {g.x0 + (bot ? 28 : 0) + (col == 0 ? 3 : 8) + 0.5, g.y0 + 1.0, g.z0 + 3 + row * 5 + 0.5};
        }

        static int firstFree(int[] a) {
            for (int i = 0; i < 6; i++) if (a[i] < 0) return i;
            return -1;
        }

        static int count(int[] a) {
            int c = 0;
            for (int i = 0; i < 6; i++) if (a[i] >= 0) c++;
            return c;
        }

        static int randomVariant() {
            int tot = 0;
            for (int i = 0; i < 6; i++) tot += WEIGHT[i];
            int r = RND.nextInt(tot);
            for (int i = 0; i < 6; i++) {
                r -= WEIGHT[i];
                if (r < 0) return i;
            }
            return 0;
        }

        static EntityBrainrot spawnBr(World w, int v, int state, int slot, double x, double y, double z, float yaw) {
            EntityBrainrot b = new EntityBrainrot(w);
            b.setLocationAndAngles(x, y, z, yaw, 0);
            b.setVariant(v);
            b.setState(state);
            b.setSlot(slot);
            w.spawnEntityInWorld(b);
            return b;
        }

        // ----- criar a arena -----
        static void start(EntityPlayer p, GameData g) {
            World w = p.worldObj;
            g.active = true;
            g.dim = w.provider.dimensionId;
            g.x0 = (int) Math.floor(p.posX) - 6;
            g.z0 = (int) Math.floor(p.posZ) - 8;
            g.y0 = (int) Math.floor(p.posY) - 1;
            g.owner = p.getCommandSenderName();
            g.money = 100;
            for (int i = 0; i < 6; i++) {
                g.ps[i] = -1;
                g.bs[i] = -1;
            }
            g.bs[0] = RND.nextInt(3);
            g.bs[1] = RND.nextInt(4);
            g.pLock = 0;
            g.bLock = 0;
            g.lockCd = 0;
            g.carrying = -1;
            g.botCarry = -1;
            raidState = 0;
            nextRaid = w.getTotalWorldTime() + 2400;
            build(w, g);
            g.markDirty();
            p.setPositionAndUpdate(g.x0 + 6.5, g.y0 + 1.0, g.z0 + 8.5);
            p.inventory.addItemStackToInventory(new ItemStack(batItem));
            p.inventory.addItemStackToInventory(new ItemStack(lockItem));
            msg(p, "Brainrot iniciado! Clique nos brainrots da esteira para comprar (voce tem $100).");
            msg(p, "Sua base e a azul. Roube da base vermelha, mas o bot vai te perseguir. Use o bastao!");
        }

        static void build(World w, GameData g) {
            for (int x = 0; x <= 40; x++) {
                for (int z = 0; z <= 16; z++) {
                    Block fl = Blocks.stonebrick;
                    int meta = 0;
                    if (x <= 12) {
                        fl = Blocks.stained_hardened_clay;
                        meta = 3;
                    } else if (x >= 28) {
                        fl = Blocks.stained_hardened_clay;
                        meta = 14;
                    } else if (x >= 19 && x <= 21) {
                        fl = Blocks.coal_block;
                    }
                    for (int s = 0; s < 6; s++) {
                        double[] a = slotPos(g, false, s);
                        double[] b = slotPos(g, true, s);
                        if ((int) a[0] - g.x0 == x && (int) a[2] - g.z0 == z) {
                            fl = Blocks.iron_block;
                            meta = 0;
                        }
                        if ((int) b[0] - g.x0 == x && (int) b[2] - g.z0 == z) {
                            fl = Blocks.iron_block;
                            meta = 0;
                        }
                    }
                    w.setBlock(g.x0 + x, g.y0, g.z0 + z, fl, meta, 3);
                    boolean border = x == 0 || x == 40 || z == 0 || z == 16;
                    for (int y = 1; y <= 8; y++) {
                        Block b = (border && y <= 3) ? Blocks.iron_bars : Blocks.air;
                        w.setBlock(g.x0 + x, g.y0 + y, g.z0 + z, b, 0, 3);
                    }
                }
            }
        }

        // ----- tick principal (servidor) -----
        static void tick(World w, GameData g) {
            long t = w.getTotalWorldTime();
            EntityPlayer p = w.getPlayerEntityByName(g.owner);
            if (p == null) return;
            if (nextRaid == 0) nextRaid = t + 2400;

            if (p.getHealth() > 0 && !inside(g, p.posX, p.posY, p.posZ)) {
                p.setPositionAndUpdate(g.x0 + 6.5, g.y0 + 1.0, g.z0 + 8.5);
                p.fallDistance = 0;
                msg(p, "Voce nao pode sair da area do Brainrot!");
            }

            if (t % 10 == 0) botLogic(w, g, p, t);
            if (t % 20 == 0) {
                int inc = 0;
                for (int i = 0; i < 6; i++) if (g.ps[i] >= 0) inc += INCOME[g.ps[i]];
                g.money = (int) Math.min(2000000000L, (long) g.money + inc);
                g.markDirty();
                syncSlots(w, g);
                board(w, g, t, inc);
                EntityBotPlayer bot = findBot(w, g, false);
                if (bot != null) bot.setHealth(bot.getMaxHealth());
            }
            if (t % 60 == 0) {
                List belt = beltList(w, g);
                if (belt.size() < 8) {
                    spawnBr(w, randomVariant(), 0, 0, g.x0 + 20.5, g.y0 + 1.0, g.z0 + 0.8, 0F);
                }
            }
            if (t % 600 == 0 && t >= g.bLock && RND.nextInt(4) == 0) {
                g.bLock = t + 400;
                g.markDirty();
            }
            if (t % 200 == 100 && RND.nextBoolean()) botBuy(w, g);
        }

        static List beltList(World w, GameData g) {
            List out = new ArrayList();
            List all = w.getEntitiesWithinAABB(EntityBrainrot.class, arenaBox(g));
            for (Object o : all) if (((EntityBrainrot) o).getState() == 0) out.add(o);
            return out;
        }

        static void botBuy(World w, GameData g) {
            int idx = firstFree(g.bs);
            if (idx < 0) return;
            List belt = beltList(w, g);
            if (belt.isEmpty()) return;
            EntityBrainrot b = (EntityBrainrot) belt.get(RND.nextInt(belt.size()));
            g.bs[idx] = cv(b.getVariant());
            b.setDead();
            g.markDirty();
            syncSlots(w, g);
        }

        static double[] botHome(GameData g) {
            return new double[] {g.x0 + 34.5, g.y0 + 1.0, g.z0 + 8.5};
        }

        static EntityBotPlayer findBot(World w, GameData g, boolean create) {
            List l = w.getEntitiesWithinAABB(EntityBotPlayer.class, arenaBox(g));
            EntityBotPlayer bot = null;
            for (Object o : l) {
                if (bot == null) bot = (EntityBotPlayer) o;
                else ((EntityBotPlayer) o).setDead();
            }
            if (bot == null && create) {
                double[] h = botHome(g);
                bot = new EntityBotPlayer(w);
                bot.setLocationAndAngles(h[0], h[1], h[2], 90F, 0F);
                w.spawnEntityInWorld(bot);
            }
            return bot;
        }

        static void botLogic(World w, GameData g, EntityPlayer p, long t) {
            EntityBotPlayer bot = findBot(w, g, true);
            if (bot == null) return;
            double[] h = botHome(g);

            // jogador morreu carregando: devolve
            if (g.carrying >= 0 && p.getHealth() <= 0) loseCarry(w, g, p, "Voce morreu e perdeu o brainrot!");

            // perseguicao
            if (g.carrying >= 0) {
                bot.setAttackTarget(p);
                if (p.posX < g.x0 + 13) deliver(w, g, p);
            } else if (bot.getAttackTarget() != null) {
                bot.setAttackTarget(null);
            }

            // assalto do bot na base do jogador
            if (raidState == 0 && g.carrying < 0 && g.botCarry < 0 && t >= nextRaid && t >= g.pLock
                && count(g.ps) > 0) {
                int s;
                do {
                    s = RND.nextInt(6);
                } while (g.ps[s] < 0);
                raidSlot = s;
                raidState = 1;
                msg(p, "O bot esta vindo roubar da sua base! Trave a base (cadeado ou /brainrot lock) ou bata nele!");
            }
            if (raidState == 1) {
                if (t < g.pLock || g.ps[raidSlot] < 0) {
                    raidState = 0;
                    nextRaid = t + 1200;
                } else {
                    double[] tp = slotPos(g, false, raidSlot);
                    if (bot.getDistanceSq(tp[0], tp[1], tp[2]) < 6.25) {
                        g.botCarry = g.ps[raidSlot];
                        g.botCarryFrom = raidSlot;
                        g.ps[raidSlot] = -1;
                        raidState = 2;
                        g.markDirty();
                        syncSlots(w, g);
                        msg(p, "O bot roubou seu " + NAMES[g.botCarry] + "! Acerte ele com o bastao!");
                    } else {
                        bot.getNavigator().tryMoveToXYZ(tp[0], tp[1], tp[2], 1.2D);
                    }
                }
            } else if (raidState == 2) {
                if (bot.posX >= g.x0 + 28) {
                    int idx = firstFree(g.bs);
                    if (idx >= 0) g.bs[idx] = g.botCarry;
                    else g.ps[g.botCarryFrom] = g.botCarry;
                    g.botCarry = -1;
                    raidState = 0;
                    nextRaid = t + 2400;
                    g.markDirty();
                    syncSlots(w, g);
                } else {
                    bot.getNavigator().tryMoveToXYZ(h[0], h[1], h[2], 1.3D);
                }
            } else if (g.carrying < 0 && bot.getDistanceSq(h[0], h[1], h[2]) > 9.0D) {
                bot.getNavigator().tryMoveToXYZ(h[0], h[1], h[2], 1.0D);
            }
            syncCarried(w, g, p, bot);
        }

        // ----- acoes do jogador -----
        static void interact(EntityPlayer p, EntityBrainrot e) {
            World w = p.worldObj;
            GameData g = get(w);
            if (!g.active || !p.getCommandSenderName().equals(g.owner)) return;
            int v = cv(e.getVariant());
            int st = e.getState();
            if (st == 0) {
                int idx = firstFree(g.ps);
                if (idx < 0) {
                    msg(p, "Sua base esta cheia (6/6 slots).");
                } else if (g.money < PRICE[v]) {
                    msg(p, "Dinheiro insuficiente: " + NAMES[v] + " custa $" + PRICE[v] + " (voce tem $" + g.money + ").");
                } else {
                    g.money -= PRICE[v];
                    g.ps[idx] = v;
                    e.setDead();
                    g.markDirty();
                    syncSlots(w, g);
                    msg(p, "Comprou " + NAMES[v] + "! Gera $" + INCOME[v] + "/s. Dinheiro: $" + g.money);
                }
            } else if (st == 1) {
                msg(p, NAMES[v] + " gera $" + INCOME[v] + "/s para voce.");
            } else if (st == 2) {
                int idx = e.getSlot();
                long now = w.getTotalWorldTime();
                if (g.carrying >= 0) {
                    msg(p, "Voce ja esta carregando um brainrot!");
                } else if (now < g.bLock) {
                    msg(p, "A base do bot esta TRAVADA! Espere " + ((g.bLock - now) / 20 + 1) + "s.");
                } else if (firstFree(g.ps) < 0) {
                    msg(p, "Sem slot livre na sua base para guardar o roubo.");
                } else if (idx >= 0 && idx < 6 && g.bs[idx] == v) {
                    g.carrying = v;
                    g.carryFrom = idx;
                    g.bs[idx] = -1;
                    g.markDirty();
                    syncSlots(w, g);
                    msg(p, "Roubou " + NAMES[v] + "! Corra para a sua base (azul)! O bot vai te perseguir!");
                }
            }
        }

        static void deliver(World w, GameData g, EntityPlayer p) {
            int idx = firstFree(g.ps);
            if (idx < 0) return;
            g.ps[idx] = g.carrying;
            msg(p, "Roubo concluido: " + NAMES[g.carrying] + " e seu agora!");
            g.carrying = -1;
            g.markDirty();
            syncSlots(w, g);
        }

        static void loseCarry(World w, GameData g, EntityPlayer p, String why) {
            int idx = g.bs[g.carryFrom < 0 ? 0 : g.carryFrom] < 0 ? g.carryFrom : firstFree(g.bs);
            if (idx >= 0 && idx < 6) g.bs[idx] = g.carrying;
            g.carrying = -1;
            g.markDirty();
            syncSlots(w, g);
            msg(p, why);
        }

        static void botHitPlayer(EntityPlayer p) {
            World w = p.worldObj;
            GameData g = get(w);
            if (!g.active || !p.getCommandSenderName().equals(g.owner)) return;
            if (g.carrying >= 0) {
                loseCarry(w, g, p, "O bot deu uma paulada! O brainrot voltou para a base dele.");
            }
        }

        static void botHurt(EntityBotPlayer bot) {
            World w = bot.worldObj;
            GameData g = get(w);
            if (!g.active || g.botCarry < 0) return;
            int idx = g.ps[g.botCarryFrom] < 0 ? g.botCarryFrom : firstFree(g.ps);
            if (idx >= 0) g.ps[idx] = g.botCarry;
            g.botCarry = -1;
            raidState = 0;
            nextRaid = w.getTotalWorldTime() + 2400;
            g.markDirty();
            syncSlots(w, g);
            EntityPlayer p = w.getPlayerEntityByName(g.owner);
            if (p != null) msg(p, "Voce acertou o bot e recuperou seu brainrot!");
        }

        static void toggleLock(EntityPlayer p) {
            World w = p.worldObj;
            GameData g = get(w);
            if (!g.active || !p.getCommandSenderName().equals(g.owner)) {
                msg(p, "Voce nao esta no jogo Brainrot.");
                return;
            }
            long now = w.getTotalWorldTime();
            if (now < g.pLock) {
                msg(p, "Sua base ja esta travada por mais " + ((g.pLock - now) / 20 + 1) + "s.");
            } else if (now < g.lockCd) {
                msg(p, "Cadeado recarregando: " + ((g.lockCd - now) / 20 + 1) + "s.");
            } else {
                g.pLock = now + 1200;
                g.lockCd = now + 2400;
                g.markDirty();
                msg(p, "Base TRAVADA por 60 segundos! (recarga de 120s)");
            }
        }

        // ----- sincronizacao de entidades com os dados -----
        static void syncSlots(World w, GameData g) {
            for (int side = 0; side < 2; side++) {
                boolean bot = side == 1;
                int[] arr = bot ? g.bs : g.ps;
                int state = bot ? 2 : 1;
                for (int i = 0; i < 6; i++) {
                    double[] pos = slotPos(g, bot, i);
                    List l = w.getEntitiesWithinAABB(EntityBrainrot.class,
                        AxisAlignedBB.getBoundingBox(pos[0] - 0.6, pos[1] - 0.2, pos[2] - 0.6, pos[0] + 0.6, pos[1] + 2, pos[2] + 0.6));
                    EntityBrainrot keep = null;
                    for (Object o : l) {
                        EntityBrainrot b = (EntityBrainrot) o;
                        if (b.getState() != state) continue;
                        if (keep == null && b.getVariant() == arr[i]) keep = b;
                        else b.setDead();
                    }
                    if (arr[i] >= 0 && keep == null) {
                        spawnBr(w, arr[i], state, i, pos[0], pos[1], pos[2], bot ? 90F : -90F);
                    }
                }
            }
        }

        static void syncCarried(World w, GameData g, EntityPlayer p, EntityBotPlayer bot) {
            boolean hasP = false;
            boolean hasB = false;
            for (Object o : new ArrayList(w.loadedEntityList)) {
                if (!(o instanceof EntityBrainrot)) continue;
                EntityBrainrot b = (EntityBrainrot) o;
                if (b.getState() != 3) continue;
                int own = b.getOwnerId();
                if (own == p.getEntityId() && g.carrying >= 0 && b.getVariant() == g.carrying && !hasP) hasP = true;
                else if (bot != null && own == bot.getEntityId() && g.botCarry >= 0 && b.getVariant() == g.botCarry && !hasB)
                    hasB = true;
                else b.setDead();
            }
            if (g.carrying >= 0 && !hasP) {
                EntityBrainrot b = spawnBr(w, g.carrying, 3, 0, p.posX, p.posY, p.posZ, 0F);
                b.setOwnerId(p.getEntityId());
            }
            if (g.botCarry >= 0 && bot != null && !hasB) {
                EntityBrainrot b = spawnBr(w, g.botCarry, 3, 0, bot.posX, bot.posY, bot.posZ, 0F);
                b.setOwnerId(bot.getEntityId());
            }
        }

        // ----- placar (mostra dinheiro e bases) -----
        static void board(World w, GameData g, long t, int inc) {
            Scoreboard sb = w.getScoreboard();
            ScoreObjective o = sb.getObjective("brainrot");
            if (o == null) {
                o = sb.addScoreObjective("brainrot", new ScoreDummyCriteria("dummy"));
                o.setDisplayName("BRAINROT");
                sb.setObjectiveInDisplaySlot(1, o);
            }
            set(sb, o, "Dinheiro $", g.money);
            set(sb, o, "Renda por seg", inc);
            set(sb, o, "Sua base", count(g.ps));
            set(sb, o, "Base do bot", count(g.bs));
            set(sb, o, "Trava voce (s)", (int) Math.max(0, (g.pLock - t) / 20));
            set(sb, o, "Trava bot (s)", (int) Math.max(0, (g.bLock - t) / 20));
        }

        static void set(Scoreboard sb, ScoreObjective o, String n, int v) {
            sb.getValueFromObjective(n, o).setScorePoints(v);
        }

        static void reset(World w, GameData g) {
            g.active = false;
            g.carrying = -1;
            g.botCarry = -1;
            g.markDirty();
            for (Object o : new ArrayList(w.loadedEntityList)) {
                if (o instanceof EntityBrainrot || o instanceof EntityBotPlayer) ((Entity) o).setDead();
            }
            w.getScoreboard().setObjectiveInDisplaySlot(1, null);
        }
    }

    // ================= EVENTOS =================
    public static class Events {
        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent e) {
            if (e.phase != TickEvent.Phase.END || e.world.isRemote) return;
            GameData g = Game.get(e.world);
            if (!g.active || g.dim != e.world.provider.dimensionId) return;
            Game.tick(e.world, g);
        }

        @SubscribeEvent
        public void onBreak(BlockEvent.BreakEvent e) {
            if (e.world.isRemote) return;
            GameData g = Game.get(e.world);
            if (g.active && g.dim == e.world.provider.dimensionId && Game.inBlocks(g, e.x, e.y, e.z)) {
                e.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void onInteract(PlayerInteractEvent e) {
            if (e.world.isRemote || e.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
            ItemStack st = e.entityPlayer.getCurrentEquippedItem();
            if (st == null || !(st.getItem() instanceof ItemBlock)) return;
            GameData g = Game.get(e.world);
            if (g.active && g.dim == e.world.provider.dimensionId && Game.inside(g, e.x, e.y, e.z)) {
                e.useItem = Event.Result.DENY;
            }
        }

        @SubscribeEvent
        public void onHurt(LivingHurtEvent e) {
            if (e.entityLiving.worldObj.isRemote) return;
            if (e.entityLiving instanceof EntityBotPlayer && e.source.getEntity() instanceof EntityPlayer) {
                Game.botHurt((EntityBotPlayer) e.entityLiving);
            }
        }
    }

    // ================= COMANDO =================
    public static class CmdBrainrot extends CommandBase {
        @Override
        public String getCommandName() {
            return "brainrot";
        }

        @Override
        public String getCommandUsage(ICommandSender s) {
            return "/brainrot  (inicia)  |  /brainrot lock  |  /brainrot reset";
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
            World w = p.worldObj;
            GameData g = Game.get(w);
            if (a.length > 0 && a[0].equalsIgnoreCase("lock")) {
                Game.toggleLock(p);
                return;
            }
            if (a.length > 0 && a[0].equalsIgnoreCase("reset")) {
                if (!s.canCommandSenderUseCommand(2, "brainrot")) {
                    msg(p, "Apenas operadores podem resetar o jogo.");
                    return;
                }
                Game.reset(w, g);
                msg(p, "Jogo Brainrot removido deste mundo.");
                return;
            }
            if (g.active) {
                msg(p, "O jogo Brainrot ja existe neste mundo (so pode haver 1).");
                return;
            }
            Game.start(p, g);
        }
    }

    // ================= CLIENTE =================
    @SideOnly(Side.CLIENT)
    public static class ClientSide {
        public static void init() {
            RenderingRegistry.registerEntityRenderingHandler(EntityBrainrot.class, new RenderBrainrot());
            RenderingRegistry.registerEntityRenderingHandler(EntityBotPlayer.class, new RenderBot());
            MinecraftForgeClient.registerItemRenderer(batItem, new ToolItemRenderer(0));
            MinecraftForgeClient.registerItemRenderer(lockItem, new ToolItemRenderer(1));
        }
    }

    // ---------- texturas feitas por codigo ----------
    @SideOnly(Side.CLIENT)
    public static class Tex {
        static boolean ready = false;
        public static ResourceLocation WHITE, EYE, WOOD;

        static int rgb(int r, int g, int b) {
            return 0xFF000000 | (r << 16) | (g << 8) | b;
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
            BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) im.setRGB(x, y, 0xFFFFFFFF);
            WHITE = make("white", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int c = rgb(250, 250, 250);
                    if (x >= 6 && x <= 10 && y >= 5 && y <= 11) c = rgb(10, 10, 10);
                    if (x == 7 && y == 6) c = rgb(255, 255, 255);
                    im.setRGB(x, y, c);
                }
            }
            EYE = make("eye", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int n = (x * 5 + y * 3 + x * y) % 4;
                    int r = 150 + n * 8;
                    int g = 100 + n * 6;
                    int b = 55 + n * 4;
                    if (y % 5 == 0) {
                        r -= 35;
                        g -= 28;
                        b -= 20;
                    }
                    im.setRGB(x, y, rgb(r, g, b));
                }
            }
            WOOD = make("wood", im);
        }
    }

    // ---------- modelos 3D feitos por codigo ----------
    @SideOnly(Side.CLIENT)
    public static class Models {
        static int br = FULL;

        static void begin() {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            br = FULL;
            Tex.init();
        }

        static void end() {
            GL11.glPopAttrib();
        }

        static void box(ResourceLocation tex, double x1, double y1, double z1, double x2, double y2, double z2,
            float r, float g, float b, float a) {
            Tex.bind(tex);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.setBrightness(br);
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

        // caixa colorida lisa
        static void c(double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b) {
            box(Tex.WHITE, x1, y1, z1, x2, y2, z2, r, g, b, 1f);
        }

        // caixa de madeira
        static void wd(double x1, double y1, double z1, double x2, double y2, double z2) {
            box(Tex.WOOD, x1, y1, z1, x2, y2, z2, 1f, 1f, 1f, 1f);
        }

        // olho (face voltada para +Z)
        static void eye(double x1, double y1, double x2, double y2, double z) {
            Tex.bind(Tex.EYE);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.setBrightness(br);
            t.setColorRGBA_F(1f, 1f, 1f, 1f);
            t.addVertexWithUV(x1, y1, z, 0, 1);
            t.addVertexWithUV(x2, y1, z, 1, 1);
            t.addVertexWithUV(x2, y2, z, 1, 0);
            t.addVertexWithUV(x1, y2, z, 0, 0);
            t.draw();
        }

        // frente do modelo = +Z
        static void drawBrainrot(int v) {
            begin();
            switch (v) {
                case 0: // Tung Tung Sahur
                    wd(-0.18, 0.15, -0.15, 0.18, 0.95, 0.15);
                    eye(-0.14, 0.65, -0.02, 0.77, 0.152);
                    eye(0.02, 0.65, 0.14, 0.77, 0.152);
                    c(-0.07, 0.40, 0.15, 0.07, 0.50, 0.155, 0.1f, 0.04f, 0.04f);
                    wd(-0.14, 0.0, -0.06, -0.04, 0.15, 0.06);
                    wd(0.04, 0.0, -0.06, 0.14, 0.15, 0.06);
                    wd(-0.26, 0.45, -0.05, -0.18, 0.75, 0.05);
                    wd(0.18, 0.45, -0.05, 0.26, 0.75, 0.05);
                    c(0.26, 0.40, 0.00, 0.33, 1.05, 0.10, 0.62f, 0.42f, 0.22f);
                    break;
                case 1: // Tralalero Tralala
                    c(-0.14, 0.35, -0.45, 0.14, 0.70, 0.40, 0.35f, 0.50f, 0.78f);
                    c(-0.12, 0.35, -0.40, 0.12, 0.42, 0.38, 0.85f, 0.88f, 0.92f);
                    c(-0.13, 0.40, 0.40, 0.13, 0.65, 0.55, 0.35f, 0.50f, 0.78f);
                    eye(-0.11, 0.54, -0.02, 0.63, 0.552);
                    eye(0.02, 0.54, 0.11, 0.63, 0.552);
                    c(-0.015, 0.70, -0.10, 0.015, 0.95, 0.15, 0.30f, 0.45f, 0.72f);
                    c(-0.015, 0.40, -0.55, 0.015, 0.68, -0.45, 0.30f, 0.45f, 0.72f);
                    c(-0.10, 0.06, -0.30, -0.04, 0.35, -0.24, 0.90f, 0.75f, 0.60f);
                    c(0.04, 0.06, -0.30, 0.10, 0.35, -0.24, 0.90f, 0.75f, 0.60f);
                    c(-0.03, 0.06, 0.20, 0.03, 0.35, 0.26, 0.90f, 0.75f, 0.60f);
                    c(-0.13, 0.0, -0.34, -0.01, 0.06, -0.18, 0.95f, 0.95f, 0.95f);
                    c(0.01, 0.0, -0.34, 0.13, 0.06, -0.18, 0.95f, 0.95f, 0.95f);
                    c(-0.06, 0.0, 0.16, 0.06, 0.06, 0.32, 0.95f, 0.95f, 0.95f);
                    break;
                case 2: // Lirili Larila
                    c(-0.20, 0.10, -0.20, 0.20, 0.75, 0.20, 0.20f, 0.55f, 0.25f);
                    c(-0.20, 0.70, -0.18, 0.20, 1.05, 0.22, 0.60f, 0.60f, 0.65f);
                    c(-0.34, 0.72, -0.10, -0.20, 1.02, 0.14, 0.55f, 0.55f, 0.60f);
                    c(0.20, 0.72, -0.10, 0.34, 1.02, 0.14, 0.55f, 0.55f, 0.60f);
                    c(-0.05, 0.45, 0.22, 0.05, 0.80, 0.32, 0.55f, 0.55f, 0.60f);
                    eye(-0.15, 0.86, -0.04, 0.98, 0.222);
                    eye(0.04, 0.86, 0.15, 0.98, 0.222);
                    c(-0.16, 0.0, -0.12, -0.04, 0.10, 0.14, 0.45f, 0.30f, 0.15f);
                    c(0.04, 0.0, -0.12, 0.16, 0.10, 0.14, 0.45f, 0.30f, 0.15f);
                    break;
                case 3: // Cappuccino Assassino
                    c(-0.20, 0.0, -0.20, 0.20, 0.55, 0.20, 0.92f, 0.92f, 0.88f);
                    c(-0.21, 0.55, -0.21, 0.21, 0.60, 0.21, 0.40f, 0.25f, 0.12f);
                    c(-0.15, 0.60, -0.15, 0.15, 0.90, 0.15, 0.10f, 0.10f, 0.12f);
                    c(-0.16, 0.76, -0.16, 0.16, 0.82, 0.16, 0.80f, 0.10f, 0.10f);
                    eye(-0.12, 0.64, -0.02, 0.74, 0.152);
                    eye(0.02, 0.64, 0.12, 0.74, 0.152);
                    c(0.20, 0.20, 0.00, 0.28, 0.45, 0.08, 0.60f, 0.50f, 0.40f);
                    c(0.30, 0.05, -0.02, 0.33, 0.95, 0.04, 0.80f, 0.80f, 0.85f);
                    c(0.26, 0.40, -0.03, 0.37, 0.44, 0.05, 0.20f, 0.20f, 0.20f);
                    c(-0.28, 0.20, 0.00, -0.20, 0.45, 0.08, 0.60f, 0.50f, 0.40f);
                    c(-0.33, 0.05, -0.02, -0.30, 0.95, 0.04, 0.80f, 0.80f, 0.85f);
                    c(-0.37, 0.40, -0.03, -0.26, 0.44, 0.05, 0.20f, 0.20f, 0.20f);
                    break;
                case 4: // Ballerina Cappuccina
                    c(-0.30, 0.35, -0.30, 0.30, 0.42, 0.30, 1.0f, 0.65f, 0.80f);
                    c(-0.10, 0.42, -0.08, 0.10, 0.65, 0.08, 1.0f, 0.70f, 0.85f);
                    c(-0.08, 0.0, -0.03, -0.02, 0.35, 0.03, 0.95f, 0.80f, 0.70f);
                    c(0.02, 0.0, -0.03, 0.08, 0.35, 0.03, 0.95f, 0.80f, 0.70f);
                    c(-0.20, 0.65, -0.20, 0.20, 1.00, 0.20, 0.55f, 0.35f, 0.20f);
                    c(-0.21, 0.98, -0.21, 0.21, 1.03, 0.21, 0.95f, 0.93f, 0.85f);
                    eye(-0.14, 0.78, -0.03, 0.89, 0.202);
                    eye(0.03, 0.78, 0.14, 0.89, 0.202);
                    c(-0.17, 0.55, -0.04, -0.10, 0.95, 0.04, 0.95f, 0.80f, 0.70f);
                    c(0.10, 0.55, -0.04, 0.17, 0.95, 0.04, 0.95f, 0.80f, 0.70f);
                    break;
                default: // Bombardiro Crocodilo
                    c(-0.15, 0.35, -0.50, 0.15, 0.62, 0.45, 0.25f, 0.50f, 0.25f);
                    c(-0.12, 0.38, 0.45, 0.12, 0.55, 0.78, 0.30f, 0.60f, 0.30f);
                    c(-0.12, 0.38, 0.60, 0.12, 0.41, 0.78, 0.95f, 0.95f, 0.90f);
                    eye(-0.12, 0.56, -0.03, 0.66, 0.452);
                    eye(0.03, 0.56, 0.12, 0.66, 0.452);
                    c(-0.70, 0.45, -0.20, -0.15, 0.49, 0.15, 0.40f, 0.40f, 0.45f);
                    c(0.15, 0.45, -0.20, 0.70, 0.49, 0.15, 0.40f, 0.40f, 0.45f);
                    c(-0.30, 0.45, -0.55, 0.30, 0.49, -0.45, 0.40f, 0.40f, 0.45f);
                    c(-0.02, 0.50, -0.55, 0.02, 0.78, -0.45, 0.40f, 0.40f, 0.45f);
                    c(-0.06, 0.12, -0.05, 0.06, 0.35, 0.25, 0.10f, 0.10f, 0.12f);
                    c(-0.04, 0.0, 0.0, 0.04, 0.12, 0.20, 0.20f, 0.20f, 0.22f);
                    break;
            }
            end();
        }

        static void drawBat() {
            begin();
            wd(-0.04, 0.0, -0.04, 0.04, 0.45, 0.04);
            wd(-0.06, 0.45, -0.06, 0.06, 0.80, 0.06);
            wd(-0.07, 0.80, -0.07, 0.07, 0.95, 0.07);
            c(-0.05, -0.04, -0.05, 0.05, 0.0, 0.05, 0.35f, 0.22f, 0.10f);
            end();
        }

        static void drawLock() {
            begin();
            c(-0.25, 0.10, -0.10, 0.25, 0.55, 0.10, 0.85f, 0.70f, 0.15f);
            c(-0.20, 0.55, -0.06, -0.12, 0.85, 0.06, 0.65f, 0.65f, 0.70f);
            c(0.12, 0.55, -0.06, 0.20, 0.85, 0.06, 0.65f, 0.65f, 0.70f);
            c(-0.20, 0.80, -0.06, 0.20, 0.88, 0.06, 0.65f, 0.65f, 0.70f);
            c(-0.03, 0.25, 0.10, 0.03, 0.40, 0.11, 0.05f, 0.05f, 0.05f);
            end();
        }

        static void label(String s, double x, double y, double z) {
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(-0.02F, -0.02F, 0.02F);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            fr.drawString(s, -fr.getStringWidth(s) / 2, 0, 0xFFFFFF);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glPopMatrix();
        }
    }

    // ---------- renderizadores ----------
    @SideOnly(Side.CLIENT)
    public static class RenderBrainrot extends Render {
        @Override
        public void doRender(Entity e, double x, double y, double z, float yaw, float pt) {
            EntityBrainrot b = (EntityBrainrot) e;
            int v = cv(b.getVariant());
            int st = b.getState();
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);
            float sc = st == 3 ? 0.6F : 1.0F;
            GL11.glScalef(sc, sc, sc);
            GL11.glRotatef(-e.rotationYaw, 0.0F, 1.0F, 0.0F);
            Models.drawBrainrot(v);
            GL11.glPopMatrix();
            if (st == 0) Models.label(NAMES[v] + " $" + PRICE[v], x, y + 1.35, z);
            else if (st == 1 || st == 2) Models.label(NAMES[v] + " +$" + INCOME[v] + "/s", x, y + 1.35, z);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity e) {
            return TextureMap.locationBlocksTexture;
        }
    }

    @SideOnly(Side.CLIENT)
    public static class RenderBot extends RenderBiped {
        static final ResourceLocation SKIN = new ResourceLocation("textures/entity/steve.png");

        public RenderBot() {
            super(new ModelBiped(), 0.5F);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity e) {
            return SKIN;
        }
    }

    // ---------- renderizador dos itens 3D (bastao e cadeado) ----------
    @SideOnly(Side.CLIENT)
    public static class ToolItemRenderer implements IItemRenderer {
        private final int kind; // 0 bastao, 1 cadeado

        public ToolItemRenderer(int kind) {
            this.kind = kind;
        }

        @Override
        public boolean handleRenderType(ItemStack item, ItemRenderType type) {
            return type != ItemRenderType.FIRST_PERSON_MAP;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
            return helper == ItemRendererHelper.INVENTORY_BLOCK || helper == ItemRendererHelper.ENTITY_ROTATION
                || helper == ItemRendererHelper.ENTITY_BOBBING || helper == ItemRendererHelper.BLOCK_3D;
        }

        @Override
        public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
            GL11.glPushMatrix();
            switch (type) {
                case INVENTORY:
                    GL11.glTranslatef(0.0F, -0.5F, 0.0F);
                    if (kind == 0) {
                        GL11.glTranslatef(0.0F, 0.5F, 0.0F);
                        GL11.glRotatef(40.0F, 0.0F, 0.0F, 1.0F);
                        GL11.glTranslatef(0.0F, -0.5F, 0.0F);
                    }
                    break;
                case ENTITY:
                    GL11.glScalef(0.5F, 0.5F, 0.5F);
                    GL11.glTranslatef(0.0F, -0.5F, 0.0F);
                    break;
                default: // na mao (1a e 3a pessoa)
                    GL11.glTranslatef(0.5F, 0.3F, 0.5F);
                    GL11.glRotatef(-20.0F, 0.0F, 0.0F, 1.0F);
                    GL11.glScalef(0.9F, 0.9F, 0.9F);
                    break;
            }
            if (kind == 0) Models.drawBat();
            else Models.drawLock();
            GL11.glPopMatrix();
        }
    }
}
