package com.foguetes3d;

import java.util.List;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Foguetes 3D - Mod completo para Minecraft 1.7.10 (Forge)
 * Adiciona foguetes com modelo 3D, trilha de fumaça/fogo, explosão e receita.
 *
 * Como usar:
 * 1. Coloque este arquivo em: src/main/java/com/foguetes3d/Foguetes3D.java
 * 2. Compile com o template do Forge 1.7.10
 * 3. No jogo: craft o Foguete (pólvora + papel + pó de blaze) e clique com o botão direito
 */
@Mod(modid = Foguetes3D.MODID, name = "Foguetes 3D", version = "1.0", acceptedMinecraftVersions = "[1.7.10]")
public class Foguetes3D {

    public static final String MODID = "foguetes3d";

    @Instance(MODID)
    public static Foguetes3D instance;

    public static Item itemFoguete;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Item
        itemFoguete = new ItemFoguete();
        GameRegistry.registerItem(itemFoguete, "foguete");

        // Entidade
        EntityRegistry.registerModEntity(EntityFoguete.class, "Foguete", 0, this, 64, 1, true);

        // Render só no client
        if (FMLCommonHandler.instance().getSide().isClient()) {
            RenderingRegistry.registerEntityRenderingHandler(EntityFoguete.class, new RenderFoguete());
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Nome em português
        LanguageRegistry.instance().addStringLocalization("item.foguete.name", "Foguete 3D");

        // Receita: 1 pólvora + 1 papel + 1 pó de blaze = 2 foguetes
        GameRegistry.addShapelessRecipe(new ItemStack(itemFoguete, 2),
                Items.gunpowder,
                Items.paper,
                Items.blaze_powder);
    }

    // ============================================================================================
    // ITEM DO FOGUETE
    // ============================================================================================
    public static class ItemFoguete extends Item {

        public ItemFoguete() {
            setUnlocalizedName("foguete");
            setCreativeTab(CreativeTabs.tabCombat);
            setMaxStackSize(16);
        }

        @Override
        public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
            if (!player.capabilities.isCreativeMode) {
                --stack.stackSize;
            }

            world.playSoundAtEntity(player, "fireworks.launch", 1.0F, 0.9F + world.rand.nextFloat() * 0.2F);

            if (!world.isRemote) {
                EntityFoguete foguete = new EntityFoguete(world, player);
                world.spawnEntityInWorld(foguete);
            }

            return stack;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerIcons(net.minecraft.client.renderer.texture.IIconRegister reg) {
            // Não precisa de textura própria - usamos o ícone dos fogos de artifício
        }

        @Override
        @SideOnly(Side.CLIENT)
        public net.minecraft.util.IIcon getIconFromDamage(int damage) {
            return Items.fireworks.getIconFromDamage(0);
        }
    }

    // ============================================================================================
    // ENTIDADE DO FOGUETE
    // ============================================================================================
    public static class EntityFoguete extends Entity {

        private int life = 0;
        private static final int MAX_LIFE = 80; // ~4 segundos
        private EntityPlayer shooter;

        public EntityFoguete(World world) {
            super(world);
            setSize(0.4F, 0.4F);
            this.yOffset = 0.0F;
        }

        public EntityFoguete(World world, EntityPlayer player) {
            this(world);
            this.shooter = player;

            // Posição um pouco à frente e acima do olho do jogador
            setLocationAndAngles(
                    player.posX,
                    player.posY + player.getEyeHeight() - 0.1D,
                    player.posZ,
                    player.rotationYaw,
                    player.rotationPitch);

            // Lançamento na direção que o jogador está olhando
            float speed = 1.8F;
            this.motionX = -MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI)
                    * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI) * speed;
            this.motionZ = MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI)
                    * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI) * speed;
            this.motionY = -MathHelper.sin(this.rotationPitch / 180.0F * (float) Math.PI) * speed;

            // Pequeno impulso extra pra cima
            this.motionY += 0.15D;
        }

        @Override
        protected void entityInit() {
            // Nada especial
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            life++;

            // Trilha de partículas
            if (worldObj.isRemote) {
                // Fumaça
                for (int i = 0; i < 2; i++) {
                    worldObj.spawnParticle("smoke",
                            posX + (rand.nextDouble() - 0.5D) * 0.2D,
                            posY + (rand.nextDouble() - 0.5D) * 0.2D,
                            posZ + (rand.nextDouble() - 0.5D) * 0.2D,
                            0.0D, 0.0D, 0.0D);
                }
                // Fogo enquanto ainda tem empuxo
                if (life < 35) {
                    worldObj.spawnParticle("flame",
                            posX + (rand.nextDouble() - 0.5D) * 0.15D,
                            posY + (rand.nextDouble() - 0.5D) * 0.15D,
                            posZ + (rand.nextDouble() - 0.5D) * 0.15D,
                            0.0D, 0.0D, 0.0D);
                }
            }

            // Movimento
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;

            this.posX += this.motionX;
            this.posY += this.motionY;
            this.posZ += this.motionZ;

            // Arrasto leve e gravidade
            this.motionX *= 0.99D;
            this.motionY *= 0.99D;
            this.motionZ *= 0.99D;
            this.motionY -= 0.03D; // gravidade

            // Empuxo inicial (acelera um pouco nos primeiros ticks)
            if (life < 25) {
                this.motionY += 0.04D;
            }

            setPosition(this.posX, this.posY, this.posZ);

            // Rotação visual baseada na velocidade
            float f = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
            this.rotationYaw = (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);
            this.rotationPitch = (float) (Math.atan2(this.motionY, (double) f) * 180.0D / Math.PI);

            // Colisão com blocos
            Vec3 start = Vec3.createVectorHelper(this.prevPosX, this.prevPosY, this.prevPosZ);
            Vec3 end = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
            MovingObjectPosition mop = worldObj.rayTraceBlocks(start, end);

            if (mop != null) {
                explode();
                return;
            }

            // Colisão com entidades (exceto o atirador nos primeiros ticks)
            AxisAlignedBB box = this.boundingBox.expand(0.3D, 0.3D, 0.3D);
            List list = worldObj.getEntitiesWithinAABBExcludingEntity(this, box);
            for (Object o : list) {
                Entity e = (Entity) o;
                if (e == shooter && life < 10) continue;
                if (e.canBeCollidedWith()) {
                    explode();
                    return;
                }
            }

            // Tempo de vida esgotado
            if (life >= MAX_LIFE) {
                explode();
            }
        }

        private void explode() {
            if (!worldObj.isRemote) {
                // Explosão pequena (não quebra muitos blocos)
                worldObj.createExplosion(this, this.posX, this.posY, this.posZ, 1.8F, true);
                // Dano em área leve
                List list = worldObj.getEntitiesWithinAABBExcludingEntity(this,
                        this.boundingBox.expand(3.0D, 3.0D, 3.0D));
                for (Object o : list) {
                    Entity e = (Entity) o;
                    if (e instanceof EntityPlayer || e.canBeCollidedWith()) {
                        double dist = e.getDistance(this.posX, this.posY, this.posZ);
                        if (dist < 4.0D) {
                            float dmg = (float) (6.0D - dist * 1.2D);
                            if (dmg > 0) {
                                e.attackEntityFrom(DamageSource.setExplosionSource(null), dmg);
                            }
                        }
                    }
                }
            }
            setDead();
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound tag) {
            this.life = tag.getInteger("Life");
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound tag) {
            tag.setInteger("Life", this.life);
        }

        @Override
        public boolean canBeCollidedWith() {
            return true;
        }

        @Override
        public float getCollisionBorderSize() {
            return 0.3F;
        }
    }

    // ============================================================================================
    // RENDER 3D DO FOGUETE (modelo feito com Tessellator)
    // ============================================================================================
    @SideOnly(Side.CLIENT)
    public static class RenderFoguete extends Render {

        @Override
        public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
            EntityFoguete foguete = (EntityFoguete) entity;

            GL11.glPushMatrix();
            GL11.glTranslatef((float) x, (float) y, (float) z);

            // Orientar o modelo na direção do movimento
            GL11.glRotatef(foguete.prevRotationYaw + (foguete.rotationYaw - foguete.prevRotationYaw) * partialTicks - 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(foguete.prevRotationPitch + (foguete.rotationPitch - foguete.prevRotationPitch) * partialTicks, 0.0F, 0.0F, 1.0F);

            // Escala geral
            float scale = 0.35F;
            GL11.glScalef(scale, scale, scale);

            // Desenhar sem textura (cores sólidas)
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            Tessellator t = Tessellator.instance;

            // ---------- CORPO (cinza metálico) ----------
            t.startDrawingQuads();
            t.setColorOpaque_F(0.55F, 0.55F, 0.60F);

            // Face superior (Y+)
            t.addVertex(-0.4D, 0.4D, -1.2D);
            t.addVertex(0.4D, 0.4D, -1.2D);
            t.addVertex(0.4D, 0.4D, 1.0D);
            t.addVertex(-0.4D, 0.4D, 1.0D);

            // Face inferior (Y-)
            t.addVertex(-0.4D, -0.4D, 1.0D);
            t.addVertex(0.4D, -0.4D, 1.0D);
            t.addVertex(0.4D, -0.4D, -1.2D);
            t.addVertex(-0.4D, -0.4D, -1.2D);

            // Face esquerda (X-)
            t.addVertex(-0.4D, -0.4D, -1.2D);
            t.addVertex(-0.4D, 0.4D, -1.2D);
            t.addVertex(-0.4D, 0.4D, 1.0D);
            t.addVertex(-0.4D, -0.4D, 1.0D);

            // Face direita (X+)
            t.addVertex(0.4D, -0.4D, 1.0D);
            t.addVertex(0.4D, 0.4D, 1.0D);
            t.addVertex(0.4D, 0.4D, -1.2D);
            t.addVertex(0.4D, -0.4D, -1.2D);

            // Face traseira (Z-)
            t.addVertex(-0.4D, -0.4D, -1.2D);
            t.addVertex(0.4D, -0.4D, -1.2D);
            t.addVertex(0.4D, 0.4D, -1.2D);
            t.addVertex(-0.4D, 0.4D, -1.2D);

            // Face dianteira (Z+)
            t.addVertex(-0.4D, -0.4D, 1.0D);
            t.addVertex(-0.4D, 0.4D, 1.0D);
            t.addVertex(0.4D, 0.4D, 1.0D);
            t.addVertex(0.4D, -0.4D, 1.0D);
            t.draw();

            // ---------- PONTA (laranja/vermelha) ----------
            t.startDrawing(GL11.GL_TRIANGLES);
            t.setColorOpaque_F(0.95F, 0.35F, 0.10F);

            // 4 triângulos formando a ponta
            // Frente
            t.addVertex(0.0D, 0.0D, 1.8D);
            t.addVertex(-0.4D, -0.4D, 1.0D);
            t.addVertex(0.4D, -0.4D, 1.0D);

            t.addVertex(0.0D, 0.0D, 1.8D);
            t.addVertex(0.4D, -0.4D, 1.0D);
            t.addVertex(0.4D, 0.4D, 1.0D);

            t.addVertex(0.0D, 0.0D, 1.8D);
            t.addVertex(0.4D, 0.4D, 1.0D);
            t.addVertex(-0.4D, 0.4D, 1.0D);

            t.addVertex(0.0D, 0.0D, 1.8D);
            t.addVertex(-0.4D, 0.4D, 1.0D);
            t.addVertex(-0.4D, -0.4D, 1.0D);
            t.draw();

            // ---------- ALETAS (pretas) ----------
            t.startDrawingQuads();
            t.setColorOpaque_F(0.15F, 0.15F, 0.18F);

            // Aleta inferior
            t.addVertex(-0.05D, -0.4D, -0.9D);
            t.addVertex(0.05D, -0.4D, -0.9D);
            t.addVertex(0.05D, -1.0D, -1.3D);
            t.addVertex(-0.05D, -1.0D, -1.3D);

            t.addVertex(-0.05D, -1.0D, -1.3D);
            t.addVertex(0.05D, -1.0D, -1.3D);
            t.addVertex(0.05D, -0.4D, -0.9D);
            t.addVertex(-0.05D, -0.4D, -0.9D);

            // Aleta superior
            t.addVertex(-0.05D, 0.4D, -0.9D);
            t.addVertex(-0.05D, 1.0D, -1.3D);
            t.addVertex(0.05D, 1.0D, -1.3D);
            t.addVertex(0.05D, 0.4D, -0.9D);

            // Aleta esquerda
            t.addVertex(-0.4D, -0.05D, -0.9D);
            t.addVertex(-1.0D, -0.05D, -1.3D);
            t.addVertex(-1.0D, 0.05D, -1.3D);
            t.addVertex(-0.4D, 0.05D, -0.9D);

            // Aleta direita
            t.addVertex(0.4D, -0.05D, -0.9D);
            t.addVertex(0.4D, 0.05D, -0.9D);
            t.addVertex(1.0D, 0.05D, -1.3D);
            t.addVertex(1.0D, -0.05D, -1.3D);
            t.draw();

            // ---------- FAIXA DECORATIVA (vermelha) ----------
            t.startDrawingQuads();
            t.setColorOpaque_F(0.85F, 0.15F, 0.10F);
            double bandZ1 = 0.3D;
            double bandZ2 = 0.55D;

            t.addVertex(-0.41D, -0.41D, bandZ1);
            t.addVertex(0.41D, -0.41D, bandZ1);
            t.addVertex(0.41D, -0.41D, bandZ2);
            t.addVertex(-0.41D, -0.41D, bandZ2);

            t.addVertex(-0.41D, 0.41D, bandZ2);
            t.addVertex(0.41D, 0.41D, bandZ2);
            t.addVertex(0.41D, 0.41D, bandZ1);
            t.addVertex(-0.41D, 0.41D, bandZ1);

            t.addVertex(-0.41D, -0.41D, bandZ1);
            t.addVertex(-0.41D, 0.41D, bandZ1);
            t.addVertex(-0.41D, 0.41D, bandZ2);
            t.addVertex(-0.41D, -0.41D, bandZ2);

            t.addVertex(0.41D, -0.41D, bandZ2);
            t.addVertex(0.41D, 0.41D, bandZ2);
            t.addVertex(0.41D, 0.41D, bandZ1);
            t.addVertex(0.41D, -0.41D, bandZ1);
            t.draw();

            // Restaurar estados
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopMatrix();
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return null; // Não usamos textura
        }
    }
}
