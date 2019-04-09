package software.wings.delegatetasks;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.CHART_MUSEUM_SERVER_URL;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfigValidationResponse;
import software.wings.beans.settings.helm.HelmRepoConfigValidationTaskParams;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.helpers.ext.chartmuseum.ChartMuseumClient;
import software.wings.helpers.ext.chartmuseum.ChartMuseumServerConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HelmRepoConfigValidationTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(HelmRepoConfigValidationTask.class);

  @Inject private EncryptionService encryptionService;
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  @Inject private ChartMuseumClient chartMuseumClient;

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

      validateHelmRepoConfig(taskParams);

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

  private void validateHelmRepoConfig(HelmRepoConfigValidationTaskParams taskParams) throws Exception {
    HelmRepoConfig helmRepoConfig = taskParams.getHelmRepoConfig();
    List<EncryptedDataDetail> encryptedDataDetails = taskParams.getEncryptedDataDetails();
    encryptionService.decrypt(helmRepoConfig, encryptedDataDetails);

    if (helmRepoConfig instanceof HttpHelmRepoConfig) {
      validateHttpHelmRepoConfig(helmRepoConfig);
      return;
    } else if (helmRepoConfig instanceof AmazonS3HelmRepoConfig) {
      validateAmazonS3HelmRepoConfig(helmRepoConfig, taskParams);
      return;
    }

    throw new WingsException("Unhandled type of helm repo config. Type : " + helmRepoConfig.getSettingType());
  }

  private void validateHttpHelmRepoConfig(HelmRepoConfig helmRepoConfig) throws Exception {
    HttpHelmRepoConfig httpHelmRepoConfig = (HttpHelmRepoConfig) helmRepoConfig;

    String repoAddCommand = getHttpRepoAddCommand(httpHelmRepoConfig);
    executeCommand(repoAddCommand);
  }

  private void executeCommand(String command) throws Exception {
    ProcessExecutor processExecutor =
        new ProcessExecutor().timeout(1, TimeUnit.MINUTES).commandSplit(command).readOutput(true);

    ProcessResult processResult = processExecutor.execute();
    if (processResult.getExitValue() != 0) {
      throw new WingsException("Failed to validate helm repo config. " + processResult.getOutput().getUTF8());
    }
  }

  private String getHttpRepoAddCommand(HttpHelmRepoConfig httpHelmRepoConfig) {
    StringBuilder builder = new StringBuilder(128);

    builder.append(encloseWithQuotesIfNeeded(k8sGlobalConfigService.getHelmPath()))
        .append(" repo add ")
        .append(httpHelmRepoConfig.getRepoName())
        .append(' ')
        .append(httpHelmRepoConfig.getChartRepoUrl());

    if (isNotBlank(httpHelmRepoConfig.getUsername())) {
      builder.append(" --username ").append(httpHelmRepoConfig.getUsername());

      if (httpHelmRepoConfig.getPassword() != null) {
        builder.append(" --password ").append(httpHelmRepoConfig.getPassword());
      }
    }

    return builder.toString();
  }

  private void validateAmazonS3HelmRepoConfig(
      HelmRepoConfig helmRepoConfig, HelmRepoConfigValidationTaskParams taskParams) throws Exception {
    AmazonS3HelmRepoConfig amazonS3HelmRepoConfig = (AmazonS3HelmRepoConfig) helmRepoConfig;

    AwsConfig awsConfig = (AwsConfig) taskParams.getConnectorConfig();
    List<EncryptedDataDetail> connectorEncryptedDataDetails = taskParams.getConnectorEncryptedDataDetails();
    encryptionService.decrypt(awsConfig, connectorEncryptedDataDetails);

    ChartMuseumServerConfig chartMuseumServerConfig = null;
    try {
      chartMuseumServerConfig =
          chartMuseumClient.startChartMuseumServer(amazonS3HelmRepoConfig, taskParams.getConnectorConfig());

      String repoAddCommand = getAmazonS3RepoAddCommand(amazonS3HelmRepoConfig, chartMuseumServerConfig.getPort());
      executeCommand(repoAddCommand);

    } finally {
      if (chartMuseumServerConfig != null) {
        chartMuseumClient.stopChartMuseumServer(chartMuseumServerConfig.getStartedProcess());
      }
    }
  }

  private String getAmazonS3RepoAddCommand(AmazonS3HelmRepoConfig amazonS3HelmRepoConfig, int port) {
    String repoUrl = CHART_MUSEUM_SERVER_URL.replace("${PORT}", Integer.toString(port));
    StringBuilder builder = new StringBuilder(128);

    builder.append(encloseWithQuotesIfNeeded(k8sGlobalConfigService.getHelmPath()))
        .append(" repo add ")
        .append(amazonS3HelmRepoConfig.getRepoName())
        .append(' ')
        .append(repoUrl);

    return builder.toString();
  }
}
