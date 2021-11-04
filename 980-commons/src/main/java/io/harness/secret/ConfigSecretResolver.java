package io.harness.secret;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;

@Slf4j
public class ConfigSecretResolver {
  private final SecretStorage secretStorage;

  public ConfigSecretResolver(SecretStorage secretStorage) {
    this.secretStorage = secretStorage;
  }

  public void resolveSecret(Object config) throws IOException {
    for (Field field : FieldUtils.getFieldsListWithAnnotation(config.getClass(), ConfigSecret.class)) {
      try {
        if (Modifier.isFinal(field.getModifiers())) {
          throw new ConfigSecretException(
              String.format("Annotation '%s' can't be used on final fields", ConfigSecret.class.getSimpleName()));
        }

        Object fieldValue = FieldUtils.readField(field, config, true);
        if (fieldValue == null) {
          log.warn("Failed to resolve secret! Field '{}' is null.", field);
          continue;
        }

        if (fieldValue instanceof CharSequence) {
          replaceReferenceWithSecret(config, field, fieldValue.toString(), Function.identity());

        } else if (fieldValue instanceof char[]) {
          replaceReferenceWithSecret(config, field, String.valueOf((char[]) fieldValue), String::toCharArray);

        } else {
          if (doesNotContainAnnotatedFields(fieldValue)) {
            throw new ConfigSecretException(String.format(
                "Field '%s' is annotated with '%s'. But class '%s' doesn't contain any fields annotated with '%s'",
                field, ConfigSecret.class.getSimpleName(), fieldValue.getClass(), ConfigSecret.class.getSimpleName()));
          }

          resolveSecret(fieldValue);
        }

      } catch (IllegalAccessException e) {
        log.error("Field [{}] is not accessible ", field.getName());
      }
    }
  }

  private <T> void replaceReferenceWithSecret(Object config, Field field, String secretReference,
      Function<String, T> cast) throws IOException, IllegalAccessException {
    if (secretReference.trim().isEmpty()) {
      log.warn("Failed to resolve secret! Field '{}' is empty.", field);
      return;
    }

    T secretValue = Optional.ofNullable(secretStorage.getSecretBy(secretReference))
                        .map(cast)
                        .orElseThrow(()
                                         -> new ConfigSecretException(
                                             String.format("Secret with reference '%s' not found", secretReference)));

    FieldUtils.writeField(field, config, secretValue, true);
  }

  private static boolean doesNotContainAnnotatedFields(Object object) {
    return FieldUtils.getFieldsListWithAnnotation(object.getClass(), ConfigSecret.class).isEmpty();
  }
}
