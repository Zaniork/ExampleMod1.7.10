package com.myname.mymodid;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

@Mod(modid = PcMod.MODID, name = "PC Mod", version = "1.0", acceptedMinecraftVersions = "[1.7.10]")
public class PcMod {

    public static final String MODID = "pcmod";

    // ---------- 5 variedades de cada coisa ----------
    static final String[] VARIANT = {"Classico Preto", "Slim Branco", "Gamer Vermelho", "Compacto Azul", "Retro Verde"};
    static final float[][] COLORS = {
        {0.20f, 0.20f, 0.23f}, {0.93f, 0.93f, 0.93f}, {0.86f, 0.12f, 0.12f}, {0.15f, 0.36f, 0.90f}, {0.12f, 0.68f, 0.26f}};
    static final int[] DYE = {0, 15, 1, 4, 2};
    static final double[] BEZEL = {0.05, 0.03, 0.05, 0.04, 0.07};
    static final int[] RAM_STICKS = {1, 2, 2, 3, 4};
    static final int[] GPU_FANS = {1, 2, 2, 3, 3};
    static final String[] PART_NAMES = {"Processador", "Placa de video", "Memoria RAM"};
    static final int[] DX = {1, -1, 0, 0};
    static final int[] DZ = {0, 0, 1, -1};
    static final int FULL = 15728880;

    static int cv(int d) {
        return d < 0 ? 0 : (d > 4 ? 4 : d);
    }

    public static Block caseBlock;
    public static Block monitorBlock;
    public static Item cpuItem;
    public static Item gpuItem;
    public static Item ramItem;

    public static final CreativeTabs TAB = new CreativeTabs("pcmod") {
        @Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(caseBlock);
        }
    };

    // ================= CICLO DE VIDA =================
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        caseBlock = new BlockPcCase();
        monitorBlock = new BlockMonitor();
        GameRegistry.registerBlock(caseBlock, ItemBlockVariant.class, "pcmod_case");
        GameRegistry.registerBlock(monitorBlock, ItemBlockVariant.class, "pcmod_monitor");
        cpuItem = new ItemPcPart(0);
        gpuItem = new ItemPcPart(1);
        ramItem = new ItemPcPart(2);
        GameRegistry.registerItem(cpuItem, "pcmod_cpu");
        GameRegistry.registerItem(gpuItem, "pcmod_gpu");
        GameRegistry.registerItem(ramItem, "pcmod_ram");
        GameRegistry.registerTileEntity(TileEntityPcCase.class, "pcmod_case");
        GameRegistry.registerTileEntity(TileEntityMonitor.class, "pcmod_monitor");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        LanguageRegistry lang = LanguageRegistry.instance();
        lang.addStringLocalization("itemGroup.pcmod", "PC Mod");
        for (int v = 0; v < 5; v++) {
            lang.addStringLocalization("tile.pcmod_case." + v + ".name", "Gabinete " + VARIANT[v]);
            lang.addStringLocalization("tile.pcmod_monitor." + v + ".name", "Monitor " + VARIANT[v]);
            lang.addStringLocalization("item.pcmod_cpu." + v + ".name", "Processador " + VARIANT[v]);
            lang.addStringLocalization("item.pcmod_gpu." + v + ".name", "Placa de Video " + VARIANT[v]);
            lang.addStringLocalization("item.pcmod_ram." + v + ".name", "Memoria RAM " + VARIANT[v]);
        }
        addRecipes();
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientSide.init();
        }
    }

    static void addRecipes() {
        GameRegistry.addRecipe(new ItemStack(caseBlock, 1, 0), "III", "GRG", "III",
            'I', Items.iron_ingot, 'G', Blocks.glass_pane, 'R', Items.redstone);
        GameRegistry.addRecipe(new ItemStack(monitorBlock, 1, 0), "III", "GGG", "IRI",
            'I', Items.iron_ingot, 'G', Blocks.glass_pane, 'R', Items.redstone);
        GameRegistry.addRecipe(new ItemStack(cpuItem, 1, 0), "RIR", "IGI", "RIR",
            'R', Items.redstone, 'I', Items.iron_ingot, 'G', Items.gold_ingot);
        GameRegistry.addRecipe(new ItemStack(gpuItem, 1, 0), "III", "RDR", "GGG",
            'I', Items.iron_ingot, 'R', Items.redstone, 'D', Items.diamond, 'G', Items.gold_ingot);
        GameRegistry.addRecipe(new ItemStack(ramItem, 1, 0), "RRR", "GGG",
            'R', Items.redstone, 'G', Items.gold_ingot);

        // trocar a cor/modelo: peca (qualquer cor) + tinta
        for (int v = 0; v < 5; v++) {
            ItemStack dye = new ItemStack(Items.dye, 1, DYE[v]);
            GameRegistry.addShapelessRecipe(new ItemStack(caseBlock, 1, v), new ItemStack(caseBlock, 1, 32767), dye);
            GameRegistry.addShapelessRecipe(new ItemStack(monitorBlock, 1, v), new ItemStack(monitorBlock, 1, 32767), dye);
            GameRegistry.addShapelessRecipe(new ItemStack(cpuItem, 1, v), new ItemStack(cpuItem, 1, 32767), dye);
            GameRegistry.addShapelessRecipe(new ItemStack(gpuItem, 1, v), new ItemStack(gpuItem, 1, 32767), dye);
            GameRegistry.addShapelessRecipe(new ItemStack(ramItem, 1, v), new ItemStack(ramItem, 1, 32767), dye);
        }
    }

    // ================= utilidades =================
    static void msg(EntityPlayer p, String s) {
        p.addChatMessage(new ChatComponentText(s));
    }

    static void sync(World w, int x, int y, int z, TileEntity t) {
        t.markDirty();
        w.markBlockForUpdate(x, y, z);
    }

    static void dropItem(World w, int x, int y, int z, ItemStack st) {
        EntityItem ei = new EntityItem(w, x + 0.5, y + 0.5, z + 0.5, st);
        w.spawnEntityInWorld(ei);
    }

    static void giveBack(EntityPlayer p, World w, int x, int y, int z, ItemStack st) {
        if (!p.inventory.addItemStackToInventory(st)) dropItem(w, x, y, z, st);
    }

    static Item partItem(int kind) {
        return kind == 0 ? cpuItem : (kind == 1 ? gpuItem : ramItem);
    }

    // ================= BLOCOS =================
    public static class BlockPcCase extends BlockContainer {
        public BlockPcCase() {
            super(Material.iron);
            setBlockName("pcmod_case");
            setHardness(2.0F);
            setResistance(10.0F);
            setStepSound(soundTypeMetal);
            setCreativeTab(TAB);
            setBlockBounds(0.05F, 0.0F, 0.05F, 0.95F, 0.95F, 0.95F);
        }

        @Override
        public TileEntity createNewTileEntity(World w, int meta) {
            return new TileEntityPcCase();
        }

        @Override
        public boolean isOpaqueCube() {
            return false;
        }

        @Override
        public boolean renderAsNormalBlock() {
            return false;
        }

        @Override
        public int getRenderType() {
            return -1;
        }

        @Override
        public int damageDropped(int meta) {
            return meta;
        }

        @Override
        public void registerBlockIcons(IIconRegister reg) {}

        @Override
        public IIcon getIcon(int side, int meta) {
            return Blocks.iron_block.getIcon(side, 0);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void getSubBlocks(Item item, CreativeTabs tab, List list) {
            for (int v = 0; v < 5; v++) list.add(new ItemStack(item, 1, v));
        }

        @Override
        public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase p, ItemStack s) {
            int f = MathHelper.floor_double((double) (p.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
            TileEntity t = w.getTileEntity(x, y, z);
            if (t instanceof TileEntityPcCase) {
                ((TileEntityPcCase) t).facing = f;
                sync(w, x, y, z, t);
            }
        }

        @Override
        public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy, float hz) {
            if (w.isRemote) return true;
            TileEntity t = w.getTileEntity(x, y, z);
            if (!(t instanceof TileEntityPcCase)) return true;
            TileEntityPcCase c = (TileEntityPcCase) t;
            ItemStack held = p.getCurrentEquippedItem();

            if (held != null && held.getItem() instanceof ItemPcPart) {
                int kind = ((ItemPcPart) held.getItem()).kind;
                if (c.getPart(kind) >= 0) {
                    msg(p, PART_NAMES[kind] + " ja instalado(a).");
                } else {
                    c.setPart(kind, cv(held.getItemDamage()));
                    if (!p.capabilities.isCreativeMode) {
                        held.stackSize--;
                        if (held.stackSize <= 0) p.inventory.setInventorySlotContents(p.inventory.currentItem, null);
                    }
                    sync(w, x, y, z, c);
                    msg(p, PART_NAMES[kind] + " instalado(a).");
                }
                return true;
            }

            if (p.isSneaking() && held == null) {
                int kind = c.gpu >= 0 ? 1 : (c.ram >= 0 ? 2 : (c.cpu >= 0 ? 0 : -1));
                if (kind < 0) {
                    msg(p, "O gabinete esta vazio.");
                } else {
                    giveBack(p, w, x, y, z, new ItemStack(partItem(kind), 1, c.getPart(kind)));
                    c.setPart(kind, -1);
                    sync(w, x, y, z, c);
                    msg(p, PART_NAMES[kind] + " removido(a).");
                }
                return true;
            }

            msg(p, "Gabinete: Processador " + (c.cpu >= 0 ? "OK" : "falta")
                + " | Placa de video " + (c.gpu >= 0 ? "OK" : "falta")
                + " | RAM " + (c.ram >= 0 ? "OK" : "falta")
                + (c.complete() ? " | Coloque um monitor ao lado!" : ""));
            return true;
        }

        @Override
        public void breakBlock(World w, int x, int y, int z, Block b, int meta) {
            TileEntity t = w.getTileEntity(x, y, z);
            if (t instanceof TileEntityPcCase && !w.isRemote) {
                TileEntityPcCase c = (TileEntityPcCase) t;
                for (int k = 0; k < 3; k++) {
                    if (c.getPart(k) >= 0) dropItem(w, x, y, z, new ItemStack(partItem(k), 1, c.getPart(k)));
                }
            }
            super.breakBlock(w, x, y, z, b, meta);
        }
    }

    public static class BlockMonitor extends BlockContainer {
        public BlockMonitor() {
            super(Material.iron);
            setBlockName("pcmod_monitor");
            setHardness(1.5F);
            setResistance(8.0F);
            setStepSound(soundTypeMetal);
            setCreativeTab(TAB);
            setBlockBounds(0.02F, 0.0F, 0.02F, 0.98F, 0.95F, 0.98F);
        }

        @Override
        public TileEntity createNewTileEntity(World w, int meta) {
            return new TileEntityMonitor();
        }

        @Override
        public boolean isOpaqueCube() {
            return false;
        }

        @Override
        public boolean renderAsNormalBlock() {
            return false;
        }

        @Override
        public int getRenderType() {
            return -1;
        }

        @Override
        public int damageDropped(int meta) {
            return meta;
        }

        @Override
        public void registerBlockIcons(IIconRegister reg) {}

        @Override
        public IIcon getIcon(int side, int meta) {
            return Blocks.iron_block.getIcon(side, 0);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void getSubBlocks(Item item, CreativeTabs tab, List list) {
            for (int v = 0; v < 5; v++) list.add(new ItemStack(item, 1, v));
        }

        @Override
        public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase p, ItemStack s) {
            int f = MathHelper.floor_double((double) (p.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
            TileEntity t = w.getTileEntity(x, y, z);
            if (t instanceof TileEntityMonitor) {
                ((TileEntityMonitor) t).facing = f;
                sync(w, x, y, z, t);
            }
        }

        @Override
        public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy, float hz) {
            TileEntity t = w.getTileEntity(x, y, z);
            if (t instanceof TileEntityMonitor && w.isRemote) {
                if (((TileEntityMonitor) t).powered()) {
                    ClientSide.openMonitor(x, y, z);
                } else {
                    msg(p, "PC desligado: coloque o monitor ao lado de um gabinete com processador, placa de video e RAM.");
                }
            }
            return true;
        }
    }

    public static class ItemBlockVariant extends ItemBlock {
        public ItemBlockVariant(Block b) {
            super(b);
            setHasSubtypes(true);
            setMaxDamage(0);
        }

        @Override
        public int getMetadata(int damage) {
            return cv(damage);
        }

        @Override
        public String getUnlocalizedName(ItemStack s) {
            return getUnlocalizedName() + "." + cv(s.getItemDamage());
        }
    }

    // ================= PECAS (itens) =================
    public static class ItemPcPart extends Item {
        public final int kind; // 0 processador, 1 placa de video, 2 RAM

        public ItemPcPart(int kind) {
            this.kind = kind;
            setHasSubtypes(true);
            setMaxDamage(0);
            setMaxStackSize(16);
            setCreativeTab(TAB);
            setUnlocalizedName(kind == 0 ? "pcmod_cpu" : (kind == 1 ? "pcmod_gpu" : "pcmod_ram"));
        }

        @Override
        public String getUnlocalizedName(ItemStack s) {
            return super.getUnlocalizedName() + "." + cv(s.getItemDamage());
        }

        @Override
        public void registerIcons(IIconRegister r) {}

        @Override
        public IIcon getIconFromDamage(int d) {
            return Items.iron_ingot.getIconFromDamage(0);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void getSubItems(Item i, CreativeTabs t, List l) {
            for (int v = 0; v < 5; v++) l.add(new ItemStack(i, 1, v));
        }
    }

    // ================= TILE ENTITIES =================
    public static class TileEntityPcCase extends TileEntity {
        public int facing = 0;
        public int cpu = -1;
        public int gpu = -1;
        public int ram = -1;

        public int getPart(int k) {
            return k == 0 ? cpu : (k == 1 ? gpu : ram);
        }

        public void setPart(int k, int v) {
            if (k == 0) cpu = v;
            else if (k == 1) gpu = v;
            else ram = v;
        }

        public boolean complete() {
            return cpu >= 0 && gpu >= 0 && ram >= 0;
        }

        public boolean powered() {
            if (!complete() || worldObj == null) return false;
            for (int i = 0; i < 4; i++) {
                TileEntity t = worldObj.getTileEntity(xCoord + DX[i], yCoord, zCoord + DZ[i]);
                if (t instanceof TileEntityMonitor) return true;
            }
            return false;
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            tag.setInteger("facing", facing);
            tag.setInteger("cpu", cpu);
            tag.setInteger("gpu", gpu);
            tag.setInteger("ram", ram);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            facing = tag.getInteger("facing");
            cpu = tag.hasKey("cpu") ? tag.getInteger("cpu") : -1;
            gpu = tag.hasKey("gpu") ? tag.getInteger("gpu") : -1;
            ram = tag.hasKey("ram") ? tag.getInteger("ram") : -1;
        }

        @Override
        public Packet getDescriptionPacket() {
            NBTTagCompound tag = new NBTTagCompound();
            writeToNBT(tag);
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
        }

        @Override
        public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
            readFromNBT(pkt.func_148857_g());
        }
    }

    public static class TileEntityMonitor extends TileEntity {
        public int facing = 0;

        public boolean powered() {
            if (worldObj == null) return false;
            for (int i = 0; i < 4; i++) {
                TileEntity t = worldObj.getTileEntity(xCoord + DX[i], yCoord, zCoord + DZ[i]);
                if (t instanceof TileEntityPcCase && ((TileEntityPcCase) t).complete()) return true;
            }
            return false;
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            tag.setInteger("facing", facing);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            facing = tag.getInteger("facing");
        }

        @Override
        public Packet getDescriptionPacket() {
            NBTTagCompound tag = new NBTTagCompound();
            writeToNBT(tag);
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
        }

        @Override
        public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
            readFromNBT(pkt.func_148857_g());
        }
    }

    // ================= CLIENTE =================
    @SideOnly(Side.CLIENT)
    public static class ClientSide {
        public static void init() {
            ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPcCase.class, new CaseRenderer());
            ClientRegistry.bindTileEntitySpecialRenderer(TileEntityMonitor.class, new MonitorRenderer());
            MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(caseBlock), new PartItemRenderer(0));
            MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(monitorBlock), new PartItemRenderer(1));
            MinecraftForgeClient.registerItemRenderer(cpuItem, new PartItemRenderer(2));
            MinecraftForgeClient.registerItemRenderer(gpuItem, new PartItemRenderer(3));
            MinecraftForgeClient.registerItemRenderer(ramItem, new PartItemRenderer(4));
            FMLCommonHandler.instance().bus().register(new CameraTask());
        }

        public static void openMonitor(int x, int y, int z) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiMonitor(x, y, z));
        }
    }

    // ---------- texturas feitas por codigo ----------
    @SideOnly(Side.CLIENT)
    public static class Tex {
        static boolean ready = false;
        public static ResourceLocation WHITE, METAL, PCB, CHIP, FAN, SCREEN_ON, SCREEN_OFF, GLASS;

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
                    int n = 205 + ((x * 7 + y * 13 + x * y) % 5) * 6;
                    boolean edge = x == 0 || y == 0 || x == 15 || y == 15;
                    int v = edge ? n - 45 : n;
                    if ((x == 2 || x == 13) && (y == 2 || y == 13)) v = 120;
                    im.setRGB(x, y, rgb(v, v, v));
                }
            }
            METAL = make("metal", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int r = 24, g = 92, b = 46;
                    if (x % 4 == 1 || y % 5 == 2) {
                        r = 40;
                        g = 140;
                        b = 70;
                    }
                    if (x % 4 == 1 && y % 5 == 2) {
                        r = 200;
                        g = 170;
                        b = 60;
                    }
                    if (x == 0 || y == 0 || x == 15 || y == 15) {
                        r = 14;
                        g = 60;
                        b = 30;
                    }
                    im.setRGB(x, y, rgb(r, g, b));
                }
            }
            PCB = make("pcb", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            fill(im, 0, 0, 15, 15, rgb(150, 150, 160));
            fill(im, 1, 1, 14, 14, rgb(38, 38, 44));
            fill(im, 3, 3, 4, 4, rgb(215, 215, 225));
            CHIP = make("chip", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    double dx = x - 7.5;
                    double dy = y - 7.5;
                    double d = Math.sqrt(dx * dx + dy * dy);
                    int col;
                    if (d > 7.6) {
                        col = rgb(18, 18, 22);
                    } else if (d < 2.3) {
                        col = rgb(95, 95, 105);
                    } else {
                        double a = Math.atan2(dy, dx) + d * 0.35;
                        int sec = (int) Math.floor((a + Math.PI * 4) / (Math.PI / 4));
                        col = (sec % 2 == 0) ? rgb(90, 90, 100) : rgb(34, 34, 40);
                    }
                    im.setRGB(x, y, col);
                }
            }
            FAN = make("fan", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int r = 12;
                    int g = 70 + y * 4;
                    int b = 140 + y * 5;
                    if (y >= 14) {
                        r = 16;
                        g = 22;
                        b = 34;
                    }
                    im.setRGB(x, y, rgb(r, g, b));
                }
            }
            fill(im, 2, 2, 4, 4, rgb(235, 235, 245));
            fill(im, 2, 6, 4, 8, rgb(245, 205, 60));
            fill(im, 2, 10, 4, 12, rgb(90, 205, 115));
            fill(im, 7, 2, 13, 10, rgb(205, 210, 220));
            fill(im, 7, 2, 13, 3, rgb(30, 60, 140));
            SCREEN_ON = make("screen_on", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int v = ((x + y) % 8 == 0) ? 26 : 12;
                    im.setRGB(x, y, rgb(v, v, v + 4));
                }
            }
            SCREEN_OFF = make("screen_off", im);

            im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int a = ((x + y) % 7 == 0) ? 120 : 70;
                    im.setRGB(x, y, (a << 24) | (170 << 16) | (210 << 8) | 235);
                }
            }
            GLASS = make("glass", im);
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
            // topo
            t.setColorRGBA_F(r, g, b, a);
            t.addVertexWithUV(x1, y2, z1, 0, 0);
            t.addVertexWithUV(x1, y2, z2, 0, 1);
            t.addVertexWithUV(x2, y2, z2, 1, 1);
            t.addVertexWithUV(x2, y2, z1, 1, 0);
            // base
            t.setColorRGBA_F(r * 0.5f, g * 0.5f, b * 0.5f, a);
            t.addVertexWithUV(x1, y1, z1, 0, 0);
            t.addVertexWithUV(x2, y1, z1, 1, 0);
            t.addVertexWithUV(x2, y1, z2, 1, 1);
            t.addVertexWithUV(x1, y1, z2, 0, 1);
            // norte e sul
            t.setColorRGBA_F(r * 0.8f, g * 0.8f, b * 0.8f, a);
            t.addVertexWithUV(x1, y1, z1, 0, 1);
            t.addVertexWithUV(x1, y2, z1, 0, 0);
            t.addVertexWithUV(x2, y2, z1, 1, 0);
            t.addVertexWithUV(x2, y1, z1, 1, 1);
            t.addVertexWithUV(x1, y1, z2, 0, 1);
            t.addVertexWithUV(x2, y1, z2, 1, 1);
            t.addVertexWithUV(x2, y2, z2, 1, 0);
            t.addVertexWithUV(x1, y2, z2, 0, 0);
            // oeste e leste
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

        static void quadFront(ResourceLocation tex, double x1, double y1, double x2, double y2, double z) {
            Tex.bind(tex);
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

        // ----- gabinete com as pecas dentro -----
        static void drawCase(int v, int cpu, int ram, int gpu, boolean powered, int light) {
            float[] c = COLORS[v];
            br = light;
            begin();
            Tex.init();
            // estrutura
            box(Tex.METAL, -0.40, 0.00, -0.45, 0.40, 0.03, 0.45, c[0], c[1], c[2], 1f);
            box(Tex.METAL, -0.40, 0.92, -0.45, 0.40, 0.95, 0.45, c[0], c[1], c[2], 1f);
            box(Tex.METAL, -0.40, 0.03, -0.45, 0.40, 0.92, -0.42, c[0], c[1], c[2], 1f);
            box(Tex.METAL, -0.40, 0.03, 0.42, 0.40, 0.92, 0.45, c[0], c[1], c[2], 1f);
            // ventilacao no topo
            for (int i = 0; i < 4; i++) {
                box(Tex.WHITE, -0.30 + i * 0.15, 0.95, -0.30, -0.20 + i * 0.15, 0.953, 0.10, 0.04f, 0.04f, 0.05f, 1f);
            }
            // detalhes da frente por modelo
            switch (v) {
                case 0:
                    for (int i = 0; i < 3; i++) {
                        box(Tex.WHITE, -0.30, 0.55 + i * 0.08, 0.45, 0.30, 0.58 + i * 0.08, 0.455, 0.05f, 0.05f, 0.06f, 1f);
                    }
                    break;
                case 1:
                    box(Tex.WHITE, -0.30, 0.40, 0.45, 0.30, 0.80, 0.455, 0.06f, 0.06f, 0.07f, 1f);
                    break;
                case 2: {
                    double tt = (System.currentTimeMillis() % 3000L) / 3000.0 * Math.PI * 2;
                    float rr = (float) (0.5 + 0.5 * Math.sin(tt));
                    float gg = (float) (0.5 + 0.5 * Math.sin(tt + 2.1));
                    float bb = (float) (0.5 + 0.5 * Math.sin(tt + 4.2));
                    if (!powered) {
                        rr = 0.25f;
                        gg = 0.25f;
                        bb = 0.25f;
                    }
                    int saved = br;
                    if (powered) br = FULL;
                    box(Tex.WHITE, -0.04, 0.12, 0.45, 0.04, 0.80, 0.458, rr, gg, bb, 1f);
                    br = saved;
                    break;
                }
                case 3:
                    for (int i = 0; i < 4; i++) {
                        for (int j = 0; j < 3; j++) {
                            box(Tex.WHITE, -0.24 + i * 0.14, 0.50 + j * 0.10, 0.45, -0.16 + i * 0.14, 0.56 + j * 0.10, 0.455,
                                0.05f, 0.05f, 0.06f, 1f);
                        }
                    }
                    break;
                default:
                    box(Tex.WHITE, -0.30, 0.70, 0.45, 0.30, 0.78, 0.456, 0.75f, 0.75f, 0.70f, 1f);
                    box(Tex.WHITE, -0.24, 0.73, 0.456, 0.24, 0.75, 0.458, 0.05f, 0.05f, 0.05f, 1f);
                    box(Tex.WHITE, -0.30, 0.55, 0.45, 0.30, 0.63, 0.456, 0.75f, 0.75f, 0.70f, 1f);
                    box(Tex.WHITE, -0.24, 0.58, 0.456, 0.24, 0.60, 0.458, 0.05f, 0.05f, 0.05f, 1f);
                    break;
            }
            // botao e led
            box(Tex.WHITE, 0.22, 0.84, 0.45, 0.30, 0.90, 0.46, 0.8f, 0.8f, 0.82f, 1f);
            {
                int saved = br;
                if (powered) br = FULL;
                box(Tex.WHITE, 0.18, 0.86, 0.45, 0.20, 0.88, 0.458, powered ? 0.1f : 0.7f, powered ? 0.95f : 0.1f, 0.1f, 1f);
                br = saved;
            }
            // fonte e placa-mae
            box(Tex.METAL, -0.36, 0.03, -0.42, 0.36, 0.15, -0.14, 0.12f, 0.12f, 0.14f, 1f);
            box(Tex.PCB, -0.03, 0.18, -0.35, 0.03, 0.88, 0.32, 1f, 1f, 1f, 1f);
            box(Tex.METAL, -0.03, 0.60, -0.42, 0.03, 0.86, -0.35, 0.7f, 0.7f, 0.72f, 1f);

            // processador
            if (cpu >= 0) {
                int k = cv(cpu);
                float[] cc = COLORS[k];
                box(Tex.CHIP, 0.03, 0.64, -0.20, 0.05, 0.76, -0.08, 1f, 1f, 1f, 1f);
                if (k == 1) {
                    box(Tex.METAL, 0.05, 0.64, -0.20, 0.10, 0.76, -0.08, cc[0], cc[1], cc[2], 1f);
                } else if (k == 2) {
                    box(Tex.METAL, 0.05, 0.62, -0.22, 0.14, 0.78, -0.06, cc[0], cc[1], cc[2], 1f);
                    box(Tex.FAN, 0.14, 0.64, -0.20, 0.145, 0.76, -0.08, 1f, 1f, 1f, 1f);
                } else if (k == 3) {
                    box(Tex.METAL, 0.05, 0.64, -0.20, 0.08, 0.84, -0.17, cc[0], cc[1], cc[2], 1f);
                    box(Tex.METAL, 0.05, 0.64, -0.11, 0.08, 0.84, -0.08, cc[0], cc[1], cc[2], 1f);
                    box(Tex.METAL, 0.05, 0.82, -0.20, 0.12, 0.86, -0.08, cc[0], cc[1], cc[2], 1f);
                } else if (k == 4) {
                    box(Tex.METAL, 0.05, 0.60, -0.24, 0.16, 0.80, -0.04, cc[0], cc[1], cc[2], 1f);
                    box(Tex.WHITE, 0.16, 0.66, -0.18, 0.17, 0.74, -0.10, 0.1f, 0.1f, 0.12f, 1f);
                }
            }
            // memoria RAM
            if (ram >= 0) {
                int k = cv(ram);
                float[] rc = COLORS[k];
                for (int i = 0; i < RAM_STICKS[k]; i++) {
                    double z0 = 0.02 + i * 0.045;
                    box(Tex.PCB, 0.03, 0.56, z0, 0.045, 0.86, z0 + 0.025, 1f, 1f, 1f, 1f);
                    box(Tex.METAL, 0.045, 0.56, z0, 0.06, 0.86, z0 + 0.025, rc[0], rc[1], rc[2], 1f);
                }
            }
            // placa de video
            if (gpu >= 0) {
                int k = cv(gpu);
                float[] gc = COLORS[k];
                box(Tex.WHITE, 0.03, 0.30, -0.30, 0.08, 0.34, 0.10, 0.12f, 0.12f, 0.14f, 1f);
                box(Tex.METAL, 0.08, 0.20, -0.32, 0.12, 0.50, 0.16, 0.25f, 0.25f, 0.28f, 1f);
                box(Tex.METAL, 0.12, 0.20, -0.32, 0.17, 0.50, 0.16, gc[0], gc[1], gc[2], 1f);
                int saved = br;
                if (powered) br = FULL;
                for (int i = 0; i < GPU_FANS[k]; i++) {
                    double zc = -0.235 + i * 0.17;
                    box(Tex.FAN, 0.17, 0.28, zc - 0.07, 0.175, 0.42, zc + 0.07, 1f, 1f, 1f, 1f);
                }
                br = saved;
            }
            // vidros laterais (por ultimo)
            box(Tex.GLASS, -0.405, 0.03, -0.42, -0.395, 0.92, 0.42, 1f, 1f, 1f, 1f);
            box(Tex.GLASS, 0.395, 0.03, -0.42, 0.405, 0.92, 0.42, 1f, 1f, 1f, 1f);
            end();
        }

        // ----- monitor -----
        static void drawMonitor(int v, boolean on, ResourceLocation photo, int light) {
            float[] c = COLORS[v];
            br = light;
            begin();
            Tex.init();
            box(Tex.METAL, -0.22, 0.0, -0.20, 0.22, 0.035, 0.20, c[0], c[1], c[2], 1f);
            box(Tex.METAL, -0.04, 0.035, -0.04, 0.04, 0.30, 0.04, c[0], c[1], c[2], 1f);
            box(Tex.METAL, -0.46, 0.28, -0.06, 0.46, 0.92, 0.04, c[0], c[1], c[2], 1f);
            double bz = BEZEL[v];
            ResourceLocation tex = on ? (photo != null ? photo : Tex.SCREEN_ON) : Tex.SCREEN_OFF;
            if (on) br = FULL;
            quadFront(tex, -0.46 + bz, 0.28 + bz, 0.46 - bz, 0.92 - bz, 0.0405);
            end();
        }

        // ----- pecas soltas (icones e itens no chao) -----
        static void drawCpuItem(int v) {
            float[] c = COLORS[v];
            br = FULL;
            begin();
            Tex.init();
            box(Tex.WHITE, -0.30, 0.26, -0.30, 0.30, 0.30, 0.30, 0.85f, 0.70f, 0.20f, 1f);
            box(Tex.PCB, -0.35, 0.30, -0.35, 0.35, 0.36, 0.35, 1f, 1f, 1f, 1f);
            box(Tex.CHIP, -0.22, 0.36, -0.22, 0.22, 0.46, 0.22, 1f, 1f, 1f, 1f);
            if (v > 0) {
                box(Tex.METAL, -0.20, 0.46, -0.20, 0.20, 0.46 + 0.10 + 0.06 * v, 0.20, c[0], c[1], c[2], 1f);
            }
            end();
        }

        static void drawGpuItem(int v) {
            float[] c = COLORS[v];
            br = FULL;
            begin();
            Tex.init();
            box(Tex.WHITE, -0.30, 0.28, -0.04, 0.30, 0.32, 0.04, 0.85f, 0.70f, 0.20f, 1f);
            box(Tex.PCB, -0.46, 0.32, -0.20, 0.46, 0.36, 0.20, 1f, 1f, 1f, 1f);
            box(Tex.METAL, -0.44, 0.36, -0.18, 0.44, 0.52, 0.18, c[0], c[1], c[2], 1f);
            int nf = GPU_FANS[v];
            for (int i = 0; i < nf; i++) {
                double cx = (i - (nf - 1) / 2.0) * 0.30;
                box(Tex.FAN, cx - 0.12, 0.52, -0.12, cx + 0.12, 0.525, 0.12, 1f, 1f, 1f, 1f);
            }
            end();
        }

        static void drawRamItem(int v) {
            float[] c = COLORS[v];
            br = FULL;
            begin();
            Tex.init();
            box(Tex.WHITE, -0.40, 0.14, -0.03, 0.40, 0.20, 0.03, 0.85f, 0.70f, 0.20f, 1f);
            box(Tex.PCB, -0.40, 0.20, -0.03, 0.40, 0.62, 0.03, 1f, 1f, 1f, 1f);
            box(Tex.METAL, -0.40, 0.20, 0.03, 0.40, 0.62, 0.07, c[0], c[1], c[2], 1f);
            for (int i = 0; i < 4; i++) {
                box(Tex.CHIP, -0.32 + i * 0.18, 0.30, -0.045, -0.20 + i * 0.18, 0.52, -0.03, 1f, 1f, 1f, 1f);
            }
            end();
        }
    }

    // ---------- renderizadores dos blocos ----------
    @SideOnly(Side.CLIENT)
    public static class CaseRenderer extends TileEntitySpecialRenderer {
        @Override
        public void renderTileEntityAt(TileEntity te, double x, double y, double z, float pt) {
            if (!(te instanceof TileEntityPcCase)) return;
            TileEntityPcCase c = (TileEntityPcCase) te;
            int light = te.getWorldObj() != null
                ? te.getWorldObj().getLightBrightnessForSkyBlocks(te.xCoord, te.yCoord, te.zCoord, 0)
                : FULL;
            GL11.glPushMatrix();
            GL11.glTranslated(x + 0.5, y, z + 0.5);
            GL11.glRotatef(180.0F - c.facing * 90.0F, 0.0F, 1.0F, 0.0F);
            Models.drawCase(cv(c.getBlockMetadata()), c.cpu, c.ram, c.gpu, c.powered(), light);
            GL11.glPopMatrix();
        }
    }

    @SideOnly(Side.CLIENT)
    public static class MonitorRenderer extends TileEntitySpecialRenderer {
        @Override
        public void renderTileEntityAt(TileEntity te, double x, double y, double z, float pt) {
            if (!(te instanceof TileEntityMonitor)) return;
            TileEntityMonitor m = (TileEntityMonitor) te;
            int light = te.getWorldObj() != null
                ? te.getWorldObj().getLightBrightnessForSkyBlocks(te.xCoord, te.yCoord, te.zCoord, 0)
                : FULL;
            boolean on = m.powered();
            ResourceLocation photo = on ? Photos.texture(te.xCoord, te.yCoord, te.zCoord) : null;
            GL11.glPushMatrix();
            GL11.glTranslated(x + 0.5, y, z + 0.5);
            GL11.glRotatef(180.0F - m.facing * 90.0F, 0.0F, 1.0F, 0.0F);
            Models.drawMonitor(cv(m.getBlockMetadata()), on, photo, light);
            GL11.glPopMatrix();
        }
    }

    // ---------- renderizador dos itens (icone, mao e chao) ----------
    @SideOnly(Side.CLIENT)
    public static class PartItemRenderer implements IItemRenderer {
        private final int kind; // 0 gabinete, 1 monitor, 2 processador, 3 placa de video, 4 RAM

        public PartItemRenderer(int kind) {
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
            int v = cv(item.getItemDamage());
            GL11.glPushMatrix();
            switch (type) {
                case INVENTORY:
                    GL11.glTranslatef(0.0F, -0.5F, 0.0F);
                    break;
                case ENTITY:
                    GL11.glScalef(0.5F, 0.5F, 0.5F);
                    GL11.glTranslatef(0.0F, -0.5F, 0.0F);
                    break;
                default:
                    GL11.glTranslatef(0.5F, 0.2F, 0.0F);
                    GL11.glScalef(0.7F, 0.7F, 0.7F);
                    break;
            }
            switch (kind) {
                case 0:
                    Models.drawCase(v, -1, -1, -1, false, FULL);
                    break;
                case 1:
                    Models.drawMonitor(v, true, null, FULL);
                    break;
                case 2:
                    Models.drawCpuItem(v);
                    break;
                case 3:
                    Models.drawGpuItem(v);
                    break;
                default:
                    Models.drawRamItem(v);
                    break;
            }
            GL11.glPopMatrix();
        }
    }

    // ---------- fotos (guardadas no seu computador/celular) ----------
    @SideOnly(Side.CLIENT)
    public static class Photos {
        static final Map<String, ResourceLocation> CACHE = new HashMap<String, ResourceLocation>();
        static final Set<String> FAILED = new HashSet<String>();
        static Properties index = null;

        static File dir() {
            File d = new File(Minecraft.getMinecraft().mcDataDir, "pcmod_photos");
            if (!d.exists()) d.mkdirs();
            return d;
        }

        static Properties index() {
            if (index == null) {
                index = new Properties();
                try {
                    File f = new File(dir(), "index.properties");
                    if (f.exists()) {
                        FileInputStream in = new FileInputStream(f);
                        index.load(in);
                        in.close();
                    }
                } catch (Throwable t) {
                    // sem indice ainda
                }
            }
            return index;
        }

        static void saveIndex() {
            try {
                FileOutputStream out = new FileOutputStream(new File(dir(), "index.properties"));
                index().store(out, "PC Mod");
                out.close();
            } catch (Throwable t) {
                // ignora
            }
        }

        static String key(int x, int y, int z) {
            World w = Minecraft.getMinecraft().theWorld;
            return w.getWorldInfo().getWorldName() + "|" + w.provider.dimensionId + "|" + x + "|" + y + "|" + z;
        }

        static BufferedImage scale(BufferedImage src, int w, int h) {
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int j = 0; j < h; j++) {
                int sy = j * src.getHeight() / h;
                for (int i = 0; i < w; i++) {
                    int sx = i * src.getWidth() / w;
                    out.setRGB(i, j, src.getRGB(sx, sy));
                }
            }
            return out;
        }

        public static ResourceLocation texture(int x, int y, int z) {
            if (Minecraft.getMinecraft().theWorld == null) return null;
            String f = index().getProperty(key(x, y, z));
            if (f == null || FAILED.contains(f)) return null;
            ResourceLocation rl = CACHE.get(f);
            if (rl != null) return rl;
            try {
                BufferedImage src = ImageIO.read(new File(dir(), f));
                if (src == null) {
                    FAILED.add(f);
                    return null;
                }
                BufferedImage small = scale(src, 256, 192);
                rl = new ResourceLocation(MODID, "photo_" + Integer.toHexString(f.hashCode()));
                Minecraft.getMinecraft().getTextureManager().loadTexture(rl, new DynamicTexture(small));
                CACHE.put(f, rl);
                return rl;
            } catch (Throwable t) {
                FAILED.add(f);
                return null;
            }
        }

        public static void capture(Minecraft mc, int x, int y, int z) throws Exception {
            int w = mc.displayWidth;
            int h = mc.displayHeight;
            IntBuffer buf = BufferUtils.createIntBuffer(w * h);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            buf.clear();
            GL11.glReadPixels(0, 0, w, h, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buf);
            int[] data = new int[w * h];
            buf.get(data);

            int ch = (int) (h * 0.75);
            int cw = ch * 4 / 3;
            if (cw > w) {
                cw = w;
                ch = cw * 3 / 4;
            }
            int cx0 = (w - cw) / 2;
            int cy0 = (h - ch) / 2;
            BufferedImage img = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_RGB);
            for (int j = 0; j < ch; j++) {
                int srcRow = h - 1 - (cy0 + j);
                for (int i = 0; i < cw; i++) {
                    img.setRGB(i, j, data[srcRow * w + cx0 + i] & 0xFFFFFF);
                }
            }

            String name = "foto_" + System.currentTimeMillis() + ".png";
            ImageIO.write(img, "png", new File(dir(), name));
            String k = key(x, y, z);
            String old = index().getProperty(k);
            index().setProperty(k, name);
            saveIndex();
            if (old != null) {
                ResourceLocation rl = CACHE.remove(old);
                if (rl != null) mc.getTextureManager().deleteTexture(rl);
            }
        }
    }

    // ---------- camera: tira a foto do jogador de frente ----------
    @SideOnly(Side.CLIENT)
    public static class CameraTask {
        static int state = 0;
        static int frames = 0;
        static int bx, by, bz;
        static int prevView;
        static boolean prevHide;

        public static void start(int x, int y, int z) {
            bx = x;
            by = y;
            bz = z;
            state = 1;
        }

        @SubscribeEvent
        public void onRender(TickEvent.RenderTickEvent e) {
            if (state == 0) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.thePlayer == null) {
                state = 0;
                return;
            }
            if (e.phase == TickEvent.Phase.START) {
                if (state == 1) {
                    prevView = mc.gameSettings.thirdPersonView;
                    prevHide = mc.gameSettings.hideGUI;
                    mc.gameSettings.thirdPersonView = 2;
                    mc.gameSettings.hideGUI = true;
                    state = 2;
                    frames = 0;
                }
            } else if (state == 2) {
                if (++frames >= 5) {
                    String result;
                    try {
                        Photos.capture(mc, bx, by, bz);
                        result = "Foto salva no monitor! (pasta pcmod_photos)";
                    } catch (Throwable t) {
                        result = "Falha ao salvar a foto: " + t;
                    }
                    mc.gameSettings.thirdPersonView = prevView;
                    mc.gameSettings.hideGUI = prevHide;
                    state = 0;
                    mc.thePlayer.addChatMessage(new ChatComponentText(result));
                }
            }
        }
    }

    // ---------- tela do monitor: area de trabalho, jogo, camera e galeria ----------
    @SideOnly(Side.CLIENT)
    public static class GuiMonitor extends GuiScreen {
        final int mx, my, mz;
        int screen = 0; // 0 area de trabalho, 1 jogo, 2 camera, 3 galeria
        int wx, wy, ww, wh;
        int score = 0;
        int timeLeft = 0;
        int target = 0;
        boolean over = false;
        static int best = 0;
        final int[] order = {0, 1, 2, 3};
        final Random rnd = new Random();
        static final int[] GCOL = {0xFFE53935, 0xFF43A047, 0xFF1E88E5, 0xFFFDD835};
        static final String[] GNAME = {"VERMELHO", "VERDE", "AZUL", "AMARELO"};

        public GuiMonitor(int x, int y, int z) {
            mx = x;
            my = y;
            mz = z;
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        void layout() {
            ww = Math.min(320, width - 16);
            wh = Math.min(220, height - 16);
            wx = (width - ww) / 2;
            wy = (height - wh) / 2;
        }

        static boolean in(int px, int py, int x1, int y1, int x2, int y2) {
            return px >= x1 && px <= x2 && py >= y1 && py <= y2;
        }

        int[] gridRect(int p) {
            int bw = (ww - 70) / 2;
            int bh = (wh - 100) / 2;
            int x = wx + 30 + (p % 2) * (bw + 10);
            int y = wy + 60 + (p / 2) * (bh + 10);
            return new int[] {x, y, x + bw, y + bh};
        }

        void shuffle() {
            for (int i = 3; i > 0; i--) {
                int j = rnd.nextInt(i + 1);
                int t = order[i];
                order[i] = order[j];
                order[j] = t;
            }
        }

        void startGame() {
            score = 0;
            timeLeft = 400;
            over = false;
            target = rnd.nextInt(4);
            shuffle();
        }

        void endGame() {
            over = true;
            if (score > best) best = score;
        }

        @Override
        public void updateScreen() {
            if (screen == 1 && !over) {
                if (--timeLeft <= 0) endGame();
            }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float pt) {
            layout();
            drawRect(0, 0, width, height, 0xB0000000);
            drawRect(wx - 5, wy - 5, wx + ww + 5, wy + wh + 5, 0xFF1A1A1A);
            drawRect(wx, wy, wx + ww, wy + wh, screen == 0 ? 0xFF1E5AA8 : 0xFF14213A);

            if (screen == 0) {
                String[] names = {"Jogo: Clique Certo", "Camera", "Galeria"};
                int[] cols = {0xFF43A047, 0xFFE0E0E0, 0xFFFDD835};
                for (int i = 0; i < 3; i++) {
                    int ix = wx + 20;
                    int iy = wy + 16 + i * 58;
                    drawRect(ix, iy, ix + 36, iy + 36, cols[i]);
                    drawString(fontRendererObj, names[i], ix + 46, iy + 14, 0xFFFFFFFF);
                }
            } else {
                drawRect(wx + 6, wy + 6, wx + 62, wy + 20, 0xFF37474F);
                drawString(fontRendererObj, "< Voltar", wx + 10, wy + 10, 0xFFFFFFFF);
                if (screen == 1) drawGame();
                else if (screen == 2) drawCamera();
                else drawGallery();
            }
            drawRect(wx, wy + wh - 16, wx + ww, wy + wh, 0xFF0F1722);
            drawString(fontRendererObj, "PC Mod OS  -  ESC fecha", wx + 6, wy + wh - 12, 0xFFB0BEC5);
            super.drawScreen(mouseX, mouseY, pt);
        }

        void drawGame() {
            drawString(fontRendererObj, "Pontos: " + score, wx + 70, wy + 10, 0xFFFFFFFF);
            drawString(fontRendererObj, "Tempo: " + (timeLeft / 20 + 1) + "s", wx + ww - 70, wy + 10, 0xFFFFFFFF);
            if (!over) {
                drawCenteredString(fontRendererObj, "Clique em: " + GNAME[target], wx + ww / 2, wy + 34, GCOL[target]);
                for (int p = 0; p < 4; p++) {
                    int[] r = gridRect(p);
                    drawRect(r[0], r[1], r[2], r[3], GCOL[order[p]]);
                }
            } else {
                drawCenteredString(fontRendererObj, "Fim de jogo!", wx + ww / 2, wy + wh / 2 - 40, 0xFFFFFFFF);
                drawCenteredString(fontRendererObj, "Pontos: " + score + "   Recorde: " + best, wx + ww / 2, wy + wh / 2 - 20, 0xFFFDD835);
                drawRect(wx + ww / 2 - 50, wy + wh / 2 + 10, wx + ww / 2 + 50, wy + wh / 2 + 30, 0xFF43A047);
                drawCenteredString(fontRendererObj, "Jogar de novo", wx + ww / 2, wy + wh / 2 + 16, 0xFFFFFFFF);
            }
        }

        void drawCamera() {
            drawCenteredString(fontRendererObj, "Camera", wx + ww / 2, wy + 34, 0xFFFFFFFF);
            drawCenteredString(fontRendererObj, "Tira uma foto do jogador, de frente.", wx + ww / 2, wy + 56, 0xFFB0BEC5);
            drawCenteredString(fontRendererObj, "A tela muda para a camera frontal por um instante.", wx + ww / 2, wy + 70, 0xFFB0BEC5);
            drawRect(wx + ww / 2 - 60, wy + 100, wx + ww / 2 + 60, wy + 126, 0xFFE53935);
            drawCenteredString(fontRendererObj, "TIRAR FOTO", wx + ww / 2, wy + 109, 0xFFFFFFFF);
        }

        void drawGallery() {
            ResourceLocation tex = Photos.texture(mx, my, mz);
            if (tex == null) {
                drawCenteredString(fontRendererObj, "Nenhuma foto ainda. Use o app Camera.", wx + ww / 2, wy + wh / 2 - 6, 0xFFB0BEC5);
                return;
            }
            int ix = wx + 20;
            int iy = wy + 30;
            int iw = ww - 40;
            int ih = wh - 30 - 24;
            Tex.bind(tex);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.addVertexWithUV(ix, iy + ih, 0, 0, 1);
            t.addVertexWithUV(ix + iw, iy + ih, 0, 1, 1);
            t.addVertexWithUV(ix + iw, iy, 0, 1, 0);
            t.addVertexWithUV(ix, iy, 0, 0, 0);
            t.draw();
        }

        @Override
        protected void mouseClicked(int px, int py, int btn) {
            layout();
            if (btn != 0) return;
            if (screen == 0) {
                for (int i = 0; i < 3; i++) {
                    int ix = wx + 20;
                    int iy = wy + 16 + i * 58;
                    if (in(px, py, ix, iy, ix + 130, iy + 36)) {
                        if (i == 0) startGame();
                        screen = i + 1;
                        return;
                    }
                }
                return;
            }
            if (in(px, py, wx + 6, wy + 6, wx + 62, wy + 20)) {
                screen = 0;
                return;
            }
            if (screen == 1) {
                if (over) {
                    if (in(px, py, wx + ww / 2 - 50, wy + wh / 2 + 10, wx + ww / 2 + 50, wy + wh / 2 + 30)) startGame();
                    return;
                }
                for (int p = 0; p < 4; p++) {
                    int[] r = gridRect(p);
                    if (in(px, py, r[0], r[1], r[2], r[3])) {
                        if (order[p] == target) {
                            score++;
                            target = rnd.nextInt(4);
                            shuffle();
                        } else {
                            timeLeft -= 20;
                            if (timeLeft <= 0) endGame();
                        }
                        return;
                    }
                }
            } else if (screen == 2) {
                if (in(px, py, wx + ww / 2 - 60, wy + 100, wx + ww / 2 + 60, wy + 126)) {
                    CameraTask.start(mx, my, mz);
                    Minecraft.getMinecraft().displayGuiScreen(null);
                }
            }
        }
    }
}
