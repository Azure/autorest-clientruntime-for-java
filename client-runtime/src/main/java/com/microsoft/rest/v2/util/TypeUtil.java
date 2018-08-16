package com.microsoft.rest.v2.util;

import com.fasterxml.jackson.databind.JavaType;
import com.microsoft.rest.v2.protocol.TypeFactory;

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

    public static Type[] getTypeArguments(Type type) {
        if (type instanceof JavaType) {
            Type[] types = new Type[((JavaType) type).containedTypeCount()];
            for (int i = 0; i != ((JavaType) type).containedTypeCount(); i++) {
                types[i] = ((JavaType) type).containedType(i);
            }
            return types;
        }
        return ((ParameterizedType) type).getActualTypeArguments();
    }

    public static Type getTypeArgument(Type type) {
        if (type instanceof JavaType) {
            return ((JavaType) type).containedType(0);
        }
        return ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    public static Class<?> getRawClass(Type type) {
        if (type instanceof JavaType) {
            return ((JavaType) type).getRawClass();
        } else if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        } else {
            return (Class<?>) type;
        }
    }

    public static Type getSuperType(Type subType, Class<?> rawSuperType, TypeFactory typeFactory) {
        return null;
    }

    public static boolean isTypeOrSubTypeOf(Type subType, Type superType) {
        Class<?> sub = getRawClass(subType);
        Class<?> sup = getRawClass(superType);

        return sup.isAssignableFrom(sub);
    }
}
