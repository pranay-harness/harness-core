package io.harness.delegate.task.k8s;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.k8s.KubernetesHelperService.getKubernetesConfigFromDefaultKubeConfigFile;
import static io.harness.k8s.KubernetesHelperService.getKubernetesConfigFromServiceAccount;
import static io.harness.k8s.KubernetesHelperService.isRunningInCluster;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.harness.container.ContainerInfo;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.KubernetesClusterAuthType;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesConfig.KubernetesConfigBuilder;
import io.harness.k8s.model.OidcGrantType;
import io.harness.logging.LogCallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Singleton
public class ContainerDeploymentDelegateBaseHelper {
  @Inject private KubernetesContainerService kubernetesContainerService;

  @NotNull
  public List<Pod> getExistingPodsByLabels(KubernetesConfig kubernetesConfig, Map<String, String> labels) {
    return emptyIfNull(kubernetesContainerService.getPods(kubernetesConfig, labels));
  }

  private List<ContainerInfo> fetchContainersUsingControllersWhenReady(KubernetesConfig kubernetesConfig,
      LogCallback executionLogCallback, List<? extends HasMetadata> controllers, List<Pod> existingPods) {
    if (isNotEmpty(controllers)) {
      return controllers.stream()
          .filter(controller
              -> !(controller.getKind().equals("ReplicaSet") && controller.getMetadata().getOwnerReferences() != null))
          .flatMap(controller -> {
            boolean isNotVersioned =
                controller.getKind().equals("DaemonSet") || controller.getKind().equals("StatefulSet");
            return kubernetesContainerService
                .getContainerInfosWhenReady(kubernetesConfig, controller.getMetadata().getName(), 0, -1,
                    (int) TimeUnit.MINUTES.toMinutes(30), existingPods, isNotVersioned, executionLogCallback, true, 0,
                    kubernetesConfig.getNamespace())
                .stream();
          })
          .collect(Collectors.toList());
    }
    return emptyList();
  }

  public List<ContainerInfo> getContainerInfosWhenReadyByLabels(
      KubernetesConfig kubernetesConfig, LogCallback logCallback, Map<String, String> labels, List<Pod> existingPods) {
    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(kubernetesConfig, labels);

    logCallback.saveExecutionLog(format("Deployed Controllers [%s]:", controllers.size()));
    controllers.forEach(controller
        -> logCallback.saveExecutionLog(String.format("Kind:%s, Name:%s (desired: %s)", controller.getKind(),
            controller.getMetadata().getName(), kubernetesContainerService.getControllerPodCount(controller))));

    return fetchContainersUsingControllersWhenReady(kubernetesConfig, logCallback, controllers, existingPods);
  }

  public int getControllerCountByLabels(KubernetesConfig kubernetesConfig, Map<String, String> labels) {
    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(kubernetesConfig, labels);

    return controllers.size();
  }

  public KubernetesConfig createKubernetesConfig(K8sInfraDelegateConfig clusterConfigDTO) {
    if (clusterConfigDTO instanceof DirectK8sInfraDelegateConfig) {
      return createKubernetesConfigFromClusterConfig(
          ((DirectK8sInfraDelegateConfig) clusterConfigDTO).getKubernetesClusterConfigDTO(),
          clusterConfigDTO.getNamespace());
    } else {
      throw new InvalidRequestException("Unhandled K8sInfraDelegateConfig " + clusterConfigDTO.getClass());
    }
  }

  private KubernetesConfig createKubernetesConfigFromClusterConfig(
      KubernetesClusterConfigDTO clusterConfigDTO, String namespace) {
    String namespaceNotBlank = isNotBlank(namespace) ? namespace : "default";
    KubernetesCredentialType kubernetesCredentialType = clusterConfigDTO.getKubernetesCredentialType();

    switch (kubernetesCredentialType) {
      case INHERIT_FROM_DELEGATE:
        if (isRunningInCluster()) {
          return getKubernetesConfigFromServiceAccount(namespaceNotBlank);
        } else {
          return getKubernetesConfigFromDefaultKubeConfigFile(namespaceNotBlank);
        }

      case MANUAL_CREDENTIALS:
        return getKubernetesConfigFromManualCredentials((KubernetesClusterDetailsDTO) (clusterConfigDTO.getConfig()));

      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported Kubernetes Credential type: [%s]", kubernetesCredentialType));
    }
  }

  private KubernetesConfig getKubernetesConfigFromManualCredentials(KubernetesClusterDetailsDTO clusterDetailsDTO) {
    KubernetesConfigBuilder kubernetesConfigBuilder =
        KubernetesConfig.builder().masterUrl(clusterDetailsDTO.getMasterUrl());

    // ToDo This does not handle the older KubernetesClusterConfigs which do not have authType set.
    KubernetesAuthDTO authDTO = clusterDetailsDTO.getAuth();
    switch (authDTO.getAuthType()) {
      case USER_PASSWORD:
        kubernetesConfigBuilder.authType(KubernetesClusterAuthType.USER_PASSWORD);
        KubernetesUserNamePasswordDTO userNamePasswordDTO = (KubernetesUserNamePasswordDTO) authDTO.getCredentials();
        kubernetesConfigBuilder.username(userNamePasswordDTO.getUsername().toCharArray());
        kubernetesConfigBuilder.password(userNamePasswordDTO.getPasswordRef().getDecryptedValue());
        kubernetesConfigBuilder.caCert(userNamePasswordDTO.getCaCertRef().getDecryptedValue());
        break;

      case CLIENT_KEY_CERT:
        kubernetesConfigBuilder.authType(KubernetesClusterAuthType.CLIENT_KEY_CERT);
        KubernetesClientKeyCertDTO clientKeyCertDTO = (KubernetesClientKeyCertDTO) authDTO.getCredentials();
        kubernetesConfigBuilder.clientCert(clientKeyCertDTO.getClientCertRef().getDecryptedValue());
        kubernetesConfigBuilder.clientKey(clientKeyCertDTO.getClientKeyRef().getDecryptedValue());
        kubernetesConfigBuilder.clientKeyPassphrase(clientKeyCertDTO.getClientKeyPassphraseRef().getDecryptedValue());
        kubernetesConfigBuilder.clientKeyAlgo(clientKeyCertDTO.getClientKeyAlgo());
        break;

      case SERVICE_ACCOUNT:
        kubernetesConfigBuilder.authType(KubernetesClusterAuthType.SERVICE_ACCOUNT);
        KubernetesServiceAccountDTO serviceAccountDTO = (KubernetesServiceAccountDTO) authDTO.getCredentials();
        kubernetesConfigBuilder.serviceAccountToken(serviceAccountDTO.getServiceAccountTokenRef().getDecryptedValue());
        break;

      case OPEN_ID_CONNECT:
        kubernetesConfigBuilder.authType(KubernetesClusterAuthType.OIDC);
        KubernetesOpenIdConnectDTO openIdConnectDTO = (KubernetesOpenIdConnectDTO) authDTO.getCredentials();

        kubernetesConfigBuilder.oidcClientId(openIdConnectDTO.getOidcClientIdRef().getDecryptedValue());
        kubernetesConfigBuilder.oidcSecret(openIdConnectDTO.getOidcClientIdRef().getDecryptedValue());
        kubernetesConfigBuilder.oidcUsername(openIdConnectDTO.getOidcUsername());
        kubernetesConfigBuilder.oidcPassword(openIdConnectDTO.getOidcPasswordRef().getDecryptedValue());
        kubernetesConfigBuilder.oidcGrantType(OidcGrantType.password);
        kubernetesConfigBuilder.oidcIdentityProviderUrl(openIdConnectDTO.getOidcIssuerUrl());
        kubernetesConfigBuilder.oidcScopes(openIdConnectDTO.getOidcScopes());
        break;

      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported Manual Credential type: [%s]", authDTO.getAuthType()));
    }
    return kubernetesConfigBuilder.build();
  }

  public String getKubeconfigFileContent(K8sInfraDelegateConfig k8sInfraDelegateConfig) {
    return kubernetesContainerService.getConfigFileContent(createKubernetesConfig(k8sInfraDelegateConfig));
  }
}
