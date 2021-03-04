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
  private GenericEntityFilter.Yaml appFilter;
  private EnvFilter.Yaml envFilter;

  @Builder
  public UsageRestrictionsYaml(GenericEntityFilter.Yaml appFilter, EnvFilter.Yaml envFilter) {
    this.appFilter = appFilter;
    this.envFilter = envFilter;
  }
}
