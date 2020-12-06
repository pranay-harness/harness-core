package software.wings.helpers.ext.k8s.response;

import io.harness.k8s.model.K8sPod;

import software.wings.helpers.ext.helm.response.HelmChartInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sBlueGreenDeployResponse implements K8sTaskResponse {
  Integer releaseNumber;
  List<K8sPod> k8sPodList;
  String primaryServiceName;
  String stageServiceName;
  String primaryWorkload;
  String stageWorkload;
  String stageColor;
  HelmChartInfo helmChartInfo;
}
