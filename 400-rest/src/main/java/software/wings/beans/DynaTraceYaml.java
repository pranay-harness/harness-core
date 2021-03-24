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
public final class DynaTraceYaml extends VerificationProviderYaml {
  private String apiToken;
  private String dynaTraceUrl;

  @Builder
  public DynaTraceYaml(String type, String harnessApiVersion, String dynaTraceUrl, String apiToken,
      UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.dynaTraceUrl = dynaTraceUrl;
    this.apiToken = apiToken;
  }
}
