package org.cyberpwn.resonance;

import net.minecraftforge.common.config.Config;

@Config(modid = Resonance.MODID)
public class RConfig {
    @Config.Name("Continuous Playback")
    @Config.Comment("If there is no music to play that match the category, fill the queue with random songs so that at least some music will always be playing.")
    public static boolean continuousPlayback = true;

    @Config.Name("Fade Duration Milliseconds")
    @Config.Comment("The fade in & fade out times in milliseconds (uses parametric curves)")
    public static int fadeDurationMS = 2500;

    @Config.Name("Song Loader Tick Milliseconds")
    @Config.Comment("How many milliseconds per song loader tick. 250 = 4 times/second. Setting a slower tick rate will reduce the quality of crossfades, increasing it can cause a song to fade in & out at the same time. Adjust & Test!")
    public static int songLoaderTickRateMS = 250;

    @Config.Name("Volume Tick Rate")
    @Config.Comment("The volume tick rate. Setting this lower will increase smoothness but dramatically increase cpu usage. 50 = 20/s")
    public static int volumeTickRate = 50;

    @Config.Name("Volume Latency")
    @Config.Comment("When the volume is not changing, the player ticks at a slower rate to reduce cpu usage. When the volume needs to change, it will take this much time for the player to start transitioning. Keep this under 1000 but no lower than then volume tick rate.")
    public static int volumeLatency = 250;

    @Config.Name("Transition Latency")
    @Config.Comment("When a song ends, how quickly should the next song start? This is at the start of a fade out, or the beginning of a fade in. Setting this too low will cause a lot of cpu issues, setting this too high will cause silence between songs.")
    public static int transitionLatency = 1000;

    @Config.Name("Volume Smoothness")
    @Config.Comment("Setting this higher will make the volume transitions take longer. Setting this smaller will make transitions happen faster. Keep this above 1 for best results.")
    public static double volumeSmoothness = 14;//TODO: REMOVE ME

    @Config.Name("Song Loader Cooldown")
    @Config.Comment("After a song queue changes, delay by this much time before trying to change again. Fixes songs changing constantly as you cross through biomes too quickly.")
    public static int songLoaderCooldown = 5000;

    @Config.Name("Show Now Playing Toast")
    @Config.Comment("When a new song starts, show a now playing toast (top right) of what song is playing.")
    public static boolean showNowPlayingToast = true;
}
