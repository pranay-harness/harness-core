package software.wings.beans.ci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIK8BuildTaskParams implements CIBuildSetupTaskParams {
  private KubernetesConfig kubernetesConfig;
  private GitFetchFilesConfig gitFetchFilesConfig;
  private CIK8PodParams<CIK8ContainerParams> cik8PodParams;
  private List<EncryptedDataDetail> encryptionDetails;
  @Builder.Default private static final CIBuildSetupTaskParams.Type type = Type.GCP_K8;

  @Override
  public Type getType() {
    return type;
  }
}