package software.wings.helpers.ext.container;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.k8s.K8sConstants.CLIENT_ID_KEY;
import static io.harness.k8s.K8sConstants.CLIENT_SECRET_KEY;
import static io.harness.k8s.K8sConstants.ID_TOKEN_KEY;
import static io.harness.k8s.K8sConstants.ISSUER_URL_KEY;
import static io.harness.k8s.K8sConstants.KUBE_CONFIG_OIDC_TEMPLATE;
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
import static software.wings.helpers.ext.helm.HelmConstants.KUBE_CONFIG_TEMPLATE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.VersionInfo;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.k8s.oidc.OidcTokenRetriever;
import io.harness.logging.LogCallback;
import io.harness.oidc.model.OidcTokenRequestData;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterAuthType;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 4/20/18.
 */
@Singleton
@Slf4j
public class ContainerDeploymentDelegateHelper {
  @Inject private AzureHelperService azureHelperService;
  @Inject private GkeClusterService gkeClusterService;
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private EncryptionService encryptionService;
  @Inject private OidcTokenRetriever oidcTokenRetriever;

  private static final String KUBE_CONFIG_DIR = "./repository/helm/.kube/";
  private static final int KUBERNETESS_116_VERSION = 116;
  private static final String NON_DIGITS_REGEX = "\\D+";
  private static final int VERSION_LENGTH = 3;

  public static final LoadingCache<String, Object> lockObjects =
      CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build(CacheLoader.from(Object::new));

  public String createAndGetKubeConfigLocation(ContainerServiceParams containerServiceParam) {
    return createKubeConfig(getKubernetesConfig(containerServiceParam));
  }

  public String getKubeconfigFileContent(K8sClusterConfig k8sClusterConfig) {
    return getConfigFileContent(getKubernetesConfig(k8sClusterConfig));
  }

  private String createKubeConfig(KubernetesConfig kubernetesConfig) {
    try {
      String configFileContent = getConfigFileContent(kubernetesConfig);
      String md5Hash = DigestUtils.md5Hex(configFileContent);

      synchronized (lockObjects.get(md5Hash)) {
        String configFilePath = KUBE_CONFIG_DIR + md5Hash;
        File file = new File(configFilePath);
        if (!file.exists()) {
          logger.info("File doesn't exist. Creating file at path {}", configFilePath);
          FileUtils.forceMkdir(file.getParentFile());
          FileUtils.writeStringToFile(file, configFileContent, UTF_8);
          logger.info("Created file with size {}", file.length());
        }
        return file.getAbsolutePath();
      }
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
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

  public String getOidcIdToken(KubernetesConfig config) {
    OidcTokenRequestData oidcTokenRequestData = createOidcTokenRequestData(config);

    OpenIdOAuth2AccessToken openIdOAuth2AccessToken = retrieveOpenIdAccessToken(oidcTokenRequestData);
    if (openIdOAuth2AccessToken != null) {
      return openIdOAuth2AccessToken.getOpenIdToken();
    }

    return null;
  }

  private OidcTokenRequestData createOidcTokenRequestData(KubernetesConfig config) {
    return OidcTokenRequestData.builder()
        .providerUrl(config.getOidcIdentityProviderUrl())
        .clientId(config.getOidcClientId() == null ? null : new String(config.getOidcClientId()))
        .grantType(config.getOidcGrantType().name())
        .clientSecret(config.getOidcSecret() == null ? null : new String(config.getOidcSecret()))
        .username(config.getOidcUsername())
        .password(config.getOidcPassword() == null ? null : new String(config.getOidcPassword()))
        .scope(config.getOidcScopes())
        .build();
  }

  public String getConfigFileContent(KubernetesConfig config) {
    encodeCharsIfNeeded(config);

    if (isBlank(config.getMasterUrl())) {
      return "";
    }

    if (KubernetesClusterAuthType.OIDC == config.getAuthType()) {
      OidcTokenRequestData oidcTokenRequestData = createOidcTokenRequestData(config);
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
    OpenIdOAuth2AccessToken openIdOAuth2AccessToken = retrieveOpenIdAccessToken(oidcTokenRequestData);

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

  @VisibleForTesting
  OpenIdOAuth2AccessToken retrieveOpenIdAccessToken(OidcTokenRequestData oidcTokenRequestData) {
    OpenIdOAuth2AccessToken accessToken = null;
    Exception ex = null;
    try {
      accessToken = oidcTokenRetriever.getAccessToken(oidcTokenRequestData);
    } catch (InterruptedException intEx) {
      Thread.currentThread().interrupt();
      ex = intEx;
    } catch (Exception e) {
      ex = e;
    }

    if (ex != null) {
      throw new InvalidRequestException(
          "Failed to fetch OpenId Access Token. " + ExceptionUtils.getMessage(ex), ex, WingsException.USER);
    }
    return accessToken;
  }

  public String getKubeConfigFileContent(ContainerServiceParams containerServiceParam) {
    try {
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParam);
      if (!kubernetesConfig.isDecrypted()) {
        encryptionService.decrypt(kubernetesConfig, containerServiceParam.getEncryptionDetails());
      }
      return getConfigFileContent(kubernetesConfig);
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  public KubernetesConfig getKubernetesConfig(ContainerServiceParams containerServiceParam) {
    SettingAttribute settingAttribute = containerServiceParam.getSettingAttribute();
    List<EncryptedDataDetail> encryptedDataDetails = containerServiceParam.getEncryptionDetails();
    String clusterName = containerServiceParam.getClusterName();
    String namespace = containerServiceParam.getNamespace();

    KubernetesConfig kubernetesConfig;
    if (settingAttribute.getValue() instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) settingAttribute.getValue();
    } else if (settingAttribute.getValue() instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) settingAttribute.getValue();
      encryptionService.decrypt(kubernetesClusterConfig, encryptedDataDetails);
      kubernetesConfig = kubernetesClusterConfig.createKubernetesConfig(namespace);
    } else if (settingAttribute.getValue() instanceof GcpConfig) {
      kubernetesConfig = gkeClusterService.getCluster(settingAttribute, encryptedDataDetails, clusterName, namespace);
    } else if (settingAttribute.getValue() instanceof AzureConfig) {
      AzureConfig azureConfig = (AzureConfig) settingAttribute.getValue();
      kubernetesConfig = azureHelperService.getKubernetesClusterConfig(azureConfig, encryptedDataDetails,
          containerServiceParam.getSubscriptionId(), containerServiceParam.getResourceGroup(), clusterName, namespace);
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam(
              "args", "Unknown kubernetes cloud provider setting value: " + settingAttribute.getValue().getType());
    }

    kubernetesConfig.setDecrypted(true);
    return kubernetesConfig;
  }

  public KubernetesConfig getKubernetesConfig(K8sClusterConfig k8sClusterConfig) {
    SettingValue cloudProvider = k8sClusterConfig.getCloudProvider();
    List<EncryptedDataDetail> encryptedDataDetails = k8sClusterConfig.getCloudProviderEncryptionDetails();
    String namespace = k8sClusterConfig.getNamespace();

    KubernetesConfig kubernetesConfig;
    if (cloudProvider instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) cloudProvider;
    } else if (cloudProvider instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) cloudProvider;
      encryptionService.decrypt(kubernetesClusterConfig, encryptedDataDetails);
      kubernetesConfig = kubernetesClusterConfig.createKubernetesConfig(namespace);
    } else if (cloudProvider instanceof GcpConfig) {
      kubernetesConfig = gkeClusterService.getCluster((GcpConfig) cloudProvider, encryptedDataDetails,
          k8sClusterConfig.getGcpKubernetesCluster().getClusterName(), namespace);
    } else if (cloudProvider instanceof AzureConfig) {
      AzureConfig azureConfig = (AzureConfig) cloudProvider;
      kubernetesConfig = azureHelperService.getKubernetesClusterConfig(
          azureConfig, encryptedDataDetails, k8sClusterConfig.getAzureKubernetesCluster(), namespace);
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Unknown kubernetes cloud provider setting value: " + cloudProvider.getType());
    }

    kubernetesConfig.setDecrypted(true);
    return kubernetesConfig;
  }

  @NotNull
  public List<Pod> getExistingPodsByLabels(
      ContainerServiceParams containerServiceParams, KubernetesConfig kubernetesConfig, Map<String, String> labels) {
    return emptyIfNull(kubernetesContainerService.getPods(kubernetesConfig, labels));
  }

  public int getControllerCountByLabels(KubernetesConfig kubernetesConfig, Map<String, String> labels) {
    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(kubernetesConfig, labels);

    return controllers.size();
  }

  public List<ContainerInfo> getContainerInfosWhenReadyByLabels(KubernetesConfig kubernetesConfig,
      ExecutionLogCallback executionLogCallback, Map<String, String> labels, List<Pod> existingPods) {
    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(kubernetesConfig, labels);

    executionLogCallback.saveExecutionLog(format("Deployed Controllers [%s]:", controllers.size()));
    controllers.forEach(controller
        -> executionLogCallback.saveExecutionLog(format("Kind:%s, Name:%s (desired: %s)", controller.getKind(),
            controller.getMetadata().getName(), kubernetesContainerService.getControllerPodCount(controller))));

    return fetchContainersUsingControllersWhenReady(kubernetesConfig, executionLogCallback, controllers, existingPods);
  }

  public boolean useK8sSteadyStateCheck(boolean isK8sSteadyStateCheckEnabled,
      ContainerServiceParams containerServiceParams, ExecutionLogCallback executionLogCallback) {
    if (!isK8sSteadyStateCheckEnabled) {
      return false;
    }

    KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
    VersionInfo versionInfo = kubernetesContainerService.getVersion(kubernetesConfig);
    executionLogCallback.saveExecutionLog(
        format("Kubernetess version [%s.%s]", versionInfo.getMajor(), versionInfo.getMinor()));
    int versionMajorMin = Integer.parseInt(escapeNonDigitsAndTruncate(versionInfo.getMajor() + versionInfo.getMinor()));

    return KUBERNETESS_116_VERSION <= versionMajorMin;
  }

  private String escapeNonDigitsAndTruncate(String value) {
    String digits = value.replaceAll(NON_DIGITS_REGEX, EMPTY);
    return digits.length() > VERSION_LENGTH ? digits.substring(0, VERSION_LENGTH) : digits;
  }

  private List<ContainerInfo> fetchContainersUsingControllersWhenReady(KubernetesConfig kubernetesConfig,
      ExecutionLogCallback executionLogCallback, List<? extends HasMetadata> controllers, List<Pod> existingPods) {
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

  public List<ContainerInfo> getContainerInfosWhenReadyByLabel(String labelName, String labelValue,
      KubernetesConfig kubernetesConfig, LogCallback executionLogCallback, List<Pod> existingPods) {
    return getContainerInfosWhenReadyByLabels(kubernetesConfig, (ExecutionLogCallback) executionLogCallback,
        ImmutableMap.of(labelName, labelValue), existingPods);
  }
}
