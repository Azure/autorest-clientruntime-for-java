package com.microsoft.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@odata\\.type")
@JsonTypeName("#Favourite.Pet.FlattenableDog")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "#Favourite.Pet.FlattenablePuppyDog", value = FlattenablePuppyDog.class)
})
public class FlattenableDog extends FlattenableAnimal {
    @JsonProperty(value = "breed")
    private String breed;

    public String breed() {
        return this.breed;
    }

    public FlattenableDog withBreed(String audioLanguage) {
        this.breed = audioLanguage;
        return this;
    }
}
