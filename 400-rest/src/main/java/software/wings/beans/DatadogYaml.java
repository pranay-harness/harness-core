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
public final class DatadogYaml extends VerificationProviderYaml {
  private String url;
  private String apiKey;
  private String applicationKey;

  @Builder
  public DatadogYaml(String type, String harnessApiVersion, String url, String apiKey, String applicationKey,
      UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.url = url;
    this.apiKey = apiKey;
    this.applicationKey = applicationKey;
  }
}
