package com.microsoft.rest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@odata\\.type")
@JsonTypeName("FlattenableAnimal")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "#Favourite.Pet.FlattenableDog", value = FlattenableDog.class),
        @JsonSubTypes.Type(name = "#Favourite.Pet.FlattenableCat", value = FlattenableCat.class),
        @JsonSubTypes.Type(name = "#Favourite.Pet.FlattenableRabbit", value = FlattenableRabbit.class)
})
public class FlattenableAnimal {
}

