package com.myname.mymodid;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

@Mod(modid = Tags.MODID, name = Tags.MODNAME, version = Tags.VERSION)
public class MyMod {

    public static final Logger LOG = LogManager.getLogger(Tags.MODID);

    public static Block gabineteBlock;
    public static Block monitorBlock;
    public static Item ramItem;
    public static Item processadorItem;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        gabineteBlock = new BlockGabinete().setBlockName("gabinete").setCreativeTab(CreativeTabs.tabBlock);
        monitorBlock  = new BlockMonitor().setBlockName("monitor").setCreativeTab(CreativeTabs.tabBlock);
        GameRegistry.registerBlock(gabineteBlock, "gabinete");
        GameRegistry.registerBlock(monitorBlock,  "monitor");

        ramItem         = new ItemRAM().setUnlocalizedName("ram").setCreativeTab(CreativeTabs.tabMisc).setTextureName("egg");
        processadorItem = new ItemProcessador().setUnlocalizedName("processador").setCreativeTab(CreativeTabs.tabMisc).setTextureName("egg");
        GameRegistry.registerItem(ramItem, "ram");
        GameRegistry.registerItem(processadorItem, "processador");

        GameRegistry.registerTileEntity(TileEntityGabinete.class, "mymodid.gabinete");
        GameRegistry.registerTileEntity(TileEntityMonitor.class,  "mymodid.monitor");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide() == Side.CLIENT) {
            ClientSetup.register();
        }
    }

    // tudo de cliente fica aqui dentro, o server ignora essa classe inteira
    @SideOnly(Side.CLIENT)
    public static class ClientSetup {
        public static void register() {
            ClientRegistry.bindTileEntitySpecialRenderer(TileEntityGabinete.class, new RenderGabinete());
            ClientRegistry.bindTileEntitySpecialRenderer(TileEntityMonitor.class,  new RenderMonitor());

            MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(gabineteBlock), new RenderItemGabinete());
            MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(monitorBlock),  new RenderItemMonitor());
            MinecraftForgeClient.registerItemRenderer(ramItem,         new RenderItemRAM());
            MinecraftForgeClient.registerItemRenderer(processadorItem, new RenderItemCPU());
        }
    }

    // ========== BLOCOS ==========
    public static class BlockGabinete extends Block implements ITileEntityProvider {
        public BlockGabinete() {
            super(Material.iron);
            setHardness(2.0F);
            setStepSound(soundTypeMetal);
            setBlockBounds(0.15F, 0.0F, 0.15F, 0.85F, 1.0F, 0.85F);
        }
        @Override public TileEntity createNewTileEntity(World w, int meta) { return new TileEntityGabinete(); }
        @Override public int getRenderType() { return -1; }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public boolean renderAsNormalBlock() { return false; }

        @Override
        public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
            int dir = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
            world.setBlockMetadataWithNotify(x, y, z, dir, 2);
        }

        @Override
        public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player,
                                        int side, float hx, float hy, float hz) {
            if (player.isSneaking() && player.getHeldItem() == null) {
                if (!world.isRemote) {
                    int meta = world.getBlockMetadata(x, y, z);
                    world.setBlockMetadataWithNotify(x, y, z, (meta + 1) & 3, 2);
                }
                return true;
            }
            if (world.isRemote) return true;
            TileEntity te = world.getTileEntity(x, y, z);
            if (!(te instanceof TileEntityGabinete)) return false;
            TileEntityGabinete gab = (TileEntityGabinete) te;
            ItemStack held = player.getHeldItem();
            if (held != null) {
                if (held.getItem() == ramItem && !gab.temRAM) {
                    gab.temRAM = true;
                    if (!player.capabilities.isCreativeMode) held.stackSize--;
                    world.markBlockForUpdate(x, y, z);
                    return true;
                }
                if (held.getItem() == processadorItem && !gab.temProcessador) {
                    gab.temProcessador = true;
                    if (!player.capabilities.isCreativeMode) held.stackSize--;
                    world.markBlockForUpdate(x, y, z);
                    return true;
                }
            }
            return false;
        }
    }

    public static class BlockMonitor extends Block implements ITileEntityProvider {
        public BlockMonitor() {
            super(Material.iron);
            setHardness(2.0F);
            setStepSound(soundTypeMetal);
            setBlockBounds(0.10F, 0.0F, 0.10F, 0.90F, 0.90F, 0.90F);
        }
        @Override public TileEntity createNewTileEntity(World w, int meta) { return new TileEntityMonitor(); }
        @Override public int getRenderType() { return -1; }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public boolean renderAsNormalBlock() { return false; }

        @Override
        public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
            int dir = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
            world.setBlockMetadataWithNotify(x, y, z, dir, 2);
        }

        @Override
        public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player,
                                        int side, float hx, float hy, float hz) {
            if (player.isSneaking() && player.getHeldItem() == null) {
                if (!world.isRemote) {
                    int meta = world.getBlockMetadata(x, y, z);
                    world.setBlockMetadataWithNotify(x, y, z, (meta + 1) & 3, 2);
                }
                return true;
            }
            return false;
        }
    }

    // ========== ITENS ==========
    public static class ItemRAM extends Item {
        public ItemRAM() { super(); maxStackSize = 16; }
        @Override public String getItemStackDisplayName(ItemStack stack) { return "Memória RAM"; }
    }
    public static class ItemProcessador extends Item {
        public ItemProcessador() { super(); maxStackSize = 16; }
        @Override public String getItemStackDisplayName(ItemStack stack) { return "Processador"; }
    }

    // ========== TILE ENTITIES ==========
    public static class TileEntityGabinete extends TileEntity {
        public boolean temRAM = false;
        public boolean temProcessador = false;

        @Override public void writeToNBT(NBTTagCompound nbt) {
            super.writeToNBT(nbt);
            nbt.setBoolean("temRAM", temRAM);
            nbt.setBoolean("temProcessador", temProcessador);
        }
        @Override public void readFromNBT(NBTTagCompound nbt) {
            super.readFromNBT(nbt);
            temRAM = nbt.getBoolean("temRAM");
            temProcessador = nbt.getBoolean("temProcessador");
        }
        public boolean estaLigado() { return temRAM && temProcessador; }
    }

    public static class TileEntityMonitor extends TileEntity {
        public boolean ligado = false;

        @Override
        public void updateEntity() {
            if (worldObj.isRemote) return;
            boolean achou = false, gLigado = false;
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                Block b = worldObj.getBlock(xCoord + d.offsetX, yCoord + d.offsetY, zCoord + d.offsetZ);
                if (b == gabineteBlock) {
                    achou = true;
                    TileEntity te = worldObj.getTileEntity(xCoord + d.offsetX, yCoord + d.offsetY, zCoord + d.offsetZ);
                    if (te instanceof TileEntityGabinete && ((TileEntityGabinete) te).estaLigado()) gLigado = true;
                }
            }
            boolean novo = achou && gLigado;
            if (ligado != novo) {
                ligado = novo;
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }
        @Override public void writeToNBT(NBTTagCompound nbt) {
            super.writeToNBT(nbt);
            nbt.setBoolean("ligado", ligado);
        }
        @Override public void readFromNBT(NBTTagCompound nbt) {
            super.readFromNBT(nbt);
            ligado = nbt.getBoolean("ligado");
        }
    }

    // ========== HELPER 3D ==========
    @SideOnly(Side.CLIENT)
    public static void box(float x1, float y1, float z1, float x2, float y2, float z2,
                           float r, float g, float b) {
        GL11.glColor3f(r, g, b);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(x1, y1, z1); GL11.glVertex3f(x2, y1, z1);
        GL11.glVertex3f(x2, y1, z2); GL11.glVertex3f(x1, y1, z2);
        GL11.glVertex3f(x1, y2, z1); GL11.glVertex3f(x1, y2, z2);
        GL11.glVertex3f(x2, y2, z2); GL11.glVertex3f(x2, y2, z1);
        GL11.glVertex3f(x1, y1, z1); GL11.glVertex3f(x1, y2, z1);
        GL11.glVertex3f(x2, y2, z1); GL11.glVertex3f(x2, y1, z1);
        GL11.glVertex3f(x1, y1, z2); GL11.glVertex3f(x2, y1, z2);
        GL11.glVertex3f(x2, y2, z2); GL11.glVertex3f(x1, y2, z2);
        GL11.glVertex3f(x1, y1, z1); GL11.glVertex3f(x1, y1, z2);
        GL11.glVertex3f(x1, y2, z2); GL11.glVertex3f(x1, y2, z1);
        GL11.glVertex3f(x2, y1, z1); GL11.glVertex3f(x2, y2, z1);
        GL11.glVertex3f(x2, y2, z2); GL11.glVertex3f(x2, y1, z2);
        GL11.glEnd();
    }

    // ========== TESR: GABINETE ==========
    @SideOnly(Side.CLIENT)
    public static class RenderGabinete extends TileEntitySpecialRenderer {
        @Override
        public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
            if (!(te instanceof TileEntityGabinete)) return;
            TileEntityGabinete gab = (TileEntityGabinete) te;

            GL11.glPushMatrix();
            GL11.glTranslated(x + 0.5, y, z + 0.5);
            GL11.glRotatef(te.getBlockMetadata() * 90F, 0F, 1F, 0F);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);

            box(-0.35F, 0.00F, -0.35F,  0.35F, 1.00F,  0.35F, 0.22F, 0.22F, 0.25F);
            box(-0.32F, 0.02F,  0.33F,  0.32F, 0.98F,  0.36F, 0.35F, 0.35F, 0.38F);
            box(-0.32F, 0.86F,  0.36F,  0.32F, 0.90F,  0.37F, 0.18F, 0.18F, 0.20F);
            box( 0.20F, 0.90F, 0.36F,  0.28F, 0.95F,  0.38F, 0.75F, 0.10F, 0.10F);
            box( 0.10F, 0.91F, 0.36F,  0.14F, 0.94F,  0.38F,
                 gab.estaLigado() ? 0.10F : 0.20F,
                 gab.estaLigado() ? 1.00F : 0.20F,
                 gab.estaLigado() ? 0.10F : 0.20F);

            box(-0.30F, 0.55F, 0.20F, -0.10F, 0.75F, 0.30F,
                gab.temRAM ? 0.15F : 0.08F,
                gab.temRAM ? 0.85F : 0.08F,
                gab.temRAM ? 0.20F : 0.08F);

            box( 0.05F, 0.55F, 0.20F,  0.25F, 0.75F, 0.30F,
                gab.temProcessador ? 0.80F : 0.08F,
                gab.temProcessador ? 0.80F : 0.08F,
                gab.temProcessador ? 0.85F : 0.08F);

            for (int i = 0; i < 5; i++) {
                float yy = 0.10F + i * 0.08F;
                box(-0.28F, yy, 0.37F, 0.05F, yy + 0.03F, 0.38F, 0.10F, 0.10F, 0.10F);
            }

            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
    }

    // ========== TESR: MONITOR ==========
    @SideOnly(Side.CLIENT)
    public static class RenderMonitor extends TileEntitySpecialRenderer {
        @Override
        public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
            if (!(te instanceof TileEntityMonitor)) return;
            TileEntityMonitor mon = (TileEntityMonitor) te;

            GL11.glPushMatrix();
            GL11.glTranslated(x + 0.5, y, z + 0.5);
            GL11.glRotatef(te.getBlockMetadata() * 90F, 0F, 1F, 0F);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);

            box(-0.18F, 0.00F, -0.10F,  0.18F, 0.05F,  0.10F, 0.12F, 0.12F, 0.12F);
            box(-0.05F, 0.05F, -0.05F,  0.05F, 0.38F,  0.05F, 0.12F, 0.12F, 0.12F);
            box(-0.40F, 0.38F, -0.12F,  0.40F, 0.90F,  0.05F, 0.08F, 0.08F, 0.08F);
            box(-0.40F, 0.38F,  0.05F,  0.40F, 0.90F,  0.08F, 0.16F, 0.16F, 0.16F);

            if (mon.ligado) {
                box(-0.35F, 0.42F, 0.08F, 0.35F, 0.86F, 0.09F, 0.10F, 0.45F, 0.85F);
                box(-0.30F, 0.46F, 0.09F, 0.30F, 0.50F, 0.10F, 0.45F, 0.85F, 1.00F);
                box(-0.30F, 0.55F, 0.09F, 0.20F, 0.59F, 0.10F, 0.45F, 0.85F, 1.00F);
                box(-0.30F, 0.64F, 0.09F, 0.26F, 0.68F, 0.10F, 0.45F, 0.85F, 1.00F);
                box(-0.30F, 0.73F, 0.09F, 0.18F, 0.77F, 0.10F, 0.45F, 0.85F, 1.00F);
            } else {
                box(-0.35F, 0.42F, 0.08F, 0.35F, 0.86F, 0.09F, 0.06F, 0.06F, 0.06F);
            }

            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
    }

    // ========== IItemRenderer: RAM ==========
    @SideOnly(Side.CLIENT)
    public static class RenderItemRAM implements IItemRenderer {
        @Override public boolean handleRenderType(ItemStack item, ItemRenderType type) { return true; }
        @Override public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
            return helper == ItemRendererHelper.ENTITY_BOBBING || helper == ItemRendererHelper.ENTITY_ROTATION;
        }
        @Override
        public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);

            if (type == ItemRenderType.INVENTORY) {
                GL11.glTranslatef(8F, 8F, 0F);
                GL11.glScalef(13F, 13F, 13F);
                GL11.glRotatef(25F, 1F, 0F, 0F);
                GL11.glRotatef(45F, 0F, 1F, 0F);
                GL11.glTranslatef(-0.5F, -0.05F, -0.15F);
            } else if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                GL11.glTranslatef(0.5F, 0.4F, 0.5F);
                GL11.glRotatef(-90F, 0F, 1F, 0F);
                GL11.glTranslatef(-0.5F, -0.03F, -0.15F);
            } else if (type == ItemRenderType.ENTITY) {
                GL11.glTranslatef(-0.5F, -0.03F, -0.15F);
            }

            box(0.00F, 0.00F, 0.00F, 1.00F, 0.04F, 0.30F, 0.10F, 0.45F, 0.15F);
            for (int i = 0; i < 8; i++) {
                float px = 0.05F + i * 0.11F;
                box(px, 0.04F, 0.00F, px + 0.06F, 0.05F, 0.02F, 0.90F, 0.75F, 0.10F);
            }
            for (int i = 0; i < 4; i++) {
                float px = 0.10F + i * 0.22F;
                box(px, 0.04F, 0.08F, px + 0.18F, 0.10F, 0.22F, 0.05F, 0.05F, 0.05F);
            }
            box(0.35F, 0.04F, 0.06F, 0.65F, 0.12F, 0.24F, 0.05F, 0.05F, 0.05F);

            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
    }

    // ========== IItemRenderer: CPU ==========
    @SideOnly(Side.CLIENT)
    public static class RenderItemCPU implements IItemRenderer {
        @Override public boolean handleRenderType(ItemStack item, ItemRenderType type) { return true; }
        @Override public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
            return helper == ItemRendererHelper.ENTITY_BOBBING || helper == ItemRendererHelper.ENTITY_ROTATION;
        }
        @Override
        public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);

            if (type == ItemRenderType.INVENTORY) {
                GL11.glTranslatef(8F, 8F, 0F);
                GL11.glScalef(15F, 15F, 15F);
                GL11.glRotatef(25F, 1F, 0F, 0F);
                GL11.glRotatef(45F, 0F, 1F, 0F);
                GL11.glTranslatef(-0.5F, -0.05F, -0.5F);
            } else if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                GL11.glTranslatef(0.5F, 0.4F, 0.5F);
                GL11.glRotatef(-90F, 0F, 1F, 0F);
                GL11.glTranslatef(-0.5F, -0.03F, -0.5F);
            } else if (type == ItemRenderType.ENTITY) {
                GL11.glTranslatef(-0.5F, -0.03F, -0.5F);
            }

            box(0.00F, 0.00F, 0.00F, 1.00F, 0.04F, 1.00F, 0.10F, 0.45F, 0.15F);
            for (int i = 0; i < 10; i++) {
                float px = 0.05F + i * 0.09F;
                box(px, 0.00F, -0.03F, px + 0.05F, 0.03F, 0.00F, 0.90F, 0.75F, 0.10F);
                box(px, 0.00F,  1.00F, px + 0.05F, 0.03F, 1.03F, 0.90F, 0.75F, 0.10F);
                box(-0.03F, 0.00F, px, 0.00F, 0.03F, px + 0.05F, 0.90F, 0.75F, 0.10F);
                box( 1.00F, 0.00F, px, 1.03F, 0.03F, px + 0.05F, 0.90F, 0.75F, 0.10F);
            }
            box(0.20F, 0.04F, 0.20F, 0.80F, 0.11F, 0.80F, 0.75F, 0.75F, 0.80F);
            box(0.35F, 0.11F, 0.35F, 0.65F, 0.13F, 0.65F, 0.60F, 0.60F, 0.65F);
            box(0.05F, 0.04F, 0.05F, 0.15F, 0.08F, 0.15F, 0.25F, 0.25F, 0.25F);
            box(0.85F, 0.04F, 0.05F, 0.95F, 0.08F, 0.15F, 0.25F, 0.25F, 0.25F);

            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
    }

    // ========== IItemRenderer: Gabinete ==========
    @SideOnly(Side.CLIENT)
    public static class RenderItemGabinete implements IItemRenderer {
        @Override public boolean handleRenderType(ItemStack item, ItemRenderType type) { return true; }
        @Override public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
            return helper == ItemRendererHelper.ENTITY_BOBBING || helper == ItemRendererHelper.ENTITY_ROTATION;
        }
        @Override
        public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);

            if (type == ItemRenderType.INVENTORY) {
                GL11.glTranslatef(8F, 8F, 0F);
                GL11.glScalef(13F, 13F, 13F);
                GL11.glRotatef(20F, 1F, 0F, 0F);
                GL11.glRotatef(35F, 0F, 1F, 0F);
                GL11.glTranslatef(-0.5F, -0.05F, -0.5F);
            } else if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                GL11.glTranslatef(0.6F, 0.4F, 0.5F);
                GL11.glRotatef(-90F, 0F, 1F, 0F);
                GL11.glTranslatef(-0.5F, 0F, -0.5F);
            } else if (type == ItemRenderType.ENTITY) {
                GL11.glTranslatef(-0.5F, 0F, -0.5F);
            }

            box(0.15F, 0.00F, 0.15F, 0.85F, 1.00F, 0.85F, 0.22F, 0.22F, 0.25F);
            box(0.18F, 0.02F, 0.83F, 0.82F, 0.98F, 0.86F, 0.35F, 0.35F, 0.38F);
            box(0.70F, 0.90F, 0.86F, 0.78F, 0.95F, 0.88F, 0.75F, 0.10F, 0.10F);
            for (int i = 0; i < 5; i++) {
                float yy = 0.10F + i * 0.08F;
                box(0.22F, yy, 0.87F, 0.55F, yy + 0.03F, 0.88F, 0.10F, 0.10F, 0.10F);
            }

            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
    }

    // ========== IItemRenderer: Monitor ==========
    @SideOnly(Side.CLIENT)
    public static class RenderItemMonitor implements IItemRenderer {
        @Override public boolean handleRenderType(ItemStack item, ItemRenderType type) { return true; }
        @Override public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
            return helper == ItemRendererHelper.ENTITY_BOBBING || helper == ItemRendererHelper.ENTITY_ROTATION;
        }
        @Override
        public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);

            if (type == ItemRenderType.INVENTORY) {
                GL11.glTranslatef(8F, 8F, 0F);
                GL11.glScalef(13F, 13F, 13F);
                GL11.glRotatef(20F, 1F, 0F, 0F);
                GL11.glRotatef(35F, 0F, 1F, 0F);
                GL11.glTranslatef(-0.5F, -0.05F, -0.5F);
            } else if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                GL11.glTranslatef(0.6F, 0.4F, 0.5F);
                GL11.glRotatef(-90F, 0F, 1F, 0F);
                GL11.glTranslatef(-0.5F, 0F, -0.5F);
            } else if (type == ItemRenderType.ENTITY) {
                GL11.glTranslatef(-0.5F, 0F, -0.5F);
            }

            box(0.32F, 0.00F, 0.35F, 0.68F, 0.05F, 0.65F, 0.12F, 0.12F, 0.12F);
            box(0.45F, 0.05F, 0.45F, 0.55F, 0.38F, 0.55F, 0.12F, 0.12F, 0.12F);
            box(0.10F, 0.38F, 0.40F, 0.90F, 0.90F, 0.60F, 0.08F, 0.08F, 0.08F);
            box(0.10F, 0.38F, 0.60F, 0.90F, 0.90F, 0.63F, 0.16F, 0.16F, 0.16F);
            box(0.15F, 0.42F, 0.63F, 0.85F, 0.86F, 0.64F, 0.06F, 0.06F, 0.06F);

            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
    }
}
