package io.harness.k8s.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sContainer {
  private String containerId;
  private String name;
  private String image;
}
