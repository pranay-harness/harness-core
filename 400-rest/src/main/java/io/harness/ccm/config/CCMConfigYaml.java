package io.harness.ccm.config;

import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
