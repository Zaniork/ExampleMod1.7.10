code = r'''package com.myname.mymodid;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(
    modid = ExpMod.MODID,
    name = ExpMod.NAME,
    version = ExpMod.VERSION,
    acceptableRemoteVersions = "*"
)
public class ExpMod {
    public static final String MODID = "expoptimizer1710";
    public static final String NAME = "ExpMod";
    public static final String VERSION = "1.0.0";

    private static ExpMod instance;
    private Configuration config;
    private final OptimizerSettings settings = new OptimizerSettings();
    private final WorldOptimizer worldOptimizer = new WorldOptimizer();
    private int serverTickCounter;
    private int maintenanceCounter;
    private int reportCounter;
    private long lastMaintenanceTime;
    private long lastReportTime;
    private final AtomicInteger removedItems = new AtomicInteger();
    private final AtomicInteger removedExperience = new AtomicInteger();
    private final AtomicInteger removedEntities = new AtomicInteger();

    @cpw.mods.fml.common.Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        instance = this;
        File configFile = event.getSuggestedConfigurationFile();
        config = new Configuration(configFile);
        loadConfiguration();
        FMLCommonHandler.instance().bus().register(this);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        log("Configuração carregada.");
        log("Modo agressivo: " + settings.aggressiveMode);
        log("Limpeza automática de itens: " + settings.removeGroundItems);
        log("Limpeza automática de XP: " + settings.removeExperienceOrbs);
    }

    @cpw.mods.fml.common.Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            registerClientOptimizer();
        }
        log("Inicialização concluída. Nenhum comando é necessário.");
    }

    @cpw.mods.fml.common.Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        serverTickCounter = 0;
        maintenanceCounter = 0;
        reportCounter = 0;
        lastMaintenanceTime = System.currentTimeMillis();
        lastReportTime = lastMaintenanceTime;
        log("Servidor iniciado. Otimização automática ativa.");
    }

    @cpw.mods.fml.common.Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        logSummary();
        log("Finalizando otimizador.");
    }

    private void loadConfiguration() {
        try {
            config.load();
            settings.aggressiveMode = config.getBoolean("aggressiveMode", Configuration.CATEGORY_GENERAL, true, "Ativa políticas agressivas.");
            settings.removeGroundItems = config.getBoolean("removeGroundItems", Configuration.CATEGORY_GENERAL, true, "Remove itens antigos do chão.");
            settings.removeExperienceOrbs = config.getBoolean("removeExperienceOrbs", Configuration.CATEGORY_GENERAL, true, "Remove orbes de XP antigos ou em excesso.");
            settings.itemLifetimeTicks = config.getInt("itemLifetimeTicks", Configuration.CATEGORY_GENERAL, 6000, 200, 120000, "Idade mínima dos itens para limpeza.");
            settings.itemCleanupInterval = config.getInt("itemCleanupInterval", Configuration.CATEGORY_GENERAL, 100, 20, 1200, "Intervalo entre verificações.");
            settings.maxItemsPerPass = config.getInt("maxItemsPerPass", Configuration.CATEGORY_GENERAL, 200, 10, 5000, "Máximo de itens removidos por verificação.");
            settings.maxExperienceOrbsPerWorld = config.getInt("maxExperienceOrbsPerWorld", Configuration.CATEGORY_GENERAL, 500, 20, 10000, "Limite de orbes de XP por mundo.");
            settings.maxExperienceOrbsPerPass = config.getInt("maxExperienceOrbsPerPass", Configuration.CATEGORY_GENERAL, 100, 10, 2000, "Máximo de orbes removidos por verificação.");
            settings.experienceOrbLifetimeTicks = config.getInt("experienceOrbLifetimeTicks", Configuration.CATEGORY_GENERAL, 6000, 200, 120000, "Idade mínima dos orbes para limpeza.");
            settings.maxLivingEntitiesPerWorld = config.getInt("maxLivingEntitiesPerWorld", Configuration.CATEGORY_GENERAL, 300, 50, 5000, "Limite de entidades vivas por mundo.");
            settings.enforceLivingEntityLimit = config.getBoolean("enforceLivingEntityLimit", Configuration.CATEGORY_GENERAL, false, "Remove entidades vivas acima do limite.");
            settings.livingEntityCheckInterval = config.getInt("livingEntityCheckInterval", Configuration.CATEGORY_GENERAL, 200, 20, 2400, "Intervalo de verificação de entidades.");
            settings.skipNamedEntities = config.getBoolean("skipNamedEntities", Configuration.CATEGORY_GENERAL, true, "Protege entidades com nome personalizado.");
            settings.skipTamedEntities = config.getBoolean("skipTamedEntities", Configuration.CATEGORY_GENERAL, true, "Opção reservada para compatibilidade.");
            settings.optimizeClientGraphics = config.getBoolean("optimizeClientGraphics", Configuration.CATEGORY_GENERAL, false, "Ativa ajustes gráficos automáticos.");
            settings.disableFancyGraphics = config.getBoolean("disableFancyGraphics", Configuration.CATEGORY_GENERAL, false, "Desativa gráficos sofisticados.");
            settings.disableParticles = config.getBoolean("disableParticles", Configuration.CATEGORY_GENERAL, false, "Reduz partículas.");
            settings.disableSmoothLighting = config.getBoolean("disableSmoothLighting", Configuration.CATEGORY_GENERAL, false, "Desativa iluminação suave.");
            settings.logStatistics = config.getBoolean("logStatistics", Configuration.CATEGORY_GENERAL, true, "Registra estatísticas no log.");
            settings.statisticsIntervalTicks = config.getInt("statisticsIntervalTicks", Configuration.CATEGORY_GENERAL, 6000, 200, 72000, "Intervalo dos relatórios.");
            settings.limitEntityJoinBurst = config.getBoolean("limitEntityJoinBurst", Configuration.CATEGORY_GENERAL, false, "Bloqueia excesso de entradas de entidades.");
            settings.maxEntityJoinBurst = config.getInt("maxEntityJoinBurst", Configuration.CATEGORY_GENERAL, 100, 10, 5000, "Máximo de entidades por janela.");
            settings.joinBurstWindowTicks = config.getInt("joinBurstWindowTicks", Configuration.CATEGORY_GENERAL, 20, 1, 1200, "Duração da janela em ticks.");
            settings.cleanupOnlyWhenPlayersOnline = config.getBoolean("cleanupOnlyWhenPlayersOnline", Configuration.CATEGORY_GENERAL, true, "Só limpa quando houver jogadores.");
            settings.minimumPlayersForAggressiveCleanup = config.getInt("minimumPlayersForAggressiveCleanup", Configuration.CATEGORY_GENERAL, 1, 0, 100, "Jogadores mínimos para limpeza agressiva.");
        } catch (Exception e) {
            log("Erro ao carregar configuração: " + e.getMessage());
        } finally {
            if (config.hasChanged()) config.save();
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world == null || event.world.isRemote) return;
        log("Mundo carregado: " + event.world.provider.getDimensionName());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world == null || event.world.isRemote) return;
        log("Mundo descarregado: " + event.world.provider.getDimensionName());
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!settings.limitEntityJoinBurst || event.world == null || event.world.isRemote) return;
        Entity entity = event.entity;
        if (entity == null || entity instanceof EntityPlayer) return;
        if (worldOptimizer.shouldBlockEntityJoin(event.world) && settings.aggressiveMode) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        serverTickCounter++;
        if (serverTickCounter >= Integer.MAX_VALUE - 1000) serverTickCounter = 0;
        if (serverTickCounter % settings.itemCleanupInterval == 0) {
            maintenanceCounter++;
            runMaintenance();
        }
        if (settings.logStatistics && serverTickCounter % settings.statisticsIntervalTicks == 0) {
            reportCounter++;
            logSummary();
        }
    }

    private void runMaintenance() {
        lastMaintenanceTime = System.currentTimeMillis();
        if (settings.cleanupOnlyWhenPlayersOnline && !hasOnlinePlayers()) return;
        if (settings.minimumPlayersForAggressiveCleanup > 0 &&
            getOnlinePlayerCount() < settings.minimumPlayersForAggressiveCleanup) return;

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.worldServers == null) return;

        WorldServer[] worlds = server.worldServers;
        for (WorldServer world : worlds) {
            if (world == null) continue;
            try {
                if (settings.removeGroundItems) removedItems.addAndGet(worldOptimizer.cleanOldItems(world));
                if (settings.removeExperienceOrbs) removedExperience.addAndGet(worldOptimizer.cleanExperienceOrbs(world));
                int checks = Math.max(1, settings.livingEntityCheckInterval / settings.itemCleanupInterval);
                if (settings.enforceLivingEntityLimit && maintenanceCounter % checks == 0) {
                    removedEntities.addAndGet(worldOptimizer.enforceLivingEntityLimit(world));
                }
            } catch (Throwable throwable) {
                log("Falha na manutenção do mundo " + world.provider.getDimensionName() + ": " + throwable.getMessage());
            }
        }
    }

    private boolean hasOnlinePlayers() {
        return getOnlinePlayerCount() > 0;
    }

    private int getOnlinePlayerCount() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager() == null) return 0;
        return server.getConfigurationManager().playerEntityList.size();
    }

    private void logSummary() {
        long now = System.currentTimeMillis();
        long elapsed = Math.max(1L, now - lastReportTime);
        lastReportTime = now;
        if (!settings.logStatistics) return;
        log("Estatísticas: itens removidos=" + removedItems.get()
            + ", orbes XP removidos=" + removedExperience.get()
            + ", entidades removidas=" + removedEntities.get()
            + ", manutenções=" + maintenanceCounter
            + ", relatórios=" + reportCounter
            + ", intervalo=" + elapsed + " ms.");
    }

    private void registerClientOptimizer() {
        FMLCommonHandler.instance().bus().register(new ClientOptimizer(this));
    }

    private void log(String message) {
        System.out.println("[ExpMod] " + message);
    }

    public static ExpMod getInstance() { return instance; }
    public OptimizerSettings getSettings() { return settings; }

    public static class OptimizerSettings {
        public boolean aggressiveMode = true;
        public boolean removeGroundItems = true;
        public boolean removeExperienceOrbs = true;
        public int itemLifetimeTicks = 6000;
        public int itemCleanupInterval = 100;
        public int maxItemsPerPass = 200;
        public int maxExperienceOrbsPerWorld = 500;
        public int maxExperienceOrbsPerPass = 100;
        public int experienceOrbLifetimeTicks = 6000;
        public int maxLivingEntitiesPerWorld = 300;
        public boolean enforceLivingEntityLimit = false;
        public int livingEntityCheckInterval = 200;
        public boolean skipNamedEntities = true;
        public boolean skipTamedEntities = true;
        public boolean optimizeClientGraphics = false;
        public boolean disableFancyGraphics = false;
        public boolean disableParticles = false;
        public boolean disableSmoothLighting = false;
        public boolean logStatistics = true;
        public int statisticsIntervalTicks = 6000;
        public boolean limitEntityJoinBurst = false;
        public int maxEntityJoinBurst = 100;
        public int joinBurstWindowTicks = 20;
        public boolean cleanupOnlyWhenPlayersOnline = true;
        public int minimumPlayersForAggressiveCleanup = 1;
    }

    private class WorldOptimizer {
        private final List<JoinWindow> joinWindows = new ArrayList<JoinWindow>();

        public int cleanOldItems(World world) {
            if (world == null || world.isRemote) return 0;
            int removed = 0;
            int inspected = 0;
            List<?> entities = new ArrayList<Object>(world.loadedEntityList);
            for (Object object : entities) {
                if (removed >= settings.maxItemsPerPass) break;
                if (!(object instanceof EntityItem)) continue;
                EntityItem item = (EntityItem) object;
                inspected++;
                if (item.isDead) continue;
                if (item.age >= settings.itemLifetimeTicks) {
                    item.setDead();
                    removed++;
                } else if (settings.aggressiveMode && inspected > settings.maxItemsPerPass * 2) {
                    break;
                }
            }
            return removed;
        }

        public int cleanExperienceOrbs(World world) {
            if (world == null || world.isRemote) return 0;
            List<EntityXPOrb> orbs = new ArrayList<EntityXPOrb>();
            List<?> entities = new ArrayList<Object>(world.loadedEntityList);
            for (Object object : entities) {
                if (object instanceof EntityXPOrb) {
                    EntityXPOrb orb = (EntityXPOrb) object;
                    if (!orb.isDead) orbs.add(orb);
                }
            }
            int removed = 0;
            for (EntityXPOrb orb : orbs) {
                if (removed >= settings.maxExperienceOrbsPerPass) break;
                if (orb.xpOrbAge >= settings.experienceOrbLifetimeTicks) {
                    orb.setDead();
                    removed++;
                }
            }
            int excess = orbs.size() - settings.maxExperienceOrbsPerWorld;
            if (excess > 0 && settings.aggressiveMode) {
                Collections.sort(orbs, new Comparator<EntityXPOrb>() {
                    @Override
                    public int compare(EntityXPOrb a, EntityXPOrb b) {
                        return Integer.compare(b.xpOrbAge, a.xpOrbAge);
                    }
                });
                for (EntityXPOrb orb : orbs) {
                    if (excess <= 0 || removed >= settings.maxExperienceOrbsPerPass) break;
                    if (!orb.isDead) {
                        orb.setDead();
                        removed++;
                        excess--;
                    }
                }
            }
            return removed;
        }

        public int enforceLivingEntityLimit(World world) {
            if (world == null || world.isRemote) return 0;
            List<EntityLiving> livingEntities = new ArrayList<EntityLiving>();
            List<?> entities = new ArrayList<Object>(world.loadedEntityList);
            for (Object object : entities) {
                if (!(object instanceof EntityLiving)) continue;
                EntityLiving entity = (EntityLiving) object;
                // EntityPlayer não herda de EntityLiving no Forge 1.7.10,
                // então não se faz o teste incompatível "entity instanceof EntityPlayer".
                if (entity.isDead) continue;
                if (settings.skipNamedEntities && entity.hasCustomNameTag()) continue;
                livingEntities.add(entity);
            }
            int excess = livingEntities.size() - settings.maxLivingEntitiesPerWorld;
            if (excess <= 0 || !settings.aggressiveMode) return 0;

            Collections.sort(livingEntities, new Comparator<EntityLiving>() {
                @Override
                public int compare(EntityLiving a, EntityLiving b) {
                    return Integer.compare(b.ticksExisted, a.ticksExisted);
                }
            });
            int removed = 0;
            for (EntityLiving entity : livingEntities) {
                if (excess <= 0) break;
                if (entity.isDead) continue;
                entity.setDead();
                removed++;
                excess--;
            }
            return removed;
        }

        public boolean shouldBlockEntityJoin(World world) {
            if (world == null || world.isRemote) return false;
            int currentTick = serverTickCounter;
            JoinWindow window = getJoinWindow(world);
            if (window == null) {
                window = new JoinWindow(world, currentTick);
                joinWindows.add(window);
            }
            if (currentTick - window.startTick >= settings.joinBurstWindowTicks) {
                window.startTick = currentTick;
                window.count = 0;
            }
            window.count++;
            return window.count > settings.maxEntityJoinBurst;
        }

        private JoinWindow getJoinWindow(World world) {
            Iterator<JoinWindow> iterator = joinWindows.iterator();
            while (iterator.hasNext()) {
                JoinWindow window = iterator.next();
                if (window.world == world) return window;
                if (window.world == null) iterator.remove();
            }
            return null;
        }
    }

    private static class JoinWindow {
        private final World world;
        private int startTick;
        private int count;
        private JoinWindow(World world, int startTick) {
            this.world = world;
            this.startTick = startTick;
            this.count = 0;
        }
    }

    @SideOnly(Side.CLIENT)
    private static class ClientOptimizer {
        private final ExpMod mod;
        private int tickCounter;
        private long lastTickTime;
        private long averageTickTime;

        private ClientOptimizer(ExpMod mod) {
            this.mod = mod;
            this.lastTickTime = System.nanoTime();
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            tickCounter++;
            long now = System.nanoTime();
            long delta = now - lastTickTime;
            lastTickTime = now;
            if (delta > 0L) {
                if (averageTickTime == 0L) averageTickTime = delta;
                else averageTickTime = (averageTickTime * 9L + delta) / 10L;
            }
            if (tickCounter % 100 == 0) applyClientSettings();
        }

        private void applyClientSettings() {
            OptimizerSettings settings = mod.getSettings();
            if (!settings.optimizeClientGraphics) return;
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null || minecraft.gameSettings == null) return;
            GameSettings gameSettings = minecraft.gameSettings;
            boolean changed = false;
            if (settings.disableFancyGraphics && gameSettings.fancyGraphics) {
                gameSettings.fancyGraphics = false;
                changed = true;
            }
            if (settings.disableParticles && gameSettings.particleSetting < 2) {
                gameSettings.particleSetting = 2;
                changed = true;
            }
            if (settings.disableSmoothLighting && gameSettings.ambientOcclusion != 0) {
                gameSettings.ambientOcclusion = 0;
                changed = true;
            }
            if (changed) gameSettings.saveOptions();
        }
    }
}
'''
path = "/mnt/data/ExpMod.java"
with open(path, "w", encoding="utf-8", newline="\n") as f:
    f.write(code)
print("Arquivo criado:", path)
print("Tamanho:", len(code.encode("utf-8")), "bytes")
print("Correção aplicada: removido o teste incompatível entity instanceof EntityPlayer dentro de EntityLiving.")
