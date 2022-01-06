/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(CDP)
public class Release {
  public enum Status { InProgress, Succeeded, Failed }

  private int number;
  private Status status;
  private List<KubernetesResourceId> resources;
  private KubernetesResourceId managedWorkload;
  private String managedWorkloadRevision;

  @Builder.Default private List<KubernetesResourceIdRevision> managedWorkloads = new ArrayList();
  @Builder.Default private List<KubernetesResource> customWorkloads = new ArrayList<>();
  @Builder.Default private List<KubernetesResource> resourcesWithSpec = new ArrayList<>();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class KubernetesResourceIdRevision {
    private KubernetesResourceId workload;
    private String revision;
  }
}
