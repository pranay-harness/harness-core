package io.harness.encryption;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;

import io.harness.beans.Encryptable;
import io.harness.exception.InvalidArgumentsException;
import io.harness.reflection.ReflectionUtils;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.List;
import javax.validation.constraints.NotNull;

@UtilityClass
public class EncryptionReflectUtils {
  public static List<Field> getEncryptedFields(Class<?> clazz) {
    return ReflectionUtils.getDeclaredAndInheritedFields(clazz, f -> {
      Encrypted a = f.getAnnotation(Encrypted.class);
      return a != null && a.value();
    });
  }

  public static String getEncryptedFieldTag(@NotNull Field field) {
    Encrypted a = field.getAnnotation(Encrypted.class);

    if (a == null || !a.value() || isEmpty(a.fieldName())) {
      throw new InvalidArgumentsException(
          String.format("The field %s declared in %s is not annotated correctly with encryption annotation",
              field.getName(), field.getDeclaringClass()),
          USER_SRE);
    }

    return a.fieldName();
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
