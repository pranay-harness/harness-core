package software.wings.api.k8s;

import io.harness.data.SweepingOutput;
import lombok.Builder;
import lombok.Data;
import software.wings.helpers.ext.helm.response.HelmChartInfo;

@Data
@Builder
public class K8sHelmDeploymentElement implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "k8sHelmDeploymentInfo";

  private HelmChartInfo previousDeployedHelmChart;
}
