package com.zszl.zszlScriptMod.shadowbaritone.launch.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflectionHelper {

    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    private ReflectionHelper() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T getField(Object target, String... names) {
        try {
            return (T) findField(target.getClass(), names).get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read field " + Arrays.toString(names) + " from " + target.getClass(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getStaticField(Class<?> owner, String... names) {
        try {
            return (T) findField(owner, names).get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read static field " + Arrays.toString(names) + " from " + owner, e);
        }
    }

    public static void setField(Object target, Object value, String... names) {
        try {
            findField(target.getClass(), names).set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to write field " + Arrays.toString(names) + " on " + target.getClass(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(Object target, Class<?>[] parameterTypes, Object[] args, String... names) {
        try {
            return (T) findMethod(target.getClass(), parameterTypes, names).invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke method " + Arrays.toString(names) + " on " + target.getClass(), e);
        }
    }

    private static Field findField(Class<?> type, String... names) {
        String key = type.getName() + "#FIELD#" + String.join("|", names);
        return FIELD_CACHE.computeIfAbsent(key, ignored -> {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                for (String name : names) {
                    try {
                        Field field = current.getDeclaredField(name);
                        field.setAccessible(true);
                        return field;
                    } catch (NoSuchFieldException ignoredException) {
                    }
                }
            }
            throw new IllegalStateException("Field not found: " + Arrays.toString(names) + " on " + type);
        });
    }

    private static Method findMethod(Class<?> type, Class<?>[] parameterTypes, String... names) {
        String key = type.getName() + "#METHOD#" + String.join("|", names) + "#" + Arrays.toString(parameterTypes);
        return METHOD_CACHE.computeIfAbsent(key, ignored -> {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                for (String name : names) {
                    try {
                        Method method = current.getDeclaredMethod(name, parameterTypes);
                        method.setAccessible(true);
                        return method;
                    } catch (NoSuchMethodException ignoredException) {
                    }
                }
            }
            throw new IllegalStateException("Method not found: " + Arrays.toString(names) + " on " + type);
        });
    }
}

