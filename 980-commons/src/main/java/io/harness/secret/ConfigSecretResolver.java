/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.secret;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

@OwnedBy(PL)
@Slf4j
public class ConfigSecretResolver {
  private final SecretStorage secretStorage;

  public ConfigSecretResolver(SecretStorage secretStorage) {
    this.secretStorage = secretStorage;
  }

  public void resolveSecret(Object config) throws IOException {
    for (Field field : FieldUtils.getFieldsListWithAnnotation(config.getClass(), ConfigSecret.class)) {
      try {
        log.info("Resolving secret in field '{}'...", field);

        if (Modifier.isFinal(field.getModifiers())) {
          throw new ConfigSecretException(String.format(
              "Annotation '%s' can't be used on final field '%s'", ConfigSecret.class.getSimpleName(), field));
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
    if (StringUtils.isBlank(secretReference)) {
      log.warn("Failed to resolve secret! Field '{}' is empty.", field);
      return;
    }

    T secretValue = secretStorage.getSecretBy(secretReference)
                        .map(cast)
                        .orElseThrow(()
                                         -> new ConfigSecretException(
                                             String.format("Secret with reference '%s' not found", secretReference)));

    FieldUtils.writeField(field, config, secretValue, true);
    log.info("Secret written into field {} successfully", field);
  }

  private static boolean doesNotContainAnnotatedFields(Object object) {
    return FieldUtils.getFieldsListWithAnnotation(object.getClass(), ConfigSecret.class).isEmpty();
  }
}
