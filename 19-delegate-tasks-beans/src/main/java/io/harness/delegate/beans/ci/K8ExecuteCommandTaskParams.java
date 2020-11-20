package io.harness.delegate.beans.ci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class K8ExecuteCommandTaskParams implements ExecuteCommandTaskParams {
  @NotNull private ConnectorDetails k8sConnector;
  @NotNull private K8ExecCommandParams k8ExecCommandParams;
  @Builder.Default private static final Type type = Type.GCP_K8;

  @Override
  public Type getType() {
    return type;
  }
}
