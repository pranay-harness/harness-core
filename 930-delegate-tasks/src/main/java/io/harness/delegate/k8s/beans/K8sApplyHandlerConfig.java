/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.k8s.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class K8sApplyHandlerConfig {
  private Kubectl client;
  private String releaseName;
  private List<KubernetesResource> resources;
  private List<KubernetesResource> workloads;
  private List<KubernetesResource> customWorkloads;
  private KubernetesConfig kubernetesConfig;
  private String manifestFilesDirectory;
}
