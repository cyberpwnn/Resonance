package org.cyberpwn.resonance.player;

import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;
import org.cyberpwn.resonance.Resonance;
import org.cyberpwn.resonance.config.ResonanceConfig;

public interface Player {
    /**
     * Fade in & Start Playing
     */
    void play() throws Throwable;

    /**
     * Fade out & stop playing
     */
    default void stop() throws InterruptedException {
        if(hasVolumeMultiplier("stop"))
        {
            return;
        }

        addVolumeMultiplier("stop", 0);
        waitForVolumeTarget();
        Resonance.execute(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            destroy();
        });
    }

    boolean isSudden();

    void setSudden(boolean sudden);

    int getPriority();

    void setPriority(int p);

    boolean hasVolumeMultiplier(String stop);

    default void waitForVolumeTarget() throws InterruptedException {
        while(!isOnTarget())
        {
            Thread.sleep(ResonanceConfig.volumeLatency);
        }
    }

    void destroy();

    /**
     * Get the time remaining
     * @return the amount of milliseconds remaining
     */
    long getTimeRemaining();

    default long getTimeRemainingFadeOut()
    {
        try
        {
            long v = getTimeRemaining() - ResonanceConfig.transitionLatency;
            return v < 0 ? 0 : v;
        }

        catch(Throwable e)
        {
            return 0;
        }
    }

    default double getMinecraftVolume()
    {
        return Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC) * Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER) * Resonance.dim;
    }

    public String getId();

    boolean isPlaying();

    void addVolumeMultiplier(String key, double volume);

    void removeVolumeMultiplier(String key);

    double getTargetVolume();

    boolean isOnTarget();

    long getTimecode();
}
