package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.encryption.EncryptionReflectUtils;

import java.lang.reflect.Field;
import java.util.List;

public interface Encryptable extends DecryptableEntity {
  String getAccountId();

  void setAccountId(String accountId);

  @JsonIgnore
  @SchemaIgnore
  default List<Field> getEncryptedFields() {
    return EncryptionReflectUtils.getEncryptedFields(this.getClass());
  }
}
