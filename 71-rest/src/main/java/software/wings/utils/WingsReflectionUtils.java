package software.wings.utils;

import org.apache.commons.lang3.StringUtils;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.security.EncryptionType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by rsingh on 10/16/17.
 */
public class WingsReflectionUtils {
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
    List<Field> declaredAndInheritedFields = WingsReflectionUtils.getDeclaredAndInheritedFields(clazz);
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

  public static boolean isSetByYaml(Encryptable object, Field encryptedField) throws IllegalAccessException {
    encryptedField.setAccessible(true);
    String encryptedFieldValue = (String) encryptedField.get(object);
    if (encryptedFieldValue != null) {
      for (EncryptionType encryptionType : EncryptionType.values()) {
        if (encryptedFieldValue.startsWith(encryptionType.getYamlName())) {
          return true;
        }
      }
    }

    return false;
  }
}
