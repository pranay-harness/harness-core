/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
