package io.harness.reflection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ReflectionUtils {
  private static final Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);
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

  public interface Functor { String update(String o); }

  public static void updateFieldValues(Object o, Predicate<Field> predicate, Functor functor) {
    Class<?> c = o.getClass();
    while (c.getSuperclass() != null) {
      for (Field f : c.getDeclaredFields()) {
        if (predicate.test(f)) {
          boolean isAccessible = f.isAccessible();
          f.setAccessible(true);
          try {
            String value = functor.update((String) f.get(o));
            f.set(o, value);
            f.setAccessible(isAccessible);
          } catch (IllegalAccessException e) {
            logger.error("Field [{}] is not accessible ", f.getName());
          }
        }
      }
      c = c.getSuperclass();
    }
  }
}
