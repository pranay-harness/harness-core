package io.harness.k8s.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Release {
  public enum Status { Started, Succeeded, Failed, RollbackSucceeded, RollbackFailed }

  private int number;
  private Status status;
  private List<KubernetesResourceId> resources;
  private KubernetesResourceId managedResource;
  private String managedResourceRevision;
}
