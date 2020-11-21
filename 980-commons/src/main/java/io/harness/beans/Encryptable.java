package io.harness.beans;

import io.harness.encryption.EncryptionReflectUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
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
