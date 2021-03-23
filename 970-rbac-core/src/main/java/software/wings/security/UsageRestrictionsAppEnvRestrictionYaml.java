package software.wings.security;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UsageRestrictionsAppEnvRestrictionYaml extends BaseYaml {
  private GenericEntityFilterYaml appFilter;
  private EnvFilterYaml envFilter;

  @Builder
  public UsageRestrictionsAppEnvRestrictionYaml(GenericEntityFilterYaml appFilter, EnvFilterYaml envFilter) {
    this.appFilter = appFilter;
    this.envFilter = envFilter;
  }
}
