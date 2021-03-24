package software.wings.security;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UsageRestrictionsYaml extends BaseYaml {
  private List<UsageRestrictionsAppEnvRestrictionYaml> appEnvRestrictions;

  @Builder
  public UsageRestrictionsYaml(List<UsageRestrictionsAppEnvRestrictionYaml> appEnvRestrictions) {
    this.appEnvRestrictions = appEnvRestrictions;
  }
}
