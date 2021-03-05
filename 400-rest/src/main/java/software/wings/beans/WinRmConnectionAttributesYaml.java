package software.wings.beans;

import software.wings.security.UsageRestrictionYaml;
import software.wings.settings.SettingValueYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class WinRmConnectionAttributesYaml extends SettingValueYaml {
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
      String password, boolean useSSL, int port, boolean skipCertChecks, UsageRestrictionYaml usageRestrictions) {
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
