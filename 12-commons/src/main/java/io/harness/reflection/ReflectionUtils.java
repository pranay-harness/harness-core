package io.harness.reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ReflectionUtils {
  public static Field getFieldByName(Class<?> clazz, String fieldName) {
    while (clazz.getSuperclass() != null) {
      try {
        return clazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
        continue;
      }
    }
    return null;
  }

  public static List<Field> getAllDeclaredAndInheritedFields(Class<?> clazz) {
    List<Field> declaredFields = new ArrayList<>();
    while (clazz.getSuperclass() != null) {
      Collections.addAll(declaredFields, clazz.getDeclaredFields());
      clazz = clazz.getSuperclass();
    }
    return declaredFields;
  }

  public static List<Field> getDeclaredAndInheritedFields(Class<?> clazz, Predicate<Field> predicate) {
    List<Field> declaredFields = new ArrayList<>();
    while (clazz.getSuperclass() != null) {
      for (Field f : clazz.getDeclaredFields()) {
        if (predicate.test(f)) {
          declaredFields.add(f);
        }
      }
      clazz = clazz.getSuperclass();
    }
    return declaredFields;
  }
}
