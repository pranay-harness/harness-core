package io.harness.delegate.task.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.beans.storeconfig.GcsHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class HelmValuesFetchTaskNG extends AbstractDelegateRunnableTask {
  @Inject private HelmTaskHelperBase helmTaskHelperBase;
  @Inject private SecretDecryptionService decryptionService;

  public HelmValuesFetchTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new UnsupportedOperationException("This method is deprecated. Use run(TaskParameters) instead.");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    HelmValuesFetchRequest helmValuesFetchRequest = (HelmValuesFetchRequest) parameters;
    CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();
    log.info(format("Running HelmValuesFetchTaskNG for account %s", helmValuesFetchRequest.getAccountId()));

    LogCallback logCallback = new NGDelegateLogCallback(
        getLogStreamingTaskClient(), K8sCommandUnitConstants.FetchFiles, true, commandUnitsProgress);
    HelmChartManifestDelegateConfig helmChartManifestDelegateConfig =
        helmValuesFetchRequest.getHelmChartManifestDelegateConfig();
    try {
      decryptEncryptedDetails(helmChartManifestDelegateConfig);

      String valuesFileContent = helmTaskHelperBase.fetchValuesYamlFromChart(
          helmChartManifestDelegateConfig, helmValuesFetchRequest.getTimeout(), logCallback);

      return HelmValuesFetchResponse.builder()
          .commandExecutionStatus(SUCCESS)
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .valuesFileContent(valuesFileContent)
          .build();
    } catch (Exception e) {
      log.error("HelmValuesFetchTaskNG execution failed with exception ", e);
      return HelmValuesFetchResponse.builder()
          .commandExecutionStatus(FAILURE)
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .errorMessage("Execution failed with Exception: " + e.getMessage())
          .build();
    }
  }

  private void decryptEncryptedDetails(HelmChartManifestDelegateConfig helmChartManifestDelegateConfig) {
    StoreDelegateConfig helmStoreDelegateConfig = helmChartManifestDelegateConfig.getStoreDelegateConfig();
    switch (helmStoreDelegateConfig.getType()) {
      case S3_HELM:
        S3HelmStoreDelegateConfig s3HelmStoreConfig = (S3HelmStoreDelegateConfig) helmStoreDelegateConfig;
        for (DecryptableEntity entity : s3HelmStoreConfig.getAwsConnector().getDecryptableEntities()) {
          decryptionService.decrypt(entity, s3HelmStoreConfig.getEncryptedDataDetails());
        }
        break;
      case GCS_HELM:
        GcsHelmStoreDelegateConfig gcsHelmStoreDelegateConfig = (GcsHelmStoreDelegateConfig) helmStoreDelegateConfig;
        for (DecryptableEntity entity : gcsHelmStoreDelegateConfig.getGcpConnector().getDecryptableEntities()) {
          decryptionService.decrypt(entity, gcsHelmStoreDelegateConfig.getEncryptedDataDetails());
        }
        break;
      case HTTP_HELM:
        HttpHelmStoreDelegateConfig httpHelmStoreConfig = (HttpHelmStoreDelegateConfig) helmStoreDelegateConfig;
        for (DecryptableEntity entity : httpHelmStoreConfig.getHttpHelmConnector().getDecryptableEntities()) {
          decryptionService.decrypt(entity, httpHelmStoreConfig.getEncryptedDataDetails());
        }
        break;
      default:
        throw new InvalidRequestException(
            format("Store type: %s not supported for helm values fetch task NG", helmStoreDelegateConfig.getType()));
    }
  }
}
