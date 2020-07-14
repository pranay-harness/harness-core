package io.harness.cdng.k8s;

import static io.harness.cdng.infra.yaml.InfrastructureKind.KUBERNETES_DIRECT;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.common.AmbianceHelper;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.UserNamePasswordDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.KubernetesClusterAuthType;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig.K8sClusterConfigBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

@Singleton
public class K8sStepHelper {
  @Inject private ConnectorService connectorService;

  String getReleaseName(Infrastructure infrastructure) {
    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) infrastructure;
        return k8SDirectInfrastructure.getReleaseName();
      default:
        throw new UnsupportedOperationException(
            String.format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  private ConnectorDTO getConnector(String connectorId, Ambiance ambiance) {
    Optional<ConnectorDTO> connectorDTO = connectorService.get(AmbianceHelper.getAccountId(ambiance),
        AmbianceHelper.getOrgIdentifier(ambiance), AmbianceHelper.getProjectIdentifier(ambiance), connectorId);
    if (!connectorDTO.isPresent()) {
      throw new InvalidRequestException(
          String.format("Connector not found for identifier : [%s]", connectorId), WingsException.USER);
    }
    return connectorDTO.get();
  }

  List<EncryptedDataDetail> getEncryptedDataDetails(EncryptableSetting encryptableSetting) {
    // TODO: move to new secret manager apis when available, bypassing decryption at delegate for now
    return Collections.emptyList();
  }

  K8sClusterConfig getK8sClusterConfig(Infrastructure infrastructure, Ambiance ambiance) {
    K8sClusterConfigBuilder k8sClusterConfigBuilder = K8sClusterConfig.builder();

    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) infrastructure;
        SettingAttribute cloudProvider =
            getSettingAttribute(k8SDirectInfrastructure.getConnectorIdentifier(), ambiance);
        List<EncryptedDataDetail> encryptionDetails =
            getEncryptedDataDetails((KubernetesClusterConfig) cloudProvider.getValue());
        k8sClusterConfigBuilder.cloudProvider(cloudProvider.getValue())
            .namespace(k8SDirectInfrastructure.getNamespace())
            .cloudProviderEncryptionDetails(encryptionDetails)
            .cloudProviderName(cloudProvider.getName());
        return k8sClusterConfigBuilder.build();
      default:
        throw new UnsupportedOperationException(
            String.format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  private SettingAttribute getSettingAttribute(@NotNull ConnectorDTO connectorDTO) {
    SettingAttribute.Builder builder = SettingAttribute.Builder.aSettingAttribute()
                                           .withAccountId(connectorDTO.getAccountIdentifier())
                                           .withName(connectorDTO.getName());
    switch (connectorDTO.getConnectorType()) {
      case KUBERNETES_CLUSTER:
        KubernetesClusterConfigDTO connectorConfig = (KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig();
        KubernetesClusterDetailsDTO config = (KubernetesClusterDetailsDTO) connectorConfig.getConfig();
        UserNamePasswordDTO auth = (UserNamePasswordDTO) config.getAuth().getCredentials();
        KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder()
                                                              .authType(KubernetesClusterAuthType.USER_PASSWORD)
                                                              .masterUrl(config.getMasterUrl())
                                                              .username(auth.getUsername())
                                                              .password(auth.getPassword().toCharArray())
                                                              .build();
        builder.withValue(kubernetesClusterConfig);
        break;
      case GIT:
        GitConfigDTO gitConfigDTO = (GitConfigDTO) connectorDTO.getConnectorConfig();
        GitHTTPAuthenticationDTO gitAuth = (GitHTTPAuthenticationDTO) gitConfigDTO.getGitAuth();
        GitConfig gitConfig = GitConfig.builder()
                                  .repoUrl(gitAuth.getUrl())
                                  .username(gitAuth.getUsername())
                                  .password(gitAuth.getPasswordReference().toCharArray())
                                  .branch(gitAuth.getBranchName())
                                  .authenticationScheme(HostConnectionAttributes.AuthenticationScheme.HTTP_PASSWORD)
                                  .accountId(connectorDTO.getAccountIdentifier())
                                  .build();
        builder.withValue(gitConfig);
        break;
      default:
    }
    return builder.build();
  }

  SettingAttribute getSettingAttribute(String connectorId, Ambiance ambiance) {
    ConnectorDTO connectorDTO = getConnector(connectorId, ambiance);
    return getSettingAttribute(connectorDTO);
  }
}
