package software.wings.security;

import io.harness.yaml.BaseYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UsageRestrictionYaml extends BaseYaml {
  private List<UsageRestrictionsYaml> appEnvRestrictions;

  @Builder
  public UsageRestrictionYaml(List<UsageRestrictionsYaml> appEnvRestrictions) {
    this.appEnvRestrictions = appEnvRestrictions;
  }
}
