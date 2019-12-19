package software.wings.delegatetasks;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.govern.Switch.unhandled;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.AwsConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfigValidationResponse;
import software.wings.beans.settings.helm.HelmRepoConfigValidationTaskParams;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.service.intfc.security.EncryptionService;

import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class HelmRepoConfigValidationTask extends AbstractDelegateRunnableTask {
  private static final String WORKING_DIR_BASE = "./repository/helm-validation/";

  @Inject private EncryptionService encryptionService;
  @Inject private HelmTaskHelper helmTaskHelper;

  public HelmRepoConfigValidationTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public HelmRepoConfigValidationResponse run(TaskParameters parameters) {
    HelmRepoConfigValidationTaskParams taskParams = (HelmRepoConfigValidationTaskParams) parameters;

    try {
      logger.info("Running HelmRepoConfigValidationTask for account {} app {}", taskParams.getAccountId(),
          taskParams.getAppId());

      tryAddingHelmRepo(taskParams);

      return HelmRepoConfigValidationResponse.builder().commandExecutionStatus(SUCCESS).build();
    } catch (Exception e) {
      logger.warn("HelmRepoConfigValidationTask execution failed with exception " + e);
      return HelmRepoConfigValidationResponse.builder()
          .commandExecutionStatus(FAILURE)
          .errorMessage(ExceptionUtils.getMessage(e))
          .build();
    }
  }

  @Override
  public HelmRepoConfigValidationResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  private void tryAddingHelmRepo(HelmRepoConfigValidationTaskParams taskParams) throws Exception {
    HelmRepoConfig helmRepoConfig = taskParams.getHelmRepoConfig();
    List<EncryptedDataDetail> encryptedDataDetails = taskParams.getEncryptedDataDetails();
    encryptionService.decrypt(helmRepoConfig, encryptedDataDetails);

    String workingDirectory = helmTaskHelper.createNewDirectoryAtPath(Paths.get(WORKING_DIR_BASE).toString());
    helmTaskHelper.initHelm(workingDirectory);
    String repoName = convertBase64UuidToCanonicalForm(generateUuid());

    switch (helmRepoConfig.getSettingType()) {
      case HTTP_HELM_REPO:
        tryAddingHttpHelmRepo(helmRepoConfig, repoName, taskParams.getRepoDisplayName(), workingDirectory);
        break;

      case AMAZON_S3_HELM_REPO:
        tryAddingAmazonS3HelmRepo(helmRepoConfig, repoName, taskParams, workingDirectory);
        break;

      case GCS_HELM_REPO:
        tryAddingGCSHelmRepo(helmRepoConfig, repoName, taskParams, workingDirectory);
        break;

      default:
        unhandled(helmRepoConfig.getSettingType());
        throw new WingsException("Unhandled type of helm repo config. Type : " + helmRepoConfig.getSettingType());
    }

    helmTaskHelper.removeRepo(repoName, workingDirectory);
    helmTaskHelper.cleanup(workingDirectory);
  }

  private void tryAddingGCSHelmRepo(HelmRepoConfig helmRepoConfig, String repoName,
      HelmRepoConfigValidationTaskParams taskParams, String workingDirectory) throws Exception {
    GcpConfig gcpConfig = (GcpConfig) taskParams.getConnectorConfig();
    List<EncryptedDataDetail> connectorEncryptedDataDetails = taskParams.getConnectorEncryptedDataDetails();
    encryptionService.decrypt(gcpConfig, connectorEncryptedDataDetails);

    helmTaskHelper.addHelmRepo(
        helmRepoConfig, gcpConfig, repoName, taskParams.getRepoDisplayName(), workingDirectory, "");
  }

  private void tryAddingHttpHelmRepo(HelmRepoConfig helmRepoConfig, String repoName, String repoDisplayName,
      String workingDirectory) throws Exception {
    HttpHelmRepoConfig httpHelmRepoConfig = (HttpHelmRepoConfig) helmRepoConfig;

    helmTaskHelper.addRepo(repoName, repoDisplayName, httpHelmRepoConfig.getChartRepoUrl(),
        httpHelmRepoConfig.getUsername(), httpHelmRepoConfig.getPassword(), workingDirectory);
  }

  private void tryAddingAmazonS3HelmRepo(HelmRepoConfig helmRepoConfig, String repoName,
      HelmRepoConfigValidationTaskParams taskParams, String workingDirectory) throws Exception {
    AwsConfig awsConfig = (AwsConfig) taskParams.getConnectorConfig();
    List<EncryptedDataDetail> connectorEncryptedDataDetails = taskParams.getConnectorEncryptedDataDetails();
    encryptionService.decrypt(awsConfig, connectorEncryptedDataDetails);

    helmTaskHelper.addHelmRepo(
        helmRepoConfig, awsConfig, repoName, taskParams.getRepoDisplayName(), workingDirectory, "");
  }
}
