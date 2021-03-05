package software.wings.security;

import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UsageRestrictionsYaml extends BaseYaml {
  private GenericEntityFilterYaml appFilter;
  private EnvFilterYaml envFilter;

  @Builder
  public UsageRestrictionsYaml(GenericEntityFilterYaml appFilter, EnvFilterYaml envFilter) {
    this.appFilter = appFilter;
    this.envFilter = envFilter;
  }
}
