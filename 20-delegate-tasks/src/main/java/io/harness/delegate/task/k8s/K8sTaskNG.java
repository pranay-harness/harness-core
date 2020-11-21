package io.harness.delegate.task.k8s;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.waitForDirectoryToBeAccessibleOutOfProcess;
import static io.harness.filesystem.FileIo.writeUtf8StringToFile;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.delegate.k8s.K8sRequestHandler;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
@Slf4j
public class K8sTaskNG extends AbstractDelegateRunnableTask {
  @Inject private Map<String, K8sRequestHandler> k8sTaskTypeToRequestHandler;
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  @Inject private SecretDecryptionService secretDecryptionService;

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

    log.info("Starting task execution for Command {}", k8sDeployRequest.getTaskType().name());
    decryptRequestDTOs(k8sDeployRequest);

    if (k8sDeployRequest.getTaskType() == K8sTaskType.INSTANCE_SYNC) {
      try {
        return k8sTaskTypeToRequestHandler.get(k8sDeployRequest.getTaskType().name())
            .executeTask(k8sDeployRequest, null);
      } catch (Exception ex) {
        log.error("Exception in processing k8s task [{}]", k8sDeployRequest.toString(), ex);
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

        K8sDelegateTaskParams k8SDelegateTaskParams =
            K8sDelegateTaskParams.builder()
                .kubectlPath(k8sGlobalConfigService.getKubectlPath())
                .kubeconfigPath(KUBECONFIG_FILENAME)
                .workingDirectory(workingDirectory)
                .goTemplateClientPath(k8sGlobalConfigService.getGoTemplateClientPath())
                // TODO: later add helm versions support also
                .helmPath(k8sGlobalConfigService.getHelmPath(null))
                .ocPath(k8sGlobalConfigService.getOcPath())
                .kustomizeBinaryPath(k8sGlobalConfigService.getKustomizePath())
                .build();

        logK8sVersion(k8sDeployRequest, k8SDelegateTaskParams);

        return k8sTaskTypeToRequestHandler.get(k8sDeployRequest.getTaskType().name())
            .executeTask(k8sDeployRequest, k8SDelegateTaskParams);
      } catch (Exception ex) {
        log.error("Exception in processing k8s task [{}]", k8sDeployRequest.toString(), ex);
        return K8sDeployResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(ExceptionUtils.getMessage(ex))
            .build();
      } finally {
        cleanup(workingDirectory);
      }
    }
  }

  private void logK8sVersion(K8sDeployRequest k8sDeployRequest, K8sDelegateTaskParams k8sDelegateTaskParams) {
    try {
      k8sTaskTypeToRequestHandler.get(K8sTaskType.VERSION.name()).executeTask(k8sDeployRequest, k8sDelegateTaskParams);
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
    decryptK8sInfraDelegateConfig(k8sDeployRequest.getK8sInfraDelegateConfig());
  }

  private void decryptK8sInfraDelegateConfig(K8sInfraDelegateConfig k8sInfraDelegateConfig) {
    if (k8sInfraDelegateConfig instanceof DirectK8sInfraDelegateConfig) {
      DirectK8sInfraDelegateConfig directK8sInfraDelegateConfig = (DirectK8sInfraDelegateConfig) k8sInfraDelegateConfig;

      KubernetesClusterConfigDTO clusterConfigDTO = directK8sInfraDelegateConfig.getKubernetesClusterConfigDTO();
      if (clusterConfigDTO.getCredential().getKubernetesCredentialType()
          == KubernetesCredentialType.MANUAL_CREDENTIALS) {
        KubernetesClusterDetailsDTO clusterDetailsDTO =
            (KubernetesClusterDetailsDTO) clusterConfigDTO.getCredential().getConfig();
        KubernetesAuthCredentialDTO authCredentialDTO = clusterDetailsDTO.getAuth().getCredentials();
        secretDecryptionService.decrypt(authCredentialDTO, directK8sInfraDelegateConfig.getEncryptionDataDetails());
      }
    }
  }

  private void decryptManifestDelegateConfig(ManifestDelegateConfig manifestDelegateConfig) {
    if (manifestDelegateConfig == null) {
      return;
    }

    switch (manifestDelegateConfig.getManifestType()) {
      case K8S_MANIFEST:
        StoreDelegateConfig storeDelegateConfig =
            ((K8sManifestDelegateConfig) manifestDelegateConfig).getStoreDelegateConfig();
        if (storeDelegateConfig instanceof GitStoreDelegateConfig) {
          GitStoreDelegateConfig gitStoreDelegateConfig = (GitStoreDelegateConfig) storeDelegateConfig;
          secretDecryptionService.decrypt(
              gitStoreDelegateConfig.getGitConfigDTO().getGitAuth(), gitStoreDelegateConfig.getEncryptedDataDetails());
        }
        break;

      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported Manifest type: [%s]", manifestDelegateConfig.getManifestType().name()));
    }
  }
}
