package io.harness.secret;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import java.io.IOException;
import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigSecretResolver {
  private final SecretStorage secretStorage;

  public ConfigSecretResolver(SecretStorage secretStorage) {
    this.secretStorage = secretStorage;
  }

  public void resolveSecret(Object o, String secretManagerProject) throws IOException {
    Class<?> c = o.getClass();
    while (c.getSuperclass() != null) {
      for (Field f : c.getDeclaredFields()) {
        ConfigSecret annotation = f.getAnnotation(ConfigSecret.class);
        if (annotation != null) {
          boolean isAccessible = f.isAccessible();
          f.setAccessible(true);
          try {
            Object object = f.get(o);
            if (object != null) {
              if (object instanceof String && isNotEmpty(object.toString())) {
                String value = secretStorage.fetchSecret(secretManagerProject, object.toString());
                f.set(o, value);
              } else if (object instanceof char[] && ((char[]) object).length > 0) {
                String value = secretStorage.fetchSecret(secretManagerProject, String.copyValueOf((char[]) object));
                f.set(o, value.toCharArray());
              } else {
                resolveSecret(object, secretManagerProject);
              }
            }
            f.setAccessible(isAccessible);
          } catch (IllegalAccessException e) {
            log.error("Field [{}] is not accessible ", f.getName());
          }
          f.setAccessible(isAccessible);
        }
      }
      c = c.getSuperclass();
    }
  }
}
