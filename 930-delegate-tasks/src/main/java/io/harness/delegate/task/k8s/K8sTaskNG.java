package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.waitForDirectoryToBeAccessibleOutOfProcess;
import static io.harness.filesystem.FileIo.writeUtf8StringToFile;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.beans.storeconfig.GcsHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.delegate.k8s.K8sRequestHandler;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.git.GitDecryptionHelper;
import io.harness.exception.ExceptionUtils;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.HelmVersion;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
@Slf4j
@OwnedBy(CDP)
public class K8sTaskNG extends AbstractDelegateRunnableTask {
  @Inject private Map<String, K8sRequestHandler> k8sTaskTypeToRequestHandler;
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  @Inject private GitDecryptionHelper gitDecryptionHelper;
  @Inject private SecretDecryptionService decryptionService;

  private static final String WORKING_DIR_BASE = "./repository/k8s/";
  public static final String KUBECONFIG_FILENAME = "config";
  public static final String MANIFEST_FILES_DIR = "manifest-files";

  public K8sTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public K8sDeployResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public K8sDeployResponse run(TaskParameters parameters) {
    K8sDeployRequest k8sDeployRequest = (K8sDeployRequest) parameters;
    CommandUnitsProgress commandUnitsProgress = k8sDeployRequest.getCommandUnitsProgress() != null
        ? k8sDeployRequest.getCommandUnitsProgress()
        : CommandUnitsProgress.builder().build();

    log.info("Starting task execution for Command {}", k8sDeployRequest.getTaskType().name());
    decryptRequestDTOs(k8sDeployRequest);

    if (k8sDeployRequest.getTaskType() == K8sTaskType.INSTANCE_SYNC) {
      try {
        return k8sTaskTypeToRequestHandler.get(k8sDeployRequest.getTaskType().name())
            .executeTask(k8sDeployRequest, null, getLogStreamingTaskClient(), commandUnitsProgress);
      } catch (Exception ex) {
        log.error("Exception in processing k8s task [{}]",
            k8sDeployRequest.getCommandName() + ":" + k8sDeployRequest.getTaskType(), ex);
        return K8sDeployResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(ExceptionUtils.getMessage(ex))
            .build();
      }
    } else {
      String workingDirectory = Paths.get(WORKING_DIR_BASE, convertBase64UuidToCanonicalForm(generateUuid()))
                                    .normalize()
                                    .toAbsolutePath()
                                    .toString();

      try {
        String kubeconfigFileContent = containerDeploymentDelegateBaseHelper.getKubeconfigFileContent(
            k8sDeployRequest.getK8sInfraDelegateConfig());

        createDirectoryIfDoesNotExist(workingDirectory);
        waitForDirectoryToBeAccessibleOutOfProcess(workingDirectory, 10);
        writeUtf8StringToFile(Paths.get(workingDirectory, KUBECONFIG_FILENAME).toString(), kubeconfigFileContent);

        createDirectoryIfDoesNotExist(Paths.get(workingDirectory, MANIFEST_FILES_DIR).toString());
        HelmVersion helmVersion =
            (k8sDeployRequest.getManifestDelegateConfig() != null
                && k8sDeployRequest.getManifestDelegateConfig().getManifestType() == ManifestType.HELM_CHART)
            ? ((HelmChartManifestDelegateConfig) k8sDeployRequest.getManifestDelegateConfig()).getHelmVersion()
            : null;

        K8sDelegateTaskParams k8SDelegateTaskParams =
            K8sDelegateTaskParams.builder()
                .kubectlPath(k8sGlobalConfigService.getKubectlPath())
                .kubeconfigPath(KUBECONFIG_FILENAME)
                .workingDirectory(workingDirectory)
                .goTemplateClientPath(k8sGlobalConfigService.getGoTemplateClientPath())
                .helmPath(k8sGlobalConfigService.getHelmPath(helmVersion))
                .ocPath(k8sGlobalConfigService.getOcPath())
                .kustomizeBinaryPath(k8sGlobalConfigService.getKustomizePath())
                .helmVersion(k8sGlobalConfigService.getHelmVersion(helmVersion))
                .kubectlVersion(k8sGlobalConfigService.getKubectlVersion())
                .build();
        // TODO: @anshul/vaibhav , fix this
        //        logK8sVersion(k8sDeployRequest, k8SDelegateTaskParams, commandUnitsProgress);

        K8sDeployResponse k8sDeployResponse = k8sTaskTypeToRequestHandler.get(k8sDeployRequest.getTaskType().name())
                                                  .executeTask(k8sDeployRequest, k8SDelegateTaskParams,
                                                      getLogStreamingTaskClient(), commandUnitsProgress);

        k8sDeployResponse.setCommandUnitsProgress(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress));
        return k8sDeployResponse;
      } catch (Exception ex) {
        log.error("Exception in processing k8s task [{}]",
            k8sDeployRequest.getCommandName() + ":" + k8sDeployRequest.getTaskType(), ex);
        return K8sDeployResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(ExceptionUtils.getMessage(ex))
            .commandUnitsProgress(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
            .build();
      } finally {
        cleanup(workingDirectory);
      }
    }
  }

  @VisibleForTesting
  void logK8sVersion(K8sDeployRequest k8sDeployRequest, K8sDelegateTaskParams k8sDelegateTaskParams,
      CommandUnitsProgress commandUnitsProgress) {
    try {
      k8sTaskTypeToRequestHandler.get(K8sTaskType.VERSION.name())
          .executeTask(k8sDeployRequest, k8sDelegateTaskParams, getLogStreamingTaskClient(), commandUnitsProgress);
    } catch (Exception ex) {
      log.error("Error fetching K8s Server Version: ", ex);
    }
  }

  private void cleanup(String workingDirectory) {
    try {
      log.warn("Cleaning up directory " + workingDirectory);
      deleteDirectoryAndItsContentIfExists(workingDirectory);
    } catch (Exception ex) {
      log.warn("Exception in directory cleanup.", ex);
    }
  }

  public void decryptRequestDTOs(K8sDeployRequest k8sDeployRequest) {
    decryptManifestDelegateConfig(k8sDeployRequest.getManifestDelegateConfig());
    containerDeploymentDelegateBaseHelper.decryptK8sInfraDelegateConfig(k8sDeployRequest.getK8sInfraDelegateConfig());
  }

  private void decryptManifestDelegateConfig(ManifestDelegateConfig manifestDelegateConfig) {
    if (manifestDelegateConfig == null) {
      return;
    }

    StoreDelegateConfig storeDelegateConfig = manifestDelegateConfig.getStoreDelegateConfig();
    switch (storeDelegateConfig.getType()) {
      case GIT:
        GitStoreDelegateConfig gitStoreDelegateConfig = (GitStoreDelegateConfig) storeDelegateConfig;
        GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO(gitStoreDelegateConfig.getGitConfigDTO());
        gitDecryptionHelper.decryptGitConfig(gitConfigDTO, gitStoreDelegateConfig.getEncryptedDataDetails());
        break;

      case HTTP_HELM:
        HttpHelmStoreDelegateConfig httpHelmStoreConfig = (HttpHelmStoreDelegateConfig) storeDelegateConfig;
        for (DecryptableEntity entity : httpHelmStoreConfig.getHttpHelmConnector().getDecryptableEntities()) {
          decryptionService.decrypt(entity, httpHelmStoreConfig.getEncryptedDataDetails());
        }
        break;

      case S3_HELM:
        S3HelmStoreDelegateConfig s3HelmStoreConfig = (S3HelmStoreDelegateConfig) storeDelegateConfig;
        List<DecryptableEntity> s3DecryptableEntityList = s3HelmStoreConfig.getAwsConnector().getDecryptableEntities();
        if (isNotEmpty(s3DecryptableEntityList)) {
          for (DecryptableEntity decryptableEntity : s3DecryptableEntityList) {
            decryptionService.decrypt(decryptableEntity, s3HelmStoreConfig.getEncryptedDataDetails());
          }
        }
        break;

      case GCS_HELM:
        GcsHelmStoreDelegateConfig gcsHelmStoreDelegateConfig = (GcsHelmStoreDelegateConfig) storeDelegateConfig;
        List<DecryptableEntity> gcsDecryptableEntityList =
            gcsHelmStoreDelegateConfig.getGcpConnector().getDecryptableEntities();
        if (isNotEmpty(gcsDecryptableEntityList)) {
          for (DecryptableEntity decryptableEntity : gcsDecryptableEntityList) {
            decryptionService.decrypt(decryptableEntity, gcsHelmStoreDelegateConfig.getEncryptedDataDetails());
          }
        }
        break;

      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported Manifest type: [%s]", manifestDelegateConfig.getManifestType().name()));
    }
  }
}
