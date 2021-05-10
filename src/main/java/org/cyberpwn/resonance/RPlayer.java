package org.cyberpwn.resonance;

import javafx.embed.swing.JFXPanel;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;

public class RPlayer {
    private static boolean initialized = false;

    public static void ensureInitialized()
    {
        if(initialized)
        {
            return;
        }
        initialized = true;
        final CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new JFXPanel(); // initializes JavaFX environment
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean newsong = false;
    private List<File> queue;
    private File nowPlaying;
    private RFilePlayer player;
    private Map<File, Long> timecodes;
    private boolean active = true;
    private long fadeDuration;
    private Runnable playable;

    public RPlayer(int fadeDuration, Runnable playable)
    {
        this.playable = playable;
        this.fadeDuration = fadeDuration;
        ensureInitialized();
        queue = new ArrayList<>();
        nowPlaying = null;
        timecodes = new HashMap<>();
        Thread ticker = new Thread(() -> {
            while(true)
            {
                try
                {
                    tick();
                    Thread.sleep(250);
                }

                catch(Throwable e)
                {
                    System.out.println("Failed to tick RPlayer");
                    e.printStackTrace();
                }
            }
        });
        ticker.setPriority(Thread.MAX_PRIORITY);
        ticker.setName("Resonance Player");
        ticker.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if(player != null)
            {
                player.die();
            }
        }));
    }

    private void tick() {
        playable.run();

        if(player != null)
        {
            if(nowPlaying == null)
            {
                // Now playing is null. Fade out currently playing song
                RFilePlayer ff = player;
                timecodes.put(ff.getFile(), ff.getTimeCode());
                ForkJoinPool.commonPool().execute(ff::die);
                player = null;
                System.out.println("Stopped Playing");
            }

            else if(!player.getFile().equals(nowPlaying))
            {
                // New "now playing". CROSSFADE TO NEW FILE
                RFilePlayer ff = player;
                timecodes.put(ff.getFile(), ff.getTimeCode());
                ForkJoinPool.commonPool().execute(ff::die);
                player = new RFilePlayer(nowPlaying, timecodes.compute(nowPlaying, (k, v) -> v == null ? 0 : v), fadeDuration, fadeDuration);
                System.out.print("Now Playing [" + nowPlaying.getName() + "] <-");
                for(File i : queue)
                {
                    System.out.print(i.getName() + " <-");
                }
                System.out.println();
            }

            else if(player.getRemainingTime() < fadeDuration)
            {
                nextSong();
            }
        }

        else if(nowPlaying != null)
        {
            player = new RFilePlayer(nowPlaying, timecodes.compute(nowPlaying, (k, v) -> v == null ? 0 : v), fadeDuration, fadeDuration);
            System.out.print("Now Playing [" + nowPlaying.getName() + "] <-");
            for(File i : queue)
            {
                System.out.print(i.getName() + " <-");
            }
            System.out.println();
        }

        else
        {
            nextSong();
        }
    }

    public void replaceQueue(List<File> f)
    {
        dumpQueue();
        queue.addAll(f);
        try
        {
            System.out.print("Now Playing [" + nowPlaying.getName() + "] <-");
            for(File i : queue)
            {
                System.out.print(i.getName() + " <-");
            }
            System.out.println();
        }

        catch(Throwable e)
        {
            System.out.println("Now Playing [NOTHING]");
        }
    }

    public void queue(File file)
    {
        if(queue.contains(file))
        {
            return;
        }

        queue.add(file);
    }

    public void stopPlaying()
    {
        dumpQueue();
        nowPlaying = null;
    }

    public void dumpQueue()
    {
        queue.clear();
    }

    public void nextSong() {
        newsong = true;
        if(queue.isEmpty())
        {
            nowPlaying = null;
        }

        else
        {
            nowPlaying = queue.remove(0);
        }
    }

    public File getNowPlaying() {
        return nowPlaying;
    }

    public Iterable<? extends File> getQueue() {
        return queue;
    }

    public RFilePlayer getPlayer() {
        return player;
    }
}
