package org.cyberpwn.resonance.queue;

import net.minecraftforge.event.entity.ProjectileImpactEvent;
import org.cyberpwn.resonance.config.ResonanceConfig;
import org.cyberpwn.resonance.Resonance;
import org.cyberpwn.resonance.player.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ResonanceQueue implements Queue{
    private Player nowPlaying;
    private List<Player> queue;
    private Thread worker;
    private boolean next = false;

    public ResonanceQueue()
    {
        queue = new ArrayList<>();
        Resonance.execute(() -> {
            while(true)
            {
                worker = Thread.currentThread();
                tick();
                try {
                    Thread.sleep(ResonanceConfig.transitionLatency);
                } catch (InterruptedException e) {

                }
            }
        });
    }

    public void tick()
    {
        synchronized (queue)
        {
            try
            {
                if(getNowPlaying() != null && getNowPlaying().getTimeRemainingFadeOut() < ResonanceConfig.transitionLatency)
                {
                    setNowPlaying(null);
                }

                if(getNowPlaying() == null && hasNextSong())
                {
                    setNowPlaying(getQueue().get(new Random().nextInt(getQueue().size())));
                }

                if(getNowPlaying() != null && !getNowPlaying().isPlaying())
                {
                    getNowPlaying().play();
                }

                if(getNowPlaying() != null)
                {
                    boolean valid = false;

                    for(Player i : queue)
                    {
                        if(i.getId().equals(nowPlaying.getId()))
                        {
                            valid = true;
                            break;
                        }
                    }

                    if(!valid)
                    {
                        System.out.println("INVALID NOTHING MATCHES " + nowPlaying.getId());

                        for(Player i : queue)
                        {
                            System.out.println("- " + i.getId());
                        }

                        Player p = nowPlaying;
                        nowPlaying = null;
                        worker.interrupt();
                        Resonance.execute(() -> {
                            try {
                                p.stop();
                            } catch (Throwable e) {

                            }
                        });
                    }
                }

                if(nowPlaying != null && next)
                {
                    next = false;
                    Player p = nowPlaying;
                    nowPlaying = null;
                    worker.interrupt();
                    Resonance.execute(() -> {
                        try {
                            p.stop();
                        } catch (Throwable e) {

                        }
                    });
                }
            }

            catch(Throwable e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void inject(List<Player> playable) {
        synchronized (playable)
        {
            try
            {
                replaceQueue(playable);
            }

            catch(Throwable e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Player getNowPlaying() {
        return nowPlaying;
    }

    @Override
    public void setNowPlaying(Player player) {
        this.nowPlaying = player;
    }

    @Override
    public List<Player> getQueue() {
        return queue;
    }

    public void queueNext() {
        next = true;
    }
}
