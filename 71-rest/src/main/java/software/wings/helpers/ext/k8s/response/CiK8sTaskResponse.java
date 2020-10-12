package software.wings.helpers.ext.k8s.response;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
@Builder
public class CiK8sTaskResponse implements K8sTaskResponse {
  @NonNull String podName;
  @NonNull PodStatus podStatus;
  List<String> podStatusLogs;
}
