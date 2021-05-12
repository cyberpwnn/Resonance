package org.cyberpwn.resonance.player;

import org.cyberpwn.resonance.RConfig;
import org.cyberpwn.resonance.Resonance;
import scala.reflect.internal.Trees;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractResonancePlayer implements ResonancePlayer
{
    private final Map<String, Double> volumeMultipliers;
    private double volume;
    private boolean onTarget;

    public AbstractResonancePlayer() throws Throwable {
        this.volume = 0;
        this.onTarget = false;
        this.volumeMultipliers = new HashMap<>();
        setVolume(volume);
        play();
        Resonance.execute(() -> {
            while(isPlaying())
            {
                tick();
                try {
                    Thread.sleep(isOnTarget() ? RConfig.volumeLatency : RConfig.volumeTickRate);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    private void tick()
    {
        double v = getVolume();
        double t = getTargetVolume();
        double factor = (Math.abs(t - v) / RConfig.fadeDurationMS) * RConfig.volumeTickRate;

        if(volume != t)
        {
            onTarget = false;
            volume += volume > t ? -factor : factor;
            setVolume(parametric(volume));
        }

        onTarget = true;

        if(getTimeRemaining() < (RConfig.fadeDurationMS + RConfig.volumeLatency) && !volumeMultipliers.containsKey("ending-fadeout"))
        {
            addVolumeMultiplier("ending-fadeout", 0);
        }
    }

    protected abstract void setVolume(double volume);

    protected abstract double getVolume();

    protected double parametric(double t)
    {
        double sqt = t * t;
        return sqt / (2.0 * (sqt - t) + 1.0);
    }

    @Override
    public void addVolumeMultiplier(String key, double volume) {
        volumeMultipliers.put(key, volume);
    }

    @Override
    public void removeVolumeMultiplier(String key) {
        volumeMultipliers.remove(key);
    }

    @Override
    public double getTargetVolume() {
        double v = 1;
        String drop = null;

        for(String i : volumeMultipliers.keySet())
        {
            double vx = volumeMultipliers.get(i);
            vx = vx > 1 ? 1 : vx < 0 ? 0 : vx;

            if(drop == null && vx >= 1)
            {
                drop = i;
                continue;
            }

            v *= vx;
        }

        if(drop != null)
        {
            volumeMultipliers.remove(drop);
        }

        return v > 1 ? 1 : v < 0 ? 0 : v;
    }

    @Override
    public boolean isOnTarget() {
        return onTarget;
    }
}
