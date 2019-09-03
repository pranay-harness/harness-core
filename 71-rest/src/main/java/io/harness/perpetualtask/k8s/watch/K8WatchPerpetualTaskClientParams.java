package io.harness.perpetualtask.k8s.watch;

import lombok.Value;

@Value
public class K8WatchPerpetualTaskClientParams {
  private String cloudProviderId;
  private String k8sResourceKind;

  public K8WatchPerpetualTaskClientParams(String cloudProviderId, String k8sResourceKind) {
    this.cloudProviderId = cloudProviderId;
    this.k8sResourceKind = k8sResourceKind;
  }
}
