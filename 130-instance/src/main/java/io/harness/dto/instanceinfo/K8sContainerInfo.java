package io.harness.dto.instanceinfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sContainerInfo {
  private String containerId;
  private String name;
  private String image;
}
