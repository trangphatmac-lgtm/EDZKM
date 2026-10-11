package com.zelix.klassmaster.util;

public class WeightedObject implements Comparable {
    public float weight;
    public Object value;

    public int compareByWeight(WeightedObject weightedObject1) {
        return Float.compare(this.weight, weightedObject1.weight);
    }

    public WeightedObject(float weight, Object valueArgument) {
        this.weight = weight;
        this.value = valueArgument;
    }

    @Override
    public int compareTo(Object other) {
        return this.compareByWeight((WeightedObject) other);
    }
}
