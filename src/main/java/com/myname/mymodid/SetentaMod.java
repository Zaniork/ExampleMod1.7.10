package com.myname.mymodid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingEvent;

@Mod(modid = SetentaMod.MODID, name = "Setenta Mod", version = "1.0", acceptedMinecraftVersions = "[1.7.10]")
public class SetentaMod {

    public static final String MODID = "setentamod";

    // ---------- configuracao (perfis) ----------
    static String profile = "AGRESSIVO";
    static boolean adaptiveTick = true;
    static boolean entityCull = true;
    static boolean adaptiveClient = true;
    static boolean metrics = true;
    static int d2 = 32, d4 = 64, d8 = 96, d16 = 128; // distancias do ticking adaptativo
    static int cullDist = 64;
    static int fpsLow = 30, fpsHigh = 75, minRender = 4;
    static String[] blacklist = new String[0];

    // ---------- metricas ----------
    static volatile double msptAvg = 0;
    static volatile int skippedPerSec = 0;
    static volatile int culledPerSec = 0;
    static volatile int fps = 0;
    static int skippedCount = 0;
    static int culledCount = 0;

    static void applyProfile(String p) {
        profile = p == null ? "AGRESSIVO" : p.toUpperCase();
        if ("LEVE".equals(profile)) {
            d2 = 64;
            d4 = 96;
            d8 = 128;
            d16 = 192;
            cullDist = 128;
            fpsLow = 20;
            fpsHigh = 60;
        } else if ("EXTREMO".equals(profile)) {
            d2 = 24;
            d4 = 48;
            d8 = 72;
            d16 = 96;
            cullDist = 48;
            fpsLow = 45;
            fpsHigh = 90;
        } else {
            profile = "AGRESSIVO";
            d2 = 32;
            d4 = 64;
            d8 = 96;
            d16 = 128;
            cullDist = 64;
            fpsLow = 30;
            fpsHigh = 75;
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Configuration c = new Configuration(e.getSuggestedConfigurationFile());
        c.load();
        String p = c.getString("perfil", "geral", "AGRESSIVO", "LEVE, AGRESSIVO ou EXTREMO (define distancias e limites de FPS)");
        adaptiveTick = c.getBoolean("ticking_adaptativo", "modulos", true, "Entidades vivas distantes tickam menos (so servidor)");
        entityCull = c.getBoolean("culling_entidades", "modulos", true, "Nao renderiza entidades vivas alem da distancia do perfil (cliente)");
        adaptiveClient = c.getBoolean("cliente_adaptativo", "modulos", true, "Reduz render distance/graficos/particulas quando o FPS cai (cliente)");
        metrics = c.getBoolean("metricas_f3", "modulos", true, "Mostra metricas do mod no F3");
        minRender = c.getInt("render_minimo", "cliente", 4, 2, 16, "Menor render distance que o modo adaptativo pode usar");
        blacklist = c.getStringList("ticking_blacklist", "modulos", new String[0],
            "Trechos de nome de classe que NUNCA tem o tick pulado (ex.: mods com mobs sensiveis)");
        c.save();
        applyProfile(p);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        CommonEvents ce = new CommonEvents();
        MinecraftForge.EVENT_BUS.register(ce);
        FMLCommonHandler.instance().bus().register(ce);
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientEvents cl = new ClientEvents();
            MinecraftForge.EVENT_BUS.register(cl);
            FMLCommonHandler.instance().bus().register(cl);
        }
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new CmdSetenta());
    }

    // ================= SERVIDOR: ticking adaptativo + mspt =================
    public static class CommonEvents {
        long t0 = 0;
        int counter = 0;
        final Map<Class<?>, Boolean> protectedCache = new HashMap<Class<?>, Boolean>();

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                t0 = System.nanoTime();
            } else {
                double ms = (System.nanoTime() - t0) / 1000000.0;
                msptAvg = msptAvg * 0.95 + ms * 0.05;
                if (++counter >= 20) {
                    counter = 0;
                    skippedPerSec = skippedCount;
                    skippedCount = 0;
                }
            }
        }

        boolean isProtected(EntityLivingBase en) {
            Class<?> k = en.getClass();
            Boolean b = protectedCache.get(k);
            if (b == null) {
                boolean r = false;
                String n = k.getName();
                for (String s : blacklist) {
                    if (s != null && s.length() > 0 && n.contains(s)) r = true;
                }
                b = Boolean.valueOf(r);
                protectedCache.put(k, b);
            }
            return b.booleanValue();
        }

        @SubscribeEvent
        public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
            if (!adaptiveTick) return;
            EntityLivingBase en = e.entityLiving;
            if (en.worldObj == null || en.worldObj.isRemote) return;
            if (en instanceof EntityPlayer || en instanceof IBossDisplayData) return;
            if (en.riddenByEntity != null || en.ridingEntity != null) return;
            if (en.hurtTime > 0 || en.deathTime > 0 || en.getHealth() <= 0) return;
            if (en instanceof EntityLiving) {
                EntityLiving el = (EntityLiving) en;
                if (el.getAttackTarget() != null || el.getLeashed()) return;
            }
            if (isProtected(en)) return;

            List players = en.worldObj.playerEntities;
            if (players.isEmpty()) return;
            double min = Double.MAX_VALUE;
            for (int i = 0; i < players.size(); i++) {
                double d = en.getDistanceSqToEntity((EntityPlayer) players.get(i));
                if (d < min) min = d;
            }
            int iv = min > (double) d16 * d16 ? 16 : (min > (double) d8 * d8 ? 8 : (min > (double) d4 * d4 ? 4 : (min > (double) d2 * d2 ? 2 : 1)));
            if (iv > 1 && (en.worldObj.getTotalWorldTime() + en.getEntityId()) % iv != 0) {
                e.setCanceled(true);
                skippedCount++;
            }
        }
    }

    // ================= CLIENTE: culling, FPS adaptativo, F3 =================
    @SideOnly(Side.CLIENT)
    public static class ClientEvents {
        int frames = 0;
        long last = 0;
        long lastAdjust = 0;
        int lowSeconds = 0;
        int origRender = -1;
        boolean origFancy = true;
        int origParticles = 0;

        @SubscribeEvent
        public void onRenderLiving(RenderLivingEvent.Pre e) {
            if (!entityCull) return;
            if (e.entity instanceof EntityPlayer || e.entity instanceof IBossDisplayData) return;
            double d = e.x * e.x + e.y * e.y + e.z * e.z;
            if (d > (double) cullDist * cullDist) {
                e.setCanceled(true);
                culledCount++;
            }
        }

        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            frames++;
            long now = System.currentTimeMillis();
            if (now - last >= 1000) {
                fps = frames;
                frames = 0;
                last = now;
                culledPerSec = culledCount;
                culledCount = 0;
                adapt(now);
            }
        }

        void adapt(long now) {
            if (!adaptiveClient) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.currentScreen != null) return;
            if (origRender < 0) {
                origRender = mc.gameSettings.renderDistanceChunks;
                origFancy = mc.gameSettings.fancyGraphics;
                origParticles = mc.gameSettings.particleSetting;
            }
            if (fps < fpsLow) {
                lowSeconds++;
            } else {
                lowSeconds = 0;
            }
            if (lowSeconds >= 3 && now - lastAdjust > 10000) {
                boolean changed = false;
                if (mc.gameSettings.fancyGraphics) {
                    mc.gameSettings.fancyGraphics = false;
                    changed = true;
                } else if (mc.gameSettings.particleSetting < 2) {
                    mc.gameSettings.particleSetting++;
                    changed = true;
                } else if (mc.gameSettings.renderDistanceChunks > minRender) {
                    mc.gameSettings.renderDistanceChunks--;
                    changed = true;
                }
                if (changed) {
                    mc.renderGlobal.loadRenderers();
                    lastAdjust = now;
                    lowSeconds = 0;
                }
            } else if (fps > fpsHigh && now - lastAdjust > 30000) {
                boolean changed = false;
                if (mc.gameSettings.renderDistanceChunks < origRender) {
                    mc.gameSettings.renderDistanceChunks++;
                    changed = true;
                } else if (mc.gameSettings.particleSetting > origParticles) {
                    mc.gameSettings.particleSetting--;
                    changed = true;
                } else if (!mc.gameSettings.fancyGraphics && origFancy) {
                    mc.gameSettings.fancyGraphics = true;
                    changed = true;
                }
                if (changed) {
                    mc.renderGlobal.loadRenderers();
                    lastAdjust = now;
                }
            }
        }

        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Text e) {
            if (!metrics) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (!mc.gameSettings.showDebugInfo) return;
            e.left.add("");
            e.left.add("[Setenta] perfil " + profile + " | FPS " + fps + " | render " + mc.gameSettings.renderDistanceChunks
                + " | entidades ocultas/s " + culledPerSec);
            e.left.add("[Setenta] mspt " + String.format("%.1f", msptAvg) + " | ticks pulados/s " + skippedPerSec);
        }
    }

    // ================= COMANDO =================
    public static class CmdSetenta extends CommandBase {
        @Override
        public String getCommandName() {
            return "setenta";
        }

        @Override
        public String getCommandUsage(ICommandSender s) {
            return "/setenta status  |  /setenta perfil <leve|agressivo|extremo>";
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
            if (a.length >= 2 && a[0].equalsIgnoreCase("perfil")) {
                applyProfile(a[1]);
                s.addChatMessage(new ChatComponentText("Setenta: perfil " + profile + " aplicado (ate reiniciar)."));
                return;
            }
            s.addChatMessage(new ChatComponentText("Setenta: perfil " + profile + " | mspt "
                + String.format("%.1f", msptAvg) + " | ticks pulados/s " + skippedPerSec));
        }
    }
}
