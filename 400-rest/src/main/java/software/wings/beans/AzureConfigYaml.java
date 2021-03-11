package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.AzureEnvironmentType;

import software.wings.security.UsageRestrictions;
import software.wings.yaml.setting.CloudProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AzureConfigYaml extends CloudProviderYaml {
  private String clientId;
  private String tenantId;
  private String key;
  private AzureEnvironmentType azureEnvironmentType;

  @Builder
  public AzureConfigYaml(String type, String harnessApiVersion, String clientId, String tenantId, String key,
      UsageRestrictions.Yaml usageRestrictions, AzureEnvironmentType azureEnvironmentType) {
    super(type, harnessApiVersion, usageRestrictions);
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.key = key;
    this.azureEnvironmentType = azureEnvironmentType;
  }
}
