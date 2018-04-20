package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.helpers.ext.helm.HelmConstants.KUBE_CONFIG_TEMPLATE;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AzureConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.waitnotify.NotifyResponseData;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by anubhaw on 3/22/18.
 */
public class HelmCommandTask extends AbstractDelegateRunnableTask {
  @Inject private GkeClusterService gkeClusterService;
  @Inject private DelegateLogService delegateLogService;
  @Inject private HelmDeployService helmDeployService;
  @Inject private AzureHelperService azureHelperService;
  @Inject private EncryptionService encryptionService;

  public static final LoadingCache<String, Object> lockObjects =
      CacheBuilder.newBuilder()
          .expireAfterAccess(30, TimeUnit.MINUTES)
          .build(CacheLoader.from(Object::new)); // TODO:: move it to helper class

  private static final Logger logger = LoggerFactory.getLogger(HelmCommandTask.class);
  private static final String KUBE_CONFIG_DIR = "./repository/helm/.kube/";

  public HelmCommandTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public HelmCommandExecutionResponse run(Object[] parameters) {
    HelmCommandRequest helmCommandRequest = (HelmCommandRequest) parameters[0];
    HelmCommandResponse commandResponse;

    ExecutionLogCallback executionLogCallback =
        new ExecutionLogCallback(delegateLogService, helmCommandRequest.getAccountId(), helmCommandRequest.getAppId(),
            helmCommandRequest.getActivityId(), helmCommandRequest.getCommandName());

    try {
      String configLocation = createAndGetKubeConfigLocation(helmCommandRequest.getContainerServiceParams());
      helmCommandRequest.setKubeConfigLocation(configLocation);
      HelmCommandResponse helmCommandResponse = helmDeployService.ensureHelmCliAndTillerInstalled(helmCommandRequest);

      if (isAsync()) {
        executionLogCallback.saveExecutionLog(helmCommandResponse.getOutput());
        executionLogCallback.saveExecutionLog(
            "Started executing helm command", LogLevel.INFO, CommandExecutionStatus.RUNNING);
      }
      switch (helmCommandRequest.getHelmCommandType()) {
        case INSTALL:
          commandResponse =
              helmDeployService.deploy((HelmInstallCommandRequest) helmCommandRequest, executionLogCallback);
          break;
        case ROLLBACK:
          commandResponse =
              helmDeployService.rollback((HelmRollbackCommandRequest) helmCommandRequest, executionLogCallback);
          break;
        case RELEASE_HISTORY:
          commandResponse = helmDeployService.releaseHistory((HelmReleaseHistoryCommandRequest) helmCommandRequest);
          break;
        default:
          throw new HarnessException("Operation not supported");
      }
    } catch (Exception ex) {
      logger.error("Exception in processing helm task [{}]", helmCommandRequest, ex);
      return HelmCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ex.getMessage())
          .build();
    }

    if (isAsync()) {
      executionLogCallback.saveExecutionLog(
          "Command finished with status " + commandResponse.getCommandExecutionStatus(), LogLevel.INFO,
          commandResponse.getCommandExecutionStatus());
    }

    return HelmCommandExecutionResponse.builder()
        .commandExecutionStatus(commandResponse.getCommandExecutionStatus())
        .helmCommandResponse(commandResponse)
        .errorMessage(commandResponse.getOutput())
        .build();
  }

  private String createAndGetKubeConfigLocation(ContainerServiceParams containerServiceParam) {
    try {
      String clusterName = containerServiceParam.getClusterName();

      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParam);
      encryptionService.decrypt(kubernetesConfig, containerServiceParam.getEncryptionDetails());
      String configFileContent = getConfigFileContent(kubernetesConfig, clusterName);
      String md5Hash = DigestUtils.md5Hex(configFileContent);

      synchronized (lockObjects.get(md5Hash)) {
        String configFilePath = KUBE_CONFIG_DIR + md5Hash;
        File file = new File(configFilePath);
        if (!file.exists()) {
          file.getParentFile().mkdirs();
          try (FileWriter writer = new FileWriter(file)) {
            writer.write(configFileContent);
          }
        }
        return file.getAbsolutePath();
      }
    } catch (Exception e) {
      logger.error("Error occurred in creating config file", e);
    }
    return null;
  }

  private KubernetesConfig getKubernetesConfig(ContainerServiceParams containerServiceParam) {
    SettingAttribute settingAttribute = containerServiceParam.getSettingAttribute();
    List<EncryptedDataDetail> encryptedDataDetails = containerServiceParam.getEncryptionDetails();
    String clusterName = containerServiceParam.getClusterName();
    String namespace = containerServiceParam.getNamespace();

    KubernetesConfig kubernetesConfig;
    if (settingAttribute.getValue() instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) settingAttribute.getValue();
    } else if (settingAttribute.getValue() instanceof KubernetesClusterConfig) {
      kubernetesConfig = ((KubernetesClusterConfig) settingAttribute.getValue()).createKubernetesConfig(namespace);
    } else if (settingAttribute.getValue() instanceof GcpConfig) {
      kubernetesConfig = gkeClusterService.getCluster(settingAttribute, encryptedDataDetails, clusterName, namespace);
      kubernetesConfig.setDecrypted(true);
    } else if (settingAttribute.getValue() instanceof AzureConfig) {
      AzureConfig azureConfig = (AzureConfig) settingAttribute.getValue();
      kubernetesConfig = azureHelperService.getKubernetesClusterConfig(azureConfig, encryptedDataDetails,
          containerServiceParam.getSubscriptionId(), containerServiceParam.getResourceGroup(), clusterName, namespace);
      kubernetesConfig.setDecrypted(true);
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam(
              "args", "Unknown kubernetes cloud provider setting value: " + settingAttribute.getValue().getType());
    }
    return kubernetesConfig;
  }

  private String getConfigFileContent(KubernetesConfig config, String clusterName) {
    String clientCertData =
        isNotEmpty(config.getClientCert()) ? "client-certificate-data: " + new String(config.getClientCert()) : "";
    String clientKeyData =
        isNotEmpty(config.getClientKey()) ? "client-key-data: " + new String(config.getClientKey()) : "";
    String password = isNotEmpty(config.getPassword()) ? "password: " + new String(config.getPassword()) : "";

    return KUBE_CONFIG_TEMPLATE.replace("${MASTER_URL}", config.getMasterUrl())
        .replace("${CLUSTER_NAME}", clusterName)
        .replace("${USER_NAME}", config.getUsername())
        .replace("${CLIENT_CERT_DATA}", clientCertData)
        .replace("${CLIENT_KEY_DATA}", clientKeyData)
        .replace("${PASSWORD}", password);
  }
}
