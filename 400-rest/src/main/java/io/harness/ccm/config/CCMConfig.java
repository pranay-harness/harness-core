package io.harness.ccm.config;

import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
public class CCMConfig {
  boolean cloudCostEnabled;
  boolean skipK8sEventCollection;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends BaseYaml {
    private boolean continuousEfficiencyEnabled;

    @Builder
    public Yaml(boolean continuousEfficiencyEnabled) {
      this.continuousEfficiencyEnabled = continuousEfficiencyEnabled;
    }
  }
}
