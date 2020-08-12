package io.harness.delegate.task.k8s;

import io.harness.k8s.model.K8sPod;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class K8sRollingDeployResponse implements K8sNGTaskResponse {
  Integer releaseNumber;
  List<K8sPod> k8sPodList;
  String loadBalancer;
}
