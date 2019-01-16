package software.wings.helpers.ext.k8s.response;

import io.harness.k8s.model.K8sPod;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class K8sInstanceSyncResponse implements K8sTaskResponse {
  List<K8sPod> k8sPodInfoList;
}
