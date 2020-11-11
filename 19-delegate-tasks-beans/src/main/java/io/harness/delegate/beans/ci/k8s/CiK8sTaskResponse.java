package io.harness.delegate.beans.ci.k8s;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
@Builder
public class CiK8sTaskResponse {
  @NonNull String podName;
  @NonNull PodStatus podStatus;
  List<String> podStatusLogs;
}
