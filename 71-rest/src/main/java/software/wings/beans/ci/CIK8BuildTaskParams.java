package software.wings.beans.ci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.CIK8ServicePodParams;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIK8BuildTaskParams implements CIBuildSetupTaskParams {
  private KubernetesClusterConfig kubernetesClusterConfig;
  private GitFetchFilesConfig gitFetchFilesConfig;
  private CIK8PodParams<CIK8ContainerParams> cik8PodParams;
  private List<EncryptedDataDetail> encryptionDetails;
  private List<CIK8ServicePodParams> servicePodParams;
  @Builder.Default private static final CIBuildSetupTaskParams.Type type = Type.GCP_K8;

  @Override
  public Type getType() {
    return type;
  }
}