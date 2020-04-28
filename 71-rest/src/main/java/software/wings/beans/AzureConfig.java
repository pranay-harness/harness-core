package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.ccm.config.CCMConfig;
import io.harness.ccm.config.CloudCostAware;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.beans.cloudprovider.azure.AzureEnvironmentType;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CloudProviderYaml;

import java.util.Arrays;
import java.util.List;

@JsonTypeName("AZURE")
@Data
@Builder
@ToString(exclude = "key")
@EqualsAndHashCode(callSuper = false)
public class AzureConfig extends SettingValue implements EncryptableSetting, CloudCostAware {
  private static final String AZURE_URL = "https://azure.microsoft.com/";
  @Attributes(title = "Client ID [Application ID]", required = true) @NotEmpty private String clientId;

  @Attributes(title = "Tenant ID [Directory ID]", required = true) @NotEmpty private String tenantId;

  @Attributes(title = "Key", required = true) @Encrypted(fieldName = "key") private char[] key;

  @SchemaIgnore @NotEmpty private String accountId;
  @JsonInclude(Include.NON_NULL) @SchemaIgnore private CCMConfig ccmConfig;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedKey;

  private AzureEnvironmentType azureEnvironmentType = AzureEnvironmentType.AZURE;

  /**
   * Instantiates a new Azure config.
   */
  public AzureConfig() {
    super(SettingVariableTypes.AZURE.name());
  }

  public AzureConfig(String clientId, String tenantId, char[] key, String accountId, CCMConfig ccmConfig,
      String encryptedKey, AzureEnvironmentType azureEnvironmentType) {
    this();
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.key = key == null ? null : key.clone();
    this.accountId = accountId;
    this.ccmConfig = ccmConfig;
    this.encryptedKey = encryptedKey;
    this.azureEnvironmentType = azureEnvironmentType;
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.CLOUD_PROVIDER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AZURE_URL));
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends CloudProviderYaml {
    private String clientId;
    private String tenantId;
    private String key;
    private AzureEnvironmentType azureEnvironmentType;

    @Builder
    public Yaml(String type, String harnessApiVersion, String clientId, String tenantId, String key,
        UsageRestrictions.Yaml usageRestrictions, AzureEnvironmentType azureEnvironmentType) {
      super(type, harnessApiVersion, usageRestrictions);
      this.clientId = clientId;
      this.tenantId = tenantId;
      this.key = key;
      this.azureEnvironmentType = azureEnvironmentType;
    }
  }
}
