
package com.myname.modelcreator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraftforge.registries.GameData;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;


/*
 * ============================================================
 *       CRIADOR DE MODELO 3D - MINECRAFT FORGE 1.7.10
 * ============================================================
 *
 * TUDO ESTÁ NESTE ÚNICO ARQUIVO.
 *
 * ID NUMÉRICO DO BLOCO:
 *
 *     58321
 *
 * COMANDO:
 *
 *     /give @p 58321 1
 *
 * OU:
 *
 *     /modelcreator
 *
 * ============================================================
 */

@Mod(
        modid = "modelcreator3d",
        name = "Criador de Modelo 3D",
        version = "4.0",
        acceptedMinecraftVersions = "[1.7.10]"
)
public class ModelCreatorMod {

    public static final String MODID = "modelcreator3d";

    /*
     * ========================================================
     * ID NUMÉRICO
     * ========================================================
     */

    public static final int MODEL_CREATOR_ID = 58321;

    public static final Block MODEL_CREATOR =
            new ModelCreatorBlock(MODEL_CREATOR_ID);

    /*
     * ========================================================
     * INSTANCE
     * ========================================================
     */

    @Mod.Instance(MODID)
    public static ModelCreatorMod INSTANCE;

    /*
     * ========================================================
     * PROXY
     * ========================================================
     */

    @SidedProxy(
            clientSide = "com.myname.modelcreator.ModelCreatorMod$ClientProxy",
            serverSide = "com.myname.modelcreator.ModelCreatorMod$CommonProxy"
    )
    public static CommonProxy proxy;

    /*
     * ========================================================
     * REDE
     * ========================================================
     */

    public static SimpleNetworkWrapper NETWORK;

    /*
     * ========================================================
     * PRE INIT
     * ========================================================
     */

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        /*
         * Registro do bloco com ID numérico 58321.
         */
        GameRegistry.registerBlock(
                MODEL_CREATOR,
                "criadorDeModelo3D"
        );

        /*
         * TileEntity.
         */
        GameRegistry.registerTileEntity(
                ModelCreatorTile.class,
                "ModelCreatorTile"
        );

        /*
         * Canal de rede.
         */
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(
                "MC3D"
        );

        NETWORK.registerMessage(
                SaveModelMessageHandler.class,
                SaveModelMessage.class,
                0,
                Side.SERVER
        );

        NETWORK.registerMessage(
                SyncModelMessageHandler.class,
                SyncModelMessage.class,
                1,
                Side.CLIENT
        );

        /*
         * GUI.
         */
        NetworkRegistry.INSTANCE.registerGuiHandler(
                INSTANCE,
                new GuiHandler()
        );

        /*
         * Proxy.
         */
        proxy.preInit();
    }

    /*
     * ========================================================
     * INIT
     * ========================================================
     */

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

        proxy.init();

        /*
         * Comando /modelcreator
         */
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {

            FMLCommonHandler.instance()
                    .getMinecraftServerInstance()
                    .getCommandManager()
                    .registerCommand(
                            new ModelCreatorCommand()
                    );
        }
    }


    /*
     * ========================================================
     * PROXY
     * ========================================================
     */

    public static class CommonProxy {

        public void preInit() {
        }

        public void init() {
        }
    }

    @SideOnly(Side.CLIENT)
    public static class ClientProxy extends CommonProxy {

        @Override
        public void preInit() {
        }

        @Override
        public void init() {
        }
    }


    /*
     * ========================================================
     * BLOCO
     * ========================================================
     */

    public static class ModelCreatorBlock extends Block {

        public ModelCreatorBlock(int id) {

            super(id, Material.rock);

            setBlockName("criadorDeModelo3D");

            /*
             * A textura DO BLOCO é vanilla.
             *
             * A textura do MODELO é criada totalmente
             * através de Java/DynamicTexture.
             */
            setBlockTextureName("minecraft:stone");

            setHardness(2.0F);
            setResistance(10.0F);

            /*
             * Aparece no criativo.
             */
            setCreativeTab(
                    net.minecraft.creativetab.CreativeTabs.tabBlock
            );
        }

        @Override
        public TileEntity createTileEntity(
                World world,
                int metadata) {

            return new ModelCreatorTile();
        }

        @Override
        public boolean hasTileEntity(int metadata) {
            return true;
        }

        @Override
        public boolean onBlockActivated(
                World world,
                int x,
                int y,
                int z,
                EntityPlayer player,
                int side,
                float hitX,
                float hitY,
                float hitZ) {

            if (!world.isRemote) {

                player.openGui(
                        ModelCreatorMod.INSTANCE,
                        0,
                        world,
                        x,
                        y,
                        z
                );

                /*
                 * Envia os dados atuais para o cliente.
                 */
                TileEntity tile =
                        world.getTileEntity(x, y, z);

                if (tile instanceof ModelCreatorTile) {

                    sendSync(
                            (EntityPlayerMP) player,
                            (ModelCreatorTile) tile
                    );
                }
            }

            return true;
        }

        @Override
        public void breakBlock(
                World world,
                int x,
                int y,
                int z,
                Block block,
                int metadata) {

            /*
             * Mantém o comportamento normal.
             */
            super.breakBlock(
                    world,
                    x,
                    y,
                    z,
                    block,
                    metadata
            );
        }
    }


    /*
     * ========================================================
     * CUBO
     * ========================================================
     */

    public static class ModelCube {

        public String name = "Cubo";

        public float x = 0.0F;
        public float y = 0.0F;
        public float z = 0.0F;

        public float width = 1.0F;
        public float height = 1.0F;
        public float depth = 1.0F;

        public float scale = 1.0F;

        /*
         * UV básico.
         */
        public int u = 0;
        public int v = 0;

        public int uvWidth = 8;
        public int uvHeight = 8;

        /*
         * Rotação individual.
         */
        public float rotX = 0.0F;
        public float rotY = 0.0F;
        public float rotZ = 0.0F;

        public ModelCube() {
        }

        public ModelCube copy() {

            ModelCube c = new ModelCube();

            c.name = this.name;

            c.x = this.x;
            c.y = this.y;
            c.z = this.z;

            c.width = this.width;
            c.height = this.height;
            c.depth = this.depth;

            c.scale = this.scale;

            c.u = this.u;
            c.v = this.v;

            c.uvWidth = this.uvWidth;
            c.uvHeight = this.uvHeight;

            c.rotX = this.rotX;
            c.rotY = this.rotY;
            c.rotZ = this.rotZ;

            return c;
        }

        public NBTTagCompound writeNBT() {

            NBTTagCompound tag =
                    new NBTTagCompound();

            tag.setString(
                    "Name",
                    name
            );

            tag.setFloat("X", x);
            tag.setFloat("Y", y);
            tag.setFloat("Z", z);

            tag.setFloat(
                    "Width",
                    width
            );

            tag.setFloat(
                    "Height",
                    height
            );

            tag.setFloat(
                    "Depth",
                    depth
            );

            tag.setFloat(
                    "Scale",
                    scale
            );

            tag.setInteger(
                    "U",
                    u
            );

            tag.setInteger(
                    "V",
                    v
            );

            tag.setInteger(
                    "UVWidth",
                    uvWidth
            );

            tag.setInteger(
                    "UVHeight",
                    uvHeight
            );

            tag.setFloat(
                    "RotX",
                    rotX
            );

            tag.setFloat(
                    "RotY",
                    rotY
            );

            tag.setFloat(
                    "RotZ",
                    rotZ
            );

            return tag;
        }

        public void readNBT(NBTTagCompound tag) {

            name =
                    tag.getString("Name");

            x =
                    tag.getFloat("X");

            y =
                    tag.getFloat("Y");

            z =
                    tag.getFloat("Z");

            width =
                    tag.getFloat("Width");

            height =
                    tag.getFloat("Height");

            depth =
                    tag.getFloat("Depth");

            scale =
                    tag.getFloat("Scale");

            u =
                    tag.getInteger("U");

            v =
                    tag.getInteger("V");

            uvWidth =
                    tag.getInteger("UVWidth");

            uvHeight =
                    tag.getInteger("UVHeight");

            rotX =
                    tag.getFloat("RotX");

            rotY =
                    tag.getFloat("RotY");

            rotZ =
                    tag.getFloat("RotZ");
        }
    }


    /*
     * ========================================================
     * TEXTURA
     * ========================================================
     *
     * NÃO existe PNG.
     *
     * Tudo é armazenado em int[].
     *
     * Formato:
     *
     * 0xAARRGGBB
     *
     */

    public static class ModelTexture {

        public static final int WIDTH = 32;
        public static final int HEIGHT = 32;

        public int[] pixels =
                new int[WIDTH * HEIGHT];

        public ModelTexture() {

            /*
             * Inicializa transparente.
             */
            for (int i = 0; i < pixels.length; i++) {

                pixels[i] =
                        0x00000000;
            }
        }

        public void setPixel(
                int x,
                int y,
                int color) {

            if (x < 0 ||
                    y < 0 ||
                    x >= WIDTH ||
                    y >= HEIGHT) {

                return;
            }

            pixels[
                    y * WIDTH + x
            ] = color;
        }

        public int getPixel(
                int x,
                int y) {

            if (x < 0 ||
                    y < 0 ||
                    x >= WIDTH ||
                    y >= HEIGHT) {

                return 0;
            }

            return pixels[
                    y * WIDTH + x
            ];
        }

        public void clear() {

            for (int i = 0;
                    i < pixels.length;
                    i++) {

                pixels[i] =
                        0x00000000;
            }
        }

        public NBTTagCompound writeNBT() {

            NBTTagCompound tag =
                    new NBTTagCompound();

            tag.setInteger(
                    "Width",
                    WIDTH
            );

            tag.setInteger(
                    "Height",
                    HEIGHT
            );

            tag.setIntArray(
                    "Pixels",
                    pixels
            );

            return tag;
        }

        public void readNBT(
                NBTTagCompound tag) {

            int[] data =
                    tag.getIntArray(
                            "Pixels"
                    );

            if (data != null &&
                    data.length ==
                            pixels.length) {

                System.arraycopy(
                        data,
                        0,
                        pixels,
                        0,
                        pixels.length
                );
            }
        }
    }


    /*
     * ========================================================
     * TILE ENTITY
     * ========================================================
     */

    public static class ModelCreatorTile
            extends TileEntity {

        public List<ModelCube> cubes =
                new ArrayList<ModelCube>();

        public int selectedCube = 0;

        public ModelTexture texture =
                new ModelTexture();

        public String modelName =
                "Meu Modelo";

        public ModelCreatorTile() {

            /*
             * Começa com um cubo.
             */
            cubes.add(
                    new ModelCube()
            );
        }

        public ModelCube getSelectedCube() {

            if (cubes.size() == 0) {

                ModelCube cube =
                        new ModelCube();

                cubes.add(cube);
            }

            if (selectedCube < 0) {

                selectedCube = 0;
            }

            if (selectedCube >=
                    cubes.size()) {

                selectedCube =
                        cubes.size() - 1;
            }

            return cubes.get(
                    selectedCube
            );
        }

        public void addCube() {

            ModelCube cube =
                    new ModelCube();

            cube.name =
                    "Cubo " +
                    (cubes.size() + 1);

            cubes.add(cube);

            selectedCube =
                    cubes.size() - 1;

            markDirty();
        }

        public void duplicateCube() {

            if (cubes.size() == 0) {
                addCube();
                return;
            }

            ModelCube original =
                    getSelectedCube();

            ModelCube copy =
                    original.copy();

            copy.name =
                    original.name +
                    " Cópia";

            copy.x += 1.0F;

            cubes.add(copy);

            selectedCube =
                    cubes.size() - 1;

            markDirty();
        }

        public void removeCube() {

            if (cubes.size() <= 1) {

                return;
            }

            cubes.remove(
                    selectedCube
            );

            if (selectedCube >=
                    cubes.size()) {

                selectedCube =
                        cubes.size() - 1;
            }

            markDirty();
        }

        @Override
        public void writeToNBT(
                NBTTagCompound tag) {

            super.writeToNBT(tag);

            tag.setString(
                    "ModelName",
                    modelName
            );

            tag.setInteger(
                    "SelectedCube",
                    selectedCube
            );

            /*
             * Cubos.
             */
            NBTTagList list =
                    new NBTTagList();

            for (ModelCube cube : cubes) {

                list.appendTag(
                        cube.writeNBT()
                );
            }

            tag.setTag(
                    "Cubes",
                    list
            );

            /*
             * Textura.
             */
            tag.setTag(
                    "Texture",
                    texture.writeNBT()
            );
        }

        @Override
        public void readFromNBT(
                NBTTagCompound tag) {

            super.readFromNBT(tag);

            modelName =
                    tag.getString(
                            "ModelName"
                    );

            selectedCube =
                    tag.getInteger(
                            "SelectedCube"
                    );

            cubes.clear();

            NBTTagList list =
                    tag.getTagList(
                            "Cubes",
                            10
                    );

            for (int i = 0;
                    i < list.tagCount();
                    i++) {

                NBTTagCompound cubeTag =
                        list.getCompoundTagAt(i);

                ModelCube cube =
                        new ModelCube();

                cube.readNBT(
                        cubeTag
                );

                cubes.add(cube);
            }

            if (cubes.size() == 0) {

                cubes.add(
                        new ModelCube()
                );
            }

            if (tag.hasKey("Texture")) {

                texture.readNBT(
                        tag.getCompoundTag(
                                "Texture"
                        )
                );
            }

            if (selectedCube < 0) {
                selectedCube = 0;
            }

            if (selectedCube >=
                    cubes.size()) {

                selectedCube =
                        cubes.size() - 1;
            }
        }

        public NBTTagCompound createFullNBT() {

            NBTTagCompound tag =
                    new NBTTagCompound();

            writeToNBT(tag);

            return tag;
        }

        public void loadFullNBT(
                NBTTagCompound tag) {

            readFromNBT(tag);

            markDirty();
        }
    }


    /*
     * ========================================================
     * DYNAMIC TEXTURE
     * ========================================================
     */

    @SideOnly(Side.CLIENT)
    public static class DynamicTextureManager {

        private static ResourceLocation location;

        private static net.minecraft.client.renderer.texture.DynamicTexture dynamicTexture;

        private static int[] lastPixels;

        public static ResourceLocation get(
                ModelCreatorTile tile) {

            if (dynamicTexture == null) {

                dynamicTexture =
                        new net.minecraft.client.renderer.texture.DynamicTexture(
                                ModelTexture.WIDTH,
                                ModelTexture.HEIGHT
                        );

                location =
                        Minecraft.getMinecraft()
                                .getTextureManager()
                                .getDynamicTextureLocation(
                                        "model_creator_texture",
                                        dynamicTexture
                                );

                lastPixels =
                        new int[
                                tile.texture.pixels.length
                        ];

                forceUpdate(tile);
            }

            /*
             * Detecta alteração.
             */
            boolean changed = false;

            for (int i = 0;
                    i < lastPixels.length;
                    i++) {

                if (lastPixels[i] !=
                        tile.texture.pixels[i]) {

                    changed = true;
                    break;
                }
            }

            if (changed) {

                forceUpdate(tile);
            }

            return location;
        }

        private static void forceUpdate(
                ModelCreatorTile tile) {

            int[] data =
                    dynamicTexture.getTextureData();

            for (int i = 0;
                    i < data.length &&
                    i < tile.texture.pixels.length;
                    i++) {

                data[i] =
                        tile.texture.pixels[i];

                lastPixels[i] =
                        tile.texture.pixels[i];
            }

            dynamicTexture.updateDynamicTexture();
        }
    }


    /*
     * ========================================================
     * NETWORK - SALVAR
     * ========================================================
     */

    public static class SaveModelMessage
            implements IMessage {

        public int x;
        public int y;
        public int z;

        public byte[] data;

        public SaveModelMessage() {
        }

        public SaveModelMessage(
                int x,
                int y,
                int z,
                NBTTagCompound tag) {

            this.x = x;
            this.y = y;
            this.z = z;

            try {

                data =
                        net.minecraft.nbt.CompressedStreamTools
                                .compress(tag);

            } catch (IOException e) {

                data =
                        new byte[0];
            }
        }

        @Override
        public void fromBytes(
                ByteBuf buf) {

            x =
                    buf.readInt();

            y =
                    buf.readInt();

            z =
                    buf.readInt();

            int length =
                    buf.readInt();

            data =
                    new byte[length];

            buf.readBytes(data);
        }

        @Override
        public void toBytes(
                ByteBuf buf) {

            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);

            buf.writeInt(
                    data == null
                            ? 0
                            : data.length
            );

            if (data != null) {

                buf.writeBytes(data);
            }
        }
    }


    public static class SaveModelMessageHandler
            implements IMessageHandler<
                    SaveModelMessage,
                    IMessage> {

        @Override
        public IMessage onMessage(
                final SaveModelMessage message,
                final MessageContext ctx) {

            /*
             * Executa no servidor.
             */
            final EntityPlayerMP player =
                    ctx.getServerHandler()
                            .playerEntity;

            player.getServerForPlayer()
                    .addScheduledTask(
                            new Runnable() {

                                @Override
                                public void run() {

                                    World world =
                                            player.worldObj;

                                    TileEntity tile =
                                            world.getTileEntity(
                                                    message.x,
                                                    message.y,
                                                    message.z
                                            );

                                    if (!(tile instanceof ModelCreatorTile)) {
                                        return;
                                    }

                                    if (message.data ==
                                            null) {
                                        return;
                                    }

                                    try {

                                        NBTTagCompound tag =
                                                net.minecraft.nbt.CompressedStreamTools
                                                        .decompress(
                                                                message.data
                                                        );

                                        ModelCreatorTile model =
                                                (ModelCreatorTile) tile;

                                        model.loadFullNBT(tag);

                                        world.markBlockForUpdate(
                                                message.x,
                                                message.y,
                                                message.z
                                        );

                                        /*
                                         * Confirma para o cliente.
                                         */
                                        sendSync(
                                                player,
                                                model
                                        );

                                    } catch (Exception e) {

                                        e.printStackTrace();
                                    }
                                }
                            }
                    );

            return null;
        }
    }


    /*
     * ========================================================
     * NETWORK - SINCRONIZAÇÃO
     * ========================================================
     */

    public static class SyncModelMessage
            implements IMessage {

        public int x;
        public int y;
        public int z;

        public byte[] data;

        public SyncModelMessage() {
        }

        public SyncModelMessage(
                int x,
                int y,
                int z,
                NBTTagCompound tag) {

            this.x = x;
            this.y = y;
            this.z = z;

            try {

                data =
                        net.minecraft.nbt.CompressedStreamTools
                                .compress(tag);

            } catch (IOException e) {

                data =
                        new byte[0];
            }
        }

        @Override
        public void fromBytes(
                ByteBuf buf) {

            x =
                    buf.readInt();

            y =
                    buf.readInt();

            z =
                    buf.readInt();

            int length =
                    buf.readInt();

            data =
                    new byte[length];

            buf.readBytes(data);
        }

        @Override
        public void toBytes(
                ByteBuf buf) {

            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);

            buf.writeInt(
                    data == null
                            ? 0
                            : data.length
            );

            if (data != null) {

                buf.writeBytes(data);
            }
        }
    }


    public static class SyncModelMessageHandler
            implements IMessageHandler<
                    SyncModelMessage,
                    IMessage> {

        @Override
        public IMessage onMessage(
                final SyncModelMessage message,
                final MessageContext ctx) {

            Minecraft.getMinecraft()
                    .addScheduledTask(
                            new Runnable() {

                                @Override
                                public void run() {

                                    World world =
                                            Minecraft.getMinecraft()
                                                    .theWorld;

                                    if (world == null) {
                                        return;
                                    }

                                    TileEntity tile =
                                            world.getTileEntity(
                                                    message.x,
                                                    message.y,
                                                    message.z
                                            );

                                    if (!(tile instanceof ModelCreatorTile)) {
                                        return;
                                    }

                                    try {

                                        NBTTagCompound tag =
                                                net.minecraft.nbt.CompressedStreamTools
                                                        .decompress(
                                                                message.data
                                                        );

                                        ModelCreatorTile model =
                                                (ModelCreatorTile) tile;

                                        model.loadFullNBT(tag);

                                    } catch (Exception e) {

                                        e.printStackTrace();
                                    }
                                }
                            }
                    );

            return null;
        }
    }


    /*
     * ========================================================
     * SEND SYNC
     * ========================================================
     */

    public static void sendSync(
            EntityPlayerMP player,
            ModelCreatorTile tile) {

        if (NETWORK == null) {
            return;
        }

        NETWORK.sendTo(
                new SyncModelMessage(
                        tile.xCoord,
                        tile.yCoord,
                        tile.zCoord,
                        tile.createFullNBT()
                ),
                player
        );
    }


    /*
     * ========================================================
     * GUI HANDLER
     * ========================================================
     */

    public static class GuiHandler
            implements IGuiHandler {

        @Override
        public Object getServerGuiElement(
                int id,
                EntityPlayer player,
                World world,
                int x,
                int y,
                int z) {

            /*
             * Não existe Container.
             */
            return null;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public Object getClientGuiElement(
                int id,
                EntityPlayer player,
                World world,
                int x,
                int y,
                int z) {

            if (id != 0) {
                return null;
            }

            TileEntity tile =
                    world.getTileEntity(
                            x,
                            y,
                            z
                    );

            if (!(tile instanceof ModelCreatorTile)) {
                return null;
            }

            return new ModelCreatorGui(
                    player,
                    (ModelCreatorTile) tile
            );
        }
    }


    /*
     * ========================================================
     * COMANDO
     * ========================================================
     *
     * /modelcreator
     *
     * Dá o bloco ao jogador.
     *
     */

    public static class ModelCreatorCommand
            extends CommandBase {

        @Override
        public String getCommandName() {

            return "modelcreator";
        }

        @Override
        public String getCommandUsage(
                ICommandSender sender) {

            return "/modelcreator";
        }

        @Override
        public void processCommand(
                ICommandSender sender,
                String[] args) {

            if (!(sender instanceof EntityPlayer)) {

                sender.addChatMessage(
                        new ChatComponentText(
                                "Este comando deve ser usado por um jogador."
                        )
                );

                return;
            }

            EntityPlayer player =
                    (EntityPlayer) sender;

            ItemStack stack =
                    new ItemStack(
                            MODEL_CREATOR,
                            1,
                            0
                    );

            if (!player.inventory.addItemStackToInventory(stack)) {

                player.dropPlayerItemWithRandomChoice(
                        stack,
                        false
                );
            }

            player.addChatMessage(
                    new ChatComponentText(
                            "§aVocê recebeu o Criador de Modelo 3D!"
                    )
            );

            player.addChatMessage(
                    new ChatComponentText(
                            "§7ID numérico: §f58321"
                    )
            );
        }

        @Override
        public int getRequiredPermissionLevel() {

            return 0;
        }
    }


    /*
     * ========================================================
     * GUI PRINCIPAL
     * ========================================================
     */

    @SideOnly(Side.CLIENT)
    public static class ModelCreatorGui
            extends GuiScreen {

        private final EntityPlayer player;

        private final ModelCreatorTile tile;

        private int guiMode = 0;

        /*
         * 0 = editor de modelo
         * 1 = editor de textura
         */

        private GuiTextField fieldX;
        private GuiTextField fieldY;
        private GuiTextField fieldZ;

        private GuiTextField fieldWidth;
        private GuiTextField fieldHeight;
        private GuiTextField fieldDepth;

        private GuiTextField fieldScale;

        private GuiTextField fieldU;
        private GuiTextField fieldV;

        private GuiTextField fieldUVWidth;
        private GuiTextField fieldUVHeight;

        /*
         * Câmera.
         */
        private float cameraYaw =
                25.0F;

        private float cameraPitch =
                15.0F;

        private float cameraZoom =
                1.0F;

        private int lastMouseX;
        private int lastMouseY;

        private boolean draggingCamera;

        private boolean movingModel;

        /*
         * Botões.
         */
        private static final int BTN_ADD =
                1;

        private static final int BTN_DUPLICATE =
                2;

        private static final int BTN_REMOVE =
                3;

        private static final int BTN_TEXTURE =
                4;

        private static final int BTN_SAVE =
                5;

        private static final int BTN_BACK =
                6;

        private static final int BTN_RED =
                10;

        private static final int BTN_GREEN =
                11;

        private static final int BTN_BLUE =
                12;

        private static final int BTN_YELLOW =
                13;

        private static final int BTN_WHITE =
                14;

        private static final int BTN_BLACK =
                15;

        private static final int BTN_ERASER =
                16;

        private static final int BTN_CLEAR =
                17;

        private static final int BTN_ZOMBIE =
                18;

        private static final int BTN_CREEPER =
                19;

        public ModelCreatorGui(
                EntityPlayer player,
                ModelCreatorTile tile) {

            this.player = player;
            this.tile = tile;
        }

        @Override
        public void initGui() {

            buttonList.clear();

            if (guiMode == 0) {

                initModelGui();

            } else {

                initTextureGui();
            }
        }

        /*
         * ====================================================
         * GUI MODELO
         * ====================================================
         */

        private void initModelGui() {

            buttonList.add(
                    new GuiButton(
                            BTN_ADD,
                            8,
                            8,
                            95,
                            20,
                            "Adicionar Cubo"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_DUPLICATE,
                            108,
                            8,
                            95,
                            20,
                            "Duplicar"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_REMOVE,
                            208,
                            8,
                            95,
                            20,
                            "Remover"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_TEXTURE,
                            8,
                            32,
                            145,
                            20,
                            "EDITOR DE TEXTURA"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_SAVE,
                            158,
                            32,
                            145,
                            20,
                            "SALVAR MODELO"
                    )
            );

            /*
             * Campos.
             */

            fieldX =
                    new GuiTextField(
                            20,
                            fontRendererObj,
                            320,
                            80,
                            70,
                            18
                    );

            fieldY =
                    new GuiTextField(
                            21,
                            fontRendererObj,
                            400,
                            80,
                            70,
                            18
                    );

            fieldZ =
                    new GuiTextField(
                            22,
                            fontRendererObj,
                            480,
                            80,
                            70,
                            18
                    );

            fieldWidth =
                    new GuiTextField(
                            23,
                            fontRendererObj,
                            320,
                            125,
                            70,
                            18
                    );

            fieldHeight =
                    new GuiTextField(
                            24,
                            fontRendererObj,
                            400,
                            125,
                            70,
                            18
                    );

            fieldDepth =
                    new GuiTextField(
                            25,
                            fontRendererObj,
                            480,
                            125,
                            70,
                            18
                    );

            fieldScale =
                    new GuiTextField(
                            26,
                            fontRendererObj,
                            320,
                            170,
                            70,
                            18
                    );

            fieldU =
                    new GuiTextField(
                            27,
                            fontRendererObj,
                            400,
                            170,
                            70,
                            18
                    );

            fieldV =
                    new GuiTextField(
                            28,
                            fontRendererObj,
                            480,
                            170,
                            70,
                            18
                    );

            fieldUVWidth =
                    new GuiTextField(
                            29,
                            fontRendererObj,
                            400,
                            215,
                            70,
                            18
                    );

            fieldUVHeight =
                    new GuiTextField(
                            30,
                            fontRendererObj,
                            480,
                            215,
                            70,
                            18
                    );

            updateFields();
        }

        /*
         * ====================================================
         * GUI TEXTURA
         * ====================================================
         */

        private void initTextureGui() {

            buttonList.add(
                    new GuiButton(
                            BTN_BACK,
                            8,
                            8,
                            150,
                            20,
                            "VOLTAR AO MODELO"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_RED,
                            8,
                            38,
                            50,
                            20,
                            "V"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_GREEN,
                            61,
                            38,
                            50,
                            20,
                            "V"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_BLUE,
                            114,
                            38,
                            50,
                            20,
                            "V"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_YELLOW,
                            167,
                            38,
                            50,
                            20,
                            "A"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_WHITE,
                            220,
                            38,
                            50,
                            20,
                            "B"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_BLACK,
                            273,
                            38,
                            50,
                            20,
                            "P"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_ERASER,
                            326,
                            38,
                            70,
                            20,
                            "BORRACHA"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_CLEAR,
                            399,
                            38,
                            65,
                            20,
                            "LIMPAR"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_ZOMBIE,
                            8,
                            62,
                            100,
                            20,
                            "ZUMBI"
                    )
            );

            buttonList.add(
                    new GuiButton(
                            BTN_CREEPER,
                            113,
                            62,
                            100,
                            20,
                            "CREEPER"
                    )
            );
        }

        /*
         * ====================================================
         * ATUALIZAR CAMPOS
         * ====================================================
         */

        private void updateFields() {

            if (guiMode != 0) {
                return;
            }

            ModelCube c =
                    tile.getSelectedCube();

            fieldX.setText(
                    Float.toString(c.x)
            );

            fieldY.setText(
                    Float.toString(c.y)
            );

            fieldZ.setText(
                    Float.toString(c.z)
            );

            fieldWidth.setText(
                    Float.toString(c.width)
            );

            fieldHeight.setText(
                    Float.toString(c.height)
            );

            fieldDepth.setText(
                    Float.toString(c.depth)
            );

            fieldScale.setText(
                    Float.toString(c.scale)
            );

            fieldU.setText(
                    Integer.toString(c.u)
            );

            fieldV.setText(
                    Integer.toString(c.v)
            );

            fieldUVWidth.setText(
                    Integer.toString(c.uvWidth)
            );

            fieldUVHeight.setText(
                    Integer.toString(c.uvHeight)
            );
        }

        /*
         * ====================================================
         * DRAW SCREEN
         * ====================================================
         */

        @Override
        public void drawScreen(
                int mouseX,
                int mouseY,
                float partialTicks) {

            drawDefaultBackground();

            if (guiMode == 0) {

                drawModelEditor(
                        mouseX,
                        mouseY
                );

            } else {

                drawTextureEditor(
                        mouseX,
                        mouseY
                );
            }

            super.drawScreen(
                    mouseX,
                    mouseY,
                    partialTicks
            );
        }

        /*
         * ====================================================
         * EDITOR MODELO
         * ====================================================
         */

        private void drawModelEditor(
                int mouseX,
                int mouseY) {

            drawString(
                    fontRendererObj,
                    "CRIADOR DE MODELO 3D",
                    8,
                    65,
                    0xFFFFFF
            );

            drawString(
                    fontRendererObj,
                    "CUBOS:",
                    8,
                    88,
                    0xFFFFFF
            );

            /*
             * Lista de cubos.
             */

            int listY = 108;

            for (int i = 0;
                    i < tile.cubes.size();
                    i++) {

                ModelCube cube =
                        tile.cubes.get(i);

                int color =
                        i == tile.selectedCube
                                ? 0xFFFF55
                                : 0xFFFFFF;

                drawString(
                        fontRendererObj,
                        (i + 1) +
                                ". " +
                                cube.name,
                        8,
                        listY,
                        color
                );

                listY += 12;
            }

            /*
             * Campos.
             */

            drawString(
                    fontRendererObj,
                    "X",
                    320,
                    68,
                    0xFFFFFF
            );

            drawString(
                    fontRendererObj,
                    "Y",
                    400,
                    68,
                    0xFFFFFF
            );

            drawString(
                    fontRendererObj,
                    "Z",
                    480,
                    68,
                    0xFFFFFF
            );

            drawString(
                    fontRendererObj,
                    "LARGURA",
                    320,
                    112,
                    0xFFFFFF
            );

            drawString(
                    fontRendererObj,
                    "ALTURA",
                    400,
                    112,
                    0xFFFFFF
            );

            drawString(
                    fontRendererObj,
                    "PROF.",
                    480,
                    112,
                    0xFFFFFF
            );

            drawString(
                    fontRendererObj,
                    "ESCALA",
                    320,
                    157,
                    0xFFFFFF
            );

            drawString(
                    fontRendererObj,
                    "U",
                    400,
                    157,
                    0xFFFFFF
            );

            drawString(
                    fontRendererObj,
                    "V",
                    480,
                    157,
                    0xFFFFFF
            );

            drawString(
                    fontRendererObj,
                    "UV L",
                    400,
                    202,
                    0xFFFFFF
            );

            drawString(
                    fontRendererObj,
                    "UV A",
                    480,
                    202,
                    0xFFFFFF
            );

            /*
             * Preview 3D.
             */
            draw3DPreview();
        }

        /*
         * ====================================================
         * PREVIEW 3D
         * ====================================================
         */

        private void draw3DPreview() {

            int left =
                    570;

            int top =
                    70;

            int width =
                    this.width - left - 20;

            int height =
                    this.height - 100;

            if (width <= 20 ||
                    height <= 20) {

                return;
            }

            GL11.glPushMatrix();

            GL11.glViewport(
                    left,
                    this.height -
                            (top + height),
                    width,
                    height
            );

            GL11.glMatrixMode(
                    GL11.GL_PROJECTION
            );

            GL11.glLoadIdentity();

            float aspect =
                    (float) width /
                            (float) height;

            float size =
                    12.0F /
                            cameraZoom;

            GL11.glOrtho(
                    -size * aspect,
                    size * aspect,
                    -size,
                    size,
                    -100.0F,
                    100.0F
            );

            GL11.glMatrixMode(
                    GL11.GL_MODELVIEW
            );

            GL11.glLoadIdentity();

            GL11.glTranslatef(
                    0.0F,
                    0.0F,
                    -20.0F
            );

            GL11.glRotatef(
                    cameraPitch,
                    1,
                    0,
                    0
            );

            GL11.glRotatef(
                    cameraYaw,
                    0,
                    1,
                    0
            );

            /*
             * Centraliza aproximadamente o modelo.
             */
            GL11.glTranslatef(
                    0.0F,
                    -2.0F,
                    0.0F
            );

            GL11.glEnable(
                    GL11.GL_DEPTH_TEST
            );

            GL11.glEnable(
                    GL11.GL_TEXTURE_2D
            );

            GL11.glDisable(
                    GL11.GL_LIGHTING
            );

            ResourceLocation texture =
                    DynamicTextureManager.get(tile);

            Minecraft.getMinecraft()
                    .getTextureManager()
                    .bindTexture(texture);

            for (ModelCube cube :
                    tile.cubes) {

                renderCube(cube);
            }

            GL11.glDisable(
                    GL11.GL_DEPTH_TEST
            );

            GL11.glViewport(
                    0,
                    0,
                    this.width,
                    this.height
            );

            GL11.glPopMatrix();
        }

        /*
         * ====================================================
         * RENDER CUBE
         * ====================================================
         */

        private void renderCube(
                ModelCube c) {

            float x =
                    c.x;

            float y =
                    c.y;

            float z =
                    c.z;

            float w =
                    c.width *
                            c.scale;

            float h =
                    c.height *
                            c.scale;

            float d =
                    c.depth *
                            c.scale;

            float x1 =
                    x - w / 2.0F;

            float x2 =
                    x + w / 2.0F;

            float y1 =
                    y - h / 2.0F;

            float y2 =
                    y + h / 2.0F;

            float z1 =
                    z - d / 2.0F;

            float z2 =
                    z + d / 2.0F;

            float texW =
                    ModelTexture.WIDTH;

            float texH =
                    ModelTexture.HEIGHT;

            float u1 =
                    c.u / texW;

            float v1 =
                    c.v / texH;

            float u2 =
                    (c.u + c.uvWidth) /
                            texW;

            float v2 =
                    (c.v + c.uvHeight) /
                            texH;

            GL11.glPushMatrix();

            GL11.glTranslatef(
                    x,
                    y,
                    z
            );

            GL11.glRotatef(
                    c.rotX,
                    1,
                    0,
                    0
            );

            GL11.glRotatef(
                    c.rotY,
                    0,
                    1,
                    0
            );

            GL11.glRotatef(
                    c.rotZ,
                    0,
                    0,
                    1
            );

            GL11.glTranslatef(
                    -x,
                    -y,
                    -z
            );

            Tessellator tess =
                    Tessellator.instance;

            /*
             * Frente
             */
            tess.startDrawingQuads();

            tess.addVertexWithUV(
                    x1,
                    y1,
                    z2,
                    u1,
                    v2
            );

            tess.addVertexWithUV(
                    x2,
                    y1,
                    z2,
                    u2,
                    v2
            );

            tess.addVertexWithUV(
                    x2,
                    y2,
                    z2,
                    u2,
                    v1
            );

            tess.addVertexWithUV(
                    x1,
                    y2,
                    z2,
                    u1,
                    v1
            );

            /*
             * Trás
             */
            tess.addVertexWithUV(
                    x2,
                    y1,
                    z1,
                    u1,
                    v2
            );

            tess.addVertexWithUV(
                    x1,
                    y1,
                    z1,
                    u2,
                    v2
            );

            tess.addVertexWithUV(
                    x1,
                    y2,
                    z1,
                    u2,
                    v1
            );

            tess.addVertexWithUV(
                    x2,
                    y2,
                    z1,
                    u1,
                    v1
            );

            /*
             * Esquerda
             */
            tess.addVertexWithUV(
                    x1,
                    y1,
                    z1,
                    u1,
                    v2
            );

            tess.addVertexWithUV(
                    x1,
                    y1,
                    z2,
                    u2,
                    v2
            );

            tess.addVertexWithUV(
                    x1,
                    y2,
                    z2,
                    u2,
                    v1
            );

            tess.addVertexWithUV(
                    x1,
                    y2,
                    z1,
                    u1,
                    v1
            );

            /*
             * Direita
             */
            tess.addVertexWithUV(
                    x2,
                    y1,
                    z2,
                    u1,
                    v2
            );

            tess.addVertexWithUV(
                    x2,
                    y1,
                    z1,
                    u2,
                    v2
            );

            tess.addVertexWithUV(
                    x2,
                    y2,
                    z1,
                    u2,
                    v1
            );

            tess.addVertexWithUV(
                    x2,
                    y2,
                    z2,
                    u1,
                    v1
            );

            /*
             * Cima
             */
            tess.addVertexWithUV(
                    x1,
                    y2,
                    z2,
                    u1,
                    v2
            );

            tess.addVertexWithUV(
                    x2,
                    y2,
                    z2,
                    u2,
                    v2
            );

            tess.addVertexWithUV(
                    x2,
                    y2,
                    z1,
                    u2,
                    v1
            );

            tess.addVertexWithUV(
                    x1,
                    y2,
                    z1,
                    u1,
                    v1
            );

            /*
             * Baixo
             */
            tess.addVertexWithUV(
                    x1,
                    y1,
                    z1,
                    u1,
                    v1
            );

            tess.addVertexWithUV(
                    x2,
                    y1,
                    z1,
                    u2,
                    v1
            );

            tess.addVertexWithUV(
                    x2,
                    y1,
                    z2,
                    u2,
                    v2
            );

            tess.addVertexWithUV(
                    x1,
                    y1,
                    z2,
                    u1,
                    v2
            );

            tess.draw();

            GL11.glPopMatrix();
        }

        /*
         * ====================================================
         * EDITOR DE TEXTURA
         * ====================================================
         */

        private void drawTextureEditor(
                int mouseX,
                int mouseY) {

            drawString(
                    fontRendererObj,
                    "EDITOR DE TEXTURA 32x32",
                    8,
                    90,
                    0xFFFFFF
            );

            drawString(
                    fontRendererObj,
                    "Clique nos pixels para pintar.",
                    8,
                    104,
                    0xAAAAAA
            );

            drawString(
                    fontRendererObj,
                    "Botão direito = apagar pixel.",
                    8,
                    116,
                    0xAAAAAA
            );

            int texX =
                    30;

            int texY =
                    145;

            int pixelSize =
                    12;

            /*
             * Fundo.
             */
            drawRect(
                    texX - 2,
                    texY - 2,
                    texX +
                            ModelTexture.WIDTH *
                            pixelSize +
                            2,
                    texY +
                            ModelTexture.HEIGHT *
                            pixelSize +
                            2,
                    0xFF222222
            );

            /*
             * Pixels.
             */
            for (int y = 0;
                    y < ModelTexture.HEIGHT;
                    y++) {

                for (int x = 0;
                        x < ModelTexture.WIDTH;
                        x++) {

                    int color =
                            tile.texture.getPixel(
                                    x,
                                    y
                            );

                    if ((color >>> 24) == 0) {

                        color =
                                0xFF555555;
                    }

                    int px =
                            texX +
                            x *
                            pixelSize;

                    int py =
                            texY +
                            y *
                            pixelSize;

                    drawRect(
                            px,
                            py,
                            px +
                                    pixelSize -
                                    1,
                            py +
                                    pixelSize -
                                    1,
                            color
                    );
                }
            }

            /*
             * Mini preview.
             */
            drawString(
                    fontRendererObj,
                    "PREVIEW",
                    440,
                    145,
                    0xFFFFFF
            );

            int previewX =
                    440;

            int previewY =
                    165;

            int previewSize =
                    192;

            for (int y = 0;
                    y < ModelTexture.HEIGHT;
                    y++) {

                for (int x = 0;
                        x < ModelTexture.WIDTH;
                        x++) {

                    int color =
                            tile.texture.getPixel(
                                    x,
                                    y
                            );

                    if ((color >>> 24) == 0) {

                        color =
                                0xFF444444;
                    }

                    int px =
                            previewX +
                            x *
                            previewSize /
                            ModelTexture.WIDTH;

                    int py =
                            previewY +
                            y *
                            previewSize /
                            ModelTexture.HEIGHT;

                    int ps =
                            previewSize /
                            ModelTexture.WIDTH;

                    drawRect(
                            px,
                            py,
                            px + ps,
                            py + ps,
                            color
                    );
                }
            }
        }

        /*
         * ====================================================
         * BUTTON CLICK
         * ====================================================
         */

        @Override
        protected void actionPerformed(
                GuiButton button) {

            if (button.id == BTN_ADD) {

                tile.addCube();
                updateFields();

            } else if (
                    button.id ==
                            BTN_DUPLICATE) {

                tile.duplicateCube();
                updateFields();

            } else if (
                    button.id ==
                            BTN_REMOVE) {

                tile.removeCube();
                updateFields();

            } else if (
                    button.id ==
                            BTN_TEXTURE) {

                guiMode = 1;

                initGui();

            } else if (
                    button.id ==
                            BTN_SAVE) {

                applyFields();

                saveToServer();

                player.addChatMessage(
                        new ChatComponentText(
                                "§aModelo enviado para salvar!"
                        )
                );

            } else if (
                    button.id ==
                            BTN_BACK) {

                guiMode = 0;

                initGui();

            } else if (
                    button.id ==
                            BTN_RED) {

                paintColor =
                        0xFFFF0000;

            } else if (
                    button.id ==
                            BTN_GREEN) {

                paintColor =
                        0xFF00FF00;

            } else if (
                    button.id ==
                            BTN_BLUE) {

                paintColor =
                        0xFF0000FF;

            } else if (
                    button.id ==
                            BTN_YELLOW) {

                paintColor =
                        0xFFFFFF00;

            } else if (
                    button.id ==
                            BTN_WHITE) {

                paintColor =
                        0xFFFFFFFF;

            } else if (
                    button.id ==
                            BTN_BLACK) {

                paintColor =
                        0xFF000000;

            } else if (
                    button.id ==
                            BTN_ERASER) {

                paintColor =
                        0x00000000;

            } else if (
                    button.id ==
                            BTN_CLEAR) {

                tile.texture.clear();

            } else if (
                    button.id ==
                            BTN_ZOMBIE) {

                createZombieTexture();

            } else if (
                    button.id ==
                            BTN_CREEPER) {

                createCreeperTexture();
            }
        }

        private int paintColor =
                0xFFFFFFFF;

        /*
         * ====================================================
         * TEXTURA ZUMBI
         * ====================================================
         */

        private void createZombieTexture() {

            tile.texture.clear();

            /*
             * Pele.
             */
            int skin =
                    0xFF4CAF50;

            for (int y = 8;
                    y < 24;
                    y++) {

                for (int x = 8;
                        x < 24;
                        x++) {

                    tile.texture.setPixel(
                            x,
                            y,
                            skin
                    );
                }
            }

            /*
             * Olhos.
             */
            tile.texture.setPixel(
                    12,
                    13,
                    0xFFFFFFFF
            );

            tile.texture.setPixel(
                    19,
                    13,
                    0xFFFFFFFF
            );

            /*
             * Pupilas.
             */
            tile.texture.setPixel(
                    13,
                    13,
                    0xFF000000
            );

            tile.texture.setPixel(
                    20,
                    13,
                    0xFF000000
            );

            /*
             * Boca.
             */
            for (int x = 14;
                    x <= 18;
                    x++) {

                tile.texture.setPixel(
                        x,
                        18,
                        0xFF222222
                );
            }
        }

        /*
         * ====================================================
         * TEXTURA CREEPER
         * ====================================================
         */

        private void createCreeperTexture() {

            tile.texture.clear();

            int green =
                    0xFF3FAE3F;

            for (int y = 8;
                    y < 24;
                    y++) {

                for (int x = 8;
                        x < 24;
                        x++) {

                    tile.texture.setPixel(
                            x,
                            y,
                            green
                    );
                }
            }

            int dark =
                    0xFF111111;

            /*
             * Olhos.
             */
            tile.texture.setPixel(
                    12,
                    13,
                    dark
            );

            tile.texture.setPixel(
                    13,
                    13,
                    dark
            );

            tile.texture.setPixel(
                    19,
                    13,
                    dark
            );

            tile.texture.setPixel(
                    20,
                    13,
                    dark
            );

            /*
             * Boca.
             */
            tile.texture.setPixel(
                    15,
                    17,
                    dark
            );

            tile.texture.setPixel(
                    16,
                    17,
                    dark
            );

            tile.texture.setPixel(
                    17,
                    17,
                    dark
            );

            tile.texture.setPixel(
                    14,
                    18,
                    dark
            );

            tile.texture.setPixel(
                    15,
                    19,
                    dark
            );

            tile.texture.setPixel(
                    16,
                    19,
                    dark
            );

            tile.texture.setPixel(
                    17,
                    19,
                    dark
            );

            tile.texture.setPixel(
                    18,
                    18,
                    dark
            );
        }

        /*
         * ====================================================
         * MOUSE
         * ====================================================
         */

        @Override
        protected void mouseClicked(
                int mouseX,
                int mouseY,
                int mouseButton)
                throws IOException {

            super.mouseClicked(
                    mouseX,
                    mouseY,
                    mouseButton
            );

            if (guiMode == 0) {

                /*
                 * Selecionar cubo.
                 */
                if (mouseX >= 5 &&
                        mouseX <= 180 &&
                        mouseY >= 100) {

                    int index =
                            (mouseY - 108) /
                                    12;

                    if (index >= 0 &&
                            index <
                                    tile.cubes.size()) {

                        tile.selectedCube =
                                index;

                        updateFields();
                    }
                }

                /*
                 * Começar rotação.
                 */
                if (mouseX > 560) {

                    draggingCamera =
                            true;

                    lastMouseX =
                            mouseX;

                    lastMouseY =
                            mouseY;
                }

            } else {

                /*
                 * Pintar textura.
                 */
                paintTexture(
                        mouseX,
                        mouseY,
                        mouseButton
                );
            }
        }

        @Override
        protected void mouseClickMove(
                int mouseX,
                int mouseY,
                int clickedMouseButton,
                long timeSinceLastClick) {

            if (guiMode == 0 &&
                    draggingCamera) {

                int dx =
                        mouseX -
                        lastMouseX;

                int dy =
                        mouseY -
                        lastMouseY;

                cameraYaw +=
                        dx *
                        0.7F;

                cameraPitch +=
                        dy *
                        0.7F;

                if (cameraPitch > 89) {
                    cameraPitch = 89;
                }

                if (cameraPitch < -89) {
                    cameraPitch = -89;
                }

                lastMouseX =
                        mouseX;

                lastMouseY =
                        mouseY;
            }

            if (guiMode == 1) {

                if (clickedMouseButton == 0 ||
                        clickedMouseButton == 1) {

                    paintTexture(
                            mouseX,
                            mouseY,
                            clickedMouseButton
                    );
                }
            }
        }

        @Override
        protected void mouseMovedOrUp(
                int mouseX,
                int mouseY,
                int state) {

            if (state == 0) {

                draggingCamera =
                        false;
            }
        }

        /*
         * ====================================================
         * PINTAR TEXTURA
         * ====================================================
         */

        private void paintTexture(
                int mouseX,
                int mouseY,
                int button) {

            int texX =
                    30;

            int texY =
                    145;

            int pixelSize =
                    12;

            int px =
                    (mouseX - texX) /
                            pixelSize;

            int py =
                    (mouseY - texY) /
                            pixelSize;

            if (px < 0 ||
                    py < 0 ||
                    px >= ModelTexture.WIDTH ||
                    py >= ModelTexture.HEIGHT) {

                return;
            }

            /*
             * Botão direito sempre apaga.
             */
            if (button == 1) {

                tile.texture.setPixel(
                        px,
                        py,
                        0x00000000
                );

            } else {

                tile.texture.setPixel(
                        px,
                        py,
                        paintColor
                );
            }
        }

        /*
         * ====================================================
         * KEYBOARD
         * ====================================================
         */

        @Override
        protected void keyTyped(
                char typedChar,
                int keyCode)
                throws IOException {

            if (guiMode == 0) {

                fieldX.textboxKeyTyped(
                        typedChar,
                        keyCode
                );

                fieldY.textboxKeyTyped(
                        typedChar,
                        keyCode
                );

                fieldZ.textboxKeyTyped(
                        typedChar,
                        keyCode
                );

                fieldWidth.textboxKeyTyped(
                        typedChar,
                        keyCode
                );

                fieldHeight.textboxKeyTyped(
                        typedChar,
                        keyCode
                );

                fieldDepth.textboxKeyTyped(
                        typedChar,
                        keyCode
                );

                fieldScale.textboxKeyTyped(
                        typedChar,
                        keyCode
                );

                fieldU.textboxKeyTyped(
                        typedChar,
                        keyCode
                );

                fieldV.textboxKeyTyped(
                        typedChar,
                        keyCode
                );

                fieldUVWidth.textboxKeyTyped(
                        typedChar,
                        keyCode
                );

                fieldUVHeight.textboxKeyTyped(
                        typedChar,
                        keyCode
                );

                applyFields();
            }

            super.keyTyped(
                    typedChar,
                    keyCode
            );
        }

        /*
         * ====================================================
         * CLICK CAMPOS
         * ====================================================
         */

        @Override
        protected void mouseReleased(
                int mouseX,
                int mouseY,
                int state) {

            if (guiMode == 0) {

                fieldX.mouseClicked(
                        mouseX,
                        mouseY,
                        state
                );

                fieldY.mouseClicked(
                        mouseX,
                        mouseY,
                        state
                );

                fieldZ.mouseClicked(
                        mouseX,
                        mouseY,
                        state
                );

                fieldWidth.mouseClicked(
                        mouseX,
                        mouseY,
                        state
                );

                fieldHeight.mouseClicked(
                        mouseX,
                        mouseY,
                        state
                );

                fieldDepth.mouseClicked(
                        mouseX,
                        mouseY,
                        state
                );

                fieldScale.mouseClicked(
                        mouseX,
                        mouseY,
                        state
                );

                fieldU.mouseClicked(
                        mouseX,
                        mouseY,
                        state
                );

                fieldV.mouseClicked(
                        mouseX,
                        mouseY,
                        state
                );

                fieldUVWidth.mouseClicked(
                        mouseX,
                        mouseY,
                        state
                );

                fieldUVHeight.mouseClicked(
                        mouseX,
                        mouseY,
                        state
                );

                applyFields();
            }

            draggingCamera =
                    false;
        }

        /*
         * ====================================================
         * UPDATE CAMPOS
         * ====================================================
         */

        private void applyFields() {

            if (guiMode != 0) {
                return;
            }

            ModelCube c =
                    tile.getSelectedCube();

            c.x =
                    parseFloat(
                            fieldX.getText(),
                            c.x
                    );

            c.y =
                    parseFloat(
                            fieldY.getText(),
                            c.y
                    );

            c.z =
                    parseFloat(
                            fieldZ.getText(),
                            c.z
                    );

            c.width =
                    parseFloat(
                            fieldWidth.getText(),
                            c.width
                    );

            c.height =
                    parseFloat(
                            fieldHeight.getText(),
                            c.height
                    );

            c.depth =
                    parseFloat(
                            fieldDepth.getText(),
                            c.depth
                    );

            c.scale =
                    parseFloat(
                            fieldScale.getText(),
                            c.scale
                    );

            c.u =
                    parseInt(
                            fieldU.getText(),
                            c.u
                    );

            c.v =
                    parseInt(
                            fieldV.getText(),
                            c.v
                    );

            c.uvWidth =
                    parseInt(
                            fieldUVWidth.getText(),
                            c.uvWidth
                    );

            c.uvHeight =
                    parseInt(
                            fieldUVHeight.getText(),
                            c.uvHeight
                    );

            tile.markDirty();
        }

        private float parseFloat(
                String value,
                float fallback) {

            try {

                return Float.parseFloat(
                        value
                );

            } catch (Exception e) {

                return fallback;
            }
        }

        private int parseInt(
                String value,
                int fallback) {

            try {

                return Integer.parseInt(
                        value
                );

            } catch (Exception e) {

                return fallback;
            }
        }

        /*
         * ====================================================
         * SALVAR SERVIDOR
         * ====================================================
         */

        private void saveToServer() {

            if (NETWORK == null) {
                return;
            }

            applyFields();

            NETWORK.sendToServer(
                    new SaveModelMessage(
                            tile.xCoord,
                            tile.yCoord,
                            tile.zCoord,
                            tile.createFullNBT()
                    )
            );
        }

        /*
         * ====================================================
         * INIT / DRAW TEXTFIELDS
         * ====================================================
         */

        @Override
        public void updateScreen() {

            super.updateScreen();

            if (guiMode == 0) {

                fieldX.updateCursorCounter();
                fieldY.updateCursorCounter();
                fieldZ.updateCursorCounter();

                fieldWidth.updateCursorCounter();
                fieldHeight.updateCursorCounter();
                fieldDepth.updateCursorCounter();

                fieldScale.updateCursorCounter();

                fieldU.updateCursorCounter();
                fieldV.updateCursorCounter();

                fieldUVWidth.updateCursorCounter();
                fieldUVHeight.updateCursorCounter();
            }
        }

        @Override
        public void onGuiClosed() {

            if (guiMode == 0) {

                applyFields();

                saveToServer();
            }

            super.onGuiClosed();
        }
    }
}
Como usar
Depois de compilar o mod, dentro do Minecraft 1.7.10:
/give @p 58321 1
ou simplesmente:
/modelcreator
Você receberá o bloco Criador de Modelo 3D.
Coloque o bloco no mundo e clique com o botão direito.
Fluxo:
Criador de Modelo 3D
        │
        └── Botão direito
                │
                ▼
          EDITOR 3D
          ├── Adicionar cubo
          ├── Duplicar
          ├── Remover
          ├── X/Y/Z
          ├── L/A/P
          ├── Escala
          ├── UV
          ├── Preview 3D
          └── Editor de textura
                    │
                    ▼
                 32 × 32
                    │
                    ├── Pintar pixels
                    ├── Apagar
                    ├── Limpar
                    ├── Zumbi
                    └── Creeper
Observação importante: o ID 58321 é um ID numérico legado do sistema de blocos do Forge 1.7.10. Se outro mod do seu modpack já ocupar esse ID, será necessário escolher outro.
Também deixei o salvamento passando pelo servidor, então o modelo não fica apenas na memória do cliente: ao clicar em SALVAR MODELO, os cubos e a textura são enviados ao servidor e gravados no TileEntity.
