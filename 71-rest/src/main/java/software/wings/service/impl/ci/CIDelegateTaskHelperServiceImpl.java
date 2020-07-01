package software.wings.service.impl.ci;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.Cd1SetupFields;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.ci.CIK8BuildTaskParams;
import software.wings.beans.ci.CIK8CleanupTaskParams;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.K8ExecuteCommandTaskParams;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private static final String REPLACE_USERNAME_HERE = "REPLACE_USERNAME_HERE";
  private static final String REPLACE_PASSWORD_HERE = "REPLACE_PASSWORD_HERE";

  @Override
  public K8sTaskExecutionResponse setBuildEnv(String k8ConnectorName, String gitConnectorName, String branchName,
      CIK8PodParams<CIK8ContainerParams> podParams) {
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
    KubernetesConfig kubernetesConfig = null;
    if (googleCloud != null) {
      kubernetesClusterConfig = (KubernetesClusterConfig) googleCloud.getValue();
      kubernetesConfig = kubernetesClusterConfig.createKubernetesConfig(null);
      encryptedDataDetails = secretManager.getEncryptionDetails(kubernetesClusterConfig);
    }

    List<CIK8ContainerParams> cik8ContainerParamsList = podParams.getContainerParamsList();
    Map<String, EncryptedDataDetail> secrets = new HashMap<>();
    cik8ContainerParamsList.forEach(cik8ContainerParams -> {
      if (isEmpty(cik8ContainerParams.getEncryptedSecrets())
          && cik8ContainerParams.getContainerType() == (CIContainerType.STEP_EXECUTOR)) {
        String userName = secretManager.getSecretByName(ACCOUNT_ID, REPLACE_USERNAME_HERE).getUuid();

        EncryptedDataDetail userEncryptedDataDetail =
            secretManager.encryptedDataDetails(ACCOUNT_ID, REPLACE_USERNAME_HERE, userName)
                .orElseThrow(() -> new IllegalArgumentException("REPLACE_USERNAME_HERE does not exist"));

        secrets.put(REPLACE_USERNAME_HERE, userEncryptedDataDetail);

        String password = secretManager.getSecretByName(ACCOUNT_ID, REPLACE_PASSWORD_HERE).getUuid();

        EncryptedDataDetail passEncryptedDataDetail =
            secretManager.encryptedDataDetails(ACCOUNT_ID, REPLACE_PASSWORD_HERE, password)
                .orElseThrow(() -> new IllegalArgumentException("REPLACE_PASSWORD_HERE does not exist"));

        secrets.put(REPLACE_PASSWORD_HERE, passEncryptedDataDetail);

        cik8ContainerParams.setEncryptedSecrets(secrets);
      }
    });

    CIK8PodParams<CIK8ContainerParams> podParamsWithGitDetails =
        CIK8PodParams.<CIK8ContainerParams>builder()
            .name(podParams.getName())
            .namespace(podParams.getNamespace())
            .stepExecVolumeName(STEP_EXEC)
            .stepExecWorkingDir(podParams.getStepExecWorkingDir())
            .gitFetchFilesConfig(gitFetchFilesConfig)
            .containerParamsList(cik8ContainerParamsList)
            .initContainerParamsList(podParams.getInitContainerParamsList())
            .build();

    try {
      ResponseData responseData = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(ACCOUNT_ID)
              .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, GLOBAL_APP_ID)
              .data(TaskData.builder()
                        .taskType(TaskType.CI_BUILD.name())
                        .async(false)
                        .parameters(new Object[] {CIK8BuildTaskParams.builder()
                                                      .gitFetchFilesConfig(gitFetchFilesConfig)
                                                      .encryptionDetails(encryptedDataDetails)
                                                      .kubernetesConfig(kubernetesConfig)
                                                      .cik8PodParams(podParamsWithGitDetails)
                                                      .build()})
                        .timeout(TimeUnit.SECONDS.toMillis(600))
                        .build())
              .build());
      logger.info(responseData.toString());

      if (responseData instanceof K8sTaskExecutionResponse) {
        return (K8sTaskExecutionResponse) responseData;
      }
    } catch (Exception e) {
      logger.error("Failed to execute delegate task to setup build", e);
    }

    return null;
  }

  @Override
  public K8sTaskExecutionResponse executeBuildCommand(String k8ConnectorName, K8ExecCommandParams params) {
    SettingAttribute googleCloud = settingsService.getSettingAttributeByName(ACCOUNT_ID, k8ConnectorName);
    KubernetesClusterConfig kubernetesClusterConfig = null;
    List<EncryptedDataDetail> encryptedDataDetails = null;
    KubernetesConfig kubernetesConfig = null;
    if (googleCloud != null) {
      kubernetesClusterConfig = (KubernetesClusterConfig) googleCloud.getValue();
      kubernetesConfig = kubernetesClusterConfig.createKubernetesConfig(null);
      encryptedDataDetails = secretManager.getEncryptionDetails(kubernetesClusterConfig);
    }

    try {
      ResponseData responseData = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(ACCOUNT_ID)
              .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, GLOBAL_APP_ID)
              .data(TaskData.builder()
                        .taskType(TaskType.EXECUTE_COMMAND.name())
                        .async(false)
                        .parameters(new Object[] {K8ExecuteCommandTaskParams.builder()
                                                      .encryptionDetails(encryptedDataDetails)
                                                      .kubernetesConfig(kubernetesConfig)
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
    KubernetesConfig kubernetesConfig = null;
    if (googleCloud != null) {
      kubernetesClusterConfig = (KubernetesClusterConfig) googleCloud.getValue();
      kubernetesConfig = kubernetesClusterConfig.createKubernetesConfig(null);
      encryptedDataDetails = secretManager.getEncryptionDetails(kubernetesClusterConfig);
    }

    try {
      ResponseData responseData = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(ACCOUNT_ID)
              .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, GLOBAL_APP_ID)
              .data(TaskData.builder()
                        .taskType(TaskType.CI_CLEANUP.name())
                        .async(false)
                        .parameters(new Object[] {CIK8CleanupTaskParams.builder()
                                                      .encryptionDetails(encryptedDataDetails)
                                                      .kubernetesConfig(kubernetesConfig)
                                                      .podName(podName)
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
}
