package com.rwz.host;

import java.lang.reflect.Field;

/**
 * date： 2020/8/14 14:15
 * author： rwz
 * description：
 **/
class ReflectUtil {

    public static Object getValue(Object object, String fieldName) throws Exception{
        return getValue(object.getClass(), object, fieldName);
    }

    public static Object getValue(Class cls, Object object, String fieldName) throws Exception{
        Field field = getField(cls, fieldName);
        field.setAccessible(true);
        return field.get(object);
    }

    public static Field getField(Class<?> cls, String fieldName) {
        try {
            return cls.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
        }
        return null;
    }

    public static void setValue(Object object, String fieldName, Object value) throws Exception{
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
    
}
