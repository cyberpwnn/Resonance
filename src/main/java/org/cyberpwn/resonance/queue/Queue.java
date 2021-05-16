package org.cyberpwn.resonance.queue;

import org.cyberpwn.resonance.Resonance;
import org.cyberpwn.resonance.player.Player;
import org.lwjgl.Sys;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface Queue {
    Player getNowPlaying();

    void setNowPlaying(Player player);

    default Player getUpNext()
    {
        try
        {
            return getQueue().size() > 0 ? getQueue().get(0) : null;
        }

        catch(Throwable e)
        {

        }

        return null;
    }

    List<Player> getQueue();

    default List<String> getQueueToString()
    {
        try
        {
            List<Player> q = getQueue();
            synchronized (q)
            {
                return q.stream().map(Object::toString).collect(Collectors.toList());
            }
        }

        catch(Throwable e)
        {

        }

        return new ArrayList<>();
    }

    default void dumpQueue()
    {
        getQueue().clear();
    }

    default boolean hasNextSong()
    {
        return getUpNext() != null;
    }

    default void replaceQueue(List<Player> queue)
    {
        dumpQueue();
        getQueue().addAll(queue);
    }

    void tick();

    void inject(List<Player> playable);
}
