package software.wings.helpers.ext.k8s.request;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.GcpKubernetesCluster;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.settings.SettingValue;

import java.util.List;

@Data
@Builder
public class K8sClusterConfig {
  private SettingValue cloudProvider;
  private List<EncryptedDataDetail> cloudProviderEncryptionDetails;
  private AzureKubernetesCluster azureKubernetesCluster;
  private GcpKubernetesCluster gcpKubernetesCluster;
  private String namespace;
}
