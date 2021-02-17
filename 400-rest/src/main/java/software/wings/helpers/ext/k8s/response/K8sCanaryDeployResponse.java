package software.wings.helpers.ext.k8s.response;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.k8s.model.K8sPod;

import software.wings.helpers.ext.helm.response.HelmChartInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class K8sCanaryDeployResponse implements K8sTaskResponse {
  Integer releaseNumber;
  List<K8sPod> k8sPodList;
  Integer currentInstances;
  String canaryWorkload;
  HelmChartInfo helmChartInfo;
}
