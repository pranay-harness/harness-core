package io.harness.cdng.k8s;

import static io.harness.cdng.infra.yaml.InfrastructureKind.KUBERNETES_DIRECT;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.common.AmbianceHelper;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.k8s.DirectK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sManifestDelegateConfig;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.k8s.model.KubernetesClusterAuthType;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.utils.IdentifierRefHelper;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig.K8sClusterConfigBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

@Singleton
public class K8sStepHelper {
  @Named(DEFAULT_CONNECTOR_SERVICE) @Inject private ConnectorService connectorService;
  @Inject private SecretManagerClientService secretManagerClientService;

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

  public ConnectorDTO getConnector(String connectorId, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        connectorId, ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    Optional<ConnectorDTO> connectorDTO = connectorService.get(identifierRef.getAccountId(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    if (!connectorDTO.isPresent()) {
      throw new InvalidRequestException(
          String.format("Connector not found for identifier : [%s]", connectorId), WingsException.USER);
    }
    return connectorDTO.get();
  }

  List<EncryptedDataDetail> getEncryptedDataDetails(EncryptableSetting encryptableSetting) {
    return secretManagerClientService.getEncryptionDetails(encryptableSetting);
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
        KubernetesUserNamePasswordDTO auth = (KubernetesUserNamePasswordDTO) config.getAuth().getCredentials();
        // todo @Vaibhav/@Deepak: Now the k8 uses the new secret and this secret requires identifier and previous
        // required uuid, this has to be changed according to the framework
        KubernetesClusterConfig kubernetesClusterConfig =
            KubernetesClusterConfig.builder()
                .authType(KubernetesClusterAuthType.USER_PASSWORD)
                .masterUrl(config.getMasterUrl())
                .username(auth.getUsername() != null ? auth.getUsername().toCharArray() : null)
                .build();
        builder.withValue(kubernetesClusterConfig);
        break;
      case GIT:
        GitConfigDTO gitConfigDTO = (GitConfigDTO) connectorDTO.getConnectorConfig();
        GitHTTPAuthenticationDTO gitAuth = (GitHTTPAuthenticationDTO) gitConfigDTO.getGitAuth();
        GitConfig gitConfig =
            GitConfig.builder()
                .repoUrl(gitConfigDTO.getUrl())
                .username(gitAuth.getUsername())
                // todo @Vaibhav/@Deepak: Now the git uses the new secret and this secret requires identifier and
                // previous required uuid, this has to be changed according to the framework
                /* .encryptedPassword(SecretRefHelper.getSecretConfigString())*/
                .branch(gitConfigDTO.getBranchName())
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

  public ManifestDelegateConfig getManifestDelegateConfig(StoreConfig storeConfig, Ambiance ambiance) {
    if (storeConfig.getKind().equals(ManifestStoreType.GIT)) {
      GitStore gitStore = (GitStore) storeConfig;
      ConnectorDTO connectorDTO = getConnector(gitStore.getConnectorIdentifier(), ambiance);
      GitConfigDTO gitConfigDTO = (GitConfigDTO) connectorDTO.getConnectorConfig();

      NGAccess basicNGAccessObject = AmbianceHelper.getNgAccess(ambiance);
      List<EncryptedDataDetail> encryptedDataDetailList =
          secretManagerClientService.getEncryptionDetails(basicNGAccessObject, gitConfigDTO.getGitAuth());

      return K8sManifestDelegateConfig.builder()
          .storeDelegateConfig(getGitStoreDelegateConfig(gitStore, connectorDTO, encryptedDataDetailList))
          .build();
    } else {
      throw new UnsupportedOperationException(
          String.format("Unsupported Store Config type: [%s]", storeConfig.getKind()));
    }
  }

  public GitStoreDelegateConfig getGitStoreDelegateConfig(@Nonnull GitStore gitStore,
      @Nonnull ConnectorDTO connectorDTO, @Nonnull List<EncryptedDataDetail> encryptedDataDetailList) {
    return GitStoreDelegateConfig.builder()
        .gitConfigDTO((GitConfigDTO) connectorDTO.getConnectorConfig())
        .encryptedDataDetails(encryptedDataDetailList)
        .fetchType(gitStore.getGitFetchType())
        .branch(gitStore.getBranch())
        .commitId(gitStore.getCommitId())
        .paths(gitStore.getPaths())
        .connectorName(connectorDTO.getName())
        .build();
  }

  private List<EncryptedDataDetail> getEncryptionDataDetails(
      @Nonnull ConnectorDTO connectorDTO, @Nonnull NGAccess ngAccess) {
    switch (connectorDTO.getConnectorType()) {
      case KUBERNETES_CLUSTER:
        KubernetesClusterConfigDTO connectorConfig = (KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig();
        if (connectorConfig.getKubernetesCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
          KubernetesClusterDetailsDTO clusterDetailsDTO = (KubernetesClusterDetailsDTO) connectorConfig.getConfig();

          KubernetesAuthCredentialDTO authCredentialDTO = clusterDetailsDTO.getAuth().getCredentials();
          return secretManagerClientService.getEncryptionDetails(ngAccess, authCredentialDTO);
        } else {
          return Collections.emptyList();
        }
      case APP_DYNAMICS:
      case SPLUNK:
      case GIT:
      default:
        throw new UnsupportedOperationException(
            format("Unsupported connector type : [%s]", connectorDTO.getConnectorType()));
    }
  }

  public K8sInfraDelegateConfig getK8sInfraDelegateConfig(Infrastructure infrastructure, Ambiance ambiance) {
    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) infrastructure;
        ConnectorDTO connectorDTO = getConnector(k8SDirectInfrastructure.getConnectorIdentifier(), ambiance);

        return DirectK8sInfraDelegateConfig.builder()
            .namespace(k8SDirectInfrastructure.getNamespace())
            .kubernetesClusterConfigDTO((KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig())
            .encryptionDataDetails(getEncryptionDataDetails(connectorDTO, AmbianceHelper.getNgAccess(ambiance)))
            .build();

      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported Infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  public List<EncryptedDataDetail> getEncryptedDataDetails(
      @Nonnull GitConfigDTO gitConfigDTO, @Nonnull Ambiance ambiance) {
    return secretManagerClientService.getEncryptionDetails(
        AmbianceHelper.getNgAccess(ambiance), gitConfigDTO.getGitAuth());
  }
}
