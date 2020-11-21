package io.harness.perpetualtask.k8s.watch;

import io.harness.perpetualtask.PerpetualTaskClientParams;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class K8WatchPerpetualTaskClientParams implements PerpetualTaskClientParams {
  private String cloudProviderId;
  private String clusterId;
  private String clusterName;
}
