package io.harness.perpetualtask.k8s.informer;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ClusterDetails {
  @NonNull String clusterId;
  @NonNull String cloudProviderId;
  @NonNull String clusterName;
  @Nonnull String kubeSystemUid;
  @Builder.Default boolean isSeen = false;
}
