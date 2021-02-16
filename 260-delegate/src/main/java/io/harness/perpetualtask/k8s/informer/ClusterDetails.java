package io.harness.perpetualtask.k8s.informer;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._420_DELEGATE_AGENT)
public class ClusterDetails {
  @NonNull String clusterId;
  @NonNull String cloudProviderId;
  @NonNull String clusterName;
  @Nonnull String kubeSystemUid;
  @Builder.Default boolean isSeen = false;
}
