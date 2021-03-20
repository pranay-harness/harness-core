package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictions;
import software.wings.service.impl.analysis.ElkValidationType;
import software.wings.yaml.setting.VerificationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class ElkConfigYaml extends VerificationProviderYaml {
  private String elkUrl;
  private String username;
  private String password;
  private String connectorType;
  private ElkValidationType validationType;

  @Builder
  public ElkConfigYaml(String type, String harnessApiVersion, String elkUrl, ElkValidationType validationType,
      String username, String password, String connectorType, UsageRestrictions.Yaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.elkUrl = elkUrl;
    this.validationType = validationType;
    this.username = username;
    this.password = password;
    this.connectorType = connectorType;
  }
}
