package software.wings.beans;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.VerificationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class SplunkConfigYaml extends VerificationProviderYaml {
  private String splunkUrl;
  private String username;
  private String password;

  @Builder
  public SplunkConfigYaml(String type, String harnessApiVersion, String splunkUrl, String username, String password,
      UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.splunkUrl = splunkUrl;
    this.username = username;
    this.password = password;
  }
}
