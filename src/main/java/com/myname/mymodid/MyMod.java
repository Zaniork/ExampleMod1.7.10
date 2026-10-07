package com.myname.mymodid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.world.WorldEvent;

@Mod(modid = MyMod.MODID, version = Tags.VERSION, name = "MyMod", acceptedMinecraftVersions = "[1.7.10]")
public class MyMod {

    public static final String MODID = "mymodid";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "com.myname.mymodid.ClientProxy", serverSide = "com.myname.mymodid.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);

        // limpa itens do chão
        FMLCommonHandler.instance().bus().register(new ItemCleaner());
        // sem grama alta, flores e arbustos secos
        MinecraftForge.TERRAIN_GEN_BUS.register(new NoGrass());
        // só no cliente: sem céu/nuvens e mobs a 16 blocos
        if (event.getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new ClientPerf());
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    // ---------- limpador de itens ----------
    public static class ItemCleaner {
        private static final int INTERVAL = 20 * 30; // a cada 30 s
        private static final int MIN_AGE = 20 * 10;  // itens com mais de 10 s

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent e) {
            if (e.phase != TickEvent.Phase.END || e.world.isRemote) return;
            if (e.world.getTotalWorldTime() % INTERVAL != 0) return;

            for (Object o : new ArrayList<Object>(e.world.loadedEntityList)) {
                if (o instanceof EntityItem) {
                    EntityItem item = (EntityItem) o;
                    if (item.age > MIN_AGE) item.setDead();
                }
            }
        }
    }

    // ---------- sem grama alta, flores e arbusto seco ----------
    public static class NoGrass {
        @SubscribeEvent
        public void onDecorate(DecorateBiomeEvent.Decorate e) {
            switch (e.type) {
                case GRASS:
                case FLOWERS:
                case DEAD_BUSH:
                    e.setResult(Event.Result.DENY);
                    break;
                default:
                    break;
            }
        }
    }

    // ---------- cliente: sem céu/nuvens, mobs a 16 blocos ----------
    public static class ClientPerf {
        private static final double MAX_DIST = 16.0;

        private static class Empty extends IRenderHandler {
            @Override
            public void render(float partialTicks, WorldClient world, Minecraft mc) {}
        }

        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load e) {
            if (e.world.isRemote) {
                e.world.provider.setSkyRenderer(new Empty());
                e.world.provider.setCloudRenderer(new Empty());
            }
        }

        @SubscribeEvent
        public void onRenderLiving(RenderLivingEvent.Pre e) {
            Entity player = Minecraft.getMinecraft().thePlayer;
            if (player == null || e.entity == player) return;
            if (e.entity.getDistanceSqToEntity(player) > MAX_DIST * MAX_DIST) {
                e.setCanceled(true);
            }
        }
    }
}
