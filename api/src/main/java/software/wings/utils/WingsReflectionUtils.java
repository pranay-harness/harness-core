package software.wings.utils;

import software.wings.annotation.Encrypted;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by rsingh on 10/16/17.
 */
public class WingsReflectionUtils {
  public static List<Field> getDeclaredAndInheritedFields(Class<?> clazz) {
    List<Field> declaredFields = new ArrayList<>();
    while (clazz.getSuperclass() != null) {
      Collections.addAll(declaredFields, clazz.getDeclaredFields());
      clazz = clazz.getSuperclass();
    }
    return declaredFields;
  }

  public static List<Field> getEncryptedFields(Class<?> clazz) {
    List<Field> rv = new ArrayList<>();
    List<Field> declaredAndInheritedFields = WingsReflectionUtils.getDeclaredAndInheritedFields(clazz);
    for (Field f : declaredAndInheritedFields) {
      Encrypted a = f.getAnnotation(Encrypted.class);
      if (a != null && a.value()) {
        rv.add(f);
      }
    }

    return rv;
  }
}
