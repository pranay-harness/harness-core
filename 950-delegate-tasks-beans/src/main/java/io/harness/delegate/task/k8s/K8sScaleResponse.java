package io.harness.delegate.task.k8s;

import io.harness.k8s.model.K8sPod;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sScaleResponse implements K8sNGTaskResponse {
  List<K8sPod> k8sPodList;
}
