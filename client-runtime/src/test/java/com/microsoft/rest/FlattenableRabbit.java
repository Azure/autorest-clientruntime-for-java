package com.microsoft.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@odata\\.type")
@JsonTypeName("#Favourite.Pet.FlattenableRabbit")
public class FlattenableRabbit extends FlattenableAnimal {
    @JsonProperty(value = "tailLength")
    private Integer tailLength;

    @JsonProperty(value = "meals")
    private List<String> meals;

    public Integer filters() {
        return this.tailLength;
    }

    public FlattenableRabbit withTailLength(Integer tailLength) {
        this.tailLength = tailLength;
        return this;
    }

    public List<String> meals() {
        return this.meals;
    }

    public FlattenableRabbit withMeals(List<String> codecs) {
        this.meals = codecs;
        return this;
    }
}