package software.wings.delegatetasks.citasks.cik8handler;

/**
 * Delegate task handler to execute a command or list of commands on a pod's container in a kubernetes cluster.
 */

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.esotericsoftware.kryo.NotNull;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.logging.AutoLogContext;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.ci.ExecuteCommandTaskParams;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.K8ExecuteCommandTaskParams;
import software.wings.delegatetasks.citasks.ExecuteCommandTaskHandler;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.impl.KubernetesHelperService;

import java.util.List;

@Singleton
@Slf4j
public class K8ExecuteCommandTaskHandler implements ExecuteCommandTaskHandler {
  @NotNull private ExecuteCommandTaskHandler.Type type = ExecuteCommandTaskHandler.Type.K8;
  @Inject private CIK8CtlHandler cik8CtlHandler;
  @Inject private K8CommandExecutor k8CommandExecutor;
  @Inject private KubernetesHelperService kubernetesHelperService;

  @Override
  public ExecuteCommandTaskHandler.Type getType() {
    return type;
  }

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(ExecuteCommandTaskParams executeCommandTaskParams) {
    K8ExecuteCommandTaskParams k8ExecuteCommandTaskParams = (K8ExecuteCommandTaskParams) executeCommandTaskParams;
    KubernetesConfig kubernetesConfig = k8ExecuteCommandTaskParams.getKubernetesConfig();
    List<EncryptedDataDetail> encryptedDataDetails = k8ExecuteCommandTaskParams.getEncryptionDetails();

    K8ExecCommandParams k8ExecCommandParams = k8ExecuteCommandTaskParams.getK8ExecCommandParams();
    String podName = k8ExecCommandParams.getPodName();
    String containerName = k8ExecCommandParams.getContainerName();
    List<String> commands = k8ExecCommandParams.getCommands();

    K8sTaskExecutionResponse result;
    try (AutoLogContext ignore = new K8LogContext(podName, containerName, OVERRIDE_ERROR)) {
      Config config = kubernetesHelperService.getConfig(kubernetesConfig, encryptedDataDetails, StringUtils.EMPTY);
      OkHttpClient okHttpClient = kubernetesHelperService.createHttpClientWithProxySetting(config);
      try (DefaultKubernetesClient kubernetesClient = new DefaultKubernetesClient(okHttpClient, config)) {
        ExecCommandStatus status = k8CommandExecutor.executeCommand(kubernetesClient, k8ExecCommandParams);
        if (status == ExecCommandStatus.SUCCESS) {
          logger.info("Successfully executed commands {} on container {} of pod {}", commands, containerName, podName);
          result = K8sTaskExecutionResponse.builder()
                       .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.SUCCESS)
                       .build();
        } else {
          logger.info("Failed to execute commands {} on container {} of pod {} with status {}", commands, containerName,
              podName, status);
          result = K8sTaskExecutionResponse.builder()
                       .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.FAILURE)
                       .build();
        }

      } catch (Exception e) {
        logger.error("Exception in processing CI execute command task: {}", commands, e);
        result = K8sTaskExecutionResponse.builder()
                     .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.FAILURE)
                     .errorMessage(e.getMessage())
                     .build();
      }
    }
    return result;
  }
}