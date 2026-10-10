package com.myname.mymodid;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
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
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = JjkCoreMod.MODID, name = "Jujutsu Kaisen Core", version = "1.0", acceptedMinecraftVersions = "[1.7.10]")
public class JjkCoreMod {

    public static final String MODID = "jjkcore";
    public static JjkCoreMod instance;
    public static SimpleNetworkWrapper NET;
    public static Item bookItem;
    public static Item domItem;

    public static final String[] CHARS = { "gojo", "sukuna", "geto", "yuta" };
    public static final String[] NOMES = { "Gojo Satoru", "Ryomen Sukuna", "Suguru Geto", "Yuta Okkotsu" };
    public static final String[] DESC = { "O feiticeiro mais forte.", "O Rei das Maldicoes.", "Mestre de espiritos.", "Vinculo com Rika." };
    public static final String[][] HABS = { { "Azul", "Vermelho", "Roxo" }, { "Dismantle", "Cleave", "Santuario" }, { "Espirito", "Uzumaki", "Fala" }, { "Copia", "Rika", "Feixe Rika" } };
    public static final int[][] CUSTO = { { 15, 25, 75 }, { 15, 25, 75 }, { 20, 35, 80 }, { 15, 30, 75 } };
    public static final int[] EAMAX = { 200, 200, 180, 200 };
    public static final int[] EAINI = { 100, 100, 100, 100 };
    public static final Map<String, Long> DOM_UNTIL = new HashMap<String, Long>();
    public static final Map<String, Integer> DOM_COLOR = new HashMap<String, Integer>();

    public static final String[] DOM_NAME = { "Vazio Infinito", "Santuario Malevolente", "Corrente de Maldicoes", "Amor Eterno" };
    public static final String[] DOM_SUB = { "Dentro do infinito, nao ha escapatoria.", "Tudo sera cortado. Nada sobrevive.", "As maldicoes consomem tudo.", "O amor que nunca termina." };
    public static final int DOM_DURATION_MS = 12000;
    public static final int DOM_COOLDOWN_TICKS = 600;
    public static final int DOM_CLASH_RADIUS = 24;
    public static final Map<String, int[]> DOM_DUELS = new HashMap<String, int[]>();

    public static final Map<String, Long> SEAL_UNTIL = new HashMap<String, Long>();
    public static final int SEAL_DURATION_MS = 1500;

    public static final double ORB_BASE_RANGE = 6.0;
    public static final double ORB_MAX_RANGE = 42.0;
    public static final float ORB_DMG_MIN = 0.5f;
    public static final float ORB_DMG_MAX = 2.0f;

    public interface GuiOpener { void open(); }
    public static GuiOpener guiOpener = null;

    public JjkCoreMod() { instance = this; }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        bookItem = new ItemBook().setUnlocalizedName("jjk_book").setTextureName("minecraft:book_normal").setCreativeTab(CreativeTabs.tabMisc);
        GameRegistry.registerItem(bookItem, "jjk_book");

        domItem = new ItemDomain();
        GameRegistry.registerItem(domItem, "jjk_domain");

        EntityRegistry.registerModEntity(EntityCursed.class, "jjk_cursed", 0, this, 80, 3, true);
        EntityRegistry.registerModEntity(EntityNPC.class, "jjk_npc", 1, this, 80, 3, true);
        EntityRegistry.registerModEntity(EntityOrb.class, "jjk_orb", 2, this, 80, 3, true);
        EntityRegistry.registerModEntity(EBossSukuna.class, "jjk_bsuk", 3, this, 128, 3, true);
        EntityRegistry.registerModEntity(EBossSukunaHeian.class, "jjk_bsukh", 4, this, 160, 3, true);
        EntityRegistry.registerModEntity(EBossGojo.class, "jjk_bgojo", 5, this, 160, 3, true);

        NET = NetworkRegistry.INSTANCE.newSimpleChannel("jjk");
        NET.registerMessage(MsgSel.H.class, MsgSel.class, 0, Side.SERVER);
        NET.registerMessage(MsgCast.H.class, MsgCast.class, 1, Side.SERVER);
        NET.registerMessage(MsgSeal.H.class, MsgSeal.class, 2, Side.CLIENT);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        GameRegistry.addShapelessRecipe(new ItemStack(bookItem),
            new ItemStack(Items.book), new ItemStack(Items.dye, 1, 0), new ItemStack(Items.paper, 2));
        MinecraftForge.EVENT_BUS.register(new SEv());
        FMLCommonHandler.instance().bus().register(new SEv());
        GameRegistry.registerWorldGenerator(new WorldGen(), 10);
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new CmdAjuda());
    }

    public static NBTTagCompound nbt(EntityPlayer p) {
        NBTTagCompound t = p.getEntityData();
        if (!t.hasKey("JJK")) t.setTag("JJK", new NBTTagCompound());
        return t.getCompoundTag("JJK");
    }

    public static String getChar(EntityPlayer p) { return nbt(p).getString("char"); }
    public static int getEA(EntityPlayer p) { return nbt(p).getInteger("ea"); }
    public static int getEAMax(EntityPlayer p) { return nbt(p).getInteger("eaMax"); }

    public static int idx(String c) {
        for (int i = 0; i < CHARS.length; i++) if (CHARS[i].equals(c)) return i;
        return -1;
    }

    public static void setEA(EntityPlayer p, int v) {
        NBTTagCompound t = nbt(p);
        if (v > t.getInteger("eaMax")) v = t.getInteger("eaMax");
        if (v < 0) v = 0;
        t.setInteger("ea", v);
    }

    public static void setChar(EntityPlayer p, String c) {
        int i = idx(c);
        if (i < 0) return;
        NBTTagCompound t = nbt(p);
        t.setString("char", c);
        t.setInteger("ea", EAINI[i]);
        t.setInteger("eaMax", EAMAX[i]);
        t.setInteger("cd1", 0);
        t.setInteger("cd2", 0);
        t.setInteger("cd3", 0);
    }

    public static boolean hasDomain(EntityPlayer p) { return nbt(p).getBoolean("hasDom"); }
    public static void setHasDomain(EntityPlayer p, boolean b) { nbt(p).setBoolean("hasDom", b); }

    public static boolean domActive(EntityPlayer p) {
        NBTTagCompound t = nbt(p);
        if (!t.getBoolean("domOn")) return false;
        if (System.currentTimeMillis() > t.getLong("domUntil")) {
            t.setBoolean("domOn", false);
            t.setInteger("domCd", DOM_COOLDOWN_TICKS);
            return false;
        }
        return true;
    }

    public static void startDomain(EntityPlayer p) {
        NBTTagCompound t = nbt(p);
        t.setBoolean("domOn", true);
        t.setLong("domUntil", System.currentTimeMillis() + DOM_DURATION_MS);
        t.setInteger("domDmg", 0);
        t.setInteger("domTaken", 0);
        DOM_UNTIL.put(p.getCommandSenderName(), t.getLong("domUntil"));
        DOM_COLOR.put(p.getCommandSenderName(), domColorOf(getChar(p)));
    }

    public static void endDomain(EntityPlayer p, boolean punished) {
        NBTTagCompound t = nbt(p);
        t.setBoolean("domOn", false);
        t.setInteger("domCd", punished ? DOM_COOLDOWN_TICKS * 2 : DOM_COOLDOWN_TICKS);
        DOM_UNTIL.remove(p.getCommandSenderName());
        DOM_COLOR.remove(p.getCommandSenderName());
    }

    public static int domColorOf(String c) {
        if ("gojo".equals(c)) return 0xAA00FF;
        if ("sukuna".equals(c)) return 0xFF2222;
        if ("geto".equals(c)) return 0x8844AA;
        if ("yuta".equals(c)) return 0xFF66AA;
        return 0xAA00FF;
    }

    public static EntityPlayer findClashOpponent(EntityPlayer p) {
        List pl = p.worldObj.playerEntities;
        if (pl == null) return null;
        for (int i = 0; i < pl.size(); i++) {
            Object o = pl.get(i);
            if (!(o instanceof EntityPlayer)) continue;
            EntityPlayer q = (EntityPlayer) o;
            if (q == p) continue;
            if (!domActive(q)) continue;
            double dx = q.posX - p.posX, dz = q.posZ - p.posZ;
            if (dx * dx + dz * dz < DOM_CLASH_RADIUS * DOM_CLASH_RADIUS) return q;
        }
        return null;
    }

    public static void ensureDomainSlot(EntityPlayer p) {
        if (!hasDomain(p)) return;
        ItemStack slot0 = p.inventory.mainInventory[0];
        if (slot0 != null && slot0.getItem() == domItem) return;
        for (int i = 1; i < p.inventory.mainInventory.length; i++) {
            ItemStack s = p.inventory.mainInventory[i];
            if (s != null && s.getItem() == domItem) {
                p.inventory.mainInventory[i] = slot0;
                p.inventory.mainInventory[0] = s;
                return;
            }
        }
        ItemStack slot0Back = p.inventory.mainInventory[0];
        p.inventory.mainInventory[0] = new ItemStack(domItem);
        if (slot0Back != null) {
            if (!p.inventory.addItemStackToInventory(slot0Back)) {
                p.dropPlayerItemWithRandomChoice(slot0Back, false);
            }
        }
    }

    // ================= ITENS =================
    public static class ItemBook extends Item {
        @Override
        public ItemStack onItemRightClick(ItemStack s, World w, EntityPlayer p) {
            if (w.isRemote && guiOpener != null) guiOpener.open();
            return s;
        }
        @Override
        public void addInformation(ItemStack s, EntityPlayer p, List l, boolean a) {
            String c = getChar(p);
            if (c.isEmpty()) {
                l.add(EnumChatFormatting.GRAY + "Clique direito para escolher");
                l.add(EnumChatFormatting.GRAY + "seu feiticeiro.");
            } else {
                int i = idx(c);
                l.add(EnumChatFormatting.GREEN + "Atual: " + NOMES[i]);
                l.add(EnumChatFormatting.YELLOW + "EA: " + getEA(p) + "/" + getEAMax(p));
            }
        }
    }

    public static class ItemDomain extends Item {
        public ItemDomain() {
            setUnlocalizedName("jjk_domain");
            setTextureName("minecraft:nether_star");
            setMaxStackSize(1);
            setCreativeTab(null);
        }

        @Override
        public ItemStack onItemRightClick(ItemStack s, World w, EntityPlayer p) {
            if (w.isRemote) return s;
            String c = getChar(p);
            if (c.isEmpty()) {
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Escolha um personagem primeiro."));
                return s;
            }
            if (!hasDomain(p)) {
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Voce nao tem um Dominio."));
                return s;
            }
            NBTTagCompound t = nbt(p);
            int cd = t.getInteger("domCd");
            if (cd > 0) {
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Dominio em cooldown: " + (cd / 20) + "s"));
                return s;
            }
            if (domActive(p)) {
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Dominio ja esta ativo."));
                return s;
            }
            int i = idx(c);
            startDomain(p);
            long until = System.currentTimeMillis() + SEAL_DURATION_MS;
            SEAL_UNTIL.put(p.getCommandSenderName(), until);
            NET.sendToAll(new MsgSeal(p.getCommandSenderName(), until));
            p.addChatMessage(new ChatComponentText(""));
            p.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "\u00A7l\u2726 EXPANSAO DE DOMINIO \u2726"));
            p.addChatMessage(new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + DOM_NAME[i]));
            p.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + DOM_SUB[i]));
            p.addChatMessage(new ChatComponentText(""));
            p.worldObj.playSoundAtEntity(p, "mob.wither.spawn", 1f, 0.6f);
            p.worldObj.playSoundAtEntity(p, "random.explode", 0.6f, 0.4f);
            return s;
        }

        @Override
        public boolean onDroppedByPlayer(ItemStack s, EntityPlayer p) {
            return false;
        }

        @Override
        public void addInformation(ItemStack s, EntityPlayer p, List l, boolean a) {
            String c = getChar(p);
            if (c.isEmpty()) { l.add(EnumChatFormatting.GRAY + "Escolha um personagem primeiro."); return; }
            int i = idx(c);
            l.add(EnumChatFormatting.LIGHT_PURPLE + DOM_NAME[i]);
            NBTTagCompound t = nbt(p);
            int cd = t.getInteger("domCd");
            if (domActive(p)) l.add(EnumChatFormatting.GREEN + "ATIVO");
            else if (cd > 0) l.add(EnumChatFormatting.RED + "Cooldown: " + (cd / 20) + "s");
            else l.add(EnumChatFormatting.YELLOW + "Pronto para usar (clique direito)");
        }
    }

    // ================= PACKETS =================
    public static class MsgSel implements IMessage {
        public int id;
        public MsgSel() {}
        public MsgSel(int i) { id = i; }
        public void fromBytes(ByteBuf b) { id = b.readInt(); }
        public void toBytes(ByteBuf b) { b.writeInt(id); }
        public static class H implements IMessageHandler<MsgSel, IMessage> {
            public IMessage onMessage(MsgSel m, MessageContext c) {
                EntityPlayer p = c.getServerHandler().playerEntity;
                if (m.id < 0 || m.id >= CHARS.length) return null;
                setChar(p, CHARS[m.id]);
                setHasDomain(p, true);
                for (int i = 0; i < p.inventory.mainInventory.length; i++) {
                    ItemStack s = p.inventory.mainInventory[i];
                    if (s != null && s.getItem() == bookItem) { p.inventory.mainInventory[i] = null; break; }
                }
                ensureDomainSlot(p);
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "\u2726 " + NOMES[m.id] + " \u2726"));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Habilidades: " + EnumChatFormatting.WHITE + HABS[m.id][0] + EnumChatFormatting.GRAY + " [R], " + EnumChatFormatting.WHITE + HABS[m.id][1] + EnumChatFormatting.GRAY + " [G], " + EnumChatFormatting.WHITE + HABS[m.id][2] + EnumChatFormatting.GRAY + " [B]"));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "Voce aprendeu: " + EnumChatFormatting.LIGHT_PURPLE + DOM_NAME[m.id]));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "O " + EnumChatFormatting.LIGHT_PURPLE + "Cristal de Dominio" + EnumChatFormatting.GRAY + " foi colocado no seu 1o slot."));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Clique direito no Cristal para abrir sua Expansao de Dominio."));
                return null;
            }
        }
    }

    public static class MsgCast implements IMessage {
        public int slot;
        public MsgCast() {}
        public MsgCast(int s) { slot = s; }
        public void fromBytes(ByteBuf b) { slot = b.readInt(); }
        public void toBytes(ByteBuf b) { b.writeInt(slot); }
        public static class H implements IMessageHandler<MsgCast, IMessage> {
            public IMessage onMessage(MsgCast m, MessageContext c) { cast(c.getServerHandler().playerEntity, m.slot); return null; }
        }
    }

    public static class MsgSeal implements IMessage {
        public String name;
        public long until;
        public MsgSeal() {}
        public MsgSeal(String n, long u) { name = n; until = u; }
        public void fromBytes(ByteBuf b) {
            int len = b.readInt();
            byte[] arr = new byte[len];
            b.readBytes(arr);
            name = new String(arr);
            until = b.readLong();
        }
        public void toBytes(ByteBuf b) {
            byte[] arr = name.getBytes();
            b.writeInt(arr.length);
            b.writeBytes(arr);
            b.writeLong(until);
        }
        public static class H implements IMessageHandler<MsgSeal, IMessage> {
            public IMessage onMessage(MsgSeal m, MessageContext c) {
                SEAL_UNTIL.put(m.name, m.until);
                return null;
            }
        }
    }

    // ================= CAST =================
    public static void cast(EntityPlayer p, int slot) {
        String c = getChar(p);
        if (c.isEmpty()) return;
        if (slot < 0 || slot > 2) return;
        if (domActive(p)) {
            EntityPlayer op = findClashOpponent(p);
            if (op != null) {
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Voce esta em um DOMAIN CLASH. So o dano do dominio importa."));
                return;
            }
        }
        int i = idx(c), custo = CUSTO[i][slot], ea = getEA(p);
        NBTTagCompound t = nbt(p);
        int cd = t.getInteger("cd" + (slot + 1));
        if (cd > 0) { p.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Aguarde " + cd + "s")); return; }
        if (slot == 2 && ea < 75) { p.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "\u26A0 Precisa 75+ EA para " + HABS[i][2])); return; }
        if (ea < custo) { p.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "\u26A0 EA insuficiente (" + ea + "/" + custo + ")")); return; }
        setEA(p, ea - custo);
        t.setInteger("cd" + (slot + 1), slot == 2 ? 5 : 3);
        execute(p, c, slot);
    }

    static void execute(EntityPlayer p, String c, int slot) {
        World w = p.worldObj;
        float baseDmg;
        int color;
        boolean breaks = false;

        if (c.equals("gojo")) {
            if (slot == 0) { baseDmg = 10f; color = 0x4499FF; }
            else if (slot == 1) { baseDmg = 14f; color = 0xFF3322; }
            else { baseDmg = 30f; color = 0xAA00FF; breaks = true; }
        } else if (c.equals("sukuna")) {
            if (slot == 0) { baseDmg = 14f; color = 0xFF2222; }
            else if (slot == 1) { baseDmg = 18f; color = 0xAA0000; }
            else { baseDmg = 35f; color = 0x880000; breaks = true; }
        } else if (c.equals("geto")) {
            if (slot == 0) { baseDmg = 10f; color = 0x8844AA; }
            else if (slot == 1) { baseDmg = 22f; color = 0x4422AA; }
            else { baseDmg = 25f; color = 0xAA22AA; breaks = true; }
        } else {
            if (slot == 0) { baseDmg = 12f; color = 0xFFAACC; }
            else if (slot == 1) { baseDmg = 18f; color = 0xFF66AA; }
            else { baseDmg = 28f; color = 0xFF2299; breaks = true; }
        }

        int ea = getEA(p);
        int mx = getEAMax(p);
        float factor = mx > 0 ? Math.min(1f, (float) ea / mx) : 0f;

        double orbRange = ORB_BASE_RANGE + (ORB_MAX_RANGE - ORB_BASE_RANGE) * factor;
        float dmgMult = ORB_DMG_MIN + (ORB_DMG_MAX - ORB_DMG_MIN) * factor;
        float dmg = baseDmg * dmgMult;

        int count = slot == 2 ? 8 : 5;
        for (int i = 0; i < count; i++) {
            EntityOrb orb = new EntityOrb(w, p, dmg / count, color);
            orb.setPosition(p.posX, p.posY + 1.4, p.posZ);
            orb.maxRange = orbRange;
            orb.breakBlocks = breaks;
            double yaw = Math.toRadians(p.rotationYaw) + (Math.random() - 0.5) * 0.4;
            double pitch = Math.toRadians(-p.rotationPitch) + (Math.random() - 0.5) * 0.2;
            double sp = 1.2;
            orb.motionX = -Math.sin(yaw) * Math.cos(pitch) * sp;
            orb.motionY = Math.sin(pitch) * sp;
            orb.motionZ = Math.cos(yaw) * Math.cos(pitch) * sp;
            w.spawnEntityInWorld(orb);
        }

        p.addChatMessage(new ChatComponentText(
            EnumChatFormatting.LIGHT_PURPLE + "\u00BB " + EnumChatFormatting.WHITE + HABS[idx(c)][slot]
            + EnumChatFormatting.GRAY + " | " + count + " orbes | "
            + EnumChatFormatting.AQUA + ea + " EA"
            + EnumChatFormatting.GRAY + " | alcance "
            + String.format("%.1f", orbRange) + "b"
            + " | dano x" + String.format("%.2f", dmgMult)));
    }

    // ================= ORBE =================
    public static class EntityOrb extends EntityThrowable {
        public int color = 0xAA00FF;
        public float dmg = 10f;
        public double maxRange = ORB_BASE_RANGE;
        public boolean breakBlocks = false;

        private double startX, startY, startZ;
        private boolean init = false;

        public EntityOrb(World w) { super(w); }
        public EntityOrb(World w, EntityLivingBase o, float d, int c) { super(w, o); this.dmg = d; this.color = c; }

        @Override protected void entityInit() { super.entityInit(); }

        @Override
        public void onUpdate() {
            if (!init) {
                init = true;
                startX = posX; startY = posY; startZ = posZ;
            }

            double dx = posX - startX, dy = posY - startY, dz = posZ - startZ;
            double traveled = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (traveled > maxRange) {
                if (worldObj.isRemote) {
                    for (int k = 0; k < 15; k++) {
                        worldObj.spawnParticle("witchMagic", posX, posY, posZ,
                            (Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2);
                    }
                }
                setDead();
                return;
            }

            if (breakBlocks && !worldObj.isRemote) {
                int bx = (int) Math.floor(posX);
                int by = (int) Math.floor(posY);
                int bz = (int) Math.floor(posZ);
                destroyBlock(worldObj, bx, by, bz);
                for (int ox = -1; ox <= 1; ox++)
                    for (int oy = -1; oy <= 1; oy++)
                        for (int oz = -1; oz <= 1; oz++)
                            destroyBlock(worldObj, bx + ox, by + oy, bz + oz);
            }

            super.onUpdate();

            if (ticksExisted > 100) { setDead(); return; }
            if (worldObj.isRemote) worldObj.spawnParticle("witchMagic", posX, posY, posZ, 0, 0, 0);
        }

        static void destroyBlock(World w, int x, int y, int z) {
            if (y < 1 || y > 254) return;
            Block b = w.getBlock(x, y, z);
            if (b == null) return;
            if (b == Blocks.bedrock) return;
            if (b == Blocks.air) return;
            if (b.getMaterial() == Material.air) return;
            if (b.getMaterial() == Material.water) return;
            w.setBlockToAir(x, y, z);
        }

        @Override protected void onImpact(MovingObjectPosition m) {
            if (worldObj.isRemote) return;
            if (m.entityHit instanceof EntityLivingBase) {
                EntityLivingBase t = (EntityLivingBase) m.entityHit;
                if (t != getThrower()) t.attackEntityFrom(DamageSource.magic, dmg);
            }
            List<EntityLivingBase> l = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, boundingBox.expand(3, 3, 3));
            for (EntityLivingBase e2 : l) { if (e2 == getThrower()) continue; e2.attackEntityFrom(DamageSource.magic, dmg * 0.5f); }
            setDead();
        }

        @Override public void writeEntityToNBT(NBTTagCompound t) {
            super.writeEntityToNBT(t);
            t.setDouble("maxRange", maxRange);
            t.setBoolean("breakBlocks", breakBlocks);
        }
        @Override public void readEntityFromNBT(NBTTagCompound t) {
            super.readEntityFromNBT(t);
            maxRange = t.getDouble("maxRange");
            breakBlocks = t.getBoolean("breakBlocks");
        }
    }

    // ================= MALDICAO =================
    public static class EntityCursed extends EntityMob {
        public EntityCursed(World w) {
            super(w);
            setSize(1.2f, 2f);
            initAI();
        }
        private void initAI() {
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0D, false));
            tasks.addTask(3, new EntityAIAttackOnCollide(this, EntityVillager.class, 1.0D, true));
            tasks.addTask(3, new EntityAIAttackOnCollide(this, EntityNPC.class, 1.0D, true));
            tasks.addTask(5, new EntityAIWander(this, 0.8D));
            tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 16F));
            tasks.addTask(7, new EntityAILookIdle(this));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
            targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityVillager.class, 0, true));
            targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityNPC.class, 0, true));
        }
        @Override protected void entityInit() { super.entityInit(); dataWatcher.addObject(20, (byte) 1); }
        public int getGrau() { return dataWatcher.getWatchableObjectByte(20) & 0xFF; }
        public void setGrau(int g) {
            if (g < 1) g = 1;
            if (g > 5) g = 5;
            dataWatcher.updateObject(20, (byte) g);
            float hp = 10f + g * 8f, dmg = 2f + g * 2f, spd = 0.25f + g * 0.02f;
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(hp);
            getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(dmg);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(spd);
            setHealth(hp);
            if (g == 5) setSize(3f, 5f);
            else if (g >= 3) setSize(1.5f, 2.5f);
        }
        @Override protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(18D);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.28D);
            getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(4D);
        }
        @Override public boolean getCanSpawnHere() { return worldObj.difficultySetting.getDifficultyId() > 0 && super.getCanSpawnHere(); }
        @Override protected void dropFewItems(boolean hit, int loot) { int n = getGrau(); for (int i = 0; i < n; i++) dropItem(Items.ender_pearl, 1); }
        @Override public void onDeath(DamageSource s) {
            super.onDeath(s);
            if (s.getEntity() instanceof EntityPlayer) {
                EntityPlayer p = (EntityPlayer) s.getEntity();
                int bonus = getGrau() * 5;
                setEA(p, getEA(p) + bonus);
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "+" + bonus + " EA" + EnumChatFormatting.GRAY + " | Total: " + EnumChatFormatting.AQUA + getEA(p) + "/" + getEAMax(p)));
                if (getGrau() == 5) { p.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_RED + "\u00A7l\u2694 Maldicao SPECIAL derrotada! \u2694")); dropItem(bookItem, 1); }
                else if (getGrau() == 4) p.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "\u2694 Maldicao de Grau 1 derrotada!"));
            }
        }
        @Override public void writeEntityToNBT(NBTTagCompound t) { super.writeEntityToNBT(t); t.setInteger("grau", getGrau()); }
        @Override public void readEntityFromNBT(NBTTagCompound t) { super.readEntityFromNBT(t); setGrau(t.getInteger("grau")); }
    }

    // ================= NPC =================
    public static class EntityNPC extends EntityCreature {
        public EntityNPC(World w) {
            super(w);
            setSize(0.8f, 2f);
            initAI();
        }
        private void initAI() {
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(5, new EntityAIWander(this, 0.7D));
            tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 12F));
            tasks.addTask(7, new EntityAILookIdle(this));
        }
        @Override protected void entityInit() { super.entityInit(); dataWatcher.addObject(20, (byte) 0); }
        public int getKind() { return dataWatcher.getWatchableObjectByte(20) & 0xFF; }
        public void setKind(int k) { dataWatcher.updateObject(20, (byte) k); }
        @Override protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(40D);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.3D);
        }
        @Override public boolean isAIEnabled() { return true; }
        @Override public boolean getCanSpawnHere() { return true; }
        @Override public boolean interactFirst(EntityPlayer p) {
            if (worldObj.isRemote) return true;
            int k = getKind();
            String[] l;
            if (k == 0) l = new String[] { "\u00A7bGojo: \u00A7f\u201CVoce esta em uma epoca interessante.\u201D", "\u00A7bGojo: \u00A7f\u201CTreine, ou vai virar comida de maldicao.\u201D", "\u00A7bGojo: \u00A7f\u201CSukuna e forte, mas eu sou mais.\u201D" };
            else if (k == 1) l = new String[] { "\u00A75Geto: \u00A7f\u201CAs maldicoes sao reflexos dos humanos.\u201D", "\u00A75Geto: \u00A7f\u201CVoce pode me odiar, mas tenho razao.\u201D", "\u00A75Geto: \u00A7f\u201CEncontre o seu caminho.\u201D" };
            else l = new String[] { "\u00A77Yuta: \u00A7f\u201CRika nao e um monstro. Ela me protege.\u201D", "\u00A77Yuta: \u00A7f\u201CEu tambem tenho medo, mas sigo.\u201D", "\u00A77Yuta: \u00A7f\u201CVoce esta pronto para o que vem?\u201D" };
            p.addChatMessage(new ChatComponentText(l[(int) (Math.random() * l.length)]));
            return true;
        }
        @Override public void writeEntityToNBT(NBTTagCompound t) { super.writeEntityToNBT(t); t.setInteger("kind", getKind()); }
        @Override public void readEntityFromNBT(NBTTagCompound t) { super.readEntityFromNBT(t); setKind(t.getInteger("kind")); }
    }

    // ================= BOSSES =================
    public static class EBossSukuna extends EntityMob {
        public EBossSukuna(World w) {
            super(w);
            setSize(1.5f, 3f);
            initAI();
        }
        private void initAI() {
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.2D, false));
            tasks.addTask(5, new EntityAIWander(this, 1D));
            tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 32F));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
        }
        @Override protected void entityInit() { super.entityInit(); }
        @Override protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(150D);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.35D);
            getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(12D);
            getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(1D);
        }
        @Override public boolean isAIEnabled() { return true; }
        @Override public boolean getCanSpawnHere() { return false; }
        @Override public void onDeath(DamageSource s) {
            super.onDeath(s);
            if (s.getEntity() instanceof EntityPlayer) {
                EntityPlayer p = (EntityPlayer) s.getEntity();
                setEA(p, getEAMax(p));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_RED + "\u00A7l\u2694 SUKUNA DERROTADO! \u2694"));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Energia restaurada."));
            }
        }
    }

    public static class EBossSukunaHeian extends EntityMob {
        int ac = 0;
        public EBossSukunaHeian(World w) {
            super(w);
            setSize(3.5f, 6.5f);
            initAI();
        }
        private void initAI() {
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1D, false));
            tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 48F));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
        }
        @Override protected void entityInit() { super.entityInit(); }
        @Override protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(500D);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.30D);
            getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(25D);
            getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(1D);
            getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(48D);
        }
        @Override public boolean isAIEnabled() { return true; }
        @Override public boolean getCanSpawnHere() { return false; }
        @Override public void onUpdate() {
            super.onUpdate();
            if (worldObj.isRemote) return;
            if (ac > 0) ac--;
            EntityPlayer t = worldObj.getClosestVulnerablePlayerToEntity(this, 40D);
            if (t != null && ac <= 0) {
                for (int i = -1; i <= 1; i++) {
                    double dx = t.posX - posX, dy = t.posY + 1 - posY - 1.5, dz = t.posZ - posZ;
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    EntityLargeFireball fb = new EntityLargeFireball(worldObj, this, dx / d + i * 0.2, dy / d, dz / d + i * 0.2);
                    fb.posX = posX; fb.posY = posY + 3; fb.posZ = posZ;
                    worldObj.spawnEntityInWorld(fb);
                }
                ac = 60;
            }
        }
        @Override public void onDeath(DamageSource s) {
            super.onDeath(s);
            if (s.getEntity() instanceof EntityPlayer) {
                EntityPlayer p = (EntityPlayer) s.getEntity();
                setEA(p, getEAMax(p));
                p.addChatMessage(new ChatComponentText(""));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_RED + "\u00A7l\u2694\u2694 SUKUNA HEIAN DERROTADO \u2694\u2694"));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Voce venceu o Rei das Maldicoes!"));
                p.addChatMessage(new ChatComponentText(""));
                for (int i = 0; i < 3; i++) dropItem(bookItem, 1);
            }
        }
    }

    public static class EBossGojo extends EntityMob {
        int ac = 0;
        int phase = 0;
        public EBossGojo(World w) {
            super(w);
            setSize(1f, 2.2f);
            initAI();
        }
        private void initAI() {
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.2D, false));
            tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 64F));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
        }
        @Override protected void entityInit() { super.entityInit(); }
        @Override protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(800D);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.40D);
            getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(30D);
            getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(1D);
            getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(64D);
        }
        @Override public boolean isAIEnabled() { return true; }
        @Override public boolean getCanSpawnHere() { return false; }
        @Override public void onUpdate() {
            super.onUpdate();
            if (worldObj.isRemote) return;
            if (ac > 0) ac--;
            if (ticksExisted % 5 == 0) worldObj.spawnParticle("witchMagic", posX + (Math.random() - 0.5) * 2, posY + 1, posZ + (Math.random() - 0.5) * 2, 0, 0.1, 0);
            EntityPlayer t = worldObj.getClosestVulnerablePlayerToEntity(this, 50D);
            if (t != null && ac <= 0) {
                phase++;
                if (phase % 2 == 0) {
                    double dx = posX - t.posX, dy = posY - t.posY, dz = posZ - t.posZ;
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz) + 0.001;
                    t.motionX += dx / d * 1.5;
                    t.motionY += 0.6;
                    t.motionZ += dz / d * 1.5;
                } else {
                    double dx = t.posX - posX, dy = t.posY - posY + 1, dz = t.posZ - posZ;
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz) + 0.001;
                    t.motionX += dx / d * 1.5;
                    t.motionY += 0.5;
                    t.motionZ += dz / d * 1.5;
                    t.attackEntityFrom(DamageSource.magic, 6f);
                }
                ac = 40;
            }
        }
        @Override public void onDeath(DamageSource s) {
            super.onDeath(s);
            if (s.getEntity() instanceof EntityPlayer) {
                EntityPlayer p = (EntityPlayer) s.getEntity();
                p.addChatMessage(new ChatComponentText(""));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "\u00A7l\u2726 GOJO 500% DERROTADO \u2726"));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Voce derrotou o feiticeiro mais forte."));
                p.addChatMessage(new ChatComponentText(""));
                for (int i = 0; i < 5; i++) dropItem(bookItem, 1);
            }
        }
    }

    // ================= WORLDGEN =================
    public static class WorldGen implements IWorldGenerator {
        static final Random R = new Random();
        @Override
        public void generate(Random r, int cx, int cz, World w, IChunkProvider cp, IChunkProvider cpp) {
            if (w.provider.dimensionId != 0) return;
            int bx = cx * 16, bz = cz * 16;
            if (r.nextInt(300) == 0) school(w, bx + 8, bz + 8);
            if (r.nextInt(800) == 0) city(w, bx + 8, bz + 8);
            if (r.nextInt(200) == 0) cage(w, bx + 8, bz + 8);
            if (r.nextInt(400) == 0) shrine(w, bx + 8, bz + 8);
            if (r.nextInt(250) == 0) grave(w, bx + 8, bz + 8);
            if (r.nextInt(150) == 0) spawnNPC(w, bx + 8, bz + 8);
            if (r.nextInt(33) == 0) boss(w, bx + 8, bz + 8, 0);
            if (r.nextInt(200) == 0) boss(w, bx + 8, bz + 8, 1);
            if (r.nextInt(333) == 0) boss(w, bx + 8, bz + 8, 2);
        }
        static void school(World w, int x, int z) {
            int gy = g(w, x, z);
            if (gy < 60 || gy > 100) return;
            for (int dx = -15; dx <= 15; dx++)
                for (int dz = -15; dz <= 15; dz++) {
                    w.setBlock(x + dx, gy, z + dz, Blocks.stonebrick);
                    w.setBlock(x + dx, gy - 1, z + dz, Blocks.stonebrick);
                }
            for (int dx = -15; dx <= 15; dx++)
                for (int h = 1; h <= 8; h++) {
                    w.setBlock(x + dx, gy + h, z - 15, Blocks.stonebrick);
                    w.setBlock(x + dx, gy + h, z + 15, Blocks.stonebrick);
                }
            for (int dz = -15; dz <= 15; dz++)
                for (int h = 1; h <= 8; h++) {
                    w.setBlock(x - 15, gy + h, z + dz, Blocks.stonebrick);
                    w.setBlock(x + 15, gy + h, z + dz, Blocks.stonebrick);
                }
            for (int dx = -15; dx <= 15; dx++)
                for (int dz = -15; dz <= 15; dz++) w.setBlock(x + dx, gy + 9, z + dz, Blocks.stone_slab);
            for (int h = 1; h <= 4; h++) {
                w.setBlock(x, gy + h, z - 15, Blocks.air);
                w.setBlock(x + 1, gy + h, z - 15, Blocks.air);
            }
            w.setBlock(x, gy + 5, z - 15, Blocks.torch);
            w.setBlock(x + 1, gy + 5, z - 15, Blocks.torch);
            for (int dz = -15; dz <= 15; dz++)
                for (int h = 1; h <= 8; h++) w.setBlock(x, gy + h, z + dz, Blocks.stonebrick);
            for (int dx = -15; dx <= 15; dx++)
                for (int h = 1; h <= 8; h++) w.setBlock(x + dx, gy + h, z, Blocks.stonebrick);
            for (int h = 1; h <= 3; h++) {
                w.setBlock(x, gy + h, z - 8, Blocks.air);
                w.setBlock(x - 8, gy + h, z, Blocks.air);
                w.setBlock(x + 8, gy + h, z, Blocks.air);
                w.setBlock(x, gy + h, z + 8, Blocks.air);
            }
            w.setBlock(x - 8, gy + 8, z - 8, Blocks.glowstone);
            w.setBlock(x + 8, gy + 8, z - 8, Blocks.glowstone);
            w.setBlock(x - 8, gy + 8, z + 8, Blocks.glowstone);
            w.setBlock(x + 8, gy + 8, z + 8, Blocks.glowstone);
            for (int i = 0; i < 6; i++) {
                EntityCursed c = new EntityCursed(w);
                c.setGrau(1 + R.nextInt(3));
                c.setLocationAndAngles(x + R.nextInt(20) - 10, gy + 1, z + R.nextInt(20) - 10, 0, 0);
                w.spawnEntityInWorld(c);
            }
        }
        static void city(World w, int x, int z) {
            int gy = g(w, x, z);
            if (gy < 60 || gy > 90) return;
            for (int dx = -250; dx <= 250; dx++)
                for (int dz = -250; dz <= 250; dz++) {
                    if (dx * dx + dz * dz > 62500) continue;
                    w.setBlock(x + dx, gy, z + dz, Blocks.stone_slab);
                }
            for (int i = 0; i < 12; i++) {
                int px = x + R.nextInt(400) - 200, pz = z + R.nextInt(400) - 200;
                int pg = g(w, px, pz);
                if (pg < 60 || pg > 90) continue;
                bld(w, px, pg + 1, pz, 8 + R.nextInt(8), 15 + R.nextInt(25));
            }
            for (int dx = -250; dx <= 250; dx++)
                for (int dz = -3; dz <= 3; dz++) {
                    w.setBlock(x + dx, gy, z + dz, Blocks.stonebrick);
                    w.setBlock(x + dx, gy + 1, z + dz, Blocks.air);
                }
            for (int dz = -250; dz <= 250; dz++)
                for (int dx = -3; dx <= 3; dx++) {
                    w.setBlock(x + dx, gy, z + dz, Blocks.stonebrick);
                    w.setBlock(x + dx, gy + 1, z + dz, Blocks.air);
                }
            for (int i = 0; i < 20; i++) {
                int px = x + R.nextInt(480) - 240, pz = z + R.nextInt(480) - 240;
                int pg = g(w, px, pz);
                if (pg < 60 || pg > 90) continue;
                w.setBlock(px, pg + 1, pz, Blocks.fence);
                w.setBlock(px, pg + 2, pz, Blocks.fence);
                w.setBlock(px, pg + 3, pz, Blocks.fence);
                w.setBlock(px, pg + 4, pz, Blocks.glowstone);
            }
            for (int i = 0; i < 25; i++) {
                int px = x + R.nextInt(400) - 200, pz = z + R.nextInt(400) - 200;
                int pg = g(w, px, pz);
                if (pg < 60 || pg > 90) continue;
                EntityCursed c = new EntityCursed(w);
                c.setGrau(1 + R.nextInt(5));
                c.setLocationAndAngles(px, pg + 1, pz, 0, 0);
                w.spawnEntityInWorld(c);
            }
        }
        static void bld(World w, int x, int y, int z, int sz, int h) {
            for (int hh = 0; hh < h; hh++)
                for (int dx = 0; dx < sz; dx++)
                    for (int dz = 0; dz < sz; dz++) {
                        boolean e = dx == 0 || dx == sz - 1 || dz == 0 || dz == sz - 1;
                        if (e) w.setBlock(x + dx, y + hh, z + dz, Blocks.stonebrick);
                    }
            for (int hh = 3; hh < h - 1; hh += 4) {
                for (int dx = 2; dx < sz - 2; dx += 2) {
                    w.setBlock(x + dx, y + hh, z, Blocks.glass);
                    w.setBlock(x + dx, y + hh, z + sz - 1, Blocks.glass);
                }
                for (int dz = 2; dz < sz - 2; dz += 2) {
                    w.setBlock(x, y + hh, z + dz, Blocks.glass);
                    w.setBlock(x + sz - 1, y + hh, z + dz, Blocks.glass);
                }
            }
            w.setBlock(x + sz / 2, y + h - 1, z + sz / 2, Blocks.glowstone);
        }
        static void cage(World w, int x, int z) {
            int gy = g(w, x, z);
            if (gy < 60 || gy > 100) return;
            for (int dx = -4; dx <= 4; dx++)
                for (int dy = 0; dy <= 8; dy++)
                    for (int dz = -4; dz <= 4; dz++) {
                        boolean wall = Math.abs(dx) == 4 || Math.abs(dz) == 4 || dy == 0 || dy == 8;
                        if (wall) w.setBlock(x + dx, gy + dy, z + dz, Blocks.obsidian);
                        else w.setBlock(x + dx, gy + dy, z + dz, Blocks.air);
                    }
            w.setBlock(x, gy + 8, z, Blocks.air);
            w.setBlock(x, gy + 8, z + 1, Blocks.air);
            w.setBlock(x, gy + 1, z, Blocks.glowstone);
            for (int i = 0; i < 8; i++) {
                EntityCursed c = new EntityCursed(w);
                c.setGrau(2 + R.nextInt(3));
                c.setLocationAndAngles(x + R.nextInt(7) - 3, gy + 1, z + R.nextInt(7) - 3, 0, 0);
                w.spawnEntityInWorld(c);
            }
        }
        static void shrine(World w, int x, int z) {
            int gy = g(w, x, z);
            if (gy < 60 || gy > 100) return;
            for (int dx = -8; dx <= 8; dx++)
                for (int dz = -8; dz <= 8; dz++) w.setBlock(x + dx, gy, z + dz, Blocks.stone_slab);
            int[][] cs = { { -7, -7 }, { 7, -7 }, { -7, 7 }, { 7, 7 } };
            for (int[] c : cs) {
                for (int h = 1; h <= 10; h++) w.setBlock(x + c[0], gy + h, z + c[1], Blocks.nether_brick);
                w.setBlock(x + c[0], gy + 11, z + c[1], Blocks.glowstone);
            }
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) w.setBlock(x + dx, gy + 1, z + dz, Blocks.obsidian);
            w.setBlock(x, gy + 2, z, Blocks.beacon);
            for (int i = 0; i < 4; i++) {
                EntityCursed c = new EntityCursed(w);
                c.setGrau(3 + R.nextInt(3));
                c.setLocationAndAngles(x + R.nextInt(10) - 5, gy + 1, z + R.nextInt(10) - 5, 0, 0);
                w.spawnEntityInWorld(c);
            }
        }
        static void grave(World w, int x, int z) {
            int gy = g(w, x, z);
            if (gy < 60 || gy > 100) return;
            for (int dx = -20; dx <= 20; dx++)
                for (int dz = -20; dz <= 20; dz++) w.setBlock(x + dx, gy, z + dz, Blocks.grass);
            for (int i = 0; i < 30; i++) {
                int px = x + R.nextInt(36) - 18, pz = z + R.nextInt(36) - 18;
                w.setBlock(px, gy + 1, pz, Blocks.stone_slab);
                w.setBlock(px, gy + 2, pz, Blocks.stone_slab);
                w.setBlock(px, gy + 3, pz, Blocks.stone_slab);
            }
            for (int dx = -20; dx <= 20; dx++) {
                w.setBlock(x + dx, gy + 1, z - 20, Blocks.fence);
                w.setBlock(x + dx, gy + 1, z + 20, Blocks.fence);
            }
            for (int dz = -20; dz <= 20; dz++) {
                w.setBlock(x - 20, gy + 1, z + dz, Blocks.fence);
                w.setBlock(x + 20, gy + 1, z + dz, Blocks.fence);
            }
            for (int i = 0; i < 10; i++) {
                int px = x + R.nextInt(36) - 18, pz = z + R.nextInt(36) - 18;
                int pg = g(w, px, pz);
                if (pg < 60 || pg > 100) continue;
                EntityCursed c = new EntityCursed(w);
                c.setGrau(1 + R.nextInt(4));
                c.setLocationAndAngles(px, pg + 1, pz, 0, 0);
                w.spawnEntityInWorld(c);
            }
        }
        static void spawnNPC(World w, int x, int z) {
            int gy = g(w, x, z);
            if (gy < 60 || gy > 100) return;
            EntityNPC n = new EntityNPC(w);
            n.setKind(R.nextInt(3));
            n.setLocationAndAngles(x, gy + 1, z, R.nextInt(360), 0);
            w.spawnEntityInWorld(n);
        }
        static void boss(World w, int x, int z, int t) {
            int gy = g(w, x, z);
            if (gy < 60 || gy > 120) return;
            if (t == 0) { EBossSukuna b = new EBossSukuna(w); b.setLocationAndAngles(x, gy + 1, z, 0, 0); w.spawnEntityInWorld(b); }
            else if (t == 1) { EBossSukunaHeian b = new EBossSukunaHeian(w); b.setLocationAndAngles(x, gy + 1, z, 0, 0); w.spawnEntityInWorld(b); }
            else { EBossGojo b = new EBossGojo(w); b.setLocationAndAngles(x, gy + 1, z, 0, 0); w.spawnEntityInWorld(b); }
        }
        static int g(World w, int x, int z) {
            for (int y = 120; y > 40; y--) {
                if (!w.isAirBlock(x, y, z) && !w.getBlock(x, y, z).getMaterial().isLiquid()) return y;
            }
            return 64;
        }
    }

    // ================= EVENTOS SERVIDOR =================
    public static class SEv {
        @SubscribeEvent
        public void onTick(TickEvent.PlayerTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            if (e.player.worldObj.isRemote) return;
            EntityPlayer p = e.player;
            NBTTagCompound t = nbt(p);

            if (!t.getBoolean("gave_book")) {
                t.setBoolean("gave_book", true);
                if (!p.inventory.addItemStackToInventory(new ItemStack(bookItem))) p.dropPlayerItemWithRandomChoice(new ItemStack(bookItem), false);
                p.addChatMessage(new ChatComponentText(""));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "\u00A7l\u2726 Jujutsu Kaisen \u2726"));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Voce recebeu um " + EnumChatFormatting.WHITE + "Livro de Feiticeiro"));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Clique com botao direito nele para escolher seu personagem."));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Depois use R, G, B para suas habilidades."));
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Digite " + EnumChatFormatting.WHITE + "/jjk" + EnumChatFormatting.GRAY + " a qualquer momento para ver a ajuda."));
                p.addChatMessage(new ChatComponentText(""));
            }

            if (getChar(p).isEmpty()) return;

            ensureDomainSlot(p);

            if (p.ticksExisted % 40 == 0) {
                int ea = getEA(p), mx = getEAMax(p);
                if (ea < mx) setEA(p, ea + 1);
            }
            for (int s = 1; s <= 3; s++) {
                int cd = t.getInteger("cd" + s);
                if (cd > 0) t.setInteger("cd" + s, cd - 1);
            }
            int dcd = t.getInteger("domCd");
            if (dcd > 0) t.setInteger("domCd", dcd - 1);
            if (t.getBoolean("domOn") && System.currentTimeMillis() > t.getLong("domUntil")) {
                endDomain(p, false);
                p.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Sua Expansao de Dominio terminou."));
            }

            if (p.ticksExisted % 60 == 0) {
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<String, Long>> it = SEAL_UNTIL.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Long> en = it.next();
                    if (en.getValue() == null || now > en.getValue()) it.remove();
                }
            }
        }

        @SubscribeEvent
        public void onDom(TickEvent.ServerTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            MinecraftServer srv = MinecraftServer.getServer();
            if (srv == null) return;

            for (WorldServer ws : srv.worldServers) {
                if (ws == null) continue;
                List pl = ws.playerEntities;
                if (pl == null || pl.isEmpty()) continue;
                for (int i = 0; i < pl.size(); i++) {
                    EntityPlayer p = (EntityPlayer) pl.get(i);
                    if (!domActive(p)) continue;
                    List<EntityLivingBase> l = ws.getEntitiesWithinAABB(EntityLivingBase.class, p.boundingBox.expand(12, 8, 12));
                    for (EntityLivingBase t : l) {
                        if (t == p) continue;
                        if (t instanceof EntityPlayer && domActive((EntityPlayer) t)) continue;
                        t.attackEntityFrom(DamageSource.magic, 2f);
                        NBTTagCompound pt = nbt(p);
                        pt.setInteger("domDmg", pt.getInteger("domDmg") + 2);
                    }
                }
            }

            DOM_DUELS.clear();
            for (WorldServer ws : srv.worldServers) {
                if (ws == null) continue;
                List pl = ws.playerEntities;
                if (pl == null || pl.size() < 2) continue;
                for (int i = 0; i < pl.size(); i++) {
                    EntityPlayer a = (EntityPlayer) pl.get(i);
                    if (!domActive(a)) continue;
                    for (int j = i + 1; j < pl.size(); j++) {
                        EntityPlayer b = (EntityPlayer) pl.get(j);
                        if (!domActive(b)) continue;
                        double dx = a.posX - b.posX, dz = a.posZ - b.posZ;
                        if (dx * dx + dz * dz > DOM_CLASH_RADIUS * DOM_CLASH_RADIUS) continue;
                        resolveClash(a, b);
                    }
                }
            }
        }

        void resolveClash(EntityPlayer a, EntityPlayer b) {
            NBTTagCompound ta = nbt(a);
            NBTTagCompound tb = nbt(b);

            if (a.ticksExisted % 20 == 0) {
                b.attackEntityFrom(DamageSource.magic, 3f);
                a.attackEntityFrom(DamageSource.magic, 3f);
                ta.setInteger("domDmg", ta.getInteger("domDmg") + 3);
                tb.setInteger("domDmg", tb.getInteger("domDmg") + 3);
                ta.setInteger("domTaken", ta.getInteger("domTaken") + 3);
                tb.setInteger("domTaken", tb.getInteger("domTaken") + 3);
            }

            float scoreA = (ta.getInteger("domDmg") + 1) / (float) (ta.getInteger("domTaken") + 1);
            float scoreB = (tb.getInteger("domDmg") + 1) / (float) (tb.getInteger("domTaken") + 1);

            if (scoreA > scoreB * 2.0f) {
                endDomain(b, true);
                b.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "\u00A7l\u2716 Seu DOMINIO foi esmagado pelo de " + a.getCommandSenderName()));
                a.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "\u00A7l\u2726 Sua EXPANSAO sobrescreveu a de " + b.getCommandSenderName()));
            } else if (scoreB > scoreA * 2.0f) {
                endDomain(a, true);
                a.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "\u00A7l\u2716 Seu DOMINIO foi esmagado pelo de " + b.getCommandSenderName()));
                b.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "\u00A7l\u2726 Sua EXPANSAO sobrescreveu a de " + a.getCommandSenderName()));
            }
        }
    }

    // ================= COMANDO =================
    public static class CmdAjuda extends CommandBase {
        @Override public String getCommandName() { return "jjk"; }
        @Override public String getCommandUsage(ICommandSender s) { return "/jjk"; }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }
        @Override
        public void processCommand(ICommandSender s, String[] a) {
            s.addChatMessage(new ChatComponentText(""));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "\u00A7l\u2726 Jujutsu Kaisen \u2726"));
            s.addChatMessage(new ChatComponentText(""));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "1. " + EnumChatFormatting.WHITE + "Voce recebe um " + EnumChatFormatting.GOLD + "Livro de Feiticeiro" + EnumChatFormatting.WHITE + " no inventario."));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "2. " + EnumChatFormatting.WHITE + "Clique com " + EnumChatFormatting.AQUA + "botao direito" + EnumChatFormatting.WHITE + " nele para escolher seu personagem."));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "3. " + EnumChatFormatting.WHITE + "Voce ganha o " + EnumChatFormatting.LIGHT_PURPLE + "Cristal de Dominio" + EnumChatFormatting.WHITE + " no 1o slot (nao pode ser dropado)."));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "4. " + EnumChatFormatting.WHITE + "Use " + EnumChatFormatting.AQUA + "R, G, B" + EnumChatFormatting.WHITE + " para as 3 habilidades."));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "5. " + EnumChatFormatting.WHITE + "Use o " + EnumChatFormatting.LIGHT_PURPLE + "Cristal" + EnumChatFormatting.WHITE + " com botao direito para abrir sua " + EnumChatFormatting.DARK_PURPLE + "Expansao de Dominio" + EnumChatFormatting.WHITE + "."));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "6. " + EnumChatFormatting.WHITE + "Quanto mais EA, mais longe e mais forte o Roxo fica."));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "7. " + EnumChatFormatting.WHITE + "Cace " + EnumChatFormatting.RED + "maldicoes" + EnumChatFormatting.WHITE + " para ganhar EA."));
            s.addChatMessage(new ChatComponentText(""));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "DOMAIN CLASH: se dois dominios se encontram, quem causar MAIS dano vence."));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "O perdedor perde o dominio e ganha o DOBRO de cooldown."));
            s.addChatMessage(new ChatComponentText(""));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Estruturas: " + EnumChatFormatting.WHITE + "Escola Jujutsu, Cidade, Cativeiro, Santuario, Cemiterio."));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Bosses: " + EnumChatFormatting.RED + "Sukuna" + EnumChatFormatting.GRAY + ", " + EnumChatFormatting.DARK_RED + "Sukuna Heian" + EnumChatFormatting.GRAY + ", " + EnumChatFormatting.AQUA + "Gojo 500%"));
            s.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Craft do livro: " + EnumChatFormatting.WHITE + "1 Livro + 1 Tinta + 2 Papel"));
            s.addChatMessage(new ChatComponentText(""));
        }
    }
}
