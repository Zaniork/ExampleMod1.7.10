package com.myname.mymodid;

import java.util.Random;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import org.lwjgl.opengl.GL11;

@Mod(modid = SunGlareMod.MODID, name = "Sun Glare Mod", version = "1.2", acceptedMinecraftVersions = "[1.7.10]")
public class SunGlareMod {

    public static final String MODID = "sunglaremod";
    public static SunGlareMod instance;

    // ---- config: reflexo do sol ----
    static int     radius       = 14;
    static float   strength     = 0.45f;
    static boolean onlySolid    = true;
    static boolean reflectTop   = true;
    static boolean reflectSides = true;
    static float   rTint = 1.00f, gTint = 0.96f, bTint = 0.78f;

    // ---- config: reflexo da lua ----
    static boolean moonGlare    = true;
    static float   moonStrength = 0.28f;
    static float   mr = 0.60f, mg = 0.75f, mb = 1.00f;

    // ---- config: agua ----
    static boolean waterRipple   = true;
    static int     waterRadius   = 20;
    static float   waterStrength = 0.55f;
    static float   rippleSpeed   = 1.6f;
    static float   rippleFreq    = 0.9f;
    static float   wr = 0.70f, wg = 0.90f, wb = 1.00f;

    // ---- config: espuma ----
    static boolean waterFoam    = true;
    static float   foamStrength = 0.75f;

    // ---- config: cintilancia ----
    static boolean waterSparkle    = true;
    static float   sparkleStrength = 0.85f;
    static int     sparkleDensity  = 14; // 1..64, maior = mais sparkles

    public SunGlareMod() { instance = this; }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Configuration c = new Configuration(e.getSuggestedConfigurationFile());
        c.load();

        c.addCustomCategoryComment("reflexo_sol", "Reflexo do sol nos blocos");
        radius       = c.getInt("raio", "reflexo_sol", 14, 4, 40, "Alcance em blocos");
        strength     = c.getFloat("forca", "reflexo_sol", 0.45f, 0.05f, 1.5f, "Intensidade");
        onlySolid    = c.getBoolean("somente_solidos", "reflexo_sol", true, "So cubos cheios");
        reflectTop   = c.getBoolean("refletir_topo", "reflexo_sol", true, "Face de cima");
        reflectSides = c.getBoolean("refletir_laterais", "reflexo_sol", true, "Faces laterais");
        rTint        = c.getFloat("cor_r", "reflexo_sol", 1.00f, 0f, 1f, "Tom vermelho");
        gTint        = c.getFloat("cor_g", "reflexo_sol", 0.96f, 0f, 1f, "Tom verde");
        bTint        = c.getFloat("cor_b", "reflexo_sol", 0.78f, 0f, 1f, "Tom azul");

        c.addCustomCategoryComment("reflexo_lua", "Reflexo azulado da lua nos blocos (a noite)");
        moonGlare    = c.getBoolean("ativar", "reflexo_lua", true, "Ligar reflexo da lua");
        moonStrength = c.getFloat("forca", "reflexo_lua", 0.28f, 0.05f, 1.0f, "Intensidade");
        mr           = c.getFloat("cor_r", "reflexo_lua", 0.60f, 0f, 1f, "Tom vermelho");
        mg           = c.getFloat("cor_g", "reflexo_lua", 0.75f, 0f, 1f, "Tom verde");
        mb           = c.getFloat("cor_b", "reflexo_lua", 1.00f, 0f, 1f, "Tom azul");

        c.addCustomCategoryComment("agua", "Ondulacao / brilho na superficie da agua");
        waterRipple   = c.getBoolean("ativar", "agua", true, "Ligar ondulacao");
        waterRadius   = c.getInt("raio", "agua", 20, 4, 48, "Alcance em blocos");
        waterStrength = c.getFloat("forca", "agua", 0.55f, 0.05f, 1.5f, "Intensidade");
        rippleSpeed   = c.getFloat("velocidade", "agua", 1.6f, 0.1f, 6.0f, "Velocidade das ondas");
        rippleFreq    = c.getFloat("frequencia", "agua", 0.9f, 0.1f, 4.0f, "Frequencia");
        wr            = c.getFloat("cor_r", "agua", 0.70f, 0f, 1f, "Tom vermelho");
        wg            = c.getFloat("cor_g", "agua", 0.90f, 0f, 1f, "Tom verde");
        wb            = c.getFloat("cor_b", "agua", 1.00f, 0f, 1f, "Tom azul");

        c.addCustomCategoryComment("espuma", "Linha branca ondulada onde agua encosta em bloco solido");
        waterFoam    = c.getBoolean("ativar", "espuma", true, "Ligar espuma");
        foamStrength = c.getFloat("forca", "espuma", 0.75f, 0.05f, 1.5f, "Intensidade");

        c.addCustomCategoryComment("cintilancia", "Pontinhos brilhantes aleatorios na agua");
        waterSparkle    = c.getBoolean("ativar", "cintilancia", true, "Ligar cintilancia");
        sparkleStrength = c.getFloat("forca", "cintilancia", 0.85f, 0.05f, 1.5f, "Intensidade");
        sparkleDensity  = c.getInt("densidade", "cintilancia", 14, 1, 64, "1..64, maior = mais sparkles");

        c.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new Handler());
        }
    }

    // ================= HANDLER =================
    @SideOnly(Side.CLIENT)
    public static class Handler {
        @SubscribeEvent
        public void onRenderWorld(RenderWorldLastEvent e) {
            float pt = e.partialTicks;
            Renderer.renderGlare(pt);           // sol
            if (moonGlare)    Renderer.renderMoon(pt);   // lua
            if (waterRipple)  Renderer.renderWater(pt);
            if (waterFoam)    Renderer.renderFoam(pt);
            if (waterSparkle) Renderer.renderSparkles(pt);
        }
    }

    // ================= RENDER =================
    @SideOnly(Side.CLIENT)
    public static class Renderer {

        private static final Random rng = new Random();

        // ====== REFLEXO DO SOL ======
        public static void renderGlare(float pt) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.thePlayer == null || mc.isGamePaused()) return;

            World w = mc.theWorld;
            EntityPlayer p = mc.thePlayer;

            float celest = w.getCelestialAngle(pt);
            double sunAngle = celest * Math.PI * 2.0;
            double sunX = -Math.sin(sunAngle);
            double sunY =  Math.cos(sunAngle);
            if (sunY <= 0.05) return;

            double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt;
            double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt;
            double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt;

            int bx = MathHelper.floor_double(px);
            int by = MathHelper.floor_double(py);
            int bz = MathHelper.floor_double(pz);

            beginAdditive(px, py, pz);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();

            int r = radius, r2 = r * r;
            for (int x = bx - r; x <= bx + r; x++) {
                for (int y = by - r; y <= by + r; y++) {
                    if (y < 0 || y > 255) continue;
                    for (int z = bz - r; z <= bz + r; z++) {
                        Block b = w.getBlock(x, y, z);
                        if (b == null || b.getMaterial() == Material.air) continue;
                        if (onlySolid && (!b.isOpaqueCube() || b.getRenderType() != 0)) continue;

                        double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
                        double d2 = dx*dx + dy*dy + dz*dz;
                        if (d2 > r2) continue;
                        double falloff = 1.0 - Math.sqrt(d2) / r;

                        if (reflectTop && isAir(w, x, y + 1, z)) {
                            double a = sunY * falloff * strength;
                            if (a > 0.02) quad(t,
                                x, y+1.002, z, x+1, y+1.002, z,
                                x+1, y+1.002, z+1, x, y+1.002, z+1,
                                rTint, gTint, bTint, a);
                        }
                        if (reflectSides && sunX > 0 && isAir(w, x + 1, y, z)) {
                            double a = sunX * falloff * strength;
                            if (a > 0.02) quad(t,
                                x+1.002, y, z, x+1.002, y, z+1,
                                x+1.002, y+1, z+1, x+1.002, y+1, z,
                                rTint, gTint, bTint, a);
                        }
                        if (reflectSides && sunX < 0 && isAir(w, x - 1, y, z)) {
                            double a = -sunX * falloff * strength;
                            if (a > 0.02) quad(t,
                                x-0.002, y, z, x-0.002, y+1, z,
                                x-0.002, y+1, z+1, x-0.002, y, z+1,
                                rTint, gTint, bTint, a);
                        }
                    }
                }
            }
            t.draw();
            endAdditive();
        }

        // ====== REFLEXO DA LUA ======
        // Mesma logica do sol, mas com sinal invertido (sol baixo -> lua alta)
        // e cor azulada. Roda so quando o sol esta abaixo do horizonte.
        public static void renderMoon(float pt) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.thePlayer == null || mc.isGamePaused()) return;

            World w = mc.theWorld;
            EntityPlayer p = mc.thePlayer;

            float celest = w.getCelestialAngle(pt);
            double sunAngle = celest * Math.PI * 2.0;
            double sunY = Math.cos(sunAngle);
            if (sunY >= -0.05) return;        // só quando é noite
            double moonY = -sunY;             // altura da lua (positiva)
            double moonX = -Math.sin(sunAngle + Math.PI);

            double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt;
            double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt;
            double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt;

            int bx = MathHelper.floor_double(px);
            int by = MathHelper.floor_double(py);
            int bz = MathHelper.floor_double(pz);

            beginAdditive(px, py, pz);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();

            int r = radius, r2 = r * r;
            for (int x = bx - r; x <= bx + r; x++) {
                for (int y = by - r; y <= by + r; y++) {
                    if (y < 0 || y > 255) continue;
                    for (int z = bz - r; z <= bz + r; z++) {
                        Block b = w.getBlock(x, y, z);
                        if (b == null || b.getMaterial() == Material.air) continue;
                        if (onlySolid && (!b.isOpaqueCube() || b.getRenderType() != 0)) continue;

                        double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
                        double d2 = dx*dx + dy*dy + dz*dz;
                        if (d2 > r2) continue;
                        double falloff = 1.0 - Math.sqrt(d2) / r;

                        if (reflectTop && isAir(w, x, y + 1, z)) {
                            double a = moonY * falloff * moonStrength;
                            if (a > 0.02) quad(t,
                                x, y+1.002, z, x+1, y+1.002, z,
                                x+1, y+1.002, z+1, x, y+1.002, z+1,
                                mr, mg, mb, a);
                        }
                        if (reflectSides && moonX > 0 && isAir(w, x + 1, y, z)) {
                            double a = moonX * falloff * moonStrength;
                            if (a > 0.02) quad(t,
                                x+1.002, y, z, x+1.002, y, z+1,
                                x+1.002, y+1, z+1, x+1.002, y+1, z,
                                mr, mg, mb, a);
                        }
                        if (reflectSides && moonX < 0 && isAir(w, x - 1, y, z)) {
                            double a = -moonX * falloff * moonStrength;
                            if (a > 0.02) quad(t,
                                x-0.002, y, z, x-0.002, y+1, z,
                                x-0.002, y+1, z+1, x-0.002, y, z+1,
                                mr, mg, mb, a);
                        }
                    }
                }
            }
            t.draw();
            endAdditive();
        }

        // ====== ONDULACAO NA AGUA ======
        public static void renderWater(float pt) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.thePlayer == null || mc.isGamePaused()) return;

            World w = mc.theWorld;
            EntityPlayer p = mc.thePlayer;

            float celest = w.getCelestialAngle(pt);
            double sunY = Math.cos(celest * Math.PI * 2.0);
            if (Math.abs(sunY) <= 0.05) return;
            boolean day = sunY > 0;

            double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt;
            double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt;
            double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt;
            int bx = MathHelper.floor_double(px);
            int by = MathHelper.floor_double(py);
            int bz = MathHelper.floor_double(pz);

            double t = (w.getTotalWorldTime() + pt) * 0.05;

            beginAdditive(px, py, pz);
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();

            int r = waterRadius, r2 = r * r;
            double absSun = Math.abs(sunY);

            for (int x = bx - r; x <= bx + r; x++) {
                for (int y = by - r; y <= by + r; y++) {
                    if (y < 0 || y > 255) continue;
                    for (int z = bz - r; z <= bz + r; z++) {
                        Block b = w.getBlock(x, y, z);
                        if (b == null || b.getMaterial() != Material.water) continue;
                        if (!isAir(w, x, y + 1, z)) continue;

                        double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
                        double d2 = dx*dx + dy*dy + dz*dz;
                        if (d2 > r2) continue;
                        double falloff = 1.0 - Math.sqrt(d2) / r;

                        double wave =
                            Math.sin(x * rippleFreq + t * rippleSpeed) * 0.5
                          + Math.cos(z * rippleFreq + t * rippleSpeed * 0.85) * 0.5;

                        double a = (wave * 0.5 + 0.5) * falloff * waterStrength * absSun;
                        if (a <= 0.02) continue;

                        float rr = wr, gg = wg, bb = wb;
                        if (day && sunY < 0.35) {
                            float k = (float)(1.0 - sunY / 0.35);
                            rr = wr + (1.00f - wr) * k * 0.6f;
                            gg = wg + (0.55f - wg) * k * 0.4f;
                            bb = wb + (0.30f - wb) * k * 0.5f;
                        } else if (!day) {
                            // lua refletindo na agua
                            rr = 0.55f; gg = 0.70f; bb = 1.00f;
                        }

                        double ytop = y + 1.005 + wave * 0.004;
                        quad(tess,
                            x, ytop, z, x + 1, ytop, z,
                            x + 1, ytop, z + 1, x, ytop, z + 1,
                            rr, gg, bb, a);
                    }
                }
            }
            tess.draw();
            endAdditive();
        }

        // ====== ESPUMA NAS BORDAS ======
        // Onde a agua encosta em bloco solido (lateral), desenha uma faixa
        // branca ondulada em cima da agua, seguindo o mesmo seno do ripple.
        public static void renderFoam(float pt) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.thePlayer == null || mc.isGamePaused()) return;

            World w = mc.theWorld;
            EntityPlayer p = mc.thePlayer;

            double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt;
            double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt;
            double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt;
            int bx = MathHelper.floor_double(px);
            int by = MathHelper.floor_double(py);
            int bz = MathHelper.floor_double(pz);

            double t = (w.getTotalWorldTime() + pt) * 0.06;

            beginAdditive(px, py, pz);
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();

            int r = waterRadius, r2 = r * r;
            double w0 = 0.08;  // largura da faixa (em bloco)

            for (int x = bx - r; x <= bx + r; x++) {
                for (int y = by - r; y <= by + r; y++) {
                    if (y < 0 || y > 255) continue;
                    for (int z = bz - r; z <= bz + r; z++) {
                        Block b = w.getBlock(x, y, z);
                        if (b == null || b.getMaterial() != Material.water) continue;
                        if (!isAir(w, x, y + 1, z)) continue;

                        double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
                        double d2 = dx*dx + dy*dy + dz*dz;
                        if (d2 > r2) continue;
                        double falloff = 1.0 - Math.sqrt(d2) / r;

                        // oscilacao global (mesma fase do ripple pra parecer continua)
                        double wave =
                            Math.sin(x * rippleFreq + t * rippleSpeed) * 0.5
                          + Math.cos(z * rippleFreq + t * rippleSpeed * 0.85) * 0.5;
                        double yt = y + 1.007 + wave * 0.003;

                        // checa cada lado: se vizinho e solido e nao-agua, poe faixa
                        boolean nN = solidSide(w, x, y, z - 1);
                        boolean nS = solidSide(w, x, y, z + 1);
                        boolean nW = solidSide(w, x - 1, y, z);
                        boolean nE = solidSide(w, x + 1, y, z);

                        double a = falloff * foamStrength * (0.55 + 0.45 * (wave * 0.5 + 0.5));
                        if (a <= 0.03) continue;
                        float fa = (float) Math.min(1.0, a);

                        // faixa branca em cada lado encostado (com leve jitter)
                        double jitter = 0.005 * Math.sin(t * 3 + x + z);
                        if (nN) quad(tess,
                            x, yt, z + jitter, x + 1, yt, z + jitter,
                            x + 1, yt, z + w0 + jitter, x, yt, z + w0 + jitter,
                            1f, 1f, 1f, fa);
                        if (nS) quad(tess,
                            x, yt, z + 1 - w0 + jitter, x + 1, yt, z + 1 - w0 + jitter,
                            x + 1, yt, z + 1 + jitter, x, yt, z + 1 + jitter,
                            1f, 1f, 1f, fa);
                        if (nW) quad(tess,
                            x + jitter, yt, z, x + w0 + jitter, yt, z,
                            x + w0 + jitter, yt, z + 1, x + jitter, yt, z + 1,
                            1f, 1f, 1f, fa);
                        if (nE) quad(tess,
                            x + 1 - w0 + jitter, yt, z, x + 1 + jitter, yt, z,
                            x + 1 + jitter, yt, z + 1, x + 1 - w0 + jitter, yt, z + 1,
                            1f, 1f, 1f, fa);
                    }
                }
            }
            tess.draw();
            endAdditive();
        }

        // ====== CINTILANCIA ======
        // Em agua calma (sem bloco solido em volta), desenha pontinhos
        // aleatorios que piscam devagar. Deterministico por posicao + tempo.
        public static void renderSparkles(float pt) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.thePlayer == null || mc.isGamePaused()) return;

            World w = mc.theWorld;
            EntityPlayer p = mc.thePlayer;

            // menos sparkles a noite
            float celest = w.getCelestialAngle(pt);
            double sunY = Math.cos(celest * Math.PI * 2.0);
            double light = 0.35 + 0.65 * Math.max(0, sunY); // 0.35 a 1.0

            double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt;
            double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt;
            double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt;
            int bx = MathHelper.floor_double(px);
            int by = MathHelper.floor_double(py);
            int bz = MathHelper.floor_double(pz);

            double t = (w.getTotalWorldTime() + pt) * 0.12;

            beginAdditive(px, py, pz);
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();

            int r = waterRadius, r2 = r * r;
            // probabilidade por bloco por frame: density/256
            int chance = sparkleDensity;

            for (int x = bx - r; x <= bx + r; x++) {
                for (int y = by - r; y <= by + r; y++) {
                    if (y < 0 || y > 255) continue;
                    for (int z = bz - r; z <= bz + r; z++) {
                        Block b = w.getBlock(x, y, z);
                        if (b == null || b.getMaterial() != Material.water) continue;
                        if (!isAir(w, x, y + 1, z)) continue;

                        // só em água "calma" — sem bloco solido em volta
                        if (solidSide(w, x, y, z - 1) || solidSide(w, x, y, z + 1)
                         || solidSide(w, x - 1, y, z) || solidSide(w, x + 1, y, z)) continue;

                        double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
                        double d2 = dx*dx + dy*dy + dz*dz;
                        if (d2 > r2) continue;
                        double falloff = 1.0 - Math.sqrt(d2) / r;

                        // hash pseudo-aleatorio por bloco + pulso de tempo
                        long h = (x * 73856093L) ^ (y * 19349663L) ^ (z * 83492791L);
                        rng.setSeed(h);
                        int roll = rng.nextInt(256);
                        if (roll > chance) continue;

                        // fase propria para cada sparkle (pisca com o tempo)
                        double phase = (h & 0xFFFF) / 65535.0 * Math.PI * 2;
                        double pulse = 0.5 + 0.5 * Math.sin(t * 2.5 + phase);
                        if (pulse < 0.25) continue;

                        double a = pulse * falloff * sparkleStrength * light * 0.9;
                        if (a <= 0.03) continue;

                        // posicao dentro do bloco (deterministica)
                        double offX = ((h >>>  8) & 0xFF) / 255.0 * 0.7 + 0.15;
                        double offZ = ((h >>> 16) & 0xFF) / 255.0 * 0.7 + 0.15;
                        double size = 0.04 + ((h >>> 24) & 0x0F) / 255.0 * 0.05;

                        double yt = y + 1.010;
                        double cx = x + offX;
                        double cz = z + offZ;

                        quad(tess,
                            cx - size, yt, cz - size,
                            cx + size, yt, cz - size,
                            cx + size, yt, cz + size,
                            cx - size, yt, cz + size,
                            1f, 1f, 0.95f, a);
                    }
                }
            }
            tess.draw();
            endAdditive();
        }

        // ====== helpers ======
        private static boolean solidSide(World w, int x, int y, int z) {
            Block b = w.getBlock(x, y, z);
            if (b == null) return false;
            if (b.getMaterial() == Material.water) return false;
            if (b.getMaterial() == Material.air)   return false;
            return b.isOpaqueCube();
        }

        private static void beginAdditive(double px, double py, double pz) {
            GL11.glPushMatrix();
            GL11.glTranslated(-px, -py, -pz);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);
        }

        private static void endAdditive() {
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }

        private static boolean isAir(World w, int x, int y, int z) {
            if (y < 0 || y > 255) return true;
            Block b = w.getBlock(x, y, z);
            return b == null || b.getMaterial() == Material.air;
        }

        private static void quad(Tessellator t,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 double x3, double y3, double z3,
                                 double x4, double y4, double z4,
                                 float r, float g, float b, double a) {
            float alpha = (float) Math.min(1.0, a);
            t.setColorRGBA_F(r, g, b, alpha);
            t.addVertex(x1, y1, z1);
            t.addVertex(x2, y2, z2);
            t.addVertex(x3, y3, z3);
            t.addVertex(x4, y4, z4);
        }
    }
}
