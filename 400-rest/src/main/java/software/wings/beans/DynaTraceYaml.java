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
public final class DynaTraceYaml extends VerificationProviderYaml {
  private String apiToken;
  private String dynaTraceUrl;

  @Builder
  public DynaTraceYaml(String type, String harnessApiVersion, String dynaTraceUrl, String apiToken,
      UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.dynaTraceUrl = dynaTraceUrl;
    this.apiToken = apiToken;
  }
}
