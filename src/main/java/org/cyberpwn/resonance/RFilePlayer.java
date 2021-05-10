package org.cyberpwn.resonance;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.util.concurrent.ForkJoinPool;

public class RFilePlayer
{
    private final MediaPlayer player;
    private long startTime = 0;
    private double totalDuration;
    private long fadeInMS;
    private long fadeOutMS;
    private boolean playing;
    private boolean fadingOut;
    private File file;

    public RFilePlayer(File file, long startAt, long fadeInMS, long fadeOutMS)
    {
        this.file = file;
        RPlayer.ensureInitialized();
        Media media = new Media(file.toURI().toString());
        this.fadeInMS = fadeInMS;
        this.fadeOutMS = fadeOutMS;
        player = new MediaPlayer(media);
        player.setVolume(0);
        player.setOnPlaying(this::onPlaying);
        player.setOnEndOfMedia(this::onEnd);
        player.setStartTime(Duration.millis(startAt));
        player.play();
        playing = true;
    }

    public File getFile()
    {
        return file;
    }

    public long getTimeCode()
    {
        return (long) player.getCurrentTime().toMillis();
    }

    public long getRemainingTime()
    {
        if(playing)
        {
            return (long) (totalDuration - player.getCurrentTime().toMillis());
        }

        return 0;
    }

    private void onEnd() {
        playing = false;
    }
    private void onPlaying() {
        startTime = System.currentTimeMillis();
        totalDuration = player.getTotalDuration().toMillis();
        ForkJoinPool.commonPool().execute(() -> {
            long m = (long) (totalDuration - (fadeOutMS + 200));

            if(m < 0)
            {
                try {
                    fadeOut(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return;
            }

            try {
                Thread.sleep(m);
                fadeOut(fadeOutMS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        ForkJoinPool.commonPool().execute(() -> {
            try {
                fadeIn(fadeInMS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void fadeIn(long duration) throws InterruptedException {
        double at = System.currentTimeMillis();

        while(System.currentTimeMillis() < at + duration)
        {
            Thread.sleep(50);
            double now = System.currentTimeMillis();
            player.setVolume(((now - at) / duration) * Resonance.volumeMultiplier);
        }

        player.setVolume(Resonance.volumeMultiplier);
    }

    public void die()
    {
        try {
            fadeOut(fadeOutMS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        player.dispose();
    }

    public void dieFast()
    {
        try {
            fadeOut(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        player.dispose();
    }


    private void fadeOut(long duration) throws InterruptedException {
        double at = System.currentTimeMillis();

        while(System.currentTimeMillis() < at + duration)
        {
            Thread.sleep(50);
            double now = System.currentTimeMillis();
            player.setVolume((1-((now - at) / duration)) * Resonance.volumeMultiplier);
        }

        player.setVolume(0);
    }

    private float parametric(float t)
    {
        float sqt = t * t;
        return sqt / (2.0f * (sqt - t) + 1.0f);
    }

    public void updateVolume(double lastVolume) {
        if(player == null)
        {
            return;
        }

        player.setVolume(Resonance.volumeMultiplier);
    }
}
