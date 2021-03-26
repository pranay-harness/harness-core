package io.harness.ccm.config;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CCMConfigYaml extends BaseYaml {
  private boolean continuousEfficiencyEnabled;

  @Builder
  public CCMConfigYaml(boolean continuousEfficiencyEnabled) {
    this.continuousEfficiencyEnabled = continuousEfficiencyEnabled;
  }
}
