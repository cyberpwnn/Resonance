package org.cyberpwn.resonance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.cyberpwn.resonance.config.ResonanceConfig;
import org.cyberpwn.resonance.queue.Queue;
import org.cyberpwn.resonance.queue.ResonanceQueue;
import org.cyberpwn.resonance.util.JFXInjector;
import org.lwjgl.input.Keyboard;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(modid = Resonance.MODID, name = Resonance.NAME, version = Resonance.VERSION, useMetadata = true)
public class Resonance
{
    public static ExecutorService pool;
    public static int poolThreadCount = 1;
    private static boolean donestartup = false;
    public static final String MODID = "resonance";
    public static final String NAME = "Resonance";
    public static final String VERSION = "@VERSION@";
    public static Queue queue;
    public static ResonanceTagManager tagManager;
    public static File folder;
    public static File music;
    public static double dim = 1;
    public static Double overrideVolume = 0.25;
    public static String startupTag = "startup";

    public static KeyBinding[] keys = new KeyBinding[]{
            new KeyBinding("key.nextsong.desc", Keyboard.KEY_N, "key.resonance.category")
    };

    public Resonance()
    {

    }

    private static void onStartup()
    {
        overrideVolume = 0.25;
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

        execute(() -> {
            while(true)
            {
                try {
                    Thread.sleep(ResonanceConfig.transitionLatency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                if(!ResonanceConfig.startupMusic && !donestartup)
                {
                    return;
                }
                tagManager.updateTags();

            }
        });

        execute(() -> {
            while(true)
            {
                try {
                    Thread.sleep(ResonanceConfig.transitionLatency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                if(!ResonanceConfig.startupMusic && !donestartup)
                {
                    return;
                }

                queue.inject(tagManager.getPlayable());
            }
        });
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


    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void on(LivingHurtEvent event) {
        try
        {
            if(event.getEntityLiving().getEntityId() == Minecraft.getMinecraft().player.getEntityId())
            {
                tagManager.increaseConflict(1.25 * event.getAmount());
            }
        }

        catch(Throwable e)
        {

        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void on(LivingDamageEvent event) {
        try{
            if(event.getEntityLiving().getEntityId() == Minecraft.getMinecraft().player.getEntityId() || event.getSource().getTrueSource().getEntityId() == Minecraft.getMinecraft().player.getEntityId())
            {
                tagManager.increaseConflict(1.125 * event.getAmount());
            }
        }

        catch(Throwable e)
        {

        }
    }


    @SubscribeEvent
    public void on(RenderGameOverlayEvent.Text event) {
        try
        {
            if(!Minecraft.getMinecraft().gameSettings.showDebugInfo)
            {
                if(ResonanceConfig.showTagsHud)
                {
                    for(String i : tagManager.getTags())
                    {
                        event.getLeft().add(i);
                    }

                    event.getLeft().add("");
                    event.getLeft().add("Conflict: " + (int)tagManager.getConflict()  + "%");
                    event.getLeft().add("Lushness: " +(int)((tagManager.getLushness() * 100)) + "%" );
                }

                if(ResonanceConfig.showQueueHud)
                {
                    event.getRight().add("Now Playing " + queue.getNowPlaying());
                    queue.getQueueToString().forEach((i) -> event.getRight().add(i));
                }
            }
        }

        catch(Throwable e)
        {

        }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        registerKeys();
        execute(tagManager::writeTagGuide);
        startupTag = "startup.preinit";
        MinecraftForge.EVENT_BUS.register(this);
        tagManager.updatePlayable();
        overrideVolume = 0.25;
    }

    private void registerKeys() {
        for(KeyBinding i : keys)
        {
            ClientRegistry.registerKeyBinding(i);
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        overrideVolume = 0.25;
        startupTag = "startup.init";
        tagManager.updatePlayable();
    }

    @SubscribeEvent(priority= EventPriority.NORMAL, receiveCanceled=true)
    public void onEvent(InputEvent.KeyInputEvent event)
    {
        if (keys[0].isPressed())
        {
            ((ResonanceQueue)queue).queueNext();
        }
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        overrideVolume = 0.25;
        startupTag = "startup.postinit";
        tagManager.updatePlayable();
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if(event.getModID().equals(MODID)) {
            ResonanceConfig.syncConfig();
        }
    }

    @EventHandler
    public void loadComplete(FMLLoadCompleteEvent event)
    {
        donestartup = true;
        startupTag = "menu";
        overrideVolume = null;
    }

    static
    {
        onStartup();
    }
}
