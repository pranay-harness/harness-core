package software.wings.beans;

import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@NoArgsConstructor
@Entity(value = "globalApiKeys", noClassnameStored = true)
public class GlobalApiKey implements PersistentEntity, UuidAware {
  @Id private String uuid;
  @NotEmpty private byte[] encryptedKey;
  @NotEmpty private ProviderType providerType;

  @Builder
  private GlobalApiKey(String uuid, byte[] encryptedKey, ProviderType providerType) {
    this.uuid = uuid;
    this.encryptedKey = encryptedKey;
    this.providerType = providerType;
  }

  @Override
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  public enum ProviderType { SALESFORCE, PROMETHEUS }
}
