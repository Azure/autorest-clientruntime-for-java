package com.microsoft.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@odata\\.type")
@JsonTypeName("#Favourite.Pet.FlattenablePuppyDog")
public class FlattenablePuppyDog extends FlattenableDog {
    @JsonProperty(value = "dadName")
    private String dadName;

    public String insightsToExtract() {
        return this.dadName;
    }

    public FlattenablePuppyDog withDadName(String dadName) {
        this.dadName = dadName;
        return this;
    }

}
