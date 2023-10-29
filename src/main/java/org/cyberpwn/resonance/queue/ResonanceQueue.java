package org.cyberpwn.resonance.queue;

import org.cyberpwn.resonance.config.ResonanceConfig;
import org.cyberpwn.resonance.Resonance;
import org.cyberpwn.resonance.player.Player;

import java.util.*;

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
                    setNowPlaying(findNextSong());
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

                if(nowPlaying != null && next || (hasSudden() && !nowPlaying.isSudden()))
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
                setNowPlaying(null);
            }
        }
    }

    private boolean hasSudden()
    {
        for(Player i : queue)
        {
            if(i.isSudden())
            {
                return true;
            }
        }

        return false;
    }

    private Player findNextSong() {
        synchronized (queue)
        {
            try
            {
                for(Player i : queue)
                {
                    if(i.isSudden())
                    {
                        return i;
                    }
                }

                Player[] m = new Player[Math.max(Math.min(getQueue().size() / 3, 10), 1)];
                for(int i = 0; i < m.length; i++)
                {
                    m[i] = getQueue().get(new Random().nextInt(getQueue().size()));
                }

                Player r = null;
                int pri = Integer.MIN_VALUE;

                for(Player i : m)
                {
                    if(i.getPriority() > pri)
                    {
                        pri = i.getPriority();
                        r = i;
                    }
                }

                if(r != null)
                {
                    return r;
                }

                return getQueue().get(new Random().nextInt(getQueue().size()));
            }

            catch(Throwable e)
            {

            }

            return getQueue().get(0);
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
