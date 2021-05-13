package org.cyberpwn.resonance;

import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.cyberpwn.resonance.config.ResonanceConfig;
import org.cyberpwn.resonance.queue.Queue;
import org.cyberpwn.resonance.queue.ResonanceQueue;
import org.cyberpwn.resonance.util.JFXInjector;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(modid = Resonance.MODID, name = Resonance.NAME, version = Resonance.VERSION, useMetadata = true)
public class Resonance
{
    public static ExecutorService pool;
    public static int poolThreadCount = 1;
    public static final String MODID = "resonance";
    public static final String NAME = "Resonance";
    public static final String VERSION = "@VERSION@";
    public static Queue queue;
    public static ResonanceTagManager tagManager;
    public static File folder;
    public static File music;

    public Resonance()
    {

    }

    private static void onStartup()
    {
        folder = new File("resonance");
        music = new File(folder, "music");
        music.mkdirs();
        JFXInjector.inject();
        pool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setContextClassLoader(JFXInjector.loader);
            t.setName("Resonator " + poolThreadCount++);
            t.setPriority(Thread.MAX_PRIORITY);
            t.setUncaughtExceptionHandler((t1, e) -> e.printStackTrace());
            return t;
        });
        tagManager = new ResonanceTagManager();
        tagManager.tag("startup");
        tagManager.tag("startup.preinit");
        queue = new ResonanceQueue();
    }

    public static void execute(Runnable r)
    {
        pool.execute(r);
    }

    @SubscribeEvent
    public void onBackgroundMusic(PlaySoundEvent event) {
        if(event.getSound().getCategory() == SoundCategory.MUSIC) {
            if(event.isCancelable())
            {
                event.setCanceled(true);
            }

            event.setResultSound(null);
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if(!Minecraft.getMinecraft().gameSettings.showDebugInfo)
        {
            return;
        }

        event.getRight().add("");
        event.getRight().add("Now Playing " + queue.getNowPlaying());
        queue.getQueueToString().forEach((i) -> event.getRight().add(i));
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        execute(tagManager::writeTagGuide);
        tagManager.tag("startup.init");
        MinecraftForge.EVENT_BUS.register(this);
        tagManager.updatePlayable();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        tagManager.untag("startup.preinit");
        tagManager.tag("startup.postinit");
        tagManager.updatePlayable();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        tagManager.tag("startup.postinit");
        tagManager.updatePlayable();
    }

    @EventHandler
    public void loadComplete(FMLLoadCompleteEvent event)
    {
        tagManager.updateTags();
        execute(() -> {
            while(true)
            {
                tagManager.updateTags();
                try {
                    Thread.sleep(ResonanceConfig.transitionLatency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });

        execute(() -> {
            while(true)
            {
                queue.inject(tagManager.getPlayable());

                try {
                    Thread.sleep(ResonanceConfig.transitionLatency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
    }

    static
    {
        onStartup();
    }
}
