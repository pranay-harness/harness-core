package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.expression.SecretString.SECRET_MASK;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.mongo.index.FdIndex;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rsingh on 11/02/17.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"authToken", "secretId"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "VaultConfigKeys")
public class VaultConfig extends SecretManagerConfig implements ExecutionCapabilityDemander {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "Vault Url", required = true) @FdIndex private String vaultUrl;

  @Attributes(title = "Auth token") @Encrypted(fieldName = "auth_token") private String authToken;

  @Attributes(title = "AppRole Id") private String appRoleId;

  @Attributes(title = "Secret Id") @Encrypted(fieldName = "secret_id") private String secretId;

  @Attributes(title = "Base Path") private String basePath;

  @Attributes(title = "Renew token interval", required = true) private int renewIntervalHours;

  @Attributes(title = "Token Renewal Interval in minutes", required = true) private long renewalInterval;

  @Attributes(title = "Is Vault Read Only") private boolean isReadOnly;

  /**
   * Vault 0.11 is using secrete engine V2 by default and it mandate a slightly different way of read/write secrets
   * This field should have value "1" or "2". For backward compatibility, null of value "0" will be converted to value
   * "1" automatically.
   */
  @SchemaIgnore private int secretEngineVersion;

  @Default private String secretEngineName = "secret";

  private long renewedAt;

  public enum AccessType { APP_ROLE, TOKEN }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getEncryptionServiceUrl() {
    return vaultUrl;
  }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getValidationCriteria() {
    return vaultUrl;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(vaultUrl));
  }

  @Override
  public EncryptionType getEncryptionType() {
    return EncryptionType.VAULT;
  }

  @Override
  public void maskSecrets() {
    this.authToken = SECRET_MASK;
    this.secretId = SECRET_MASK;
  }

  public AccessType getAccessType() {
    return isNotEmpty(appRoleId) ? AccessType.APP_ROLE : AccessType.TOKEN;
  }

  @Override
  public SecretManagerConfigDTO toDTO() {
    VaultConfigDTO ngVaultConfigDTO = VaultConfigDTO.builder()
                                          .uuid(getUuid())
                                          .encryptionType(getEncryptionType())
                                          .name(getName())
                                          .isDefault(isDefault())
                                          .isReadOnly(isReadOnly())
                                          .basePath(getBasePath())
                                          .secretEngineName(getSecretEngineName())
                                          .renewIntervalHours(getRenewIntervalHours())
                                          .vaultUrl(getVaultUrl())
                                          .build();
    NGSecretManagerMetadata ngMetadata = getNgMetadata();
    if (ngMetadata != null) {
      ngVaultConfigDTO.setAccountIdentifier(ngMetadata.getAccountIdentifier());
      ngVaultConfigDTO.setOrgIdentifier(ngMetadata.getOrgIdentifier());
      ngVaultConfigDTO.setProjectIdentifier(ngMetadata.getProjectIdentifier());
      ngVaultConfigDTO.setIdentifier(ngMetadata.getIdentifier());
      ngVaultConfigDTO.setTags(ngMetadata.getTags());
    }
    return ngVaultConfigDTO;
  }
}
