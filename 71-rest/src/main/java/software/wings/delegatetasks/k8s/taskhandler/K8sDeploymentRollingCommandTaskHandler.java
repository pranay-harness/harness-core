package software.wings.delegatetasks.k8s.taskhandler;

import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.inject.Singleton;

import io.harness.exception.InvalidArgumentsException;
import io.harness.filesystem.FileIo;
import io.harness.k8s.kubectl.ApplyCommand;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.OutputFormat;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.k8s.K8sCommandTaskParams;
import software.wings.helpers.ext.k8s.request.K8sCommandRequest;
import software.wings.helpers.ext.k8s.request.K8sDeploymentRollingSetupRequest;
import software.wings.helpers.ext.k8s.response.K8sCommandExecutionResponse;

@NoArgsConstructor
@Singleton
public class K8sDeploymentRollingCommandTaskHandler extends K8sCommandTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sDeploymentRollingCommandTaskHandler.class);

  public K8sCommandExecutionResponse executeTaskInternal(
      K8sCommandRequest k8sCommandRequest, K8sCommandTaskParams k8sCommandTaskParams) throws Exception {
    if (!(k8sCommandRequest instanceof K8sDeploymentRollingSetupRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sCommandRequest", "Must be instance of K8sDeploymentRollingSetupRequest"));
    }

    K8sDeploymentRollingSetupRequest request = (K8sDeploymentRollingSetupRequest) k8sCommandRequest;

    executionLogCallback.saveExecutionLog("Starting Kubernetes Rolling Deployment Command");

    FileIo.writeUtf8StringToFile(k8sCommandTaskParams.getWorkingDirectory() + "/manifests.yaml",
        request.getManifestFiles().get(0).getFileContent());

    Kubectl client = Kubectl.client(k8sCommandTaskParams.getKubectlPath(), k8sCommandTaskParams.getKubeconfigPath());

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").record(true).output(OutputFormat.yaml);

    executionLogCallback.saveExecutionLog(applyCommand.command());

    ProcessResult result = applyCommand.execute(k8sCommandTaskParams.getWorkingDirectory(),
        new LogOutputStream() {
          @Override
          protected void processLine(String line) {
            executionLogCallback.saveExecutionLog(line, INFO);
          }
        },
        new LogOutputStream() {
          @Override
          protected void processLine(String line) {
            executionLogCallback.saveExecutionLog(line, ERROR);
          }
        });

    if (result.getExitValue() != 0) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
  }
}
