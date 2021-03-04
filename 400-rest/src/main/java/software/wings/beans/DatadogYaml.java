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
public final class DatadogYaml extends VerificationProviderYaml {
  private String url;
  private String apiKey;
  private String applicationKey;

  @Builder
  public DatadogYaml(String type, String harnessApiVersion, String url, String apiKey, String applicationKey,
      UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.url = url;
    this.apiKey = apiKey;
    this.applicationKey = applicationKey;
  }
}
