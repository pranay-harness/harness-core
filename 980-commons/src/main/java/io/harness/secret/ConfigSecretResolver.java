package io.harness.secret;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;

@Slf4j
public class ConfigSecretResolver {
  private final SecretStorage secretStorage;

  public ConfigSecretResolver(SecretStorage secretStorage) {
    this.secretStorage = secretStorage;
  }

  public void resolveSecret(Object o) throws IOException {
    for (Field field : FieldUtils.getFieldsListWithAnnotation(o.getClass(), ConfigSecret.class)) {
      if (Modifier.isFinal(field.getModifiers())) {
        throw new ConfigSecretException(ConfigSecret.class.getSimpleName() + " can't be used on final fields");
      }

      boolean isAccessible = field.isAccessible();
      field.setAccessible(true);
      try {
        Object object = field.get(o);
        if (object != null) {
          if (object instanceof String && isNotEmpty(object.toString())) {
            String value = secretStorage.getSecretBy(object.toString());
            FieldUtils.writeField(field, o, value, true);
          } else if (object instanceof char[] && ((char[]) object).length > 0) {
            String value = secretStorage.getSecretBy(String.copyValueOf((char[]) object));
            FieldUtils.writeField(field, o, value.toCharArray(), true);
          } else {
            resolveSecret(object);
          }
        }
        field.setAccessible(isAccessible);
      } catch (IllegalAccessException e) {
        log.error("Field [{}] is not accessible ", field.getName());
      }
      field.setAccessible(isAccessible);
    }
  }
}
