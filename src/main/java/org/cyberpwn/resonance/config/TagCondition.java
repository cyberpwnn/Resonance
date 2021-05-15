package org.cyberpwn.resonance.config;

import java.util.ArrayList;
import java.util.List;

public class TagCondition
{
    private List<String> when;
    private TagConditionMode mode;
    private boolean sudden;
    private int priority;
    private List<String> play;

    public TagCondition()
    {
        this.sudden = false;
        this.priority = 0;
        this.when = new ArrayList<>();
        this.mode = TagConditionMode.OR;
        this.play = new ArrayList<>();
    }

    public boolean isSudden() {
        return sudden;
    }

    public void setSudden(boolean sudden) {
        this.sudden = sudden;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<String> getWhen() {
        return when;
    }

    public void setWhen(List<String> when) {
        this.when = when;
    }

    public TagConditionMode getMode() {
        return mode;
    }

    public void setMode(TagConditionMode mode) {
        this.mode = mode;
    }

    public List<String> getPlay() {
        return play;
    }

    public void setPlay(List<String> play) {
        this.play = play;
    }
}
