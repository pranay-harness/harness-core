package software.wings.delegatetasks.validation;

import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.network.Http.connectableHttpUrl;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.beans.TaskType;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfigValidationTaskParams;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmValuesFetchTaskParameters;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class HelmRepoConfigValidation extends AbstractDelegateValidateTask {
  private static final String UNHANDLED_CONFIG_MSG = "Unhandled type of helm repo config. Type : ";
  private static final String AWS_URL = "https://aws.amazon.com/";

  private static final String HELM_VERSION_COMMAND = "${HELM_PATH} version -c";

  private static final String CHART_MUSEUM_VERSION_COMMAND = "${CHART_MUSEUM_PATH} -v";

  @Inject private K8sGlobalConfigService k8sGlobalConfigService;

  public HelmRepoConfigValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    logger.info(format("Running validation for task %s", delegateTaskId));

    HelmRepoConfig helmRepoConfig = getHelmRepoConfig();

    return taskValidationResult(validateHelmRepoConfig(helmRepoConfig));
  }

  @Override
  public List<String> getCriteria() {
    HelmRepoConfig helmRepoConfig = getHelmRepoConfig();

    if (helmRepoConfig == null) {
      return singletonList(getCriteriaForEmptyHelmRepoConfigInValuesFetch());
    } else if (helmRepoConfig instanceof HttpHelmRepoConfig) {
      return singletonList("HTTP_HELM_REPO: " + ((HttpHelmRepoConfig) helmRepoConfig).getChartRepoUrl());
    } else if (helmRepoConfig instanceof AmazonS3HelmRepoConfig) {
      AmazonS3HelmRepoConfig amazonS3HelmRepoConfig = (AmazonS3HelmRepoConfig) helmRepoConfig;
      return singletonList("AMAZON_S3_HELM_REPO: " + amazonS3HelmRepoConfig.getBucketName() + ":"
          + amazonS3HelmRepoConfig.getFolderPath() + ":" + amazonS3HelmRepoConfig.getRegion());
    } else if (helmRepoConfig instanceof GCSHelmRepoConfig) {
      GCSHelmRepoConfig gcsHelmRepoConfig = (GCSHelmRepoConfig) helmRepoConfig;
      return singletonList(gcsHelmRepoConfig.getType() + ":" + gcsHelmRepoConfig.getBucketName() + ":"
          + gcsHelmRepoConfig.getFolderPath());
    }

    throw new WingsException(UNHANDLED_CONFIG_MSG + helmRepoConfig.getSettingType());
  }

  private boolean validateHelmRepoConfig(HelmRepoConfig helmRepoConfig) {
    if (helmRepoConfig == null) {
      return isHelmInstalled();
    } else if (helmRepoConfig instanceof HttpHelmRepoConfig) {
      return validateHttpHelmRepoConfig(helmRepoConfig);
    } else if (helmRepoConfig instanceof AmazonS3HelmRepoConfig) {
      return validateAmazonS3HelmRepoConfig();
    } else if (helmRepoConfig instanceof GCSHelmRepoConfig) {
      return validateGcsHelmRepoConfig();
    }

    throw new WingsException(UNHANDLED_CONFIG_MSG + helmRepoConfig.getSettingType());
  }

  private boolean validateGcsHelmRepoConfig() {
    return isHelmInstalled() && isChartMuseumInstalled();
  }

  private List<DelegateConnectionResult> taskValidationResult(boolean validated) {
    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  private boolean validateHttpHelmRepoConfig(HelmRepoConfig helmRepoConfig) {
    HttpHelmRepoConfig httpHelmRepoConfig = (HttpHelmRepoConfig) helmRepoConfig;

    return isHelmInstalled() && isConnectableHttpUrl(httpHelmRepoConfig.getChartRepoUrl());
  }

  private boolean validateAmazonS3HelmRepoConfig() {
    return isHelmInstalled() && isChartMuseumInstalled() && isConnectableHttpUrl(AWS_URL);
  }

  private boolean isConnectableHttpUrl(String url) {
    if (!connectableHttpUrl(url)) {
      logger.info(format("Unreachable URL %s for task %s from delegate", url, delegateTaskId));
      return false;
    }

    return true;
  }

  private boolean isChartMuseumInstalled() {
    String chartMuseumPath = k8sGlobalConfigService.getChartMuseumPath();
    if (isBlank(chartMuseumPath)) {
      logger.info(format("chartmuseum not installed in delegate for task %s", delegateTaskId));
      return false;
    }

    String chartMuseumVersionCommand =
        CHART_MUSEUM_VERSION_COMMAND.replace("${CHART_MUSEUM_PATH}", encloseWithQuotesIfNeeded(chartMuseumPath));

    return executeCommand(chartMuseumVersionCommand);
  }

  private boolean isHelmInstalled() {
    String helmPath = k8sGlobalConfigService.getHelmPath();
    if (isBlank(helmPath)) {
      logger.info(format("Helm not installed in delegate for task %s", delegateTaskId));
      return false;
    }

    String helmVersionCommand = HELM_VERSION_COMMAND.replace("${HELM_PATH}", encloseWithQuotesIfNeeded(helmPath));

    return executeCommand(helmVersionCommand);
  }

  private String getCriteriaForEmptyHelmRepoConfigInValuesFetch() {
    HelmValuesFetchTaskParameters valuesTaskParams = (HelmValuesFetchTaskParameters) getParameters()[0];
    HelmChartConfigParams helmChartConfigTaskParams = valuesTaskParams.getHelmChartConfigTaskParams();

    StringBuilder builder = new StringBuilder(64).append("DIRECT_HELM_REPO: ");
    if (isNotBlank(helmChartConfigTaskParams.getChartName())) {
      builder.append(helmChartConfigTaskParams.getChartName());
    }

    if (isNotBlank(helmChartConfigTaskParams.getChartUrl())) {
      builder.append(':').append(helmChartConfigTaskParams.getChartUrl());
    }

    if (isNotBlank(helmChartConfigTaskParams.getChartVersion())) {
      builder.append(':').append(helmChartConfigTaskParams.getChartVersion());
    }

    return builder.toString();
  }

  private HelmRepoConfig getHelmRepoConfig() {
    TaskType taskType = Enum.valueOf(TaskType.class, getTaskType());

    switch (taskType) {
      case HELM_REPO_CONFIG_VALIDATION:
        HelmRepoConfigValidationTaskParams repoConfigTaskParams =
            (HelmRepoConfigValidationTaskParams) getParameters()[0];
        return repoConfigTaskParams.getHelmRepoConfig();

      case HELM_VALUES_FETCH:
        HelmValuesFetchTaskParameters valuesTaskParams = (HelmValuesFetchTaskParameters) getParameters()[0];
        return valuesTaskParams.getHelmChartConfigTaskParams().getHelmRepoConfig();

      default:
        unhandled(taskType);
    }

    return null;
  }

  private boolean executeCommand(String command) {
    try {
      logger.info("Executing command: " + command);

      ProcessExecutor processExecutor =
          new ProcessExecutor().timeout(2, TimeUnit.MINUTES).commandSplit(command).readOutput(true);

      ProcessResult processResult = processExecutor.execute();

      if (processResult.getExitValue() != 0) {
        logger.info("Failed to execute command: " + command + ". Error: " + processResult.getOutput().getUTF8());
        return false;
      }

      return true;
    } catch (Exception ex) {
      logger.error("Error executing command: " + command, ex);
      return false;
    }
  }
}