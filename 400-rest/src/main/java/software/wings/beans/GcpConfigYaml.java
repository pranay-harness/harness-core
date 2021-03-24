package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.setting.CloudProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class GcpConfigYaml extends CloudProviderYaml {
  private String serviceAccountKeyFileContent;
  private boolean useDelegate;
  private String delegateSelector;
  private boolean skipValidation;

  @Builder
  public GcpConfigYaml(String type, String harnessApiVersion, String serviceAccountKeyFileContent,
      UsageRestrictionsYaml usageRestrictions, boolean useDelegate, String delegateSelector, boolean skipValidation) {
    super(type, harnessApiVersion, usageRestrictions);
    this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
    this.delegateSelector = delegateSelector;
    this.useDelegate = useDelegate;
    this.skipValidation = skipValidation;
  }
}
