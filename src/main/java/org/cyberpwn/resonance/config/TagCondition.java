package org.cyberpwn.resonance.config;

import java.util.ArrayList;
import java.util.List;

public class TagCondition
{
    private List<String> when;
    private TagConditionMode mode;
    private List<String> play;

    public TagCondition()
    {
        this.when = new ArrayList<>();
        this.mode = TagConditionMode.OR;
        this.play = new ArrayList<>();
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
