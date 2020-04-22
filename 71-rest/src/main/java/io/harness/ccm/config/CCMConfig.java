package io.harness.ccm.config;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYaml;

@Data
@Builder
public class CCMConfig {
  boolean cloudCostEnabled;

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
