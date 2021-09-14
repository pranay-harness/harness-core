/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import java.util.List;
import java.util.Map;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DeploymentConfigSpec {
  private Integer minReadySeconds;
  private Boolean paused;
  private Integer replicas;
  private Integer revisionHistoryLimit;
  private Map<String, String> selector;
  private DeploymentStrategy strategy;
  private V1PodTemplateSpec template;
  private Boolean test;
  private List<DeploymentTriggerPolicy> triggers;
}
