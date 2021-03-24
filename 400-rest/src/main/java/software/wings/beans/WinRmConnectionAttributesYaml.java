package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.settings.SettingValue;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class WinRmConnectionAttributesYaml extends SettingValue.Yaml {
  private WinRmConnectionAttributes.AuthenticationScheme authenticationScheme;
  private String domain;
  private String userName;
  private String password;
  private boolean useSSL;
  private int port;
  private boolean skipCertChecks;

  @lombok.Builder
  public WinRmConnectionAttributesYaml(String type, String harnessApiVersion,
      WinRmConnectionAttributes.AuthenticationScheme authenticationScheme, String domain, String userName,
      String password, boolean useSSL, int port, boolean skipCertChecks, UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.authenticationScheme = authenticationScheme;
    this.domain = domain;
    this.userName = userName;
    this.password = password;
    this.useSSL = useSSL;
    this.port = port;
    this.skipCertChecks = skipCertChecks;
  }
}
