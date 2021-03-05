package software.wings.beans;

import software.wings.security.UsageRestrictionYaml;
import software.wings.service.impl.analysis.ElkValidationType;
import software.wings.yaml.setting.VerificationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
      String username, String password, String connectorType, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.elkUrl = elkUrl;
    this.validationType = validationType;
    this.username = username;
    this.password = password;
    this.connectorType = connectorType;
  }
}
