package software.wings.helpers.ext.k8s.response;

import io.harness.k8s.model.K8sPod;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sScaleResponse implements K8sTaskResponse {
  List<K8sPod> k8sPodList;
}
