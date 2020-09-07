package io.harness.perpetualtask.k8s.informer;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nonnull;

@Value
@Builder
public class ClusterDetails {
  @NonNull String clusterId;
  @NonNull String cloudProviderId;
  @NonNull String clusterName;
  @Nonnull String kubeSystemUid;
}
