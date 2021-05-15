package org.cyberpwn.resonance.player;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.cyberpwn.resonance.config.ResonanceConfig;
import org.cyberpwn.resonance.Resonance;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractPlayer implements Player
{
    private final Map<String, Double> volumeMultipliers;
    private double volume;
    private boolean onTarget;
    private long startTime;
    private boolean playing;
    private int priority;
    private boolean sudden;

    public AbstractPlayer(long startTime) throws Throwable {
        this.volume = 0;
        this.onTarget = false;
        this.startTime = startTime;
        this.volumeMultipliers = new HashMap<>();
        playing = false;
        sudden = false;
        priority = 0;
        setVolume(volume);
    }

    public void setSudden(boolean sudden)
    {
        this.sudden = sudden;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    public int getPriority()
    {
        return priority;
    }

    public boolean isSudden()
    {
        return sudden;
    }

    public boolean hasVolumeMultiplier(String k)
    {
        return volumeMultipliers.containsKey(k);
    }

    public long getStartTime()
    {
        return startTime;
    }

    public abstract void onPlay() throws Throwable;

    @Override
    public void play() throws Throwable {
        onPlay();
        Resonance.execute(() -> {
            playing = true;
            while(isPlaying())
            {
                tick();
                try {
                    Thread.sleep(isOnTarget() ? ResonanceConfig.volumeLatency : ResonanceConfig.volumeTickRate);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });

        try
        {
            if(ResonanceConfig.nowPlayingMessages)
            {
                Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentString("Now Playing " + getDisplayName()), true);
            }
        }
        catch(Throwable e)
        {

        }
    }

    protected abstract String getDisplayName();

    @Override
    public boolean isPlaying() {
        return playing;
    }

    private void tick()
    {
        double v = getVolume();
        double t = getTargetVolume();
        double factor = (Math.abs(t - v) / ResonanceConfig.fadeDurationMS) * ResonanceConfig.volumeTickRate * 4;

        if(volume != t)
        {
            onTarget = false;
            volume += volume > t ? -factor : factor;
            setVolume(parametric(volume));
        }

        else
        {
            onTarget = true;
        }

        if(Math.abs(volume - t) < 0.03)
        {
            volume = t;
        }

        if(getTimeRemaining() < (ResonanceConfig.fadeDurationMS + ResonanceConfig.volumeLatency) && !volumeMultipliers.containsKey("ending-fadeout"))
        {
            addVolumeMultiplier("ending-fadeout", 0);
        }
    }

    public void stopped()
    {
        playing = false;
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

        return (v > 1 ? 1 : v < 0 ? 0 : v) * getMinecraftVolume();
    }

    @Override
    public boolean isOnTarget() {
        return onTarget;
    }
}
