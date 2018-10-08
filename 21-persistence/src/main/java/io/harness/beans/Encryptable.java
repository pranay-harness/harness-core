package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.reflection.ReflectUtils;

import java.lang.reflect.Field;
import java.util.List;

public interface Encryptable {
  String getAccountId();
  void setAccountId(String accountId);

  @JsonIgnore
  @SchemaIgnore
  default List<Field> getEncryptedFields() {
    return ReflectUtils.getEncryptedFields(this.getClass());
  }

  @JsonIgnore
  @SchemaIgnore
  default boolean isDecrypted() {
    return false;
  }

  default void setDecrypted(boolean decrypted) {}
}
