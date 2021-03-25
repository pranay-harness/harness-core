package software.wings.helpers.ext.k8s.response;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.k8s.model.K8sPod;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class K8sInstanceSyncResponse implements K8sTaskResponse {
  List<K8sPod> k8sPodInfoList;
}
