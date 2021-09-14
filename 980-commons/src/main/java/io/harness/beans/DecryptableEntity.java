/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import io.harness.encryption.SecretReference;
import io.harness.reflection.ReflectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import java.lang.reflect.Field;
import java.util.List;

public interface DecryptableEntity {
  @JsonIgnore
  @SchemaIgnore
  default List<Field> getSecretReferenceFields() {
    return ReflectionUtils.getDeclaredAndInheritedFields(getClass(), f -> {
      SecretReference a = f.getAnnotation(SecretReference.class);
      return a != null;
    });
  }

  @JsonIgnore
  @SchemaIgnore
  default boolean isDecrypted() {
    return false;
  }

  default void setDecrypted(boolean decrypted) {}
}
