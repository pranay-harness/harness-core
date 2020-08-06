package io.harness.delegate.task.k8s;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.k8s.K8sConstants.CLIENT_ID_KEY;
import static io.harness.k8s.K8sConstants.CLIENT_SECRET_KEY;
import static io.harness.k8s.K8sConstants.ID_TOKEN_KEY;
import static io.harness.k8s.K8sConstants.ISSUER_URL_KEY;
import static io.harness.k8s.K8sConstants.KUBE_CONFIG_OIDC_TEMPLATE;
import static io.harness.k8s.K8sConstants.KUBE_CONFIG_TEMPLATE;
import static io.harness.k8s.K8sConstants.MASTER_URL;
import static io.harness.k8s.K8sConstants.NAME;
import static io.harness.k8s.K8sConstants.NAMESPACE;
import static io.harness.k8s.K8sConstants.NAMESPACE_KEY;
import static io.harness.k8s.K8sConstants.OIDC_AUTH_NAME;
import static io.harness.k8s.K8sConstants.OIDC_AUTH_NAME_VAL;
import static io.harness.k8s.K8sConstants.OIDC_CLIENT_ID;
import static io.harness.k8s.K8sConstants.OIDC_CLIENT_SECRET;
import static io.harness.k8s.K8sConstants.OIDC_ID_TOKEN;
import static io.harness.k8s.K8sConstants.OIDC_ISSUER_URL;
import static io.harness.k8s.K8sConstants.OIDC_RERESH_TOKEN;
import static io.harness.k8s.K8sConstants.REFRESH_TOKEN;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.harness.container.ContainerInfo;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.KubernetesClusterAuthType;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.oidc.OidcTokenRetriever;
import io.harness.logging.LogCallback;
import io.harness.oidc.model.OidcTokenRequestData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Singleton
public class ContainerDeploymentDelegateBaseHelper {
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private OidcTokenRetriever oidcTokenRetriever;

  @NotNull
  public List<Pod> getExistingPodsByLabels(KubernetesConfig kubernetesConfig, Map<String, String> labels) {
    return emptyIfNull(kubernetesContainerService.getPods(kubernetesConfig, labels));
  }

  private char[] getEncodedChars(char[] chars) {
    if (isEmpty(chars) || !(new String(chars).startsWith("-----BEGIN "))) {
      return chars;
    }
    return encodeBase64(chars).toCharArray();
  }

  private void encodeCharsIfNeeded(KubernetesConfig config) {
    config.setCaCert(getEncodedChars(config.getCaCert()));
    config.setClientCert(getEncodedChars(config.getClientCert()));
    config.setClientKey(getEncodedChars(config.getClientKey()));
  }

  public String getConfigFileContent(KubernetesConfig config) {
    encodeCharsIfNeeded(config);

    if (isBlank(config.getMasterUrl())) {
      return "";
    }

    if (KubernetesClusterAuthType.OIDC == config.getAuthType()) {
      OidcTokenRequestData oidcTokenRequestData = oidcTokenRetriever.createOidcTokenRequestData(config);
      return generateKubeConfigStringForOpenID(config, oidcTokenRequestData);
    }

    String clientCertData =
        isNotEmpty(config.getClientCert()) ? "client-certificate-data: " + new String(config.getClientCert()) : "";
    String clientKeyData =
        isNotEmpty(config.getClientKey()) ? "client-key-data: " + new String(config.getClientKey()) : "";
    String password = isNotEmpty(config.getPassword()) ? "password: " + new String(config.getPassword()) : "";
    String username = isNotEmpty(config.getUsername()) ? "username: " + config.getUsername() : "";
    String namespace = isNotEmpty(config.getNamespace()) ? "namespace: " + config.getNamespace() : "";
    String serviceAccountTokenData =
        isNotEmpty(config.getServiceAccountToken()) ? "token: " + new String(config.getServiceAccountToken()) : "";

    return KUBE_CONFIG_TEMPLATE.replace("${MASTER_URL}", config.getMasterUrl())
        .replace("${NAMESPACE}", namespace)
        .replace("${USER_NAME}", username)
        .replace("${CLIENT_CERT_DATA}", clientCertData)
        .replace("${CLIENT_KEY_DATA}", clientKeyData)
        .replace("${PASSWORD}", password)
        .replace("${SERVICE_ACCOUNT_TOKEN_DATA}", serviceAccountTokenData);
  }

  @VisibleForTesting
  String generateKubeConfigStringForOpenID(KubernetesConfig config, OidcTokenRequestData oidcTokenRequestData) {
    OpenIdOAuth2AccessToken openIdOAuth2AccessToken =
        oidcTokenRetriever.retrieveOpenIdAccessToken(oidcTokenRequestData);

    String clientIdData =
        isNotEmpty(oidcTokenRequestData.getClientId()) ? CLIENT_ID_KEY + oidcTokenRequestData.getClientId() : EMPTY;
    String clientSecretData = isNotEmpty(oidcTokenRequestData.getClientSecret())
        ? CLIENT_SECRET_KEY + oidcTokenRequestData.getClientSecret()
        : EMPTY;
    String idToken = isNotEmpty(openIdOAuth2AccessToken.getOpenIdToken())
        ? ID_TOKEN_KEY + openIdOAuth2AccessToken.getOpenIdToken()
        : EMPTY;
    String providerUrl = isNotEmpty(oidcTokenRequestData.getProviderUrl())
        ? ISSUER_URL_KEY + oidcTokenRequestData.getProviderUrl()
        : EMPTY;
    String refreshToken = isNotEmpty(openIdOAuth2AccessToken.getRefreshToken())
        ? REFRESH_TOKEN + openIdOAuth2AccessToken.getRefreshToken()
        : EMPTY;
    String authConfigName = NAME + OIDC_AUTH_NAME_VAL;
    String namespace = isNotEmpty(config.getNamespace()) ? NAMESPACE_KEY + config.getNamespace() : EMPTY;

    return KUBE_CONFIG_OIDC_TEMPLATE.replace(MASTER_URL, config.getMasterUrl())
        .replace(NAMESPACE, namespace)
        .replace(OIDC_CLIENT_ID, clientIdData)
        .replace(OIDC_CLIENT_SECRET, clientSecretData)
        .replace(OIDC_ID_TOKEN, idToken)
        .replace(OIDC_ISSUER_URL, providerUrl)
        .replace(OIDC_RERESH_TOKEN, refreshToken)
        .replace(OIDC_AUTH_NAME, authConfigName);
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
}
