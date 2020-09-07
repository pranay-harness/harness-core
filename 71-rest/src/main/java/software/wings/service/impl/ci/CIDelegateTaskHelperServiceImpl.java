package software.wings.service.impl.ci;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;
import static software.wings.settings.SettingVariableTypes.CONFIG_FILE;
import static software.wings.settings.SettingVariableTypes.SECRET_TEXT;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.Cd1SetupFields;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.DockerConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.ci.CIK8BuildTaskParams;
import software.wings.beans.ci.CIK8CleanupTaskParams;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.K8ExecuteCommandTaskParams;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.EncryptedVariableWithType;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Send CI pipeline tasks to delegate, It sends following tasks taking input from CI
 *     - Build Pod
 *     - Execute command on Pod
 *     - Delete Pod
 *
 *     Currently SecretManager and SettingsService can not be injected due to subsequent dependencies
 *     We have to remove this code when delegate microservice will be ready
 */

@Slf4j
public class CIDelegateTaskHelperServiceImpl implements CIDelegateTaskHelperService {
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private DelegateService delegateService;
  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";

  @Override
  public K8sTaskExecutionResponse setBuildEnv(String k8ConnectorName, String gitConnectorName, String branchName,
      CIK8PodParams<CIK8ContainerParams> podParams) {
    logger.info("Received pod creation call for pod {} on manager", podParams.getName());
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(ACCOUNT_ID, gitConnectorName);
    GitFetchFilesConfig gitFetchFilesConfig = null;
    if (cloudProvider != null) {
      GitConfig gitConfig = (GitConfig) cloudProvider.getValue();
      List<EncryptedDataDetail> gitEncryptedDataDetails = secretManager.getEncryptionDetails(gitConfig);
      gitFetchFilesConfig = GitFetchFilesConfig.builder()
                                .encryptedDataDetails(gitEncryptedDataDetails)
                                .gitFileConfig(GitFileConfig.builder().branch(branchName).build())
                                .gitConfig(gitConfig)
                                .build();
    }
    SettingAttribute googleCloud = settingsService.getSettingAttributeByName(ACCOUNT_ID, k8ConnectorName);
    KubernetesClusterConfig kubernetesClusterConfig = null;
    List<EncryptedDataDetail> encryptedDataDetails = null;
    if (googleCloud != null) {
      kubernetesClusterConfig = (KubernetesClusterConfig) googleCloud.getValue();
      encryptedDataDetails = secretManager.getEncryptionDetails(kubernetesClusterConfig);
    }

    addImageRegistryConnectorSecrets(podParams);

    addSecrets(podParams);

    CIK8PodParams<CIK8ContainerParams> podParamsWithGitDetails =
        CIK8PodParams.<CIK8ContainerParams>builder()
            .name(podParams.getName())
            .namespace(podParams.getNamespace())
            .pvcParamList(podParams.getPvcParamList())
            .stepExecVolumeName(STEP_EXEC)
            .stepExecWorkingDir(podParams.getStepExecWorkingDir())
            .gitFetchFilesConfig(gitFetchFilesConfig)
            .containerParamsList(podParams.getContainerParamsList())
            .initContainerParamsList(podParams.getInitContainerParamsList())
            .build();

    try {
      logger.info("Sending delegate task for pod creation from manager podname: {}", podParams.getName());
      DelegateResponseData responseData = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(ACCOUNT_ID)
              .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, GLOBAL_APP_ID)
              .data(TaskData.builder()
                        .taskType(TaskType.CI_BUILD.name())
                        .async(false)
                        .parameters(new Object[] {CIK8BuildTaskParams.builder()
                                                      .gitFetchFilesConfig(gitFetchFilesConfig)
                                                      .encryptionDetails(encryptedDataDetails)
                                                      .kubernetesClusterConfig(kubernetesClusterConfig)
                                                      .cik8PodParams(podParamsWithGitDetails)
                                                      .build()})
                        .timeout(TimeUnit.SECONDS.toMillis(600))
                        .build())
              .build());
      logger.info("delegate response for pod creation for ", responseData.toString());

      if (responseData instanceof K8sTaskExecutionResponse) {
        return (K8sTaskExecutionResponse) responseData;
      }
    } catch (Exception e) {
      logger.error("Failed to execute delegate task to setup build", e);
    }

    return null;
  }

  private void addImageRegistryConnectorSecrets(CIK8PodParams<CIK8ContainerParams> podParams) {
    List<CIK8ContainerParams> containerParamsList = new ArrayList<>();
    Optional.ofNullable(podParams.getContainerParamsList()).ifPresent(containerParamsList::addAll);
    Optional.ofNullable(podParams.getInitContainerParamsList()).ifPresent(containerParamsList::addAll);

    containerParamsList.forEach(cik8ContainerParams -> {
      ImageDetailsWithConnector imageDetailsWithConnector = cik8ContainerParams.getImageDetailsWithConnector();
      String connectorName = imageDetailsWithConnector.getConnectorName();
      String registryUrl = imageDetailsWithConnector.getImageDetails().getRegistryUrl();

      if (connectorName != null) {
        SettingAttribute customerImageRegistry = settingsService.getSettingAttributeByName(ACCOUNT_ID, connectorName);
        SettingValue value = customerImageRegistry.getValue();
        switch (value.getSettingType()) {
          case DOCKER:
            DockerConfig dockerConfig = (DockerConfig) value;
            String connectorRegistryUrl = dockerConfig.getDockerRegistryUrl();
            if (registryUrl != null && !registryUrl.equals(connectorRegistryUrl)) {
              throw new IllegalArgumentException("Registry url doesnt match connector registry url");
            }
            List<EncryptedDataDetail> imageConnectorEncryptionDetails =
                secretManager.getEncryptionDetails(dockerConfig);
            imageDetailsWithConnector.getImageDetails().setRegistryUrl(connectorRegistryUrl);
            imageDetailsWithConnector.setEncryptableSetting(dockerConfig);
            imageDetailsWithConnector.setEncryptedDataDetails(imageConnectorEncryptionDetails);
            break;
          default:
            unhandled(value.getSettingType());
        }
      }
    });
  }

  @Override
  public K8sTaskExecutionResponse executeBuildCommand(String k8ConnectorName, K8ExecCommandParams params) {
    SettingAttribute googleCloud = settingsService.getSettingAttributeByName(ACCOUNT_ID, k8ConnectorName);
    KubernetesClusterConfig kubernetesClusterConfig = null;
    List<EncryptedDataDetail> encryptedDataDetails = null;
    if (googleCloud != null) {
      kubernetesClusterConfig = (KubernetesClusterConfig) googleCloud.getValue();
      encryptedDataDetails = secretManager.getEncryptionDetails(kubernetesClusterConfig);
    }

    try {
      DelegateResponseData responseData = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(ACCOUNT_ID)
              .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, GLOBAL_APP_ID)
              .data(TaskData.builder()
                        .taskType(TaskType.EXECUTE_COMMAND.name())
                        .async(false)
                        .parameters(new Object[] {K8ExecuteCommandTaskParams.builder()
                                                      .encryptionDetails(encryptedDataDetails)
                                                      .kubernetesClusterConfig(kubernetesClusterConfig)
                                                      .k8ExecCommandParams(params)
                                                      .build()})
                        .timeout(TimeUnit.SECONDS.toMillis(3600))
                        .build())
              .build());
      logger.info(responseData.toString());
      if (responseData instanceof K8sTaskExecutionResponse) {
        return (K8sTaskExecutionResponse) responseData;
      }
    } catch (Exception e) {
      logger.error("Failed to execute delegate task for build execution", e);
    }
    return null;
  }

  @Override
  public K8sTaskExecutionResponse cleanupEnv(String k8ConnectorName, String namespace, String podName) {
    SettingAttribute googleCloud = settingsService.getSettingAttributeByName(ACCOUNT_ID, k8ConnectorName);
    KubernetesClusterConfig kubernetesClusterConfig = null;
    List<EncryptedDataDetail> encryptedDataDetails = null;
    if (googleCloud != null) {
      kubernetesClusterConfig = (KubernetesClusterConfig) googleCloud.getValue();
      encryptedDataDetails = secretManager.getEncryptionDetails(kubernetesClusterConfig);
    }

    List<String> podNameList = new ArrayList<>();
    podNameList.add(podName);
    try {
      DelegateResponseData responseData = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(ACCOUNT_ID)
              .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, GLOBAL_APP_ID)
              .data(TaskData.builder()
                        .taskType(TaskType.CI_CLEANUP.name())
                        .async(false)
                        .parameters(new Object[] {CIK8CleanupTaskParams.builder()
                                                      .encryptionDetails(encryptedDataDetails)
                                                      .kubernetesClusterConfig(kubernetesClusterConfig)
                                                      .podNameList(podNameList)
                                                      .namespace(namespace)
                                                      .build()})
                        .timeout(TimeUnit.SECONDS.toMillis(60))
                        .build())
              .build());
      logger.info(responseData.toString());
      if (responseData instanceof K8sTaskExecutionResponse) {
        return (K8sTaskExecutionResponse) responseData;
      }
    } catch (Exception e) {
      logger.error("Failed to cleanup pod", e);
    }
    return null;
  }

  private void addSecrets(CIK8PodParams<CIK8ContainerParams> podParams) {
    List<CIK8ContainerParams> cik8ContainerParamsList = podParams.getContainerParamsList();

    cik8ContainerParamsList.forEach(cik8ContainerParams -> {
      if (isNotEmpty(cik8ContainerParams.getContainerSecrets().getEncryptedSecrets())) {
        Map<String, EncryptedVariableWithType> secrets = new HashMap<>();
        for (Map.Entry<String, EncryptedVariableWithType> entry :
            cik8ContainerParams.getContainerSecrets().getEncryptedSecrets().entrySet()) {
          getEncryptedSecretWithType(ACCOUNT_ID, entry.getKey())
              .ifPresent(encryptedSecretWithType -> secrets.put(entry.getKey(), encryptedSecretWithType));
        }
        cik8ContainerParams.getContainerSecrets().setEncryptedSecrets(secrets);
      }

      if (isNotEmpty(cik8ContainerParams.getContainerSecrets().getPublishArtifactEncryptedValues())) {
        setPublishImageEncryptedInfo(cik8ContainerParams);
      }
    });
  }

  private void setPublishImageEncryptedInfo(CIK8ContainerParams cik8ContainerParams) {
    Map<String, EncryptableSettingWithEncryptionDetails> publishArtifactEncryptedValues = new HashMap<>();
    for (Map.Entry<String, EncryptableSettingWithEncryptionDetails> entry :
        cik8ContainerParams.getContainerSecrets().getPublishArtifactEncryptedValues().entrySet()) {
      String connectorIdentifier = entry.getKey();

      SettingAttribute publishImageSettingsAttribute =
          settingsService.getSettingAttributeByName(ACCOUNT_ID, connectorIdentifier);

      if (publishImageSettingsAttribute != null && publishImageSettingsAttribute.getValue() != null) {
        List<EncryptedDataDetail> encryptionDetails =
            secretManager.getEncryptionDetails((EncryptableSetting) publishImageSettingsAttribute.getValue());
        EncryptableSettingWithEncryptionDetails encryptableSettingWithEncryptionDetails =
            EncryptableSettingWithEncryptionDetails.builder()
                .encryptableSetting((EncryptableSetting) publishImageSettingsAttribute.getValue())
                .encryptedDataDetails(encryptionDetails)
                .build();

        publishArtifactEncryptedValues.put(connectorIdentifier, encryptableSettingWithEncryptionDetails);
      }
    }
    cik8ContainerParams.getContainerSecrets().setPublishArtifactEncryptedValues(publishArtifactEncryptedValues);
  }

  private Optional<EncryptedVariableWithType> getEncryptedSecretWithType(String accountId, String secretName) {
    EncryptedData secretByName = secretManager.getSecretByName(accountId, secretName);
    if (secretByName != null) {
      Optional<EncryptedDataDetail> encryptedDataDetail =
          secretManager.encryptedDataDetails(accountId, secretName, secretByName.getUuid(), null);
      if (encryptedDataDetail.isPresent()) {
        if (secretByName.getType() == CONFIG_FILE) {
          return Optional.of(EncryptedVariableWithType.builder()
                                 .encryptedDataDetail(encryptedDataDetail.get())
                                 .type(EncryptedVariableWithType.Type.FILE)
                                 .build());
        }
        if (secretByName.getType() == SECRET_TEXT) {
          return Optional.of(EncryptedVariableWithType.builder()
                                 .encryptedDataDetail(encryptedDataDetail.get())
                                 .type(EncryptedVariableWithType.Type.TEXT)
                                 .build());
        }
      }
    } else {
      return Optional.empty();
    }
    return Optional.empty();
  }
}
