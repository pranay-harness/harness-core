package software.wings.beans.config;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.VerificationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class LogzConfigYaml extends VerificationProviderYaml {
  private String logzUrl;
  private String token;

  @Builder
  public LogzConfigYaml(
      String type, String harnessApiVersion, String logzUrl, String token, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.logzUrl = logzUrl;
    this.token = token;
  }
}
