/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
