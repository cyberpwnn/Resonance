package org.cyberpwn.resonance.config;

import java.util.ArrayList;
import java.util.List;

public class QueueConfig 
{
    private List<TagCondition> conditions;
    
    public QueueConfig()
    {
        conditions = new ArrayList<>();
    }

    public List<TagCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<TagCondition> conditions) {
        this.conditions = conditions;
    }
}
