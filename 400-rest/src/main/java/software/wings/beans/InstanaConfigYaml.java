package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.setting.VerificationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class InstanaConfigYaml extends VerificationProviderYaml {
  private String instanaUrl;
  private String apiToken;
  @Builder
  public InstanaConfigYaml(String type, String harnessApiVersion, String instanaUrl, String apiToken,
      UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.instanaUrl = instanaUrl;
    this.apiToken = apiToken;
  }
}
