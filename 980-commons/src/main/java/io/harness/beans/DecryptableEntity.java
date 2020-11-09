package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.encryption.SecretReference;
import io.harness.reflection.ReflectionUtils;

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
