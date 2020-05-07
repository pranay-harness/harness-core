package software.wings.beans;

import static io.harness.expression.SecretString.SECRET_MASK;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.cloudprovider.azure.AzureEnvironmentType;

import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"secretKey"})
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AzureVaultConfigKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureVaultConfig extends SecretManagerConfig implements ExecutionCapabilityDemander {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "Azure Client Id", required = true) @NotEmpty private String clientId;

  @Attributes(title = "Azure Secret Id", required = true)
  @NotEmpty
  @Encrypted(fieldName = "azure_secret_id")
  private String secretKey;

  @Attributes(title = "Azure Tenant Id", required = true) @NotEmpty private String tenantId;

  @Attributes(title = "Azure Vault Name", required = true) private String vaultName;

  @Attributes(title = "Subscription") private String subscription;

  private AzureEnvironmentType azureEnvironmentType = AzureEnvironmentType.AZURE;

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
}
