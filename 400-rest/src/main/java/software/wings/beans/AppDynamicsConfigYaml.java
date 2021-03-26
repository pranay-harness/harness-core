package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.setting.VerificationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AppDynamicsConfigYaml extends VerificationProviderYaml {
  private String username;
  private String password;
  private String accountName;
  private String controllerUrl;

  @Builder
  public AppDynamicsConfigYaml(String type, String harnessApiVersion, String username, String password,
      String accountName, String controllerUrl, UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.username = username;
    this.password = password;
    this.accountName = accountName;
    this.controllerUrl = controllerUrl;
  }
}
