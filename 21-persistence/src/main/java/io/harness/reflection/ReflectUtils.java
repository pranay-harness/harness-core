package io.harness.reflection;

import io.harness.annotation.Encrypted;
import io.harness.beans.Encryptable;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReflectUtils {
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
    List<Field> declaredAndInheritedFields = getDeclaredAndInheritedFields(clazz);
    for (Field f : declaredAndInheritedFields) {
      Encrypted a = f.getAnnotation(Encrypted.class);
      if (a != null && a.value()) {
        rv.add(f);
      }
    }

    return rv;
  }

  public static Field getEncryptedRefField(Field f, Encryptable object) {
    List<Field> declaredAndInheritedFields = getDeclaredAndInheritedFields(object.getClass());
    String encryptedFieldName = "encrypted" + StringUtils.capitalize(f.getName());
    for (Field field : declaredAndInheritedFields) {
      if (field.getName().equals(encryptedFieldName)) {
        return field;
      }
    }
    throw new IllegalStateException("No field with " + encryptedFieldName + " found in class " + object.getClass());
  }

  public static Field getDecryptedField(Field f, Encryptable object) {
    List<Field> declaredAndInheritedFields = getDeclaredAndInheritedFields(object.getClass());
    String decryptedFieldName = f.getName().replace("encrypted", "");
    decryptedFieldName = Character.toLowerCase(decryptedFieldName.charAt(0)) + decryptedFieldName.substring(1);
    for (Field field : declaredAndInheritedFields) {
      if (field.getName().equals(decryptedFieldName)) {
        return field;
      }
    }
    throw new IllegalStateException("No field with " + decryptedFieldName + " found in class " + object.getClass());
  }
}
