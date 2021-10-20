package io.harness.entity;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.ClientType;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@NoArgsConstructor
@Entity(value = "harnessApiKeys", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "HarnessApiKeyKeys")
public final class HarnessApiKey implements PersistentEntity, UuidAware {
  @Id private String uuid;
  @NotEmpty private byte[] encryptedKey;
  @FdIndex @NotEmpty private ClientType clientType;

  @Builder
  private HarnessApiKey(String uuid, byte[] encryptedKey, ClientType clientType) {
    this.uuid = uuid;
    this.encryptedKey = encryptedKey;
    this.clientType = clientType;
  }

  @Override
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getUuid() {
    return uuid;
  }
}
