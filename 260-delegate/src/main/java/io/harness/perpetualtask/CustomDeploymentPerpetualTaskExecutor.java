package io.harness.perpetualtask;

import static io.harness.network.SafeHttpCall.execute;

import static java.util.Collections.emptyList;

import io.harness.beans.ExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.grpc.utils.AnyUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams;
import io.harness.shell.ExecuteCommandResponse;
import io.harness.shell.ScriptType;

import software.wings.api.shellscript.provision.ShellScriptProvisionExecutionData;
import software.wings.core.local.executors.ShellExecutorConfig;
import software.wings.core.local.executors.ShellExecutorFactory;
import software.wings.core.ssh.executors.ScriptProcessExecutor;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;

@Slf4j
public class CustomDeploymentPerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Inject private ShellExecutorFactory shellExecutorFactory;
  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    log.info("Running the Custom Deployment InstanceSync perpetual task executor for task id: {}", taskId);

    final CustomDeploymentInstanceSyncTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), CustomDeploymentInstanceSyncTaskParams.class);

    final ShellScriptProvisionExecutionData response = executeScript(taskParams, taskId.getId());
    response.setStatus(response.getExecutionStatus());

    try {
      execute(
          delegateAgentManagerClient.publishInstanceSyncResult(taskId.getId(), taskParams.getAccountId(), response));
    } catch (Exception e) {
      log.error(String.format("Failed to publish instance sync result for custom deployment. PerpetualTaskId [%s]",
                    taskId.getId()),
          e);
    }

    return getPerpetualTaskResponse(response);
  }

  private PerpetualTaskResponse getPerpetualTaskResponse(ShellScriptProvisionExecutionData response) {
    String message = "success";
    if (ExecutionStatus.FAILED == response.getExecutionStatus()) {
      message = response.getErrorMsg();
    }

    return PerpetualTaskResponse.builder().responseCode(Response.SC_OK).responseMessage(message).build();
  }

  private ShellScriptProvisionExecutionData executeScript(
      CustomDeploymentInstanceSyncTaskParams taskParams, String taskId) {
    ShellScriptProvisionExecutionData response = null;
    String outputPath = null;
    try {
      outputPath = Files.createTempFile("customDeployment", "InstanceSync.json").toString();
      final ShellExecutorConfig shellExecutorConfig =
          ShellExecutorConfig.builder()
              .accountId(taskParams.getAccountId())
              .appId(taskParams.getAppId())
              .environment(ImmutableMap.of(taskParams.getOutputPathKey(), outputPath))
              .scriptType(ScriptType.BASH)
              .executionId(taskId)
              .commandUnitName("custom-deployment-instance-sync")
              .build();

      final ScriptProcessExecutor executor = shellExecutorFactory.getExecutor(shellExecutorConfig);
      final ExecuteCommandResponse executeCommandResponse =
          executor.executeCommandString(taskParams.getScript(), emptyList());

      if (CommandExecutionStatus.SUCCESS == executeCommandResponse.getStatus()) {
        return ShellScriptProvisionExecutionData.builder()
            .executionStatus(ExecutionStatus.SUCCESS)
            .output(new String(Files.readAllBytes(Paths.get(outputPath)), StandardCharsets.UTF_8))
            .build();
      } else {
        log.error("Error Occured While Running Custom Deployment Perpetual Task:{}", taskId);
        return ShellScriptProvisionExecutionData.builder().executionStatus(ExecutionStatus.FAILED).build();
      }
    } catch (Exception ex) {
      log.error("Exception Occured While Running Custom Deployment Perpetual Task:{}, Message: {}", taskId,
          ExceptionUtils.getMessage(ex));
      return ShellScriptProvisionExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMsg(ExceptionUtils.getMessage(ex))
          .build();
    } finally {
      deleteSilently(outputPath);
    }
  }

  private void deleteSilently(String path) {
    if (path != null) {
      try {
        Files.deleteIfExists(Paths.get(path));
      } catch (IOException e) {
        log.error("Failed to delete file " + path, e);
      }
    }
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}
