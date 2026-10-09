package com.myname.mymodid;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;

@Mod(modid = OmdMod.MODID, name = "OMD Performance", version = "3.0", acceptedMinecraftVersions = "[1.7.10]")
public class OmdMod {

    public static final String MODID = "omd";
    public static OmdMod instance;

    static boolean enabled = false;

    // ========================================================================
    // CONFIG
    // ========================================================================
    static final int DIST = 12;
    static final int DIST_SQ = DIST * DIST;

    static final int PARTICLE_HARD_CAP = 5000;
    static final int PARTICLE_MAX_PER_FRAME = 200;

    static Set<String> teWhitelist = new HashSet<String>();

    // ========================================================================
    // METRICAS
    // ========================================================================
    static volatile int skippedPerSec    = 0;
    static volatile int culledPerSec     = 0;
    static volatile int particlesKilled  = 0;
    static volatile int tePaused         = 0;
    static volatile int frozenMobs       = 0;
    static volatile int fps              = 0;

    static int skippedCount      = 0;
    static int culledCount       = 0;
    static int particleKillCount = 0;
    static int tePauseCount      = 0;
    static int frozenMobCount    = 0;

    public OmdMod() { instance = this; }

    public static void setEnabled(boolean b) {
        enabled = b;
        skippedCount = culledCount = particleKillCount = 0;
        tePauseCount = frozenMobCount = 0;
    }

    public static boolean isEnabled() { return enabled; }

    static void initDefaultLists() {
        teWhitelist.add("TileEntityBeacon");
        teWhitelist.add("TileEntityEnchantmentTable");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        initDefaultLists();

        CommonEvents common = new CommonEvents();
        MinecraftForge.EVENT_BUS.register(common);
        FMLCommonHandler.instance().bus().register(common);

        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientEvents cli = new ClientEvents();
            MinecraftForge.EVENT_BUS.register(cli);
            FMLCommonHandler.instance().bus().register(cli);
        }
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new CmdOmd());
    }

    // ========================================================================
    // SERVIDOR
    // ========================================================================
    public static class CommonEvents {

        private int tickCounter = 0;

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            if (++tickCounter >= 20) {
                tickCounter = 0;
                skippedPerSec   = skippedCount;      skippedCount = 0;
                culledPerSec    = culledCount;       culledCount = 0;
                particlesKilled = particleKillCount; particleKillCount = 0;
                tePaused        = tePauseCount;      tePauseCount = 0;
                frozenMobs      = frozenMobCount;    frozenMobCount = 0;
            }
        }

        // ---- IA desligada >12b + tick adaptativo agressivo ----
        @SubscribeEvent
        public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
            if (!enabled) return;
            EntityLivingBase en = e.entityLiving;
            if (en == null || en.worldObj == null || en.worldObj.isRemote) return;
            if (en instanceof EntityPlayer) return;
            if (en instanceof IBossDisplayData) return;

            if (isProtected(en)) return;
            if (en.riddenByEntity != null || en.ridingEntity != null) return;
            if (en.hurtTime > 0 || en.deathTime > 0 || en.getHealth() <= 0f) return;

            double minSq = nearestPlayerSq(en);
            if (minSq == Double.MAX_VALUE) return;

            // IA off >12b
            if (en instanceof EntityLiving) {
                EntityLiving el = (EntityLiving) en;
                if (minSq > DIST_SQ) {
                    if (el.getAttackTarget() != null) el.setAttackTarget(null);
                    if (!el.getNavigator().noPath()) el.getNavigator().clearPathEntity();

                    el.moveStrafing = 0;
                    el.moveForward = 0;
                    el.setJumping(false);

                    // congela rotação
                    el.rotationYaw     = el.prevRotationYaw;
                    el.rotationPitch   = el.prevRotationPitch;
                    el.renderYawOffset = el.prevRenderYawOffset;

                    // zera velocidade vertical
                    el.motionY = 0;

                    // mob parado -> cancel tick inteiro
                    double dx = el.posX - el.prevPosX;
                    double dz = el.posZ - el.prevPosZ;
                    if (dx * dx + dz * dz < 0.0001) {
                        e.setCanceled(true);
                        frozenMobCount++;
                        return;
                    }
                }
            }

            // tick adaptativo: 1/8, 1/16, 1/32
            int interval;
            if (minSq <= DIST_SQ)                          interval = 1;
            else if (minSq <= (DIST * 2) * (DIST * 2))     interval = 8;
            else if (minSq <= (DIST * 4) * (DIST * 4))     interval = 16;
            else                                            interval = 32;

            if (interval > 1) {
                long t = en.worldObj.getTotalWorldTime() + en.getEntityId();
                if (t % interval != 0L) {
                    e.setCanceled(true);
                    skippedCount++;
                }
            }
        }

        private boolean isProtected(EntityLivingBase en) {
            if (en.hasCustomNameTag()) return true;
            if (en instanceof EntityTameable) {
                if (((EntityTameable) en).isTamed()) return true;
            }
            return false;
        }

        private double nearestPlayerSq(Entity en) {
            List players = en.worldObj.playerEntities;
            if (players == null || players.isEmpty()) return Double.MAX_VALUE;
            double min = Double.MAX_VALUE;
            for (int i = 0; i < players.size(); i++) {
                Object o = players.get(i);
                if (!(o instanceof EntityPlayer)) continue;
                double d = en.getDistanceSqToEntity((EntityPlayer) o);
                if (d < min) min = d;
            }
            return min;
        }

        // ---- TE throttle ----
        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent e) {
            if (!enabled) return;
            if (e.phase != TickEvent.Phase.END) return;
            World w = e.world;
            if (w == null || w.isRemote) return;

            if (w.getTotalWorldTime() % 40L == 0L) {
                doTileThrottle(w);
            }
        }

        private static Field TE_LIST_FIELD = null;
        private static boolean TE_CHECKED = false;

        @SuppressWarnings("unchecked")
        private static List<TileEntity> getTickableTE(World w) {
            if (!TE_CHECKED) {
                TE_CHECKED = true;
                String[] names = { "tickableTileEntities", "field_147483_b", "field_72997_g", "loadedTileEntityList" };
                for (int i = 0; i < names.length; i++) {
                    try {
                        Field f = World.class.getDeclaredField(names[i]);
                        f.setAccessible(true);
                        TE_LIST_FIELD = f;
                        break;
                    } catch (Throwable t) { }
                }
            }
            if (TE_LIST_FIELD == null) return null;
            try {
                Object o = TE_LIST_FIELD.get(w);
                if (o instanceof List) return (List<TileEntity>) o;
            } catch (Throwable t) { }
            return null;
        }

        private boolean isTEWhitelisted(TileEntity te) {
            String name = te.getClass().getSimpleName();
            for (String s : teWhitelist) {
                if (name.equals(s)) return true;
            }
            return false;
        }

        private void doTileThrottle(World w) {
            List<TileEntity> tick = getTickableTE(w);
            if (tick == null) return;

            List players = w.playerEntities;
            if (players == null || players.isEmpty()) return;

            try {
                for (int i = tick.size() - 1; i >= 0; i--) {
                    TileEntity te;
                    try { te = tick.get(i); } catch (Throwable t) { continue; }
                    if (te == null) continue;
                    if (isTEWhitelisted(te)) continue;

                    boolean specialized = (te instanceof TileEntityFurnace)
                                       || (te instanceof TileEntityHopper)
                                       || (te instanceof TileEntityMobSpawner);
                    if (!specialized) continue;

                    double min = Double.MAX_VALUE;
                    for (int p = 0; p < players.size(); p++) {
                        Object po = players.get(p);
                        if (!(po instanceof EntityPlayer)) continue;
                        EntityPlayer pl = (EntityPlayer) po;
                        double dx = pl.posX - (te.xCoord + 0.5);
                        double dy = pl.posY - (te.yCoord + 0.5);
                        double dz = pl.posZ - (te.zCoord + 0.5);
                        double d = dx*dx + dy*dy + dz*dz;
                        if (d < min) min = d;
                    }
                    double limit = (te instanceof TileEntityMobSpawner) ? 16.0 * 16.0 : DIST_SQ;
                    if (min > limit) {
                        tick.remove(i);
                        tePauseCount++;
                    }
                }
            } catch (Throwable t) { }
        }
    }

    // ========================================================================
    // CLIENTE
    // ========================================================================
    @SideOnly(Side.CLIENT)
    public static class ClientEvents {

        private int frames = 0;
        private long lastFps = 0;
        private int lastEnabled = -1;
        private int particlesThisFrame = 0;

        private int origRender = -1;
        private boolean origBobbing = true;

        private static Field FX_FIELD = null;
        private static boolean FX_CHECKED = false;

        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;

            particlesThisFrame = 0;

            if (enabled) smartParticleCleanup();

            frames++;
            long now = System.currentTimeMillis();
            if (now - lastFps >= 1000) {
                fps = frames; frames = 0; lastFps = now;
            }

            if (lastEnabled != (enabled ? 1 : 0)) {
                lastEnabled = enabled ? 1 : 0;
                applyClientState();
            }
        }

        // ---- Culling principal ----
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onRenderLiving(RenderLivingEvent.Pre e) {
            if (!enabled || e.entity == null) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;

            // você mesmo: nunca renderiza
            if (e.entity == mc.thePlayer) {
                e.setCanceled(true);
                return;
            }

            // outros players: normal
            if (e.entity instanceof EntityPlayer) return;

            // bosses: normal
            if (e.entity instanceof IBossDisplayData) return;

            // mobs: culling distância + atrás
            double dSq = mc.thePlayer.getDistanceSqToEntity(e.entity);
            if (dSq > DIST_SQ) {
                e.setCanceled(true);
                culledCount++;
                return;
            }

            if (isBehind(mc.thePlayer, e.entity)) {
                e.setCanceled(true);
                culledCount++;
            }
        }

        // ---- Nametag, HP bar, sombra: off pra todos ----
        @SubscribeEvent
        public void onSpecials(RenderLivingEvent.Specials.Pre e) {
            if (!enabled) return;
            e.setCanceled(true);
        }

        // ---- Corpo do player em terceira pessoa ----
        @SubscribeEvent
        public void onRenderPlayer(RenderPlayerEvent.Pre e) {
            if (!enabled || e.entityPlayer == null) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            if (e.entityPlayer == mc.thePlayer) e.setCanceled(true);
        }

        // ---- Wireframe de bloco off ----
        @SubscribeEvent
        public void onBlockHighlight(DrawBlockHighlightEvent e) {
            if (!enabled) return;
            e.setCanceled(true);
        }

        // ---- Névoa off ----
        @SubscribeEvent
        public void onFogDensity(EntityViewRenderEvent.FogDensity e) {
            if (!enabled) return;
            e.density = 0.0f;
        }

        private boolean isBehind(EntityPlayer player, Entity target) {
            Vec3 look = player.getLookVec();
            double dx = target.posX - player.posX;
            double dz = target.posZ - player.posZ;
            double lx = look.xCoord;
            double lz = look.zCoord;
            double len = Math.sqrt(lx * lx + lz * lz);
            if (len < 0.001) return false;
            lx /= len; lz /= len;
            return (dx * lx + dz * lz) < 0;
        }

        // ---- Smart particle cleanup ----
        private void smartParticleCleanup() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            if (mc.effectRenderer == null) return;

            if (!FX_CHECKED) {
                FX_CHECKED = true;
                String[] names = { "fxLayers", "field_78876_b" };
                for (int i = 0; i < names.length; i++) {
                    try {
                        Field f = EffectRenderer.class.getDeclaredField(names[i]);
                        f.setAccessible(true);
                        FX_FIELD = f;
                        break;
                    } catch (Throwable t) { }
                }
            }
            if (FX_FIELD == null) return;

            EntityPlayer pl = mc.thePlayer;
            try {
                Object obj = FX_FIELD.get(mc.effectRenderer);
                if (!(obj instanceof List[])) return;
                List[] layers = (List[]) obj;

                int totalParticles = 0;

                for (int i = 0; i < layers.length; i++) {
                    List layer = layers[i];
                    if (layer == null || layer.isEmpty()) continue;

                    totalParticles += layer.size();

                    for (int j = layer.size() - 1; j >= 0; j--) {
                        Object po = layer.get(j);
                        if (!(po instanceof Entity)) continue;
                        Entity fx = (Entity) po;

                        double dx = fx.posX - pl.posX;
                        double dy = fx.posY - pl.posY;
                        double dz = fx.posZ - pl.posZ;
                        double dSq = dx*dx + dy*dy + dz*dz;

                        boolean tooFar  = dSq > DIST_SQ;
                        boolean behind  = isBehind(pl, fx);
                        boolean tooMany = particlesThisFrame >= PARTICLE_MAX_PER_FRAME;

                        if (tooFar || behind || tooMany) {
                            layer.remove(j);
                            particleKillCount++;
                        } else {
                            particlesThisFrame++;
                        }
                    }
                }

                if (totalParticles > PARTICLE_HARD_CAP) {
                    int toKill = totalParticles - PARTICLE_HARD_CAP;
                    for (int i = 0; i < layers.length && toKill > 0; i++) {
                        List layer = layers[i];
                        if (layer == null) continue;
                        while (layer.size() > 0 && toKill > 0) {
                            layer.remove(layer.size() - 1);
                            particleKillCount++;
                            toKill--;
                        }
                    }
                }

            } catch (Throwable t) { }
        }

        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Text e) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || !mc.gameSettings.showDebugInfo) return;

            e.left.add("");
            e.left.add("\u00A7c[OMD]\u00A7r "
                + (enabled ? "\u00A7aON" : "\u00A77OFF")
                + " | fps " + fps
                + " | render " + mc.gameSettings.renderDistanceChunks);
            if (enabled) {
                e.left.add("\u00A7c[OMD]\u00A7r skip " + skippedPerSec
                    + " | cull " + culledPerSec
                    + " | part " + particlesKilled
                    + " | TE " + tePaused
                    + " | frozen " + frozenMobs);
            }
        }

        private void applyClientState() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null) return;

            if (origRender < 0) {
                origRender  = mc.gameSettings.renderDistanceChunks;
                origBobbing = mc.gameSettings.viewBobbing;
            }

            if (!enabled) {
                if (origRender > 0) mc.gameSettings.renderDistanceChunks = origRender;
                mc.gameSettings.viewBobbing = origBobbing;
                return;
            }

            mc.gameSettings.viewBobbing = false;
        }
    }

    // ========================================================================
    // COMANDO
    // ========================================================================
    public static class CmdOmd extends CommandBase {
        @Override public String getCommandName() { return "omd"; }
        @Override public String getCommandUsage(ICommandSender s) { return "/omd <0|1>"; }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }

        @Override
        public void processCommand(ICommandSender s, String[] a) {
            if (a.length == 0) {
                s.addChatMessage(new ChatComponentText(
                    "\u00A7c[OMD]\u00A7r " + (enabled ? "\u00A7aON" : "\u00A77OFF")));
                s.addChatMessage(new ChatComponentText(getCommandUsage(s)));
                return;
            }

            int v;
            try { v = Integer.parseInt(a[0].trim()); }
            catch (NumberFormatException ex) {
                s.addChatMessage(new ChatComponentText("\u00A7cUso: " + getCommandUsage(s)));
                return;
            }

            if (v == 0) {
                setEnabled(false);
                s.addChatMessage(new ChatComponentText("\u00A7c[OMD]\u00A77 DESLIGADO\u00A7r"));
                return;
            }
            if (v == 1) {
                setEnabled(true);
                s.addChatMessage(new ChatComponentText(
                    "\u00A7c[OMD]\u00A7a ATIVADO\u00A7r - " + DIST + "b"));
                return;
            }
            s.addChatMessage(new ChatComponentText("\u00A7cUse /omd 0 ou /omd 1"));
        }
    }
}
