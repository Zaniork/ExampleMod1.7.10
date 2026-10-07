package pcmod;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.OpenGlHelper;
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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

/**
 * PC Mod - Forge 1.7.10 - TUDO EM UM ARQUIVO.
 *
 * Itens: RAM, CPU, GPU, Fonte.  Blocos: Gabinete e Monitor.
 * - Modelos 3D desenhados por codigo (caixas) e texturas com texto geradas por codigo (sem PNG).
 * - Clique direito com a peca no gabinete = encaixa. Shift + mao vazia = remove.
 * - Monitor so liga se estiver COLADO (N/S/L/O) em um gabinete completo (RAM+CPU+GPU+Fonte).
 * - Clique direito no monitor ligado = abre o "sistema" com apps: Camera, Galeria, Info do PC.
 */
@Mod(modid = PCMod.MODID, name = "PC Mod", version = "1.0")
public class PCMod {

    public static final String MODID = "pcmod";

    @SidedProxy(clientSide = "pcmod.PCMod$ClientProxy", serverSide = "pcmod.PCMod$CommonProxy")
    public static CommonProxy proxy;

    /** capacidade de cada peca no gabinete: RAM(2 pentes), CPU, GPU, Fonte */
    public static final int[] CAP = { 2, 1, 1, 1 };
    public static final Item[] PARTS = new Item[4];
    public static Block blockCase;
    public static Block blockMonitor;

    /** vizinhos horizontais (x,z) */
    public static final int[][] DIRS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    public static final CreativeTabs TAB = new CreativeTabs("pcmod") {
        @Override
        @SideOnly(Side.CLIENT)
        public Item getTabIconItem() {
            return PARTS[1];
        }
    };

    static void lang(String key, String value) {
        LanguageRegistry.instance().addStringLocalization(key, "en_US", value);
        LanguageRegistry.instance().addStringLocalization(key, "pt_BR", value);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        String[] ids = { "ram", "cpu", "gpu", "psu" };
        String[] names = { "Mem\u00f3ria RAM 8GB", "Processador (CPU)", "Placa de V\u00eddeo (GPU)", "Fonte 750W" };
        for (int i = 0; i < 4; i++) {
            PARTS[i] = new ItemPart(i, ids[i]);
            GameRegistry.registerItem(PARTS[i], ids[i]);
            lang("item.pcmod." + ids[i] + ".name", names[i]);
        }
        blockCase = new BlockCase();
        blockMonitor = new BlockMonitor();
        GameRegistry.registerBlock(blockCase, "pc_case");
        GameRegistry.registerBlock(blockMonitor, "pc_monitor");
        lang("tile.pcmod.case.name", "Gabinete");
        lang("tile.pcmod.monitor.name", "Monitor");
        lang("itemGroup.pcmod", "PC Mod");
        GameRegistry.registerTileEntity(TileCase.class, "pcmod_case");
        GameRegistry.registerTileEntity(TileMonitor.class, "pcmod_monitor");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        // Receitas (sobrevivencia)
        GameRegistry.addRecipe(new ItemStack(PARTS[0]), "RRR", "III", 'R', Items.redstone, 'I', Items.iron_ingot);
        GameRegistry.addRecipe(new ItemStack(PARTS[1]), "GRG", "RDR", "GRG", 'G', Items.gold_ingot, 'R',
                Items.redstone, 'D', Items.diamond);
        GameRegistry.addRecipe(new ItemStack(PARTS[2]), "IDI", "RRR", "III", 'I', Items.iron_ingot, 'D',
                Items.diamond, 'R', Items.redstone);
        GameRegistry.addRecipe(new ItemStack(PARTS[3]), "III", "IRI", "III", 'I', Items.iron_ingot, 'R',
                Items.redstone);
        GameRegistry.addRecipe(new ItemStack(blockCase), "III", "IPI", "III", 'I', Items.iron_ingot, 'P',
                Blocks.glass_pane);
        GameRegistry.addRecipe(new ItemStack(blockMonitor), "III", "IGI", " I ", 'I', Items.iron_ingot, 'G',
                Blocks.glass);
        proxy.init();
    }

    // =====================================================================
    // PROXIES
    // =====================================================================
    public static class CommonProxy {
        public void init() {
        }

        public void openMonitor(World w, int x, int y, int z) {
        }
    }

    public static class ClientProxy extends CommonProxy {
        @Override
        public void init() {
            ItemRender ir = new ItemRender();
            for (int i = 0; i < 4; i++) {
                MinecraftForgeClient.registerItemRenderer(PARTS[i], ir);
            }
            MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(blockCase), ir);
            MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(blockMonitor), ir);
            ClientRegistry.bindTileEntitySpecialRenderer(TileCase.class, new RenderCase());
            ClientRegistry.bindTileEntitySpecialRenderer(TileMonitor.class, new RenderMonitor());
        }

        @Override
        public void openMonitor(World w, int x, int y, int z) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiMonitor(x, y, z));
        }
    }

    // =====================================================================
    // ITENS
    // =====================================================================
    public static class ItemPart extends Item {
        public final int type; // 0 RAM, 1 CPU, 2 GPU, 3 FONTE

        public ItemPart(int type, String name) {
            this.type = type;
            setUnlocalizedName("pcmod." + name);
            setCreativeTab(TAB);
            setMaxStackSize(16);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerIcons(IIconRegister r) {
            this.itemIcon = r.registerIcon("iron_ingot");
        }

        @Override
        @SideOnly(Side.CLIENT)
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void addInformation(ItemStack s, EntityPlayer p, List l, boolean adv) {
            l.add("Clique direito no Gabinete para encaixar");
        }
    }

    // =====================================================================
    // BLOCOS
    // =====================================================================
    public static abstract class BlockPC extends BlockContainer {
        private final float[] bb;

        protected BlockPC(float x1, float y1, float z1, float x2, float y2, float z2) {
            super(Material.iron);
            bb = new float[] { x1, y1, z1, x2, y2, z2 };
            setHardness(2.0F);
            setResistance(8.0F);
            setStepSound(Block.soundTypeMetal);
            setCreativeTab(TAB);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister r) {
            this.blockIcon = r.registerIcon("iron_block");
        }

        @Override
        public void setBlockBoundsBasedOnState(IBlockAccess w, int x, int y, int z) {
            int l = w.getBlockMetadata(x, y, z) & 3;
            if ((l & 1) == 0) {
                setBlockBounds(bb[0], bb[1], bb[2], bb[3], bb[4], bb[5]);
            } else {
                setBlockBounds(bb[2], bb[1], bb[0], bb[5], bb[4], bb[3]);
            }
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z) {
            setBlockBoundsBasedOnState(w, x, y, z);
            return super.getCollisionBoundingBoxFromPool(w, x, y, z);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public AxisAlignedBB getSelectedBoundingBoxFromPool(World w, int x, int y, int z) {
            setBlockBoundsBasedOnState(w, x, y, z);
            return super.getSelectedBoundingBoxFromPool(w, x, y, z);
        }

        @Override
        public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase p, ItemStack s) {
            int l = MathHelper.floor_double((double) (p.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
            w.setBlockMetadataWithNotify(x, y, z, l, 2);
        }

        @Override
        public int getRenderType() {
            return -1;
        }

        @Override
        public boolean isOpaqueCube() {
            return false;
        }

        @Override
        public boolean renderAsNormalBlock() {
            return false;
        }
    }

    /** Gabinete: fica no chao, recebe as pecas. */
    public static class BlockCase extends BlockPC {
        public BlockCase() {
            super(0.2F, 0.0F, 0.1F, 0.8F, 1.0F, 0.9F);
            setBlockName("pcmod.case");
        }

        @Override
        public TileEntity createNewTileEntity(World w, int meta) {
            return new TileCase();
        }

        @Override
        public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy,
                float hz) {
            TileEntity te = w.getTileEntity(x, y, z);
            if (!(te instanceof TileCase)) {
                return false;
            }
            TileCase c = (TileCase) te;
            ItemStack held = p.getHeldItem();

            // encaixar peca
            if (held != null && held.getItem() instanceof ItemPart) {
                if (!w.isRemote) {
                    int t = ((ItemPart) held.getItem()).type;
                    if (c.insert(t)) {
                        if (!p.capabilities.isCreativeMode) {
                            held.stackSize--;
                        }
                        p.addChatMessage(new ChatComponentText("Pe\u00e7a encaixada!"));
                    } else {
                        p.addChatMessage(new ChatComponentText("Essa pe\u00e7a j\u00e1 est\u00e1 instalada (sem espa\u00e7o)."));
                    }
                }
                return true;
            }

            // remover peca (shift + mao vazia)
            if (p.isSneaking() && held == null) {
                if (!w.isRemote) {
                    int t = c.removeLast();
                    if (t >= 0) {
                        ItemStack st = new ItemStack(PARTS[t]);
                        if (!p.inventory.addItemStackToInventory(st)) {
                            p.dropPlayerItemWithRandomChoice(st, false);
                        }
                    }
                }
                return true;
            }

            // status
            if (!w.isRemote) {
                p.addChatMessage(new ChatComponentText("Gabinete -> RAM " + c.parts[0] + "/2, CPU " + c.parts[1]
                        + "/1, GPU " + c.parts[2] + "/1, Fonte " + c.parts[3] + "/1"
                        + (c.isComplete() ? "  [PRONTO - coloque um monitor ao lado]" : "  [incompleto]")));
            }
            return true;
        }

        @Override
        public void breakBlock(World w, int x, int y, int z, Block b, int meta) {
            TileEntity te = w.getTileEntity(x, y, z);
            if (te instanceof TileCase && !w.isRemote) {
                TileCase c = (TileCase) te;
                for (int t = 0; t < 4; t++) {
                    for (int i = 0; i < c.parts[t]; i++) {
                        w.spawnEntityInWorld(new EntityItem(w, x + 0.5, y + 0.5, z + 0.5, new ItemStack(PARTS[t])));
                    }
                }
            }
            super.breakBlock(w, x, y, z, b, meta);
        }
    }

    /** Monitor: so liga ao lado de um gabinete completo. */
    public static class BlockMonitor extends BlockPC {
        public BlockMonitor() {
            super(0.04F, 0.0F, 0.3F, 0.96F, 0.88F, 0.7F);
            setBlockName("pcmod.monitor");
        }

        @Override
        public TileEntity createNewTileEntity(World w, int meta) {
            return new TileMonitor();
        }

        @Override
        public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy,
                float hz) {
            TileEntity te = w.getTileEntity(x, y, z);
            if (!(te instanceof TileMonitor)) {
                return false;
            }
            if (((TileMonitor) te).isPowered()) {
                if (w.isRemote) {
                    proxy.openMonitor(w, x, y, z);
                }
            } else if (!w.isRemote) {
                p.addChatMessage(new ChatComponentText(
                        "Monitor desligado: coloque ao lado de um gabinete com RAM, CPU, GPU e Fonte."));
            }
            return true;
        }
    }

    // =====================================================================
    // TILE ENTITIES
    // =====================================================================
    public static class TileCase extends TileEntity {
        /** [0]=RAM [1]=CPU [2]=GPU [3]=FONTE */
        public int[] parts = new int[4];

        public boolean isComplete() {
            return parts[0] >= 1 && parts[1] >= 1 && parts[2] >= 1 && parts[3] >= 1;
        }

        public boolean insert(int t) {
            if (parts[t] >= CAP[t]) {
                return false;
            }
            parts[t]++;
            sync();
            return true;
        }

        public int removeLast() {
            for (int t = 3; t >= 0; t--) {
                if (parts[t] > 0) {
                    parts[t]--;
                    sync();
                    return t;
                }
            }
            return -1;
        }

        private void sync() {
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }

        @Override
        public boolean canUpdate() {
            return false;
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            tag.setIntArray("parts", parts);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            if (tag.hasKey("parts")) {
                int[] a = tag.getIntArray("parts");
                if (a.length == 4) {
                    parts = a;
                }
            }
        }

        @Override
        public Packet getDescriptionPacket() {
            NBTTagCompound tag = new NBTTagCompound();
            writeToNBT(tag);
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
        }

        @Override
        public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
            readFromNBT(pkt.func_148857_g());
        }
    }

    public static class TileMonitor extends TileEntity {
        @Override
        public boolean canUpdate() {
            return false;
        }

        /** liga se houver um gabinete completo colado ao lado */
        public boolean isPowered() {
            if (worldObj == null) {
                return false;
            }
            for (int[] d : DIRS) {
                TileEntity t = worldObj.getTileEntity(xCoord + d[0], yCoord, zCoord + d[1]);
                if (t instanceof TileCase && ((TileCase) t).isComplete()) {
                    return true;
                }
            }
            return false;
        }
    }

    // =====================================================================
    // CLIENTE: dados (fotos)
    // =====================================================================
    public static class ClientData {
        /** foto exibida em cada monitor ("x,y,z") */
        public static final Map<String, ResourceLocation> PHOTOS = new HashMap<String, ResourceLocation>();
        /** galeria de fotos tiradas nesta sessao */
        public static final List<ResourceLocation> GALLERY = new ArrayList<ResourceLocation>();
        public static int counter = 0;
    }

    // =====================================================================
    // CLIENTE: graficos (modelos 3D por caixas + texturas com texto)
    // =====================================================================
    public static class Gfx {
        static final int DOWN = 1, UP = 2, NORTH = 4, SOUTH = 8, WEST = 16, EAST = 32, ALL = 63;
        static final Map<String, ResourceLocation> CACHE = new HashMap<String, ResourceLocation>();

        // ---------- texturas ----------
        static ResourceLocation register(String key, BufferedImage img) {
            ResourceLocation r = Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation("pcmod_" + key, new DynamicTexture(img));
            CACHE.put(key, r);
            return r;
        }

        static Graphics2D g2(BufferedImage img) {
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            return g;
        }

        /** texto centralizado na regiao (x,y,w,h), encolhendo a fonte ate caber */
        static void text(Graphics2D g, String s, int x, int y, int w, int h, int color) {
            int size = (int) (h * 0.75);
            Font f = new Font("SansSerif", Font.BOLD, size);
            FontMetrics fm = g.getFontMetrics(f);
            while (size > 5 && (fm.stringWidth(s) > w * 0.92 || fm.getHeight() > h)) {
                size--;
                f = new Font("SansSerif", Font.BOLD, size);
                fm = g.getFontMetrics(f);
            }
            g.setFont(f);
            g.setColor(new Color(color));
            g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
        }

        static ResourceLocation solid(String key, int rgb) {
            ResourceLocation r = CACHE.get(key);
            if (r != null) {
                return r;
            }
            BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = g2(img);
            g.setColor(new Color(rgb));
            g.fillRect(0, 0, 8, 8);
            g.dispose();
            return register(key, img);
        }

        /** textura com texto escrito (opcionalmente na vertical) */
        static ResourceLocation label(String key, int w, int h, int bg, int fg, boolean rotate, String txt) {
            ResourceLocation r = CACHE.get(key);
            if (r != null) {
                return r;
            }
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = g2(img);
            g.setColor(new Color(bg));
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(fg));
            g.drawRect(1, 1, w - 3, h - 3);
            if (rotate) {
                g.translate(0, h);
                g.rotate(-Math.PI / 2);
                text(g, txt, 0, 0, h, w, fg);
            } else {
                text(g, txt, 0, 0, w, h, fg);
            }
            g.dispose();
            return register(key, img);
        }

        static ResourceLocation front(boolean on) {
            String k = on ? "frontOn" : "frontOff";
            ResourceLocation r = CACHE.get(k);
            if (r != null) {
                return r;
            }
            BufferedImage img = new BufferedImage(60, 92, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = g2(img);
            g.setColor(new Color(0x1D1D24));
            g.fillRect(0, 0, 60, 92);
            g.setColor(new Color(0x3A3A46));
            for (int i = 0; i < 8; i++) {
                g.fillRect(8, 26 + i * 6, 44, 3);
            }
            text(g, "PC MOD", 0, 4, 60, 16, 0xFFFFFF);
            g.setColor(new Color(on ? 0x33FF66 : 0xFF3344));
            g.fillOval(24, 74, 12, 12);
            g.dispose();
            return register(k, img);
        }

        static ResourceLocation glass() {
            ResourceLocation r = CACHE.get("glass");
            if (r != null) {
                return r;
            }
            BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = g2(img);
            g.setColor(new Color(120, 170, 220, 70));
            g.fillRect(0, 0, 8, 8);
            g.dispose();
            return register("glass", img);
        }

        static ResourceLocation desktop() {
            ResourceLocation r = CACHE.get("desktop");
            if (r != null) {
                return r;
            }
            BufferedImage img = new BufferedImage(192, 126, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = g2(img);
            g.setPaint(new GradientPaint(0, 0, new Color(0x2A6FD6), 0, 126, new Color(0x0B2C66)));
            g.fillRect(0, 0, 192, 126);
            String[] n = { "Camera", "Galeria", "Info" };
            int[] cols = { 0xE8564A, 0x4CAF50, 0xF2B134 };
            for (int i = 0; i < 3; i++) {
                int x = 14 + i * 56;
                g.setColor(new Color(cols[i]));
                g.fillRoundRect(x, 16, 38, 38, 10, 10);
                g.setColor(Color.WHITE);
                g.fillOval(x + 11, 27, 16, 16);
                text(g, n[i], x - 8, 58, 54, 14, 0xFFFFFF);
            }
            g.setColor(new Color(0x0E141C));
            g.fillRect(0, 110, 192, 16);
            text(g, "PC MOD OS", 4, 110, 70, 16, 0xFFFFFF);
            g.dispose();
            return register("desktop", img);
        }

        static void bind(ResourceLocation r) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(r);
        }

        // ---------- caixas ----------
        static void v(Tessellator t, double x, double y, double z, double u, double vv) {
            t.addVertexWithUV(x, y, z, u, vv);
        }

        /** desenha uma caixa com a textura atual; mask escolhe as faces. Frente = lado NORTE (z menor). */
        static void box(double x1, double y1, double z1, double x2, double y2, double z2, int mask) {
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            if ((mask & DOWN) != 0) {
                t.setColorOpaque_F(0.5F, 0.5F, 0.5F);
                v(t, x1, y1, z2, 0, 0);
                v(t, x1, y1, z1, 0, 1);
                v(t, x2, y1, z1, 1, 1);
                v(t, x2, y1, z2, 1, 0);
            }
            if ((mask & UP) != 0) {
                t.setColorOpaque_F(1F, 1F, 1F);
                v(t, x1, y2, z1, 0, 0);
                v(t, x1, y2, z2, 0, 1);
                v(t, x2, y2, z2, 1, 1);
                v(t, x2, y2, z1, 1, 0);
            }
            if ((mask & NORTH) != 0) {
                t.setColorOpaque_F(0.8F, 0.8F, 0.8F);
                v(t, x2, y2, z1, 0, 0);
                v(t, x2, y1, z1, 0, 1);
                v(t, x1, y1, z1, 1, 1);
                v(t, x1, y2, z1, 1, 0);
            }
            if ((mask & SOUTH) != 0) {
                t.setColorOpaque_F(0.8F, 0.8F, 0.8F);
                v(t, x1, y2, z2, 0, 0);
                v(t, x1, y1, z2, 0, 1);
                v(t, x2, y1, z2, 1, 1);
                v(t, x2, y2, z2, 1, 0);
            }
            if ((mask & EAST) != 0) {
                t.setColorOpaque_F(0.65F, 0.65F, 0.65F);
                v(t, x2, y2, z2, 0, 0);
                v(t, x2, y1, z2, 0, 1);
                v(t, x2, y1, z1, 1, 1);
                v(t, x2, y2, z1, 1, 0);
            }
            if ((mask & WEST) != 0) {
                t.setColorOpaque_F(0.65F, 0.65F, 0.65F);
                v(t, x1, y2, z1, 0, 0);
                v(t, x1, y1, z1, 0, 1);
                v(t, x1, y1, z2, 1, 1);
                v(t, x1, y2, z2, 1, 0);
            }
            t.draw();
        }

        /** caixa com textura base e uma textura diferente nas faces faceMask */
        static void boxF(ResourceLocation base, ResourceLocation face, int faceMask, double x1, double y1, double z1,
                double x2, double y2, double z2) {
            bind(base);
            box(x1, y1, z1, x2, y2, z2, ALL & ~faceMask);
            bind(face);
            box(x1, y1, z1, x2, y2, z2, faceMask);
        }

        // ---------- modelos ----------
        /** pecas soltas (item): modelo dentro do cubo 0..1 */
        static void partModel(int t) {
            switch (t) {
            case 0: // RAM
                boxF(solid("pcb", 0x1F7A3A), label("ramI", 128, 40, 0x1F7A3A, 0xFFFFFF, false, "RAM 8GB"),
                        NORTH | SOUTH, 0.05, 0.33, 0.46, 0.95, 0.63, 0.54);
                bind(solid("gold", 0xD4AF37));
                box(0.08, 0.30, 0.46, 0.92, 0.33, 0.54, ALL);
                break;
            case 1: // CPU
                bind(solid("cpuPcb", 0x2A6B3A));
                box(0.2, 0.45, 0.2, 0.8, 0.50, 0.8, ALL);
                bind(solid("gold", 0xD4AF37));
                box(0.25, 0.42, 0.25, 0.75, 0.45, 0.75, ALL);
                boxF(solid("silver", 0xC0C4C8), label("cpuTop", 64, 64, 0xC0C4C8, 0x22262B, false, "CPU"), UP, 0.3,
                        0.50, 0.3, 0.7, 0.56, 0.7);
                break;
            case 2: // GPU
                boxF(solid("gpuBody", 0x16161C), label("gpuI", 128, 40, 0x16161C, 0x7CFC00, false, "GPU RTX"),
                        NORTH | SOUTH, 0.05, 0.30, 0.35, 0.95, 0.65, 0.65);
                bind(solid("gold", 0xD4AF37));
                box(0.15, 0.26, 0.45, 0.60, 0.30, 0.55, ALL);
                break;
            default: // FONTE
                boxF(solid("psuBody", 0x3A3A42), label("psuI", 96, 64, 0x3A3A42, 0xFFD54A, false, "PSU 750W"),
                        NORTH | SOUTH, 0.15, 0.28, 0.2, 0.85, 0.72, 0.8);
                break;
            }
        }

        /** gabinete aberto (lado direito de vidro) com as pecas instaladas */
        static void caseModel(int[] p, boolean on) {
            ResourceLocation metal = solid("caseMetal", 0x2B2D33);
            bind(metal);
            box(0.2, 0.00, 0.10, 0.8, 0.04, 0.9, ALL); // base
            box(0.2, 0.96, 0.10, 0.8, 1.00, 0.9, ALL); // topo
            box(0.2, 0.04, 0.86, 0.8, 0.96, 0.9, ALL); // fundo
            box(0.2, 0.04, 0.14, 0.24, 0.96, 0.86, ALL); // parede esquerda
            boxF(metal, front(on), NORTH, 0.2, 0.04, 0.10, 0.8, 0.96, 0.14); // frente

            // placa-mae
            boxF(solid("moboC", 0x1A5C2E), label("moboL", 68, 68, 0x1A5C2E, 0xC8FFD0, true, "MOTHERBOARD"), EAST,
                    0.24, 0.26, 0.18, 0.27, 0.94, 0.86);

            if (p[3] > 0) {
                boxF(solid("psuBody", 0x3A3A42), label("psuC", 100, 50, 0x3A3A42, 0xFFD54A, false, "PSU 750W"), EAST,
                        0.24, 0.04, 0.50, 0.74, 0.22, 0.86);
            }
            if (p[2] > 0) {
                boxF(solid("gpuBody", 0x16161C), label("gpuC", 192, 30, 0x16161C, 0x7CFC00, false, "GPU RTX"), EAST,
                        0.27, 0.28, 0.20, 0.55, 0.38, 0.84);
            }
            if (p[1] > 0) {
                boxF(solid("silver", 0xC0C4C8), label("cpuC", 64, 64, 0xC0C4C8, 0x22262B, false, "CPU"), EAST, 0.27,
                        0.58, 0.40, 0.36, 0.74, 0.56);
            }
            for (int i = 0; i < p[0] && i < 2; i++) {
                double z = 0.64 + i * 0.07;
                boxF(solid("pcb", 0x1F7A3A), label("ramC", 24, 96, 0x1F7A3A, 0xFFFFFF, true, "RAM 8GB"),
                        NORTH | SOUTH, 0.27, 0.55, z, 0.35, 0.90, z + 0.04);
            }

            // vidro (por ultimo, translucido)
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            bind(glass());
            box(0.76, 0.04, 0.10, 0.8, 0.96, 0.9, EAST | WEST);
            GL11.glDisable(GL11.GL_BLEND);
        }

        static void monitorBody() {
            bind(solid("dark", 0x16161A));
            box(0.30, 0.00, 0.30, 0.70, 0.04, 0.70, ALL); // base
            box(0.45, 0.04, 0.45, 0.55, 0.28, 0.55, ALL); // haste
            box(0.04, 0.25, 0.40, 0.96, 0.88, 0.50, ALL); // moldura
        }

        static void monitorScreen(boolean on, ResourceLocation photo) {
            ResourceLocation scr = !on ? solid("screenOff", 0x050607) : (photo != null ? photo : desktop());
            bind(scr);
            box(0.08, 0.29, 0.399, 0.92, 0.84, 0.40, NORTH);
        }

        static void drawStack(ItemStack s) {
            Item i = s.getItem();
            if (i instanceof ItemPart) {
                partModel(((ItemPart) i).type);
            } else if (i == Item.getItemFromBlock(blockCase)) {
                caseModel(new int[4], false);
            } else {
                monitorBody();
                monitorScreen(true, null);
            }
        }
    }

    // =====================================================================
    // CLIENTE: renderers
    // =====================================================================
    public static class RenderCase extends TileEntitySpecialRenderer {
        @Override
        public void renderTileEntityAt(TileEntity te, double x, double y, double z, float pt) {
            TileCase c = (TileCase) te;
            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            GL11.glTranslated(x + 0.5, y, z + 0.5);
            GL11.glRotatef(-90F * (te.getBlockMetadata() & 3), 0F, 1F, 0F);
            GL11.glTranslated(-0.5, 0, -0.5);
            Gfx.caseModel(c.parts, c.isComplete());
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    public static class RenderMonitor extends TileEntitySpecialRenderer {
        @Override
        public void renderTileEntityAt(TileEntity te, double x, double y, double z, float pt) {
            TileMonitor m = (TileMonitor) te;
            boolean on = m.isPowered();
            ResourceLocation photo = on ? ClientData.PHOTOS.get(te.xCoord + "," + te.yCoord + "," + te.zCoord) : null;
            int br = te.getWorldObj().getLightBrightnessForSkyBlocks(te.xCoord, te.yCoord, te.zCoord, 0);
            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            GL11.glTranslated(x + 0.5, y, z + 0.5);
            GL11.glRotatef(-90F * (te.getBlockMetadata() & 3), 0F, 1F, 0F);
            GL11.glTranslated(-0.5, 0, -0.5);
            Gfx.monitorBody();
            if (on) { // tela brilha
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
            }
            Gfx.monitorScreen(on, photo);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) (br % 65536),
                    (float) (br / 65536));
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    public static class ItemRender implements IItemRenderer {
        @Override
        public boolean handleRenderType(ItemStack item, ItemRenderType type) {
            return true;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
            return type == ItemRenderType.ENTITY
                    && (helper == ItemRendererHelper.ENTITY_ROTATION || helper == ItemRendererHelper.ENTITY_BOBBING);
        }

        @Override
        public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
            boolean part = item.getItem() instanceof ItemPart;
            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            switch (type) {
            case INVENTORY: {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glTranslatef(8F, 8F, 100F);
                float s = part ? 15F : 11F;
                GL11.glScalef(s, s, s);
                GL11.glRotatef(180F, 1F, 0F, 0F);
                GL11.glRotatef(-25F, 1F, 0F, 0F);
                GL11.glRotatef(30F, 0F, 1F, 0F);
                break;
            }
            case ENTITY:
                GL11.glScalef(0.7F, 0.7F, 0.7F);
                break;
            case EQUIPPED:
                GL11.glTranslatef(0.5F, 0.5F, 0.5F);
                GL11.glScalef(0.6F, 0.6F, 0.6F);
                break;
            case EQUIPPED_FIRST_PERSON:
                GL11.glTranslatef(0.4F, 0.4F, 0.3F);
                GL11.glRotatef(35F, 0F, 1F, 0F);
                GL11.glScalef(0.7F, 0.7F, 0.7F);
                break;
            default:
                break;
            }
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
            Gfx.drawStack(item);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    // =====================================================================
    // CLIENTE: tela do monitor (apps)
    // =====================================================================
    public static class GuiMonitor extends GuiScreen {
        private final int bx, by, bz;
        private final String key;
        private int app = 0; // 0 desktop, 1 camera, 2 galeria, 3 info
        private int idx = 0;
        private int flash = 0;
        private boolean shoot = false;
        private int sx, sy, ex, ey;

        public GuiMonitor(int x, int y, int z) {
            bx = x;
            by = y;
            bz = z;
            key = x + "," + y + "," + z;
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void initGui() {
            sx = 14;
            sy = 14;
            ex = width - 14;
            ey = height - 14;
            int cx = width / 2;
            buttonList.clear();
            switch (app) {
            case 0:
                buttonList.add(new GuiButton(1, sx + 12, sy + 16, 70, 20, "Camera"));
                buttonList.add(new GuiButton(2, sx + 12, sy + 42, 70, 20, "Galeria"));
                buttonList.add(new GuiButton(3, sx + 12, sy + 68, 70, 20, "Info do PC"));
                buttonList.add(new GuiButton(9, ex - 72, ey - 40, 68, 20, "Sair"));
                break;
            case 1:
                buttonList.add(new GuiButton(8, sx + 6, sy + 6, 50, 20, "Voltar"));
                buttonList.add(new GuiButton(10, cx - 45, ey - 30, 90, 20, "Tirar foto"));
                break;
            case 2:
                buttonList.add(new GuiButton(8, sx + 6, sy + 6, 50, 20, "Voltar"));
                buttonList.add(new GuiButton(15, ex - 86, sy + 6, 80, 20, "Limpar tela"));
                buttonList.add(new GuiButton(12, cx - 100, ey - 30, 30, 20, "<"));
                buttonList.add(new GuiButton(14, cx - 60, ey - 30, 120, 20, "Usar no monitor"));
                buttonList.add(new GuiButton(13, cx + 70, ey - 30, 30, 20, ">"));
                break;
            default:
                buttonList.add(new GuiButton(8, sx + 6, sy + 6, 50, 20, "Voltar"));
                break;
            }
        }

        @Override
        protected void actionPerformed(GuiButton b) {
            switch (b.id) {
            case 1:
                app = 1;
                initGui();
                break;
            case 2:
                app = 2;
                idx = Math.max(0, ClientData.GALLERY.size() - 1);
                initGui();
                break;
            case 3:
                app = 3;
                initGui();
                break;
            case 8:
                app = 0;
                initGui();
                break;
            case 9:
                mc.displayGuiScreen(null);
                break;
            case 10:
                shoot = true;
                break;
            case 12:
                if (idx > 0) {
                    idx--;
                }
                break;
            case 13:
                if (idx < ClientData.GALLERY.size() - 1) {
                    idx++;
                }
                break;
            case 14:
                if (!ClientData.GALLERY.isEmpty()) {
                    ClientData.PHOTOS.put(key, ClientData.GALLERY.get(idx));
                }
                break;
            case 15:
                ClientData.PHOTOS.remove(key);
                break;
            default:
                break;
            }
        }

        @Override
        protected void keyTyped(char c, int code) {
            if (code == Keyboard.KEY_ESCAPE) {
                if (app != 0) {
                    app = 0;
                    initGui();
                } else {
                    mc.displayGuiScreen(null);
                }
            }
        }

        @Override
        public void updateScreen() {
            super.updateScreen();
            if (flash > 0) {
                flash--;
            }
            TileEntity te = mc.theWorld.getTileEntity(bx, by, bz);
            if (!(te instanceof TileMonitor) || !((TileMonitor) te).isPowered()) {
                mc.displayGuiScreen(null);
            }
        }

        private void drawImage(ResourceLocation r, int x, int y, int w, int h) {
            mc.getTextureManager().bindTexture(r);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.addVertexWithUV(x, y + h, 0, 0, 1);
            t.addVertexWithUV(x + w, y + h, 0, 1, 1);
            t.addVertexWithUV(x + w, y, 0, 1, 0);
            t.addVertexWithUV(x, y, 0, 0, 0);
            t.draw();
        }

        @Override
        public void drawScreen(int mx, int my, float pt) {
            int cx = width / 2;
            drawRect(sx - 4, sy - 4, ex + 4, ey + 4, 0xFF0B0B0E);
            drawGradientRect(sx, sy, ex, ey, 0xFF2A6FD6, 0xFF0B2C66);

            switch (app) {
            case 0: {
                fontRendererObj.drawStringWithShadow("PC Mod OS", ex - 60, sy + 6, 0xFFFFFF);
                drawRect(sx, ey - 18, ex, ey, 0xFF0E141C);
                fontRendererObj.drawString("PC MOD OS", sx + 6, ey - 13, 0xFFFFFF);
                String clock = new SimpleDateFormat("HH:mm").format(new Date());
                fontRendererObj.drawString(clock, ex - 34, ey - 13, 0xFFFFFF);
                break;
            }
            case 1: {
                fontRendererObj.drawString("Camera - frente do jogador", sx + 64, sy + 12, 0xFFFFFF);
                int vh = Math.max(40, Math.min(150, ey - sy - 80));
                int vw = (int) (vh * 0.9F);
                int vx = cx - vw / 2;
                int vy = sy + 30;
                drawRect(vx - 2, vy - 2, vx + vw + 2, vy + vh + 2, 0xFF000000);
                drawRect(vx, vy, vx + vw, vy + vh, 0xFF8FB8D8);
                int feet = vy + vh - 6;
                boolean in = mx >= vx && mx < vx + vw && my >= vy && my < vy + vh;
                GuiInventory.func_147046_a(cx, feet, (int) (vh * 0.45F), in ? (float) (cx - mx) : 0F,
                        in ? (float) (feet - 45 - my) : 0F, mc.thePlayer);
                GL11.glColor4f(1F, 1F, 1F, 1F);
                if (shoot) {
                    shoot = false;
                    capture(vx, vy, vw, vh);
                    flash = 8;
                }
                if (flash > 0) {
                    drawRect(vx, vy, vx + vw, vy + vh, (Math.min(255, flash * 32) << 24) | 0xFFFFFF);
                }
                break;
            }
            case 2: {
                fontRendererObj.drawString("Galeria", cx - 16, sy + 12, 0xFFFFFF);
                if (ClientData.GALLERY.isEmpty()) {
                    drawCenteredString(fontRendererObj, "Nenhuma foto. Abra a Camera.", cx, height / 2, 0xFFFFFF);
                } else {
                    int ah = Math.max(40, ey - sy - 80);
                    int aw = (int) (ah * 0.9F);
                    drawImage(ClientData.GALLERY.get(idx), cx - aw / 2, sy + 30, aw, ah);
                    drawCenteredString(fontRendererObj, "Foto " + (idx + 1) + "/" + ClientData.GALLERY.size(), cx,
                            sy + 34 + ah, 0xFFFFFF);
                }
                break;
            }
            default: {
                fontRendererObj.drawString("Info do PC", cx - 24, sy + 12, 0xFFFFFF);
                TileCase c = findCase();
                int y = sy + 40;
                int x = sx + 20;
                fontRendererObj.drawString("PC Mod OS 1.0", x, y, 0xFFFFFF);
                if (c != null) {
                    fontRendererObj.drawString("Mem\u00f3ria RAM: " + (c.parts[0] * 8) + " GB (" + c.parts[0]
                            + " pente(s))", x, y + 14, 0xCCE6FF);
                    fontRendererObj.drawString("Processador: " + (c.parts[1] > 0 ? "instalado" : "-"), x, y + 28,
                            0xCCE6FF);
                    fontRendererObj.drawString("Placa de v\u00eddeo: " + (c.parts[2] > 0 ? "instalada" : "-"), x,
                            y + 42, 0xCCE6FF);
                    fontRendererObj.drawString("Fonte: " + (c.parts[3] > 0 ? "750W" : "-"), x, y + 56, 0xCCE6FF);
                }
                break;
            }
            }
            super.drawScreen(mx, my, pt);
        }

        private TileCase findCase() {
            for (int[] d : DIRS) {
                TileEntity t = mc.theWorld.getTileEntity(bx + d[0], by, bz + d[1]);
                if (t instanceof TileCase) {
                    return (TileCase) t;
                }
            }
            return null;
        }

        /** le os pixels do visor da camera e vira uma foto (textura) */
        private void capture(int vx, int vy, int vw, int vh) {
            try {
                float f = (float) mc.displayWidth / (float) width;
                int px = Math.round(vx * f);
                int pw = Math.round(vw * f);
                int ph = Math.round(vh * f);
                int py = mc.displayHeight - Math.round((vy + vh) * f);
                ByteBuffer buf = BufferUtils.createByteBuffer(pw * ph * 4);
                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
                GL11.glReadPixels(px, py, pw, ph, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
                BufferedImage img = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
                for (int yy = 0; yy < ph; yy++) {
                    for (int xx = 0; xx < pw; xx++) {
                        int i = (xx + pw * yy) * 4;
                        int r = buf.get(i) & 255;
                        int g = buf.get(i + 1) & 255;
                        int b = buf.get(i + 2) & 255;
                        img.setRGB(xx, ph - 1 - yy, (r << 16) | (g << 8) | b);
                    }
                }
                ResourceLocation rl = mc.getTextureManager().getDynamicTextureLocation(
                        "pcmod_photo" + (ClientData.counter++), new DynamicTexture(img));
                ClientData.GALLERY.add(rl);
                ClientData.PHOTOS.put(key, rl); // aparece na tela 3D do monitor
                if (ClientData.GALLERY.size() > 12) {
                    ResourceLocation old = ClientData.GALLERY.remove(0);
                    while (ClientData.PHOTOS.values().remove(old)) {
                        // remove referencias
                    }
                    mc.getTextureManager().deleteTexture(old);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
