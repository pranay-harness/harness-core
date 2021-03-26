package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DirectKubernetesInfrastructureMappingYaml extends ContainerInfrastructureMappingYamlWithComputeProvider {
  private String masterUrl;
  private String username;
  private String password;
  private String caCert;
  private String clientCert;
  private String clientKey;
  private String clientKeyPassphrase;
  private String serviceAccountToken;
  private String clientKeyAlgo;
  private String namespace;
  private String releaseName;

  @lombok.Builder
  public DirectKubernetesInfrastructureMappingYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName, String cluster,
      String masterUrl, String username, String password, String caCert, String clientCert, String clientKey,
      String clientKeyPassphrase, String serviceAccountToken, String clientKeyAlgo, String namespace,
      String releaseName, Map<String, Object> blueprints) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, cluster, blueprints);
    this.masterUrl = masterUrl;
    this.username = username;
    this.password = password;
    this.caCert = caCert;
    this.clientCert = clientCert;
    this.clientKey = clientKey;
    this.clientKeyPassphrase = clientKeyPassphrase;
    this.serviceAccountToken = serviceAccountToken;
    this.clientKeyAlgo = clientKeyAlgo;
    this.namespace = namespace;
    this.releaseName = releaseName;
  }
}
