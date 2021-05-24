package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.ReleaseHistory;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
public final class PrePruningInfo {
  private final List<KubernetesResourceId> deletedResourcesInStage;
  private final ReleaseHistory releaseHistoryBeforeStageCleanUp;
}
