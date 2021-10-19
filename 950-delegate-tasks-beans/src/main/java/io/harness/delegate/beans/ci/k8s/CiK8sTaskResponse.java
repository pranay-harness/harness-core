package io.harness.delegate.beans.ci.k8s;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
@OwnedBy(HarnessTeam.CI)
public class CiK8sTaskResponse {
  @NonNull String podName;
  @NonNull String podNamespace;
  @NonNull PodStatus podStatus;
  List<String> podStatusLogs;
}
