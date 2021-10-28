package io.harness.delegate.task.citasks.awsvm;

import static io.harness.delegate.task.citasks.awsvm.helper.CIAwsVmConstants.RUNNER_SETUP_STAGE_URL;

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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.json.JSONObject;

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
    return callRunnerForSetup(ciawsVmInitializeTaskParams.getStageRuntimeId());
  }

  private AwsVmTaskExecutionResponse callRunnerForSetup(String stageExecutionId) {
    Map<String, String> params = new HashMap<>();
    params.put("stage_id", stageExecutionId);
    JSONObject obj = new JSONObject(params);
    String body = obj.toString();

    CommandExecutionStatus executionStatus = CommandExecutionStatus.FAILURE;
    String errMessage = "";
    try {
      Response response = httpHelper.post(RUNNER_SETUP_STAGE_URL, body, 600);
      if (response != null && response.isSuccessful()) {
        executionStatus = CommandExecutionStatus.SUCCESS;
      }
    } catch (IOException e) {
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