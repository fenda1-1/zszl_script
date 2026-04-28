package com.zszl.zszlScriptMod.utils;

import java.lang.reflect.Field;

public final class ReflectionCompat {

    private ReflectionCompat() {
    }

    public static <T, E> T getPrivateValue(Class<? super E> clazz, E instance, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                T value = (T) field.get(instance);
                return value;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static <T, E> void setPrivateValue(Class<? super T> clazz, T instance, E value, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(instance, value);
                return;
            } catch (Exception ignored) {
            }
        }
    }
}
