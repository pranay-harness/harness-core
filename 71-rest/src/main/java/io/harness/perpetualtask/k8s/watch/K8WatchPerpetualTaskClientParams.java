package io.harness.perpetualtask.k8s.watch;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class K8WatchPerpetualTaskClientParams {
  private String cloudProviderId;
  private String k8sResourceKind;
}
