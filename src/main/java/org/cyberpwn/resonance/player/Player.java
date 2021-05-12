package org.cyberpwn.resonance.player;

import org.cyberpwn.resonance.RConfig;

public interface ResonancePlayer {
    /**
     * Fade in & Start Playing
     */
    void play(long startTime) throws Throwable;

    default void play() throws Throwable
    {
        play(0l);
    }

    /**
     * Fade out & stop playing
     */
    default void stop() throws InterruptedException {
        waitForVolumeTarget();
        destroy();
    }

    default void waitForVolumeTarget() throws InterruptedException {
        while(!isOnTarget())
        {
            Thread.sleep(RConfig.volumeLatency);
        }
    }

    void destroy();

    /**
     * Get the time remaining
     * @return the amount of milliseconds remaining
     */
    long getTimeRemaining();

    boolean isPlaying();

    void addVolumeMultiplier(String key, double volume);

    void removeVolumeMultiplier(String key);

    double getTargetVolume();

    boolean isOnTarget();
}
