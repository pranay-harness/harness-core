package software.wings.beans;

import static io.harness.mappers.SecretManagerConfigMapper.updateNGSecretManagerMetadata;

import io.harness.beans.SecretManagerConfig;
import io.harness.secretmanagerclient.dto.LocalConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
  public static final String HARNESS_DEFAULT_SECRET_MANAGER = "Harness Secrets Manager";
  private String uuid;
  @Builder.Default private String name = HARNESS_DEFAULT_SECRET_MANAGER;

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

  @Override
  public SecretManagerConfigDTO toDTO(boolean maskSecrets) {
    LocalConfigDTO localConfigDTO =
        LocalConfigDTO.builder().name(getName()).isDefault(isDefault()).encryptionType(getEncryptionType()).build();
    updateNGSecretManagerMetadata(getNgMetadata(), localConfigDTO);
    return localConfigDTO;
  }
}
