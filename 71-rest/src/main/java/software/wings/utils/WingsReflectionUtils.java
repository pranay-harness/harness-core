package software.wings.utils;

import io.harness.beans.Encryptable;
import software.wings.security.EncryptionType;

import java.lang.reflect.Field;

public class WingsReflectionUtils {
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
