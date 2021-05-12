package org.cyberpwn.resonance;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.toasts.SystemToast;
import net.minecraft.util.text.TextComponentString;

import java.io.File;
import java.util.concurrent.ForkJoinPool;

public class RFilePlayerTicked
{
    private final MediaPlayer player;
    private long startTime = 0;
    private double totalDuration;
    private long fadeInMS;
    private long fadeOutMS;
    private boolean playing;
    private boolean fadingOut;
    private File file;
    private double targetVolume;
    private boolean onTarget = false;

    public RFilePlayerTicked(File file, long startAt, long fadeInMS, long fadeOutMS)
    {
        this.file = file;
        targetVolume = 1;
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

        try
        {
            Minecraft.getMinecraft().getToastGui().add(new SystemToast(SystemToast.Type.TUTORIAL_HINT, new TextComponentString("Resonance"), new TextComponentString("Now Playing " + getFile().getName().split("\\Q \\E")[0])));
        }

        catch(Throwable e)
        {

        }


    }

    public void targetVolume(double v)
    {
        this.targetVolume = v;
    }

    public boolean tick()
    {
        double targetVolume = this.targetVolume * Resonance.volumeMultiplier;
        if(targetVolume != player.getVolume())
        {
            if(player.getVolume() > targetVolume)
            {
                player.setVolume(player.getVolume() - ((player.getVolume() - targetVolume)/RConfig.volumeSmoothness));
            }

            else
            {
                player.setVolume(player.getVolume() + ((targetVolume - player.getVolume())/RConfig.volumeSmoothness));
            }

            System.out.println(player.getVolume());

            if(Math.abs(player.getVolume() - targetVolume) < 0.01)
            {
                player.setVolume(targetVolume);
            }

            onTarget = false;
            return true;
        }

        onTarget = true;
        return false;
    }

    double lerp(double a, double b, double f)
    {
        return a + f * (b - a);
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

        ForkJoinPool.commonPool().execute(() -> {
            long m = (long) (totalDuration - (5000));

            if(m < 0)
            {
                targetVolume = 0;
                return;
            }

            try {
                Thread.sleep(m);
                targetVolume = 0;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        targetVolume = 1;

        Thread ticker = new Thread(() -> {
            while(playing)
            {
                try
                {
                    Thread.sleep(tick() ? RConfig.volumeTickRate : RConfig.volumeLatency);
                }

                catch(Throwable e)
                {
                    System.out.println("Failed to tick RPlayer");
                    e.printStackTrace();
                }
            }
        });
        ticker.setPriority(Thread.MAX_PRIORITY);
        ticker.start();
    }

    public void die()
    {
        targetVolume = 0;
        onTarget = false;
        while(!onTarget)
        {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        playing = false;
        player.dispose();
    }

    private float parametric(float t)
    {
        float sqt = t * t;
        return sqt / (2.0f * (sqt - t) + 1.0f);
    }

    public void updateVolume() {
        if(player == null)
        {
            return;
        }

        player.setVolume(Resonance.volumeMultiplier);
    }
}
