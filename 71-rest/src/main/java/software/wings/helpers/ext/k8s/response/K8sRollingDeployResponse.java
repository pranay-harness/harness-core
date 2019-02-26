package software.wings.helpers.ext.k8s.response;

import io.harness.k8s.model.K8sPod;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class K8sRollingDeployResponse implements K8sTaskResponse {
  Integer releaseNumber;
  List<K8sPod> k8sPodList;
  String loadBalancer;
}
