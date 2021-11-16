package io.harness.delegate.task.citasks.awsvm;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;
import io.harness.delegate.beans.ci.awsvm.AwsVmTaskExecutionResponse;
import io.harness.delegate.beans.ci.awsvm.CIAWSVmExecuteStepTaskParams;
import io.harness.delegate.beans.ci.awsvm.runner.ExecuteStepResponse;
import io.harness.delegate.task.citasks.CIExecuteStepTaskHandler;
import io.harness.delegate.task.citasks.awsvm.helper.HttpHelper;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIAwsVmExecuteStepTaskHandler implements CIExecuteStepTaskHandler {
  @Inject private HttpHelper httpHelper;
  @NotNull private Type type = Type.AWS_VM;

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public AwsVmTaskExecutionResponse executeTaskInternal(CIExecuteStepTaskParams ciExecuteStepTaskParams) {
    CIAWSVmExecuteStepTaskParams ciawsVmExecuteStepTaskParams = (CIAWSVmExecuteStepTaskParams) ciExecuteStepTaskParams;
    log.info(
        "Received request to execute step with stage runtime ID {}", ciawsVmExecuteStepTaskParams.getStageRuntimeId());
    return callRunnerForStepExecution(ciawsVmExecuteStepTaskParams);
  }

  private AwsVmTaskExecutionResponse callRunnerForStepExecution(CIAWSVmExecuteStepTaskParams taskParams) {
    Map<String, String> params = new HashMap<>();
    params.put("stage_id", taskParams.getStageRuntimeId());
    params.put("stepId", taskParams.getStepId());
    params.put("image", taskParams.getImage());
    params.put("command", taskParams.getCommand());
    params.put("log_key", taskParams.getLogKey());
    params.put("log_stream_url", taskParams.getLogStreamUrl());
    params.put("log_stream_account_id", taskParams.getAccountId());
    params.put("log_stream_token", taskParams.getLogToken());

    try {
      Response<ExecuteStepResponse> response = httpHelper.executeStepWithRetries(params);
      if (!response.isSuccessful()) {
        return AwsVmTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
      }

      if (response.body().getExitCode() == 0) {
        return AwsVmTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
      } else {
        return AwsVmTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(format("Exit code: %d", response.body().getExitCode()))
            .build();
      }
    } catch (Exception e) {
      log.error("Failed to execute step in runner", e);
      return AwsVmTaskExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(e.getMessage())
          .build();
    }
  }
}
