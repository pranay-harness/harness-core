/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@OwnedBy(CE)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
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
