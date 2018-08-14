package com.microsoft.rest.v2.util;

import com.fasterxml.jackson.databind.JavaType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TypeUtil {

    public static List<Class<?>> getAllTypes(Class<?> clazz) {
        List<Class<?>> types = new ArrayList<>();
        while (clazz != null) {
            types.add(clazz);
            clazz = clazz.getSuperclass();
        }
        return types;
    }

    public static Type[] getTypeArguments(Type type, Class<?> rawType) {
        if (type instanceof JavaType) {
            return ((JavaType) type).findTypeParameters(rawType);
        }
        return ((ParameterizedType) type).getActualTypeArguments();
    }

    public static Type getTypeArgument(Type type, Class<?> rawType) {
        if (type instanceof JavaType) {
            return ((JavaType) type).findTypeParameters(rawType)[0];
        }
        return ((ParameterizedType) type).getActualTypeArguments()[0];
    }
}
