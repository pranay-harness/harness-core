package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.K8sPod;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
public class K8sCanaryDeployResponse implements K8sNGTaskResponse {
  Integer releaseNumber;
  List<K8sPod> k8sPodList;
  Integer currentInstances;
  String canaryWorkload;
  boolean canaryWorkloadDeployed;
}
