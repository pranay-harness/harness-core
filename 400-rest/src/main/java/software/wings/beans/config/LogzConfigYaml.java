package software.wings.beans.config;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictions;
import software.wings.yaml.setting.VerificationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class LogzConfigYaml extends VerificationProviderYaml {
  private String logzUrl;
  private String token;

  @Builder
  public LogzConfigYaml(
      String type, String harnessApiVersion, String logzUrl, String token, UsageRestrictions.Yaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.logzUrl = logzUrl;
    this.token = token;
  }
}
