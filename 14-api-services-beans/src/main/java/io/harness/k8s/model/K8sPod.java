package io.harness.k8s.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class K8sPod {
  private String uid;
  private String name;
  private String namespace;
  private String releaseName;
  private String podIP;
  private List<K8sContainer> containerList;
  private boolean newPod;
}
