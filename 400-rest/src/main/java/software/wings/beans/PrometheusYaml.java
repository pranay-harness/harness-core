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
public final class PrometheusYaml extends VerificationProviderYaml {
  private String prometheusUrl;

  @Builder
  public PrometheusYaml(
      String type, String harnessApiVersion, String prometheusUrl, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.prometheusUrl = prometheusUrl;
  }
}
