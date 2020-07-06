package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.inject.Inject;

import io.harness.ccm.config.CCMConfig;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.logging.AutoLogContext;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.VersionApi;
import io.kubernetes.client.openapi.models.VersionInfo;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.apiclient.ApiClientFactoryImpl;
import software.wings.delegatetasks.k8s.logging.K8sVersionLogContext;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sVersionResponse;

@NoArgsConstructor
@Slf4j
public class K8sVersionTaskHandler extends K8sTaskHandler {
  @Inject private transient ApiClientFactoryImpl apiClientFactory;

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    return executeTaskInternal(k8sTaskParameters);
  }

  public K8sTaskExecutionResponse executeTaskInternal(K8sTaskParameters k8sTaskParameters) throws ApiException {
    KubernetesClusterConfig kubernetesClusterConfig = getKubernetesConfig(k8sTaskParameters);
    CCMConfig ccmConfig = kubernetesClusterConfig.getCcmConfig();

    VersionInfo versionInfo = getK8sVersionInfo(k8sTaskParameters.getK8sClusterConfig());
    K8sVersionResponse k8sVersionResponse = k8sVersionResponseBuilder(versionInfo);

    try (AutoLogContext ignore = new K8sVersionLogContext(kubernetesClusterConfig.getType(),
             k8sVersionResponse.getServerMajorVersion() + ":" + k8sVersionResponse.getServerMinorVersion(),
             ccmConfig.isCloudCostEnabled(), OVERRIDE_ERROR);) {
      logger.info("[cloudProvider={}, version={}, ccEnabled={}]", kubernetesClusterConfig.getType(),
          k8sVersionResponse.getServerMajorVersion() + ":" + k8sVersionResponse.getServerMinorVersion(),
          ccmConfig.isCloudCostEnabled());
    }

    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sTaskResponse(k8sVersionResponse)
        .build();
  }

  public VersionInfo getK8sVersionInfo(K8sClusterConfig k8sClusterConfig) throws ApiException {
    ApiClient client = apiClientFactory.getClient(k8sClusterConfig);
    VersionApi apiInstance = new VersionApi(client);
    return apiInstance.getCode();
  }

  public static K8sVersionResponse k8sVersionResponseBuilder(VersionInfo versionInfo) {
    return K8sVersionResponse.builder()
        .serverMajorVersion(versionInfo.getMajor())
        .serverMinorVersion(versionInfo.getMinor())
        .gitVersion(versionInfo.getGitVersion())
        .platform(versionInfo.getPlatform())
        .gitCommit(versionInfo.getGitCommit())
        .build();
  }

  public static KubernetesClusterConfig getKubernetesConfig(K8sTaskParameters k8sTaskParameters) {
    return (KubernetesClusterConfig) k8sTaskParameters.getK8sClusterConfig().getCloudProvider();
  }
}
