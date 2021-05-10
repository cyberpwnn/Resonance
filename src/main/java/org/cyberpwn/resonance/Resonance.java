package org.cyberpwn.resonance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.Logger;
import org.lwjgl.Sys;
import scala.reflect.internal.Trees;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

@Mod(modid = Resonance.MODID, name = Resonance.NAME, version = Resonance.VERSION, clientSideOnly = true)
public class Resonance
{
    public static final String MODID = "resonance";
    public static final String NAME = "Resonance";
    public static final String VERSION = "1.0";
    private RPlayer player;
    private static Logger logger;
    private static File folder;
    private Set<String> triggers;
    private int lastTriggers = 0;
    public static double volumeMultiplier = 1;
    private static double lastVolume = 1;

    public Resonance()
    {
        folder = new File("resonance");
        triggers = new HashSet<>();
        triggers.add("startup");
        triggers.add("startup.preinit");
        player = new RPlayer(2500, this::tick);
    }

    private void gen(File folder) {
        folder.mkdirs();
        File f = new File(folder, "info.txt");
        try {
            PrintWriter pw = new PrintWriter(f);
            pw.println("Depending on what the player is doing ingame, different triggers will activate and deactivate.");
            pw.println("Multiple triggers can be active at once. When triggers change, the queue of music changes.");
            pw.println("Below is a list of triggers that happen and their description.");
            pw.println();
            pw.println("Use an ! before any tag to indicate NOT");
            pw.println("Use a  # before any tag to indicate MUST HAVE");
            pw.println("Use a  ^ anywhere in the name to indicate the file must match ALL tag conditions, not just any.");
            pw.println();
            pw.println("startup - Forge is starting up");
            pw.println("startup.preinit - Forge is starting up (pre)");
            pw.println("startup.init - Forge is starting up (mid)");
            pw.println("startup.postinit - Forge is starting up (post)");
            pw.println("menu - Game is in the main menu");
            pw.println("loading - Loading world or connecting to server");
            pw.println("world - Anytime you are ingame (in any world)");
            pw.println();
            pw.println("humid_wet - Wet biome");
            pw.println("humid_dry - Dry biome");
            pw.println("humid_mild - Normal Humidity ");
            pw.println();
            pw.println("temp_freezing - Frozen biome");
            pw.println("temp_chilly - Cold biome");
            pw.println("temp_warm - Warm biome");
            pw.println("temp_hot - Hot biome");
            pw.println("temp_superhot - Very hot biome");
            pw.println();
            pw.println("height_sohigh - At or above 200");
            pw.println("height_high - Below 200");
            pw.println("height_sealevel - Below 94");
            pw.println("height_low - Below 60");
            pw.println("height_bedrock - Below 35");
            pw.println();
            pw.println("hp_low - HP Below 30%");
            pw.println("fp_low - Food below 30%");
            pw.println("dead - Death screen");
            pw.println();
            pw.println("time_night - Night time...");
            pw.println("time_day - Day time...");
            pw.println();
            pw.println("light_dark - Darkness");
            pw.println("light_mid - Middle range light");
            pw.println("light_bright - High light...");
            pw.println();

            for(Biome i : ForgeRegistries.BIOMES.getValuesCollection())
            {
                pw.println("biome_"+i.getRegistryName().getResourcePath() + " When in the " + i.getBiomeName() + " Biome");
            }

            pw.println();

            for(DimensionType i : DimensionManager.getRegisteredDimensions().keySet())
            {
                pw.println("dim" + i.getId() + " - When in " + i.getName() + " Dimension.");
            }

            pw.flush();
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void tick() {
        updateTriggers();
        if(triggers.hashCode() == lastTriggers)
        {
            return;
        }

        List<File> f = new ArrayList<>();

        searching: for(File i : folder.listFiles())
        {
            if(i.getName().endsWith(".mp3"))
            {
                String[] ftags = i.getName().replaceAll("\\Q.mp3\\E", "").split("\\Q \\E");

                for(String j : ftags)
                {
                    if(j.startsWith("#"))
                    {
                        if(!triggers.contains(j.substring(1)))
                        {
                            continue searching;
                        }
                    }

                    if(j.startsWith("!") && triggers.contains(j.substring(1)))
                    {
                        continue searching;
                    }
                }

                if(i.getName().contains("^"))
                {
                    boolean fignore = true;

                    for(String j : ftags)
                    {
                        if(fignore)
                        {
                            fignore = false;
                            continue;
                        }

                        if(!triggers.contains(j))
                        {
                            continue searching;
                        }
                    }

                    f.add(i);
                }

                else
                {
                    for(String j : ftags)
                    {
                        if(triggers.contains(j) || triggers.contains(j.substring(1)))
                        {
                            f.add(i);
                        }
                    }
                }
            }
        }

        Collections.shuffle(f);
        player.replaceQueue(f);

        if(player.getNowPlaying() != null && !f.contains(player.getNowPlaying()))
        {
            player.nextSong();
        }

        lastTriggers = triggers.hashCode();
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

    private void updateTriggers() {
        try
        {
            volumeMultiplier = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC) * Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER);

            if(player.getPlayer() != null && lastVolume != volumeMultiplier)
            {
                player.getPlayer().updateVolume(lastVolume);
                lastVolume = volumeMultiplier;
            }

            triggers.clear();
            if(Minecraft.getMinecraft().world == null)
            {
                triggers.add("menu");
            }

            else
            {
                triggers.add("world");
                EntityPlayerSP p = Minecraft.getMinecraft().player;
                Biome in = Minecraft.getMinecraft().world.getBiome(new BlockPos(p.posX, p.posY, p.posZ));
                triggers.add("biome_" + in.getRegistryName().getResourcePath());

                if(in.getRainfall() > 0.75)
                {
                    triggers.add("humid_wet");
                }
                else if(in.getRainfall() > 0.35 && in.getRainfall() < 0.75)
                {
                    triggers.add("humid_mild");
                }
                else if(in.getRainfall() < 0.35)
                {
                    triggers.add("humid_dry");
                }

                if(in.getDefaultTemperature() < 0)
                {
                    triggers.add("temp_freezing");
                }
                else if(in.getDefaultTemperature() < 0.4)
                {
                    triggers.add("temp_chilly");
                }
                else if(in.getDefaultTemperature() > 1)
                {
                    triggers.add("temp_superhot");
                }
                else if(in.getDefaultTemperature() > 0.8)
                {
                    triggers.add("temp_hot");
                }
                else if(in.getDefaultTemperature() > 0.4)
                {
                    triggers.add("temp_warm");
                }

                int pos = (int) p.posY;
                if(pos > 200)
                {
                    triggers.add("height_sohigh");
                }

                else if(pos > 94)
                {
                    triggers.add("height_high");
                }

                else if(pos > 60)
                {
                    triggers.add("height_sealevel");
                }

                else if(pos > 35)
                {
                    triggers.add("height_low");
                }

                else {
                    triggers.add("height_bedrock");
                }

                if(p.getHealth() / p.getMaxHealth() <= 0.3)
                {
                    triggers.add("hp_low");
                }

                if(p.getFoodStats().getFoodLevel() / 20.0 <= 0.3)
                {
                    triggers.add("fp_low");
                }

                if(p.isDead)
                {
                    triggers.add("dead");
                }

                if(p.world.isDaytime())
                {
                    triggers.add("time_day");
                }

                else
                {
                    triggers.add("time_night");
                }

                float b = p.world.getCombinedLight(new BlockPos(p.posX, p.posY, p.posZ), p.world.getSkylightSubtracted()) / 15f;

                if(b > 0.7)
                {
                    triggers.add("light_bright");
                }

                else if(b > 0.4)
                {
                    triggers.add("light_mid");
                }

                else
                {
                    triggers.add("light_dark");
                }

                triggers.add("dim" + p.world.getWorldType().getId());
            }
        }

        catch(Throwable e)
        {

        }
    }

    @SubscribeEvent
    public void onMessage(ClientChatEvent e)
    {
        if(e.getMessage().startsWith("/resonance"))
        {
            e.setCanceled(true);
            String[] params = e.getMessage().split("\\Q \\E");

            if(params.length == 1)
            {
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString("/resonance next"));
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString("/resonance queue"));
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString("/resonance tags"));

            }

            else if(params[1].equalsIgnoreCase("next"))
            {
                player.nextSong();

                try
                {
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Now Playing " + player.getNowPlaying().getName()));

                }

                catch(Throwable xe)
                {
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Durrrrrr Nothing left to play?"));

                }
            }

            else if(params[1].equalsIgnoreCase("queue"))
            {
                try
                {
                    StringBuilder f = new StringBuilder();
                    f = new StringBuilder("Now Playing [" + player.getNowPlaying().getName() + "] <-");
                    for(File i : player.getQueue())
                    {
                        f.append(i.getName()).append(" <-");
                    }
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentString(f.toString()));
                }

                catch(Throwable xe)
                {
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Durrrrrr Nothing left to play?"));

                }
            }

            else if(params[1].equalsIgnoreCase("tags"))
            {
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString("--- TAGS ---"));

                for(String i : triggers)
                {
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentString(i));
                }

                Minecraft.getMinecraft().player.sendMessage(new TextComponentString("--- ---- ---"));

            }

            else{
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Use /resonance for help."));
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if(!Minecraft.getMinecraft().gameSettings.showDebugInfo)
        {
            return;
        }

        for(String i : triggers) {
            event.getRight().add(i);
        }

        try
        {
            event.getRight().add("Now Playing [" + player.getNowPlaying().getName());
            StringBuilder f = new StringBuilder();
            f = new StringBuilder("Now Playing [" + player.getNowPlaying().getName() + "] <-");
            for(File i : player.getQueue())
            {
                event.getRight().add("<- " + i.getName());
            }
        }

        catch(Throwable xe)
        {

        }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        triggers.add("startup.init");
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        triggers.remove("startup.preinit");
        triggers.add("startup.postinit");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        triggers.add("startup.postinit");
    }

    @EventHandler
    public void loadComplete(FMLLoadCompleteEvent event)
    {
        gen(folder);
        triggers.remove("startup");
        triggers.remove("startup.postinit");
    }
}
