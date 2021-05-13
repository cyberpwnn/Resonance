package org.cyberpwn.resonance.player;

import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.cyberpwn.resonance.util.JFXInjector;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;

public class FilePlayer extends AbstractPlayer {
    private final File file;
    private final Class<?> mediaPlayerClass;
    private Object player;
    private CountDownLatch startLatch;
    private double totalDuration;
    private boolean onTarget;
    private double volume;
    private boolean firstPlay;

    public FilePlayer(File file, long startTime) throws Throwable {
        super(startTime);
        this.file = file;
        volume = 0;
        firstPlay = true;
        onTarget = false;
        startLatch = new CountDownLatch(1);
        mediaPlayerClass = JFXInjector.loader.loadClass("javafx.scene.media.MediaPlayer");
    }

    public String toString()
    {
        return file.getName().replaceAll("\\Q.mp3\\E", "");
    }

    @Override
    protected void setVolume(double volume) {
        if(player == null)
        {
            return;
        }

        try {
            player.getClass().getMethod("setVolume", double.class).invoke(player, volume);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected double getVolume() {
        try {
            return (double) player.getClass().getMethod("getVolume").invoke(player);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void onPlay() throws Throwable {
        Thread.currentThread().setContextClassLoader(JFXInjector.loader);
        Class<?> mcl = JFXInjector.loader.loadClass("javafx.scene.media.Media");
        player = mediaPlayerClass.getConstructor(mcl).newInstance(mcl.getConstructor(String.class)
                .newInstance(file.toURI().toString()));
        setVolume(volume);

        try
        {
            player.getClass().getMethod("setOnPlaying", Runnable.class).invoke(player, (Runnable) this::onPlaying);
            player.getClass().getMethod("setOnEndOfMedia", Runnable.class).invoke(player, (Runnable) this::onEnd);
            player.getClass().getMethod("setStartTime", JFXInjector.loader.loadClass("javafx.util.Duration")).invoke(player, durationOf(getStartTime()));
            player.getClass().getMethod("play").invoke(player);
        }

        catch(Throwable e)
        {
            e.printStackTrace();
        }

        startLatch.await();
    }

    private Object durationOf(long ms) throws ClassNotFoundException, NoSuchMethodException {
        try {
            return JFXInjector.loader.loadClass("javafx.util.Duration").getMethod("millis", double.class).invoke(null, (double)ms);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void destroy() {
        onEnd();
    }

    @Override
    public long getTimeRemaining() {
        try
        {
            Object dur = player.getClass().getMethod("getCurrentTime").invoke(player);
            return (long) (totalDuration - (double)dur.getClass().getMethod("toMillis").invoke(dur));
        }

        catch(Throwable e)
        {

        }

        return 100000;
    }

    /**
     * Called when the player starts playing (some time after play)
     */
    private void onPlaying()
    {
        if(!firstPlay)
        {
            return;
        }

        firstPlay = false;
        startLatch.countDown();
        try {
            Object dur = player.getClass().getMethod("getTotalDuration").invoke(player);
            totalDuration = (double) dur.getClass().getMethod("toMillis").invoke(dur);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the player is done playing AND when the player is disposed manually.
     */
    private void onEnd()
    {
        try {
            player.getClass().getMethod("dispose").invoke(player);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        stopped();
    }

    public String getId()
    {
        return file.getAbsolutePath();
    }

    @Override
    public long getTimecode() {
        try {
            Object dur = player.getClass().getMethod("getCurrentTime").invoke(player);
            return (long) ((double)dur.getClass().getMethod("toMillis").invoke(dur));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * Returns how many milliseconds are remaining.
     * @return the remaining time in milliseconds
     */
    public long getRemainingTime()
    {
        try {
            Object dur = player.getClass().getMethod("getCurrentTime").invoke(player);
            return (long) (totalDuration - (double)dur.getClass().getMethod("toMillis").invoke(dur));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return -1;
    }
}
