package org.cyberpwn.resonance.queue;

import org.cyberpwn.resonance.config.ResonanceConfig;
import org.cyberpwn.resonance.Resonance;
import org.cyberpwn.resonance.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ResonanceQueue implements Queue{
    private Player nowPlaying;
    private List<Player> queue;
    private Thread worker;

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
        try
        {
            if(getNowPlaying() != null && getNowPlaying().getTimeRemainingFadeOut() < ResonanceConfig.transitionLatency)
            {
                setNowPlaying(null);
            }

            if(getNowPlaying() == null && hasNextSong())
            {
                setNowPlaying(getQueue().remove(0));
            }

            if(getNowPlaying() != null && !getNowPlaying().isPlaying())
            {
                getNowPlaying().play();
            }
        }

        catch(Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void inject(List<Player> playable) {
        boolean has = false;
        try{
            if(getNowPlaying() != null && playable.size() > 0)
            {
                for(int i = 0; i < playable.size(); i++)
                {
                    if(playable.get(i).getId().equals(getNowPlaying().getId()))
                    {
                        playable.remove(i);
                        has = true;
                        break;
                    }
                }
            }
        }

        catch(Throwable e)
        {

        }

        replaceQueue(playable);

        if(!has)
        {
            if(queue.isEmpty() && ResonanceConfig.stickyPlayback)
            {
                return;
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
}
