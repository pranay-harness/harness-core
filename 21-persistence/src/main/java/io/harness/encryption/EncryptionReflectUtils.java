package io.harness.encryption;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.Encryptable;
import io.harness.reflection.ReflectionUtils;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.List;

@UtilityClass
public class EncryptionReflectUtils {
  public static List<Field> getEncryptedFields(Class<?> clazz) {
    return ReflectionUtils.getDeclaredAndInheritedFields(clazz, f -> {
      Encrypted a = f.getAnnotation(Encrypted.class);
      return a != null && a.value();
    });
  }

  public static Field getEncryptedRefField(Field field, Encryptable object) {
    String encryptedFieldName = "encrypted" + StringUtils.capitalize(field.getName());

    List<Field> declaredAndInheritedFields =
        ReflectionUtils.getDeclaredAndInheritedFields(object.getClass(), f -> f.getName().equals(encryptedFieldName));

    if (isNotEmpty(declaredAndInheritedFields)) {
      return declaredAndInheritedFields.get(0);
    }

    throw new IllegalStateException("No field with " + encryptedFieldName + " found in class " + object.getClass());
  }

  public static Field getDecryptedField(Field field, Encryptable object) {
    String baseFieldName = field.getName().replace("encrypted", "");
    final String decryptedFieldName = Character.toLowerCase(baseFieldName.charAt(0)) + baseFieldName.substring(1);

    List<Field> declaredAndInheritedFields =
        ReflectionUtils.getDeclaredAndInheritedFields(object.getClass(), f -> f.getName().equals(decryptedFieldName));

    if (isNotEmpty(declaredAndInheritedFields)) {
      return declaredAndInheritedFields.get(0);
    }

    throw new IllegalStateException("No field with " + decryptedFieldName + " found in class " + object.getClass());
  }
}
