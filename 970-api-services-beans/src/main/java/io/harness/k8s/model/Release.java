package io.harness.k8s.model;

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
public class Release {
  public enum Status { InProgress, Succeeded, Failed }

  private int number;
  private Status status;
  private List<KubernetesResourceId> resources;
  private KubernetesResourceId managedWorkload;
  private String managedWorkloadRevision;

  @Builder.Default private List<KubernetesResourceIdRevision> managedWorkloads = new ArrayList();
  @Builder.Default private List<KubernetesResource> customWorkloads = new ArrayList<>();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class KubernetesResourceIdRevision {
    private KubernetesResourceId workload;
    private String revision;
  }
}
