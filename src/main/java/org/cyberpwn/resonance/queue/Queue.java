package org.cyberpwn.resonance.queue;

import org.cyberpwn.resonance.Resonance;
import org.cyberpwn.resonance.player.Player;

import java.util.List;
import java.util.stream.Collectors;

public interface Queue {
    Player getNowPlaying();

    void setNowPlaying(Player player);

    default Player getUpNext()
    {
        return getQueue().size() > 0 ? getQueue().get(0) : null;
    }

    List<Player> getQueue();

    default List<String> getQueueToString()
    {
        return getQueue().stream().map(Object::toString).collect(Collectors.toList());
    }

    default void dumpQueue()
    {
        getQueue().clear();
    }

    default boolean hasNextSong()
    {
        return getUpNext() != null;
    }

    default void nextSong() {
        if(hasNextSong())
        {
            Resonance.execute(() -> {
                try {
                    getNowPlaying().stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            Player p = getQueue().remove(0);
            setNowPlaying(p);
        }
    }

    default void replaceQueue(List<Player> queue)
    {
        dumpQueue();
        getQueue().addAll(queue);
    }

    void tick();

    void inject(List<Player> playable);
}
