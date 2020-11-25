package io.harness.cdng.k8s;

import static io.harness.cdng.infra.yaml.InfrastructureKind.KUBERNETES_DIRECT;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.ngpipeline.common.ParameterFieldHelper.getParameterFieldValue;

import static java.lang.String.format;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
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
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.ambiance.Ambiance;
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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

@Singleton
public class K8sStepHelper {
  @Named(DEFAULT_CONNECTOR_SERVICE) @Inject private ConnectorService connectorService;
  @Inject private SecretManagerClientService secretManagerClientService;

  String getReleaseName(InfrastructureOutcome infrastructure) {
    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8sDirectInfrastructureOutcome k8SDirectInfrastructure = (K8sDirectInfrastructureOutcome) infrastructure;
        return k8SDirectInfrastructure.getReleaseName();
      default:
        throw new UnsupportedOperationException(format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  public ConnectorInfoDTO getConnector(String connectorId, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        connectorId, ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(identifierRef.getAccountIdentifier(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    if (!connectorDTO.isPresent()) {
      throw new InvalidRequestException(
          format("Connector not found for identifier : [%s]", connectorId), WingsException.USER);
    }
    return connectorDTO.get().getConnector();
  }

  List<EncryptedDataDetail> getEncryptedDataDetails(EncryptableSetting encryptableSetting) {
    return secretManagerClientService.getEncryptionDetails(encryptableSetting);
  }

  K8sClusterConfig getK8sClusterConfig(InfrastructureOutcome infrastructure, Ambiance ambiance) {
    K8sClusterConfigBuilder k8sClusterConfigBuilder = K8sClusterConfig.builder();

    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8sDirectInfrastructureOutcome k8SDirectInfrastructure = (K8sDirectInfrastructureOutcome) infrastructure;
        SettingAttribute cloudProvider = getSettingAttribute(k8SDirectInfrastructure.getConnectorRef(), ambiance);
        List<EncryptedDataDetail> encryptionDetails =
            getEncryptedDataDetails((KubernetesClusterConfig) cloudProvider.getValue());
        k8sClusterConfigBuilder.cloudProvider(cloudProvider.getValue())
            .namespace(k8SDirectInfrastructure.getNamespace())
            .cloudProviderEncryptionDetails(encryptionDetails)
            .cloudProviderName(cloudProvider.getName());
        return k8sClusterConfigBuilder.build();
      default:
        throw new UnsupportedOperationException(format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  private SettingAttribute getSettingAttribute(@NotNull ConnectorInfoDTO connectorDTO) {
    SettingAttribute.Builder builder = SettingAttribute.Builder.aSettingAttribute().withName(connectorDTO.getName());
    switch (connectorDTO.getConnectorType()) {
      case KUBERNETES_CLUSTER:
        KubernetesClusterConfigDTO connectorConfig = (KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig();
        KubernetesClusterDetailsDTO config = (KubernetesClusterDetailsDTO) connectorConfig.getCredential().getConfig();
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
                .build();
        builder.withValue(gitConfig);
        break;
      default:
    }
    return builder.build();
  }

  SettingAttribute getSettingAttribute(String connectorId, Ambiance ambiance) {
    ConnectorInfoDTO connectorDTO = getConnector(connectorId, ambiance);
    return getSettingAttribute(connectorDTO);
  }

  public ManifestDelegateConfig getManifestDelegateConfig(StoreConfig storeConfig, Ambiance ambiance) {
    if (storeConfig.getKind().equals(ManifestStoreType.GIT)) {
      GitStore gitStore = (GitStore) storeConfig;
      ConnectorInfoDTO connectorDTO = getConnector(getParameterFieldValue(gitStore.getConnectorRef()), ambiance);
      GitConfigDTO gitConfigDTO = (GitConfigDTO) connectorDTO.getConnectorConfig();

      NGAccess basicNGAccessObject = AmbianceHelper.getNgAccess(ambiance);
      List<EncryptedDataDetail> encryptedDataDetailList =
          secretManagerClientService.getEncryptionDetails(basicNGAccessObject, gitConfigDTO.getGitAuth());

      return K8sManifestDelegateConfig.builder()
          .storeDelegateConfig(getGitStoreDelegateConfig(gitStore, connectorDTO, encryptedDataDetailList))
          .build();
    } else {
      throw new UnsupportedOperationException(format("Unsupported Store Config type: [%s]", storeConfig.getKind()));
    }
  }

  public GitStoreDelegateConfig getGitStoreDelegateConfig(@Nonnull GitStore gitStore,
      @Nonnull ConnectorInfoDTO connectorDTO, @Nonnull List<EncryptedDataDetail> encryptedDataDetailList) {
    return GitStoreDelegateConfig.builder()
        .gitConfigDTO((GitConfigDTO) connectorDTO.getConnectorConfig())
        .encryptedDataDetails(encryptedDataDetailList)
        .fetchType(gitStore.getGitFetchType())
        .branch(getParameterFieldValue(gitStore.getBranch()))
        .commitId(getParameterFieldValue(gitStore.getCommitId()))
        .paths(getParameterFieldValue(gitStore.getPaths()))
        .connectorName(connectorDTO.getName())
        .build();
  }

  private List<EncryptedDataDetail> getEncryptionDataDetails(
      @Nonnull ConnectorInfoDTO connectorDTO, @Nonnull NGAccess ngAccess) {
    switch (connectorDTO.getConnectorType()) {
      case KUBERNETES_CLUSTER:
        KubernetesClusterConfigDTO connectorConfig = (KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig();
        if (connectorConfig.getCredential().getKubernetesCredentialType()
            == KubernetesCredentialType.MANUAL_CREDENTIALS) {
          KubernetesClusterDetailsDTO clusterDetailsDTO =
              (KubernetesClusterDetailsDTO) connectorConfig.getCredential().getConfig();

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

  public K8sInfraDelegateConfig getK8sInfraDelegateConfig(InfrastructureOutcome infrastructure, Ambiance ambiance) {
    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8sDirectInfrastructureOutcome k8SDirectInfrastructure = (K8sDirectInfrastructureOutcome) infrastructure;
        ConnectorInfoDTO connectorDTO = getConnector(k8SDirectInfrastructure.getConnectorRef(), ambiance);

        return DirectK8sInfraDelegateConfig.builder()
            .namespace(k8SDirectInfrastructure.getNamespace())
            .kubernetesClusterConfigDTO((KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig())
            .encryptionDataDetails(getEncryptionDataDetails(connectorDTO, AmbianceHelper.getNgAccess(ambiance)))
            .build();

      default:
        throw new UnsupportedOperationException(
            format("Unsupported Infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  public List<EncryptedDataDetail> getEncryptedDataDetails(
      @Nonnull GitConfigDTO gitConfigDTO, @Nonnull Ambiance ambiance) {
    return secretManagerClientService.getEncryptionDetails(
        AmbianceHelper.getNgAccess(ambiance), gitConfigDTO.getGitAuth());
  }
}
