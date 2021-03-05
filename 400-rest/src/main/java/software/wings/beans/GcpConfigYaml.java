package software.wings.beans;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.CloudProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
      UsageRestrictionYaml usageRestrictions, boolean useDelegate, String delegateSelector, boolean skipValidation) {
    super(type, harnessApiVersion, usageRestrictions);
    this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
    this.delegateSelector = delegateSelector;
    this.useDelegate = useDelegate;
    this.skipValidation = skipValidation;
  }
}
