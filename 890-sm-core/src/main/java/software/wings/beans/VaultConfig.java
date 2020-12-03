package software.wings.beans;

import static io.harness.beans.SecretManagerCapabilities.CAN_BE_DEFAULT_SM;
import static io.harness.beans.SecretManagerCapabilities.CREATE_FILE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.CREATE_INLINE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.CREATE_REFERENCE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.TRANSITION_SECRET_FROM_SM;
import static io.harness.beans.SecretManagerCapabilities.TRANSITION_SECRET_TO_SM;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.security.encryption.SecretManagerType.VAULT;

import io.harness.beans.SecretManagerCapabilities;
import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;
import io.harness.mappers.SecretManagerConfigMapper;
import io.harness.mongo.index.FdIndex;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.security.encryption.AccessType;
import io.harness.security.encryption.EncryptionType;
import io.harness.security.encryption.SecretManagerType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

/**
 * Created by rsingh on 11/02/17.
 */

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"authToken", "secretId"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "VaultConfigKeys")
public class VaultConfig extends SecretManagerConfig {
  public static final String VAULT_VAILDATION_URL = "harness_vault_validation";
  public static final String DEFAULT_BASE_PATH = "/harness";
  public static final String DEFAULT_SECRET_ENGINE_NAME = "secret";
  public static final String DEFAULT_KEY_NAME = "value";
  public static final String PATH_SEPARATOR = "/";
  public static final String KEY_SPEARATOR = "#";

  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "Vault Url", required = true) @FdIndex private String vaultUrl;

  @Attributes(title = "Auth token") @Encrypted(fieldName = "auth_token") private String authToken;

  @Attributes(title = "AppRole Id") private String appRoleId;

  @Attributes(title = "Secret Id") @Encrypted(fieldName = "secret_id") private String secretId;

  @Attributes(title = "Base Path") private String basePath;

  // This field is deprecated and is not used anymore, will be removed in future. Please use renewalInterval
  @Deprecated @Attributes(title = "Renew token interval", required = true) private int renewIntervalHours;

  @Attributes(title = "Token Renewal Interval in minutes", required = true) private long renewalInterval;

  @Attributes(title = "Is Vault Read Only") private boolean isReadOnly;

  @Attributes(title = "Secret Engine Version") private int secretEngineVersion;

  @Attributes(title = "Secret Engine Name") private String secretEngineName;

  @Attributes(title = "Is Secret Engine Manually Entered") private boolean engineManuallyEntered;

  private long renewedAt;

  @JsonIgnore @SchemaIgnore boolean isCertValidationRequired;

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
    return getEncryptionServiceUrl();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Collections.singletonList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(vaultUrl, maskingEvaluator));
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

  @JsonIgnore
  @SchemaIgnore
  public AccessType getAccessType() {
    return isNotEmpty(appRoleId) ? AccessType.APP_ROLE : AccessType.TOKEN;
  }

  @Override
  public List<SecretManagerCapabilities> getSecretManagerCapabilities() {
    if (isReadOnly) {
      return Lists.newArrayList(CREATE_REFERENCE_SECRET);
    }
    List<SecretManagerCapabilities> secretManagerCapabilities =
        Lists.newArrayList(CREATE_INLINE_SECRET, CREATE_REFERENCE_SECRET, CREATE_FILE_SECRET, CAN_BE_DEFAULT_SM);
    if (!isTemplatized()) {
      secretManagerCapabilities.add(TRANSITION_SECRET_FROM_SM);
      secretManagerCapabilities.add(TRANSITION_SECRET_TO_SM);
    }
    return secretManagerCapabilities;
  }

  @Override
  public SecretManagerType getType() {
    return VAULT;
  }

  @Override
  public SecretManagerConfigDTO toDTO(boolean maskSecrets) {
    VaultConfigDTO ngVaultConfigDTO = VaultConfigDTO.builder()
                                          .encryptionType(getEncryptionType())
                                          .name(getName())
                                          .isDefault(isDefault())
                                          .isReadOnly(isReadOnly())
                                          .basePath(getBasePath())
                                          .secretEngineName(getSecretEngineName())
                                          .secretEngineVersion(getSecretEngineVersion())
                                          .renewIntervalHours(getRenewIntervalHours())
                                          .vaultUrl(getVaultUrl())
                                          .build();
    SecretManagerConfigMapper.updateNGSecretManagerMetadata(getNgMetadata(), ngVaultConfigDTO);
    if (!maskSecrets) {
      ngVaultConfigDTO.setAuthToken(getAuthToken());
      ngVaultConfigDTO.setSecretId(getSecretId());
    }
    return ngVaultConfigDTO;
  }

  public boolean isCertValidationRequired() {
    return isCertValidationRequired;
  }
}
