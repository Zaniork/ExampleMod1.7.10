package com.myname.mymodid;

import cpw.mods.fml.client.registry.ClientRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.nio.ByteBuffer;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Mod separado (segundo @Mod no mesmo pacote): gabinete de PC 3D + monitor 3D.
 * Não altera MyMod, CommonProxy nem ClientProxy.
 */
@Mod(modid = MyMod.MODID, version = Tags.VERSION, name = "MyMod", acceptedMinecraftVersions = "[1.7.10]")
public class MyMod {

    public static final String MODID = "mymodid";
    public static final String MODNAME = "MyMod";
    public static final Logger LOG = LogManager.getLogger(MODID);

    /** preenchido só no cliente (tira a foto) */
    public interface PhotoHandler {
        void take(World w, int x, int y, int z);
    }

    public static PhotoHandler photoHandler;

    public static Block pcCase;
    public static Block monitor;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        pcCase = new BlockPcCase();
        monitor = new BlockMonitor();
        GameRegistry.registerBlock(pcCase, "pc_case");
        GameRegistry.registerBlock(monitor, "pc_monitor");
        GameRegistry.registerTileEntity(TilePcCase.class, "mypc_case");
        GameRegistry.registerTileEntity(TileMonitor.class, "mypc_monitor");
        LanguageRegistry.instance().addStringLocalization("tile.mypc.case.name", "Gabinete de PC");
        LanguageRegistry.instance().addStringLocalization("tile.mypc.monitor.name", "Monitor");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        GameRegistry.addRecipe(new ItemStack(pcCase), "III", "IGR", "III",
            'I', Items.iron_ingot, 'G', Blocks.glass_pane, 'R', Items.redstone);
        GameRegistry.addRecipe(new ItemStack(monitor), "III", "IGI", "SSS",
            'I', Items.iron_ingot, 'G', Blocks.glass_pane, 'S', Blocks.stone);
        if (e.getSide().isClient()) ClientInit.run();
    }

    /** true se há o bloco b numa das 4 laterais. */
    public static boolean hasBlockNear(World w, int x, int y, int z, Block b) {
        return w.getBlock(x + 1, y, z) == b || w.getBlock(x - 1, y, z) == b
            || w.getBlock(x, y, z + 1) == b || w.getBlock(x, y, z - 1) == b;
    }

    // ---------------- tile entities ----------------

    public static class TilePcCase extends TileEntity {
        public boolean demo = false; // usado só para desenhar o item

        @Override
        public boolean canUpdate() {
            return false;
        }
    }

    public static class TileMonitor extends TileEntity {
        public boolean demo = false;
        /** foto atual (só existe no cliente que tirou) */
        public net.minecraft.util.ResourceLocation photo;

        @Override
        public boolean canUpdate() {
            return false;
        }
    }

    // ---------------- blocos ----------------

    private static int facing(EntityLivingBase p) {
        return MathHelper.floor_double(p.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
    }

    public static class BlockPcCase extends BlockContainer {
        public BlockPcCase() {
            super(Material.iron);
            setBlockName("mypc.case");
            setCreativeTab(CreativeTabs.tabRedstone);
            setHardness(1.5F);
            setResistance(10.0F);
            setStepSound(Block.soundTypeMetal);
            setLightOpacity(0);
            setBlockBounds(0.12F, 0.0F, 0.12F, 0.88F, 0.94F, 0.88F);
        }

        @Override
        public TileEntity createNewTileEntity(World w, int meta) {
            return new TilePcCase();
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
            return -1; // desenhado só pelo TESR
        }

        @Override
        public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase p, ItemStack s) {
            w.setBlockMetadataWithNotify(x, y, z, facing(p), 2);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister reg) {
            this.blockIcon = reg.registerIcon("iron_block");
        }
    }

    public static class BlockMonitor extends BlockContainer {
        public BlockMonitor() {
            super(Material.iron);
            setBlockName("mypc.monitor");
            setCreativeTab(CreativeTabs.tabRedstone);
            setHardness(1.0F);
            setResistance(8.0F);
            setStepSound(Block.soundTypeMetal);
            setLightOpacity(0);
            setBlockBounds(0.12F, 0.0F, 0.12F, 0.88F, 0.94F, 0.88F);
        }

        @Override
        public TileEntity createNewTileEntity(World w, int meta) {
            return new TileMonitor();
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
        public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase p, ItemStack s) {
            w.setBlockMetadataWithNotify(x, y, z, facing(p), 2);
        }

        /** Clique direito = tira a foto do jogador (feito no cliente). */
        @Override
        public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy,
            float hz) {
            if (w.isRemote && photoHandler != null) photoHandler.take(w, x, y, z);
            return true;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister reg) {
            this.blockIcon = reg.registerIcon("iron_block");
        }
    }

    /** Registro do lado cliente (só carregada no cliente). */
    public static class ClientInit {
        public static void run() {
            ClientRegistry.bindTileEntitySpecialRenderer(TilePcCase.class, new PcClient.CaseRenderer());
            ClientRegistry.bindTileEntitySpecialRenderer(TileMonitor.class, new PcClient.MonitorRenderer());
            TilePcCase caseDummy = new TilePcCase();
            caseDummy.demo = true;
            TileMonitor monDummy = new TileMonitor();
            monDummy.demo = true;
            MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(pcCase),
                new PcClient.TesrItem(new PcClient.CaseRenderer(), caseDummy));
            MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(monitor),
                new PcClient.TesrItem(new PcClient.MonitorRenderer(), monDummy));
            photoHandler = new PhotoHandler() {
                @Override
                public void take(World w, int x, int y, int z) {
                    TileEntity te = w.getTileEntity(x, y, z);
                    if (te instanceof TileMonitor) PcClient.takePhoto(w, x, y, z, (TileMonitor) te);
                }
            };
        }
    }

    /** Tudo que é só de cliente: textura gerada em código, modelos 3D e câmera. */
    public static class PcClient {

        // ---------- índices dos tiles no atlas 64x64 (4x4 tiles de 16px) ----------
        static final int METAL = 0, MESH = 1, GLASS = 2, MOBO = 3, FAN = 4, GPU = 5, RAM = 6, PSU = 7;
        static final int HDD = 8, FINS = 9, LEDG = 10, SCR_OFF = 11, SCR_NOSIG = 12, BEZEL = 13, STAND = 14, LEDR = 15;

        static final double K = 1.0 / 16.0;
        static final double E = 0.002;

        // =====================================================================
        // TEXTURAS GERADAS EM CÓDIGO
        // =====================================================================
        private static ResourceLocation atlasLoc;
        private static int[] A;

        static ResourceLocation atlas() {
            if (atlasLoc == null) {
                DynamicTexture d = new DynamicTexture(64, 64);
                A = d.getTextureData();
                buildAtlas(new Random(1337L));
                d.updateDynamicTexture();
                atlasLoc = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("mypc_atlas", d);
            }
            return atlasLoc;
        }

        static int clamp(int v) {
            return v < 0 ? 0 : (v > 255 ? 255 : v);
        }

        static int argb(int a, int r, int g, int b) {
            return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
        }

        static int rgb(int r, int g, int b) {
            return argb(255, r, g, b);
        }

        static void px(int t, int x, int y, int c) {
            if (x < 0 || y < 0 || x > 15 || y > 15) return;
            A[((t >> 2) * 16 + y) * 64 + (t & 3) * 16 + x] = c;
        }

        static void fillTile(int t, int r, int g, int b, int noise, Random rnd) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int n = noise > 0 ? rnd.nextInt(noise * 2 + 1) - noise : 0;
                    px(t, x, y, rgb(r + n, g + n, b + n));
                }
            }
        }

        static void border(int t, int c) {
            for (int i = 0; i < 16; i++) {
                px(t, i, 0, c);
                px(t, i, 15, c);
                px(t, 0, i, c);
                px(t, 15, i, c);
            }
        }

        static void rect(int t, int x1, int y1, int x2, int y2, int c) {
            for (int y = y1; y <= y2; y++) for (int x = x1; x <= x2; x++) px(t, x, y, c);
        }

        /** Ventoinha elíptica (rx/ry compensam a deformação da textura). */
        static void fan(int t, double cx, double cy, double rx, double ry) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry;
                    double d = Math.sqrt(dx * dx + dy * dy);
                    if (d > 1.0) continue;
                    int c;
                    if (d > 0.86) {
                        c = rgb(31, 182, 255); // anel de LED
                    } else if (d < 0.2) {
                        c = d < 0.08 ? rgb(40, 44, 50) : rgb(138, 146, 160);
                    } else {
                        double a = Math.atan2(dy, dx);
                        int blade = (int) Math.floor((a + Math.PI + d * 1.6) / (2 * Math.PI) * 7.0);
                        c = (blade & 1) == 0 ? rgb(92, 99, 110) : rgb(30, 33, 38);
                    }
                    px(t, x, y, c);
                }
            }
        }

        static void buildAtlas(Random r) {
            // METAL
            fillTile(METAL, 58, 61, 66, 6, r);
            border(METAL, rgb(34, 36, 40));

            // MESH (grade da frente)
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    boolean hole = (x % 4 == 1 || x % 4 == 2) && (y % 4 == 1 || y % 4 == 2);
                    px(MESH, x, y, hole ? rgb(5, 5, 6) : rgb(37, 40, 45));
                }
            }

            // GLASS (translúcido)
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int a = 50;
                    if ((x + y) % 8 == 0) a = 105;
                    px(GLASS, x, y, argb(a, 170, 215, 240));
                }
            }
            border(GLASS, argb(190, 30, 36, 42));

            // MOBO
            fillTile(MOBO, 18, 72, 44, 4, r);
            for (int i = 0; i < 26; i++) {
                int x = r.nextInt(14), y = r.nextInt(14), len = 3 + r.nextInt(6);
                boolean h = r.nextBoolean();
                for (int k = 0; k < len; k++) px(MOBO, h ? x + k : x, h ? y : y + k, rgb(40, 130, 80));
            }
            for (int i = 0; i < 14; i++) px(MOBO, r.nextInt(16), r.nextInt(16), rgb(201, 162, 39));
            rect(MOBO, 2, 2, 6, 6, rgb(12, 12, 14));
            border(MOBO, rgb(18, 72, 44));
            rect(MOBO, 9, 8, 13, 11, rgb(14, 14, 18));
            rect(MOBO, 3, 3, 5, 5, rgb(60, 60, 68));

            // FAN
            fillTile(FAN, 21, 23, 26, 0, r);
            fan(FAN, 8.0, 8.0, 7.6, 7.6);

            // GPU (duas ventoinhas, compensando o esticamento 2:1)
            fillTile(GPU, 22, 24, 27, 2, r);
            border(GPU, rgb(70, 74, 82));
            rect(GPU, 0, 0, 15, 1, rgb(210, 40, 40)); // faixa vermelha
            fan(GPU, 4.0, 9.0, 3.6, 6.2);
            fan(GPU, 12.0, 9.0, 3.6, 6.2);

            // RAM
            fillTile(RAM, 24, 24, 28, 0, r);
            rect(RAM, 0, 0, 15, 2, rgb(57, 208, 255));
            for (int row = 0; row < 3; row++) rect(RAM, 2, 4 + row * 4, 13, 6 + row * 4, rgb(10, 10, 12));

            // PSU (grade redonda)
            fillTile(PSU, 44, 48, 54, 3, r);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    double d = Math.sqrt((x + 0.5 - 8) * (x + 0.5 - 8) + (y + 0.5 - 8) * (y + 0.5 - 8));
                    if (d < 6.5) px(PSU, x, y, ((int) d) % 2 == 0 ? rgb(18, 20, 23) : rgb(66, 70, 76));
                }
            }
            rect(PSU, 0, 14, 15, 15, rgb(200, 180, 40));

            // HDD
            fillTile(HDD, 168, 173, 180, 5, r);
            rect(HDD, 3, 4, 12, 11, rgb(232, 232, 232));
            rect(HDD, 4, 6, 11, 6, rgb(60, 60, 60));
            rect(HDD, 4, 8, 9, 8, rgb(60, 60, 60));
            px(HDD, 1, 1, rgb(80, 80, 84));
            px(HDD, 14, 1, rgb(80, 80, 84));
            px(HDD, 1, 14, rgb(80, 80, 84));
            px(HDD, 14, 14, rgb(80, 80, 84));

            // FINS (dissipador)
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) px(FINS, x, y, y % 2 == 0 ? rgb(184, 190, 198) : rgb(110, 116, 124));

            // LED verde / vermelho
            fillTile(LEDG, 57, 255, 106, 0, r);
            fillTile(LEDR, 255, 50, 40, 0, r);

            // SCR_OFF (tela desligada, com reflexo)
            fillTile(SCR_OFF, 7, 9, 12, 0, r);
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++)
                    if ((x + y) == 9 || (x + y) == 10) px(SCR_OFF, x, y, rgb(24, 31, 40));

            // SCR_NOSIG (barras de cor)
            int[] bars = { rgb(192, 192, 192), rgb(192, 192, 0), rgb(0, 192, 192), rgb(0, 192, 0), rgb(192, 0, 192),
                rgb(192, 0, 0), rgb(0, 0, 192) };
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    px(SCR_NOSIG, x, y, y < 11 ? bars[x * 7 / 16] : rgb(16, 16, 16));
                }
            }

            // BEZEL / STAND
            fillTile(BEZEL, 22, 24, 28, 2, r);
            fillTile(STAND, 139, 144, 153, 5, r);
        }

        // =====================================================================
        // DESENHO DE CAIXAS COM UV DO ATLAS
        // =====================================================================
        static final float[] SHADE = { 0.78F, 0.78F, 0.5F, 1.0F, 0.9F, 0.9F };

        /** d: 0=-x 1=+x 2=-y 3=+y 4=-z 5=+z (coordenadas em blocos). */
        static void face(Tessellator t, int d, double x1, double y1, double z1, double x2, double y2, double z2,
            double u0, double v0, double u1, double v1) {
            float s = SHADE[d];
            t.setColorRGBA_F(s, s, s, 1.0F);
            switch (d) {
                case 0:
                    t.addVertexWithUV(x1, y2, z1, u0, v0);
                    t.addVertexWithUV(x1, y2, z2, u1, v0);
                    t.addVertexWithUV(x1, y1, z2, u1, v1);
                    t.addVertexWithUV(x1, y1, z1, u0, v1);
                    break;
                case 1:
                    t.addVertexWithUV(x2, y2, z2, u0, v0);
                    t.addVertexWithUV(x2, y2, z1, u1, v0);
                    t.addVertexWithUV(x2, y1, z1, u1, v1);
                    t.addVertexWithUV(x2, y1, z2, u0, v1);
                    break;
                case 2:
                    t.addVertexWithUV(x1, y1, z1, u0, v0);
                    t.addVertexWithUV(x2, y1, z1, u1, v0);
                    t.addVertexWithUV(x2, y1, z2, u1, v1);
                    t.addVertexWithUV(x1, y1, z2, u0, v1);
                    break;
                case 3:
                    t.addVertexWithUV(x1, y2, z1, u0, v0);
                    t.addVertexWithUV(x2, y2, z1, u1, v0);
                    t.addVertexWithUV(x2, y2, z2, u1, v1);
                    t.addVertexWithUV(x1, y2, z2, u0, v1);
                    break;
                case 4:
                    t.addVertexWithUV(x2, y2, z1, u0, v0);
                    t.addVertexWithUV(x1, y2, z1, u1, v0);
                    t.addVertexWithUV(x1, y1, z1, u1, v1);
                    t.addVertexWithUV(x2, y1, z1, u0, v1);
                    break;
                default:
                    t.addVertexWithUV(x1, y2, z2, u0, v0);
                    t.addVertexWithUV(x2, y2, z2, u1, v0);
                    t.addVertexWithUV(x2, y1, z2, u1, v1);
                    t.addVertexWithUV(x1, y1, z2, u0, v1);
                    break;
            }
        }

        /** Caixa em pixels (0..16), um tile por face (-1 = não desenha). */
        static void box(Tessellator t, double a, double b, double c, double d, double e, double f, int nx, int px,
            int ny, int py, int nz, int pz) {
            a *= K;
            b *= K;
            c *= K;
            d *= K;
            e *= K;
            f *= K;
            int[] tiles = { nx, px, ny, py, nz, pz };
            for (int i = 0; i < 6; i++) {
                int tl = tiles[i];
                if (tl < 0) continue;
                double u0 = (tl & 3) * 0.25 + E, v0 = (tl >> 2) * 0.25 + E;
                face(t, i, a, b, c, d, e, f, u0, v0, u0 + 0.25 - 2 * E, v0 + 0.25 - 2 * E);
            }
        }

        static void box(Tessellator t, double a, double b, double c, double d, double e, double f, int all) {
            box(t, a, b, c, d, e, f, all, all, all, all, all, all);
        }

        // =====================================================================
        // GABINETE
        // =====================================================================
        public static class CaseRenderer extends TileEntitySpecialRenderer {
            @Override
            public void renderTileEntityAt(TileEntity te, double x, double y, double z, float pt) {
                MyMod.TilePcCase tile = (MyMod.TilePcCase) te;
                World w = te.getWorldObj();
                int meta = w != null ? te.getBlockMetadata() : 0;
                boolean linked = w != null ? MyMod.hasBlockNear(w, te.xCoord, te.yCoord, te.zCoord, MyMod.monitor)
                    : tile.demo;

                boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
                float bx = OpenGlHelper.lastBrightnessX, by = OpenGlHelper.lastBrightnessY;

                GL11.glPushMatrix();
                GL11.glTranslated(x + 0.5, y, z + 0.5);
                GL11.glRotatef(-90.0F * meta, 0, 1, 0);
                GL11.glTranslated(-0.5, 0, -0.5);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glColor4f(1, 1, 1, 1);
                Minecraft.getMinecraft().getTextureManager().bindTexture(atlas());

                Tessellator t = Tessellator.instance;

                // --- peças opacas ---
                t.startDrawingQuads();
                box(t, 4, 0, 2, 12, 0.8, 15, METAL); // base
                box(t, 4, 14.2, 2, 12, 15, 15, METAL); // topo
                box(t, 4, 0.8, 14.2, 12, 14.2, 15, METAL); // traseira
                box(t, 11.7, 0.8, 3, 12, 14.2, 14.2, METAL); // lateral direita (sólida)
                box(t, 4, 0.8, 2, 12, 14.2, 3, METAL, METAL, METAL, METAL, MESH, METAL); // frente com grade
                box(t, 5, 12.4, 1.8, 6.6, 13.8, 2, STAND); // botão power

                box(t, 11, 4.2, 3.8, 11.7, 13.8, 13.8, MOBO); // placa-mãe
                box(t, 8.2, 9.4, 7.2, 11, 12.4, 10.4, FAN, FINS, FINS, FINS, FINS, FINS); // cooler
                box(t, 10.2, 9.6, 11.2, 11, 13.6, 11.7, RAM); // RAM 1
                box(t, 10.2, 9.6, 12.2, 11, 13.6, 12.7, RAM); // RAM 2
                box(t, 9.4, 4.6, 4.5, 10.8, 8.4, 12.5, GPU, METAL, METAL, METAL, METAL, METAL); // placa de vídeo
                box(t, 5, 0.8, 8.5, 11, 4, 14.2, PSU); // fonte
                box(t, 5, 0.8, 3.4, 8.8, 3.4, 8.2, HDD, METAL, METAL, METAL, METAL, METAL); // HD
                box(t, 5, 4.5, 3, 9, 8.5, 4.3, METAL, METAL, METAL, METAL, FAN, FAN); // fan frontal 1
                box(t, 5, 9, 3, 9, 13, 4.3, METAL, METAL, METAL, METAL, FAN, FAN); // fan frontal 2
                box(t, 5, 9.4, 13, 9, 13.4, 14.2, METAL, METAL, METAL, METAL, FAN, FAN); // fan traseira
                t.draw();

                // --- peças que brilham (LED) ---
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
                t.startDrawingQuads();
                int led = linked ? LEDG : LEDR;
                box(t, 10.4, 12.8, 1.8, 11.2, 13.4, 2, led);
                box(t, 9.3, 8.4, 4.6, 9.4, 8.6, 12.4, LEDG); // fita de LED da GPU
                t.draw();
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);

                // --- vidro lateral ---
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                t.startDrawingQuads();
                box(t, 4, 0.8, 3, 4.3, 14.2, 14.2, GLASS);
                t.draw();
                GL11.glDisable(GL11.GL_BLEND);

                GL11.glEnable(GL11.GL_CULL_FACE);
                if (lighting) GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glPopMatrix();
            }
        }

        // =====================================================================
        // MONITOR
        // =====================================================================
        public static class MonitorRenderer extends TileEntitySpecialRenderer {
            @Override
            public void renderTileEntityAt(TileEntity te, double x, double y, double z, float pt) {
                MyMod.TileMonitor tile = (MyMod.TileMonitor) te;
                World w = te.getWorldObj();
                int meta = w != null ? te.getBlockMetadata() : 0;
                boolean on = w != null ? MyMod.hasBlockNear(w, te.xCoord, te.yCoord, te.zCoord, MyMod.pcCase)
                    : tile.demo;

                boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
                float bx = OpenGlHelper.lastBrightnessX, by = OpenGlHelper.lastBrightnessY;

                GL11.glPushMatrix();
                GL11.glTranslated(x + 0.5, y, z + 0.5);
                GL11.glRotatef(-90.0F * meta, 0, 1, 0);
                GL11.glTranslated(-0.5, 0, -0.5);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glColor4f(1, 1, 1, 1);

                Tessellator t = Tessellator.instance;
                Minecraft mc = Minecraft.getMinecraft();
                mc.getTextureManager().bindTexture(atlas());

                // corpo
                t.startDrawingQuads();
                box(t, 5, 0, 6, 11, 0.8, 10, STAND); // base
                box(t, 7.2, 0.8, 7.5, 8.8, 5, 9, STAND); // pescoço
                box(t, 2, 5, 7, 14, 15, 9, BEZEL); // moldura
                box(t, 4, 6, 9, 12, 14, 10.5, BEZEL); // parte de trás
                if (!on) {
                    double u0 = (SCR_OFF & 3) * 0.25, v0 = (SCR_OFF >> 2) * 0.25;
                    face(t, 4, 3 * K, 6 * K, 6.98 * K, 13 * K, 14 * K, 6.98 * K, u0, v0, u0 + 0.25, v0 + 0.25);
                }
                t.draw();

                if (on) {
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
                    // LED de energia
                    t.startDrawingQuads();
                    box(t, 11.4, 5.3, 6.9, 12.6, 5.8, 7, LEDG);
                    t.draw();

                    // tela: foto ou barras de cor
                    double u0 = 0, v0 = 0, u1 = 1, v1 = 1;
                    if (tile.photo != null) {
                        mc.getTextureManager().bindTexture(tile.photo);
                    } else {
                        u0 = (SCR_NOSIG & 3) * 0.25;
                        v0 = (SCR_NOSIG >> 2) * 0.25;
                        u1 = u0 + 0.25;
                        v1 = v0 + 0.25;
                    }
                    t.startDrawingQuads();
                    face(t, 4, 3 * K, 6 * K, 6.98 * K, 13 * K, 14 * K, 6.98 * K, u0, v0, u1, v1);
                    t.draw();
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);
                }

                GL11.glEnable(GL11.GL_CULL_FACE);
                if (lighting) GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glPopMatrix();
            }
        }

        // =====================================================================
        // ITEM (inventário / mão / chão)
        // =====================================================================
        public static class TesrItem implements IItemRenderer {
            private final TileEntitySpecialRenderer renderer;
            private final TileEntity dummy;

            public TesrItem(TileEntitySpecialRenderer renderer, TileEntity dummy) {
                this.renderer = renderer;
                this.dummy = dummy;
            }

            @Override
            public boolean handleRenderType(ItemStack item, ItemRenderType type) {
                return true;
            }

            @Override
            public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
                return true;
            }

            @Override
            public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
                GL11.glPushMatrix();
                if (type == ItemRenderType.INVENTORY || type == ItemRenderType.ENTITY) {
                    GL11.glRotatef(180.0F, 0, 1, 0);
                }
                GL11.glTranslated(-0.5, -0.5, -0.5);
                renderer.renderTileEntityAt(dummy, 0.0D, 0.0D, 0.0D, 0.0F);
                GL11.glPopMatrix();
            }
        }

        // =====================================================================
        // FOTO DO PLAYER
        // =====================================================================
        static final int PW = 160, PH = 128;

        static void chat(String s) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(s));
        }

        public static void takePhoto(World w, int bx, int by, int bz, MyMod.TileMonitor tile) {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer p = mc.thePlayer;
            if (p == null) return;

            if (!MyMod.hasBlockNear(w, bx, by, bz, MyMod.pcCase)) {
                chat("Coloque o monitor ao lado de um gabinete de PC.");
                return;
            }
            if (!OpenGlHelper.isFramebufferEnabled()) {
                chat("Seu sistema não suporta framebuffer (ative nas opções de vídeo).");
                return;
            }

            ByteBuffer buf = BufferUtils.createByteBuffer(PW * PH * 4);
            Framebuffer fb = new Framebuffer(PW, PH, true);
            boolean ok = false;
            try {
                fb.setFramebufferColor(0.16F, 0.20F, 0.26F, 1.0F);
                fb.framebufferClear();
                fb.bindFramebuffer(true);

                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPushMatrix();
                GL11.glLoadIdentity();
                GL11.glOrtho(0.0D, PW, PH, 0.0D, 1000.0D, 3000.0D);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPushMatrix();
                GL11.glLoadIdentity();
                GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
                GL11.glEnable(GL11.GL_DEPTH_TEST);

                drawPlayer(p, PW / 2.0F, PH - 8.0F, 60.0F);

                GL11.glReadPixels(0, 0, PW, PH, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
                ok = true;

                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
            } catch (Throwable ex) {
                chat("Falha ao tirar a foto: " + ex);
            } finally {
                fb.unbindFramebuffer();
                fb.deleteFramebuffer();
                mc.getFramebuffer().bindFramebuffer(true);
            }
            if (!ok) return;

            DynamicTexture dyn = new DynamicTexture(PW, PH);
            int[] data = dyn.getTextureData();
            for (int y = 0; y < PH; y++) {
                for (int x = 0; x < PW; x++) {
                    int i = ((PH - 1 - y) * PW + x) * 4; // OpenGL lê de baixo para cima
                    int r = buf.get(i) & 255, g = buf.get(i + 1) & 255, b = buf.get(i + 2) & 255;
                    data[y * PW + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
            dyn.updateDynamicTexture();

            if (tile.photo != null) mc.getTextureManager().deleteTexture(tile.photo);
            tile.photo = mc.getTextureManager().getDynamicTextureLocation("mypc_photo", dyn);

            w.playSound(bx + 0.5, by + 0.5, bz + 0.5, "random.click", 1.0F, 1.6F, false);
            chat("Foto tirada! Ela aparece no monitor.");
        }

        /** Desenha o jogador de frente (mesmo método do inventário do jogo). */
        static void drawPlayer(EntityPlayer e, float x, float y, float scale) {
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 50.0F);
            GL11.glScalef(-scale, scale, scale);
            GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);

            float ro = e.renderYawOffset, ry = e.rotationYaw, rp = e.rotationPitch;
            float ph = e.prevRotationYawHead, rh = e.rotationYawHead;

            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            RenderHelper.enableStandardItemLighting();
            GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);

            e.renderYawOffset = 12.0F;
            e.rotationYaw = 22.0F;
            e.rotationPitch = 0.0F;
            e.rotationYawHead = e.rotationYaw;
            e.prevRotationYawHead = e.rotationYaw;

            GL11.glTranslatef(0.0F, e.yOffset, 0.0F);
            RenderManager.instance.playerViewY = 180.0F;
            RenderManager.instance.renderEntityWithPosYaw(e, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);

            e.renderYawOffset = ro;
            e.rotationYaw = ry;
            e.rotationPitch = rp;
            e.prevRotationYawHead = ph;
            e.rotationYawHead = rh;

            GL11.glPopMatrix();
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }
    }
}
