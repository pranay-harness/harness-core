package io.harness.delegate.task.k8s;

import io.harness.k8s.model.K8sPod;

import java.util.List;

import io.harness.k8s.model.KubernetesResourceId;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sRollingDeployResponse implements K8sNGTaskResponse {
  Integer releaseNumber;
  List<K8sPod> k8sPodList;
  String loadBalancer;
  List<KubernetesResourceId> prunedResourceIds;
}
