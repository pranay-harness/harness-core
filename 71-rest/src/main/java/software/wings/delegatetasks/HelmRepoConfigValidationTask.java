package software.wings.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.govern.Switch.unhandled;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static software.wings.common.StateConstants.DEFAULT_STEADY_STATE_TIMEOUT;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.GcpConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfigValidationResponse;
import software.wings.beans.settings.helm.HelmRepoConfigValidationTaskParams;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.service.intfc.security.EncryptionService;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@OwnedBy(CDC)
@Slf4j
public class HelmRepoConfigValidationTask extends AbstractDelegateRunnableTask {
  private static final String WORKING_DIR_BASE = "./repository/helm-validation/";
  private static final HelmVersion defaultHelmVersion = HelmVersion.V3;
  // We do not want to generate index when validating a helm repo, so a dummy path serves the purpose by checking if the
  // s3/gcs bucket is accessible or not
  private static final String DUMMY_BASE_PATH = generateUuid();
  private static final long DEFAULT_TIMEOUT_IN_MILLIS = Duration.ofMinutes(DEFAULT_STEADY_STATE_TIMEOUT).toMillis();

  @Inject private EncryptionService encryptionService;
  @Inject private HelmTaskHelper helmTaskHelper;

  public HelmRepoConfigValidationTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
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

    helmTaskHelper.initHelm(workingDirectory, defaultHelmVersion, DEFAULT_TIMEOUT_IN_MILLIS);
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

    helmTaskHelper.removeRepo(repoName, workingDirectory, defaultHelmVersion, DEFAULT_TIMEOUT_IN_MILLIS);
    helmTaskHelper.cleanup(workingDirectory);
  }

  private void tryAddingGCSHelmRepo(HelmRepoConfig helmRepoConfig, String repoName,
      HelmRepoConfigValidationTaskParams taskParams, String workingDirectory) throws Exception {
    GcpConfig gcpConfig = (GcpConfig) taskParams.getConnectorConfig();
    List<EncryptedDataDetail> connectorEncryptedDataDetails = taskParams.getConnectorEncryptedDataDetails();
    encryptionService.decrypt(gcpConfig, connectorEncryptedDataDetails);

    helmTaskHelper.addHelmRepo(helmRepoConfig, gcpConfig, repoName, taskParams.getRepoDisplayName(), workingDirectory,
        DUMMY_BASE_PATH, defaultHelmVersion);
  }

  private void tryAddingHttpHelmRepo(HelmRepoConfig helmRepoConfig, String repoName, String repoDisplayName,
      String workingDirectory) throws Exception {
    HttpHelmRepoConfig httpHelmRepoConfig = (HttpHelmRepoConfig) helmRepoConfig;

    helmTaskHelper.addRepo(repoName, repoDisplayName, httpHelmRepoConfig.getChartRepoUrl(),
        httpHelmRepoConfig.getUsername(), httpHelmRepoConfig.getPassword(), workingDirectory, defaultHelmVersion,
        DEFAULT_TIMEOUT_IN_MILLIS);
  }

  private void tryAddingAmazonS3HelmRepo(HelmRepoConfig helmRepoConfig, String repoName,
      HelmRepoConfigValidationTaskParams taskParams, String workingDirectory) throws Exception {
    AwsConfig awsConfig = (AwsConfig) taskParams.getConnectorConfig();
    List<EncryptedDataDetail> connectorEncryptedDataDetails = taskParams.getConnectorEncryptedDataDetails();
    encryptionService.decrypt(awsConfig, connectorEncryptedDataDetails);

    helmTaskHelper.addHelmRepo(helmRepoConfig, awsConfig, repoName, taskParams.getRepoDisplayName(), workingDirectory,
        DUMMY_BASE_PATH, defaultHelmVersion);
  }
}
