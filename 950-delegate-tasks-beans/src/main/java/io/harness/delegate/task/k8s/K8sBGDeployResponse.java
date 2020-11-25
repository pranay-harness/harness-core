package io.harness.delegate.task.k8s;

import io.harness.k8s.model.K8sPod;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sBGDeployResponse implements K8sNGTaskResponse {
  Integer releaseNumber;
  List<K8sPod> k8sPodList;
  String primaryServiceName;
  String stageServiceName;
  String primaryWorkload;
  String stageWorkload;
  String stageColor;
}
