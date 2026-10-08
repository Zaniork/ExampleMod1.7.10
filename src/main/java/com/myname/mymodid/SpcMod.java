package com.myname.mymodid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

@Mod(modid = SpcMod.MODID, name = "Spc Mod", version = "1.0", acceptedMinecraftVersions = "[1.7.10]")
public class SpcMod {

    public static final String MODID = "spcmod";
    public static SpcMod instance;
    public static Block pcBlock;

    public SpcMod() { instance = this; }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        pcBlock = new BlockPC();
        pcBlock.setBlockName("spc_pc");
        pcBlock.setHardness(2.0F);
        pcBlock.setCreativeTab(CreativeTabs.tabRedstone);
        GameRegistry.registerBlock(pcBlock, "spc_pc");
        GameRegistry.registerTileEntity(TilePC.class, "spc_pc_tile");

        GameRegistry.addRecipe(new ItemStack(pcBlock),
            "III", "IRI", "III",
            'I', Items.iron_ingot,
            'R', Items.redstone);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
    }

    // ================= BLOCK =================
    public static class BlockPC extends BlockContainer {
        public BlockPC() { super(Material.iron); }

        @Override
        public TileEntity createNewTileEntity(World w, int meta) { return new TilePC(); }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon getIcon(int side, int meta) {
            // "textura em codigo": reaproveita IIcon vanilla sem precisar de assets
            // (pra usar textura propria, chame setBlockTextureName("spcmod:pc") no preInit)
            return Blocks.iron_block.getIcon(side, 0);
        }

        @Override
        public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p,
                                        int side, float hx, float hy, float hz) {
            if (!w.isRemote) p.openGui(instance, 0, w, x, y, z);
            return true;
        }

        @Override
        public boolean isOpaqueCube() { return true; }
    }

    // ================= TILE =================
    public static class TilePC extends TileEntity { }

    // ================= GUI HANDLER =================
    public static class GuiHandler implements IGuiHandler {
        @Override
        public Object getServerGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
            return null;
        }
        @Override
        @SideOnly(Side.CLIENT)
        public Object getClientGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
            if (id == 0) return new GuiTerminal();
            return null;
        }
    }

    // ================= TERMINAL =================
    @SideOnly(Side.CLIENT)
    public static class GuiTerminal extends GuiScreen {
        private GuiTextField input;
        private final List<String> lines = new ArrayList<String>();
        private String pendingGame;
        private int genTicks;

        @Override
        public void initGui() {
            Keyboard.enableRepeatEvents(true);
            this.input = new GuiTextField(fontRendererObj, width/2 - 150, height - 40, 300, 18);
            this.input.setFocused(true);
            this.input.setMaxStringLength(40);
            lines.clear();
            lines.add("SPC-OS v1.0  [procedural AI generator]");
            lines.add("----------------------------------------");
            lines.add("Digite o nome de um jogo e aperte ENTER.");
            lines.add("Ex: space shooter, corrida, cobra, gelo...");
            lines.add("");
            pendingGame = null;
            genTicks = 0;
        }

        @Override
        public void updateScreen() {
            input.updateCursorCounter();
            if (pendingGame != null) {
                genTicks++;
                if (genTicks == 15) lines.add("> Analisando \"" + pendingGame + "\"...");
                if (genTicks == 35) lines.add("> Extraindo mecanicas...");
                if (genTicks == 55) lines.add("> Proceduralizando regras...");
                if (genTicks == 75) lines.add("> Compilando...");
                if (genTicks == 95) {
                    mc.displayGuiScreen(new GuiGame(pendingGame));
                    pendingGame = null;
                    genTicks = 0;
                }
            }
        }

        @Override
        protected void keyTyped(char c, int key) {
            if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(null); return; }
            if (pendingGame != null) return;
            if (key == Keyboard.KEY_RETURN) {
                String s = input.getText().trim();
                if (!s.isEmpty()) {
                    lines.add("> " + s);
                    pendingGame = s;
                    input.setText("");
                }
                return;
            }
            input.textboxKeyTyped(c, key);
        }

        @Override
        protected void mouseClicked(int mx, int my, int btn) {
            input.mouseClicked(mx, my, btn);
        }

        @Override
        public void drawScreen(int mx, int my, float pt) {
            drawRect(0, 0, width, height, 0xFF000000);
            int bx1 = width/2 - 170, by1 = 20;
            int bx2 = width/2 + 170, by2 = height - 60;
            drawRect(bx1, by1, bx2, by2, 0xFF101010);
            drawRect(bx1+2, by1+2, bx2-2, by2-2, 0xFF001800);
            for (int i = 0; i < lines.size(); i++) {
                drawString(fontRendererObj, lines.get(i), bx1+8, by1+8 + i*11, 0xFF33FF55);
            }
            input.drawTextBox();
            drawString(fontRendererObj, "SPC-OS terminal - ESC para sair", 4, height - 10, 0xFF666666);
        }

        @Override
        public boolean doesGuiPauseGame() { return false; }
    }

    // ================= GAME (gerado proceduralmente) =================
    @SideOnly(Side.CLIENT)
    public static class GuiGame extends GuiScreen {
        private final String gameName;
        private final Random rng;

        private int cols = 30, rows = 20;
        private int[] sx, sy;
        private int snakeLen = 3;
        private int dx = 1, dy = 0;
        private int foodX, foodY;
        private int score = 0;
        private int tick = 0;
        private int speed = 8;
        private boolean gameOver = false;
        private long startTime;

        private int bgColor, snakeColor, headColor, foodColor, gridColor, textColor;
        private String genre = "arcade";

        public GuiGame(String name) {
            this.gameName = name;
            this.rng = new Random(name.toLowerCase().hashCode());
            proceduralize();
        }

        // ============ IA PROCEDURAL ============
        // Le o nome digitado, extrai "intencoes" por palavra-chave e
        // gera parametros do jogo (cor, velocidade, tamanho, tema).
        private void proceduralize() {
            String n = gameName.toLowerCase();
            bgColor    = 0xFF001020;
            snakeColor = 0xFF00CC55;
            headColor  = 0xFFFFFFFF;
            foodColor  = 0xFFFFCC00;
            gridColor  = 0xFF102030;
            textColor  = 0xFF33FF55;
            speed = 8; cols = 30; rows = 20;
            genre = "arcade";

            if (has(n, "space","nave","star","galax","cosmo")) {
                bgColor = 0xFF000008; snakeColor = 0xFF44AAFF; headColor = 0xFFFFFFFF;
                foodColor = 0xFFFF3333; gridColor = 0xFF0A1A2A; genre = "space shooter"; speed = 7;
            }
            if (has(n, "race","corrida","speed","fast","turbo","veloz")) {
                bgColor = 0xFF201000; snakeColor = 0xFFFF8800; headColor = 0xFFFFEE88;
                foodColor = 0xFF00FFFF; genre = "corrida"; speed = 4;
            }
            if (has(n, "slow","calm","relax","puzzle","zen")) {
                speed = 14; snakeColor = 0xFFAA88FF; genre = "puzzle";
            }
            if (has(n, "hard","hardcore","extreme","dificil","impossivel")) {
                speed = 3; genre = "hardcore";
            }
            if (has(n, "snake","cobra","serpente")) {
                bgColor = 0xFF002200; snakeColor = 0xFF00FF00; foodColor = 0xFFFF0000; genre = "snake";
            }
            if (has(n, "fire","fogo","inferno","lava","flame")) {
                bgColor = 0xFF200000; snakeColor = 0xFFFF5500; foodColor = 0xFFFFFF00; genre = "fire";
            }
            if (has(n, "ice","gelo","snow","neve","frost")) {
                bgColor = 0xFFD0E8FF; snakeColor = 0xFF2050A0; headColor = 0xFF102050;
                foodColor = 0xFFFF2222; gridColor = 0xFFA0C0E0; textColor = 0xFF102050; genre = "ice";
            }
            if (has(n, "huge","grande","big","epic","mega")) { cols = 40; rows = 25; }
            if (has(n, "tiny","mini","small","pequeno"))    { cols = 18; rows = 14; }

            sx = new int[cols * rows];
            sy = new int[cols * rows];
            snakeLen = 3;
            for (int i = 0; i < snakeLen; i++) { sx[i] = cols/2 - i; sy[i] = rows/2; }
            dx = 1; dy = 0;
            spawnFood();
            startTime = System.currentTimeMillis();
        }

        private boolean has(String src, String... keys) {
            for (String k : keys) if (src.contains(k)) return true;
            return false;
        }

        private void spawnFood() {
            int fx, fy;
            do { fx = rng.nextInt(cols); fy = rng.nextInt(rows); } while (hitsSnake(fx, fy));
            foodX = fx; foodY = fy;
        }

        private boolean hitsSnake(int x, int y) {
            for (int i = 0; i < snakeLen; i++) if (sx[i]==x && sy[i]==y) return true;
            return false;
        }

        private void step() {
            int nx = sx[0] + dx;
            int ny = sy[0] + dy;
            if (nx < 0) nx = cols-1; else if (nx >= cols) nx = 0;
            if (ny < 0) ny = rows-1; else if (ny >= rows) ny = 0;
            for (int i = 0; i < snakeLen; i++) if (sx[i]==nx && sy[i]==ny) { gameOver = true; return; }
            for (int i = snakeLen-1; i > 0; i--) { sx[i] = sx[i-1]; sy[i] = sy[i-1]; }
            sx[0] = nx; sy[0] = ny;
            if (nx == foodX && ny == foodY) {
                if (snakeLen < sx.length) {
                    sx[snakeLen] = sx[snakeLen-1];
                    sy[snakeLen] = sy[snakeLen-1];
                    snakeLen++;
                }
                score += 10;
                spawnFood();
            }
        }

        @Override
        public void initGui() { Keyboard.enableRepeatEvents(true); }

        @Override
        public void updateScreen() {
            if (gameOver) return;
            tick++;
            if (tick >= speed) { tick = 0; step(); }
        }

        @Override
        protected void keyTyped(char c, int key) {
            if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(null); return; }
            if (gameOver) {
                if (key == Keyboard.KEY_SPACE) { proceduralize(); score = 0; gameOver = false; tick = 0; }
                return;
            }
            if (key == Keyboard.KEY_UP    && dy == 0) { dx = 0; dy = -1; }
            else if (key == Keyboard.KEY_DOWN  && dy == 0) { dx = 0; dy = 1; }
            else if (key == Keyboard.KEY_LEFT  && dx == 0) { dx = -1; dy = 0; }
            else if (key == Keyboard.KEY_RIGHT && dx == 0) { dx = 1; dy = 0; }
        }

        @Override
        public void drawScreen(int mx, int my, float pt) {
            drawRect(0, 0, width, height, 0xFF000000);
            int cellSize = Math.min((width - 60) / cols, (height - 100) / rows);
            if (cellSize < 4) cellSize = 4;
            int gw = cellSize * cols;
            int gh = cellSize * rows;
            int ox = (width - gw) / 2;
            int oy = (height - gh) / 2 + 6;

            drawRect(ox-3, oy-3, ox+gw+3, oy+gh+3, gridColor);
            drawRect(ox, oy, ox+gw, oy+gh, bgColor);

            drawRect(ox + foodX*cellSize + 1, oy + foodY*cellSize + 1,
                     ox + (foodX+1)*cellSize - 1, oy + (foodY+1)*cellSize - 1, foodColor);

            for (int i = 0; i < snakeLen; i++) {
                int col = (i == 0) ? headColor : snakeColor;
                drawRect(ox + sx[i]*cellSize + 1, oy + sy[i]*cellSize + 1,
                         ox + (sx[i]+1)*cellSize - 1, oy + (sy[i]+1)*cellSize - 1, col);
            }

            drawString(fontRendererObj, "SPC-OS // " + gameName + "  [" + genre + "]", 8, 8, textColor);
            long secs = (System.currentTimeMillis() - startTime) / 1000L;
            drawString(fontRendererObj,
                "Score: " + score + "  Len: " + snakeLen + "  Speed: " + (12 - speed) + "  Tempo: " + secs + "s",
                8, 20, textColor);
            drawString(fontRendererObj, "Setas: mover   ESC: sair do jogo", 8, height - 20, 0xFF888888);

            if (gameOver) {
                drawRect(width/2 - 120, height/2 - 40, width/2 + 120, height/2 + 40, 0xDD000000);
                drawCenteredString(fontRendererObj, "GAME OVER", width/2, height/2 - 24, 0xFFFF4444);
                drawCenteredString(fontRendererObj, "Score final: " + score, width/2, height/2 - 8, 0xFFFFFFFF);
                drawCenteredString(fontRendererObj, "ESPAÇO para reiniciar", width/2, height/2 + 8, 0xFF33FF55);
                drawCenteredString(fontRendererObj, "ESC para sair", width/2, height/2 + 24, 0xFF888888);
            }
        }

        @Override
        public boolean doesGuiPauseGame() { return false; }
    }
}
