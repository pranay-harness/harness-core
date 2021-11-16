package io.harness.delegate.task.citasks.awsvm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.CIInitializeTaskParams;
import io.harness.delegate.beans.ci.awsvm.AwsVmTaskExecutionResponse;
import io.harness.delegate.beans.ci.awsvm.CIAWSVmInitializeTaskParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.citasks.CIInitializeTaskHandler;
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
public class CIAwsVmInitializeTaskHandler implements CIInitializeTaskHandler {
  @NotNull private Type type = CIInitializeTaskHandler.Type.AWS_VM;
  @Inject private HttpHelper httpHelper;

  @Override
  public Type getType() {
    return type;
  }

  public AwsVmTaskExecutionResponse executeTaskInternal(
      CIInitializeTaskParams ciInitializeTaskParams, ILogStreamingTaskClient logStreamingTaskClient) {
    CIAWSVmInitializeTaskParams ciawsVmInitializeTaskParams = (CIAWSVmInitializeTaskParams) ciInitializeTaskParams;
    log.info("Received request to initialize stage with stage runtime ID {}",
        ciawsVmInitializeTaskParams.getStageRuntimeId());
    return callRunnerForSetup(ciawsVmInitializeTaskParams.getStageRuntimeId());
  }

  private AwsVmTaskExecutionResponse callRunnerForSetup(String stageExecutionId) {
    Map<String, String> params = new HashMap<>();
    params.put("stage_id", stageExecutionId);

    CommandExecutionStatus executionStatus = CommandExecutionStatus.FAILURE;
    String errMessage = "";
    try {
      Response<Void> response = httpHelper.setupStageWithRetries(params);
      if (response.isSuccessful()) {
        executionStatus = CommandExecutionStatus.SUCCESS;
      }
    } catch (Exception e) {
      log.error("Failed to setup VM in runner", e);
      executionStatus = CommandExecutionStatus.FAILURE;
      errMessage = e.getMessage();
    }

    return AwsVmTaskExecutionResponse.builder()
        .errorMessage(errMessage)
        .commandExecutionStatus(executionStatus)
        .build();
  }
}