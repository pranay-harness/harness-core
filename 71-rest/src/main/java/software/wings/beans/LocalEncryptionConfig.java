package software.wings.beans;

import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.service.intfc.security.SecretManager;

/**
 * When no other secret manager is configured. LOCAL encryption secret manager will be the default.
 * This entity don't need to be persisted in MongoDB.
 *
 * @author marklu on 2019-05-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LocalEncryptionConfig extends SecretManagerConfig {
  private String uuid;
  @Builder.Default private String name = SecretManager.HARNESS_DEFAULT_SECRET_MANAGER;

  @Override
  public String getEncryptionServiceUrl() {
    return null;
  }

  @Override
  public String getValidationCriteria() {
    return "encryption type: " + EncryptionType.LOCAL;
  }

  @Override
  public EncryptionType getEncryptionType() {
    return EncryptionType.LOCAL;
  }

  @Override
  public void maskSecrets() {}
}
