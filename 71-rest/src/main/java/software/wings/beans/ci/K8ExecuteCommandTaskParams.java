package software.wings.beans.ci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.ci.pod.ConnectorDetails;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class K8ExecuteCommandTaskParams implements ExecuteCommandTaskParams {
  @NotNull private ConnectorDetails k8sConnector;
  @NotNull private K8ExecCommandParams k8ExecCommandParams;
  @Builder.Default private static final ExecuteCommandTaskParams.Type type = Type.GCP_K8;

  @Override
  public Type getType() {
    return type;
  }
}