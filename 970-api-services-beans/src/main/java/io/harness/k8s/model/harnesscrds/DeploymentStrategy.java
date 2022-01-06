/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DeploymentStrategy {
  private Long activeDeadlineSeconds;
  private Map<String, String> annotations;
  private CustomDeploymentStrategyParams customParams;
  private Map<String, String> labels;
  private RecreateDeploymentStrategyParams recreateParams;
  private ResourceRequirements resources;
  private RollingDeploymentStrategyParams rollingParams;
  private String type;
}
