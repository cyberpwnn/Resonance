package org.cyberpwn.resonance.player;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.cyberpwn.resonance.util.JFXInjector;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class FilePlayer extends AbstractPlayer {
    private final File file;
    private MediaPlayer player;
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

        player.setVolume(volume);
    }

    @Override
    protected double getVolume() {
        return player.getVolume();
    }

    @Override
    public void onPlay() throws Throwable {
        Class<?> mediaclass = JFXInjector.loader.loadClass("javafx.scene.media.Media");
        Class<?> mediaplayerclass = JFXInjector.loader.loadClass("javafx.scene.media.MediaPlayer");
        player = (MediaPlayer) mediaplayerclass.getConstructor(mediaclass).newInstance(mediaclass.getConstructor(String.class)
                .newInstance(file.toURI().toString()));
        player.setVolume(volume);
        player.setOnPlaying(this::onPlaying);
        player.setOnEndOfMedia(this::onEnd);
        player.setStartTime(Duration.millis(getStartTime()));
        player.play();
        startLatch.await();
    }

    @Override
    public void destroy() {
        onEnd();
    }

    @Override
    public long getTimeRemaining() {
        try
        {
            return (long) (totalDuration - player.getCurrentTime().toMillis());
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
        totalDuration = player.getTotalDuration().toMillis();
    }

    /**
     * Called when the player is done playing AND when the player is disposed manually.
     */
    private void onEnd()
    {
        player.dispose();
        stopped();
    }

    public String getId()
    {
        return file.getAbsolutePath();
    }

    @Override
    public long getTimecode() {
        return (long) player.getCurrentTime().toMillis();
    }

    /**
     * Returns how many milliseconds are remaining.
     * @return the remaining time in milliseconds
     */
    public long getRemainingTime()
    {
        return (long) (totalDuration - player.getCurrentTime().toMillis());
    }
}
