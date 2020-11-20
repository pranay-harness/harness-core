package io.harness.delegate.beans.ci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.CIK8PodParams;
import io.harness.delegate.beans.ci.pod.CIK8ServicePodParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIK8BuildTaskParams implements CIBuildSetupTaskParams {
  @NotNull private ConnectorDetails k8sConnector;
  @NotNull private CIK8PodParams<CIK8ContainerParams> cik8PodParams;
  @NotNull private List<CIK8ServicePodParams> servicePodParams;
  @Builder.Default private static final Type type = Type.GCP_K8;

  @Override
  public Type getType() {
    return type;
  }
}
