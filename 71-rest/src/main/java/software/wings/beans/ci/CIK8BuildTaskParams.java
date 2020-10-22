package software.wings.beans.ci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.CIK8ServicePodParams;
import software.wings.beans.ci.pod.ConnectorDetails;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIK8BuildTaskParams implements CIBuildSetupTaskParams {
  @NotNull private ConnectorDetails k8sConnector;
  @NotNull private CIK8PodParams<CIK8ContainerParams> cik8PodParams;
  @NotNull private List<CIK8ServicePodParams> servicePodParams;
  @Builder.Default private static final CIBuildSetupTaskParams.Type type = Type.GCP_K8;

  @Override
  public Type getType() {
    return type;
  }
}