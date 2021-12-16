package org.cyberpwn.resonance.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import org.cyberpwn.resonance.Resonance;

@Config(modid = Resonance.MODID)
public class ResonanceConfig {
    @Config.Name("Fade Duration Milliseconds")
    @Config.Comment("The fade in & fade out times in milliseconds (uses parametric curves)")
    public static int fadeDurationMS = 10000;

    @Config.Name("Volume Tick Rate")
    @Config.Comment("The volume tick rate. Setting this lower will increase smoothness but dramatically increase cpu usage. 50 = 20/s")
    public static int volumeTickRate = 50;

    @Config.Name("Volume Latency")
    @Config.Comment("When the volume is not changing, the player ticks at a slower rate to reduce cpu usage. When the volume needs to change, it will take this much time for the player to start transitioning. Keep this under 1000 but no lower than then volume tick rate.")
    public static int volumeLatency = 250;

    @Config.Name("Transition Latency")
    @Config.Comment("When a song ends, how quickly should the next song start? This is at the start of a fade out, or the beginning of a fade in. Setting this too low will cause a lot of cpu issues, setting this too high will cause silence between songs.")
    public static int transitionLatency = 1000;

    @Config.Name("Show Tags Debug Overlay")
    @Config.Comment("Show ingame tags that are active as a hud overlay.")
    public static boolean showTagsHud = false;

    @Config.Name("Play Startup Music")
    @Config.Comment("Play music during startup")
    public static boolean startupMusic = true;

    @Config.Name("Show Queue Debug Overlay")
    @Config.Comment("Show the ingame queue that is active as a hud overlay.")
    public static boolean showQueueHud = false;

    @Config.Name("Show Now Playing Action Messages")
    @Config.Comment("When a new song is playing, show a now playing action message.S")
    public static boolean nowPlayingMessages = true;

    @Config.Name("Block Checks")
    @Config.Comment("How many blocks to check per cycle for things like leaves, wood, snow, dirt, ore, etc")
    public static int blockChecks = 12;

    @Config.Name("Block Check Radius")
    @Config.Comment("The range of blocks to check per cycle (radius)")
    public static int blockCheckRadius = 18;

    public static void load(final Configuration config) {

    }

    public static void syncConfig() {
        System.out.println("Resonance Configuration Updated");
        ConfigManager.sync(Resonance.MODID, Config.Type.INSTANCE);
    }
}
