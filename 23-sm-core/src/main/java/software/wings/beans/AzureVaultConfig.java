package software.wings.beans;

import static io.harness.beans.AzureEnvironmentType.AZURE;
import static io.harness.beans.SecretManagerCapabilities.CAN_BE_DEFAULT_SM;
import static io.harness.beans.SecretManagerCapabilities.CREATE_FILE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.CREATE_INLINE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.CREATE_REFERENCE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.TRANSITION_SECRET_FROM_SM;
import static io.harness.beans.SecretManagerCapabilities.TRANSITION_SECRET_TO_SM;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.security.encryption.SecretManagerType.VAULT;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.AzureEnvironmentType;
import io.harness.beans.SecretManagerCapabilities;
import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.security.encryption.EncryptionType;
import io.harness.security.encryption.SecretManagerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Arrays;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"secretKey"})
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AzureVaultConfigKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureVaultConfig extends SecretManagerConfig {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "Azure Client Id", required = true) @NotEmpty private String clientId;

  @Attributes(title = "Azure Secret Id", required = true)
  @NotEmpty
  @Encrypted(fieldName = "azure_secret_id")
  private String secretKey;

  @Attributes(title = "Azure Tenant Id", required = true) @NotEmpty private String tenantId;

  @Attributes(title = "Azure Vault Name", required = true) private String vaultName;

  @Attributes(title = "Subscription") private String subscription;

  @Builder.Default private AzureEnvironmentType azureEnvironmentType = AZURE;

  @Override
  public void maskSecrets() {
    this.secretKey = SECRET_MASK;
  }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getValidationCriteria() {
    return obtainEncryptionServiceUrl();
  }

  @Override
  public SecretManagerType getType() {
    return VAULT;
  }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getEncryptionServiceUrl() {
    return obtainEncryptionServiceUrl();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(getEncryptionServiceUrl()));
  }

  @Override
  public EncryptionType getEncryptionType() {
    return EncryptionType.AZURE_VAULT;
  }

  private String obtainEncryptionServiceUrl() {
    if (this.azureEnvironmentType == null) {
      return String.format("https://%s.vault.azure.net", getVaultName());
    }

    switch (this.azureEnvironmentType) {
      case AZURE_US_GOVERNMENT:
        return String.format("https://%s.vault.usgovcloudapi.net", getVaultName());

      case AZURE:
      default:
        return String.format("https://%s.vault.azure.net", getVaultName());
    }
  }

  @Override
  public List<SecretManagerCapabilities> getSecretManagerCapabilities() {
    List<SecretManagerCapabilities> secretManagerCapabilities =
        Lists.newArrayList(CREATE_INLINE_SECRET, CREATE_REFERENCE_SECRET, CREATE_FILE_SECRET, CAN_BE_DEFAULT_SM);
    if (!isTemplatized()) {
      secretManagerCapabilities.add(TRANSITION_SECRET_FROM_SM);
      secretManagerCapabilities.add(TRANSITION_SECRET_TO_SM);
    }
    return secretManagerCapabilities;
  }

  @Override
  public SecretManagerConfigDTO toDTO(boolean maskSecrets) {
    throw new UnsupportedOperationException();
  }
}
