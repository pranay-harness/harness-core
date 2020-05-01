package software.wings.beans.ci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import software.wings.beans.KubernetesConfig;

import java.util.List;

@Data
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class K8ExecuteCommandTaskParams implements ExecuteCommandTaskParams {
  private KubernetesConfig kubernetesConfig;
  private List<EncryptedDataDetail> encryptionDetails;
  private K8ExecCommandParams k8ExecCommandParams;
  @Builder.Default private static final ExecuteCommandTaskParams.Type type = Type.GCP_K8;

  @Override
  public Type getType() {
    return type;
  }
}