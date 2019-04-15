package software.wings.delegatetasks;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfigValidationResponse;
import software.wings.beans.settings.helm.HelmRepoConfigValidationTaskParams;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class HelmRepoConfigValidationTask extends AbstractDelegateRunnableTask {
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
      logger.info(format("Running HelmRepoConfigValidationTask for account %s app %s", taskParams.getAccountId(),
          taskParams.getAppId()));

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

    String repoName = convertBase64UuidToCanonicalForm(generateUuid());
    try {
      switch (helmRepoConfig.getSettingType()) {
        case HTTP_HELM_REPO:
          tryAddingHttpHelmRepo(helmRepoConfig, repoName, taskParams.getRepoDisplayName());
          break;

        case AMAZON_S3_HELM_REPO:
          tryAddingAmazonS3HelmRepo(helmRepoConfig, repoName, taskParams);
          break;

        default:
          unhandled(helmRepoConfig.getSettingType());
          throw new WingsException("Unhandled type of helm repo config. Type : " + helmRepoConfig.getSettingType());
      }
    } finally {
      removeRepo(repoName);
    }
  }

  private void tryAddingHttpHelmRepo(HelmRepoConfig helmRepoConfig, String repoName, String repoDisplayName)
      throws Exception {
    helmTaskHelper.addHttpRepo(helmRepoConfig, repoName, repoDisplayName, null);
  }

  private void tryAddingAmazonS3HelmRepo(
      HelmRepoConfig helmRepoConfig, String repoName, HelmRepoConfigValidationTaskParams taskParams) throws Exception {
    AwsConfig awsConfig = (AwsConfig) taskParams.getConnectorConfig();
    List<EncryptedDataDetail> connectorEncryptedDataDetails = taskParams.getConnectorEncryptedDataDetails();
    encryptionService.decrypt(awsConfig, connectorEncryptedDataDetails);

    helmTaskHelper.addAmazonS3HelmRepo(helmRepoConfig, awsConfig, repoName, taskParams.getRepoDisplayName());
  }

  private void removeRepo(String repoName) {
    try {
      helmTaskHelper.removeRepo(repoName);
    } catch (Exception ex) {
      logger.warn(ExceptionUtils.getMessage(ex));
    }
  }
}
