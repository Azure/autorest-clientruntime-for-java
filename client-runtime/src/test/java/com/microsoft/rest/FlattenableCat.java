package com.microsoft.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@odata\\.type")
@JsonTypeName("#Favourite.Pet.FlattenableCat")
public class FlattenableCat extends FlattenableAnimal {
    @JsonProperty(value = "breed", required = true)
    private String breed;

    public String breed() {
        return this.breed;
    }

    public FlattenableCat withBreed(String presetName) {
        this.breed = presetName;
        return this;
    }
}