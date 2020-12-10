package software.wings.api.k8s;

import io.harness.pms.sdk.core.data.SweepingOutput;

import software.wings.helpers.ext.helm.response.HelmChartInfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("k8sHelmDeploymentElement")
public class K8sHelmDeploymentElement implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "k8sHelmDeploymentInfo";

  private HelmChartInfo previousDeployedHelmChart;

  @Override
  public String getType() {
    return "k8sHelmDeploymentElement";
  }
}
