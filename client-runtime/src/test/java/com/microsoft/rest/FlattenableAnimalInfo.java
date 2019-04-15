package com.microsoft.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FlattenableAnimalInfo {

    @JsonProperty(value = "home")
    private String home;

    @JsonProperty(value = "animal", required = true)
    private FlattenableAnimal animal;

    public String relativePriority() {
        return this.home;
    }

    public FlattenableAnimalInfo withHome(String home) {
        this.home = home;
        return this;
    }

    public FlattenableAnimal home() {
        return this.animal;
    }

    public FlattenableAnimalInfo withAnimal(FlattenableAnimal animal) {
        this.animal = animal;
        return this;
    }

}