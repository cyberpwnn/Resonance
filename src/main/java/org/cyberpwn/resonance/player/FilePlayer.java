package org.cyberpwn.resonance.player;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class FileResonancePlayer extends AbstractResonancePlayer {
    private final File file;
    private MediaPlayer player;
    private CountDownLatch startLatch;
    private double totalDuration;
    private boolean onTarget;
    private double volume;
    private boolean firstPlay;
    private boolean playing;

    public FileResonancePlayer(File file) throws Throwable {
        super();
        this.file = file;
        volume = 0;
        playing = true;
        firstPlay = true;
        onTarget = false;
        startLatch = new CountDownLatch(1);
    }

    @Override
    protected void setVolume(double volume) {
        player.setVolume(volume);
    }

    @Override
    protected double getVolume() {
        return player.getVolume();
    }

    @Override
    public void play(long timecode) throws InterruptedException {
        player = new MediaPlayer(new Media(file.toURI().toString()));
        player.setVolume(volume);
        player.setOnPlaying(this::onPlaying);
        player.setOnEndOfMedia(this::onEnd);
        player.setStartTime(Duration.millis(timecode));
        player.play();
        startLatch.await();
    }

    @Override
    public void destroy() {
        onEnd();
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    @Override
    public long getTimeRemaining() {
        return (long) (totalDuration - player.getCurrentTime().toMillis());
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
        playing = false;
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
