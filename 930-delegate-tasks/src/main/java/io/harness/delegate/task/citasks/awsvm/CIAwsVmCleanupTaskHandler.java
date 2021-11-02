package io.harness.delegate.task.citasks.awsvm;

import static io.harness.delegate.task.citasks.awsvm.helper.CIAwsVmConstants.RUNNER_CLEANUP_STAGE_URL;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.CICleanupTaskParams;
import io.harness.delegate.beans.ci.awsvm.AwsVmTaskExecutionResponse;
import io.harness.delegate.beans.ci.awsvm.CIAwsVmCleanupTaskParams;
import io.harness.delegate.task.citasks.CICleanupTaskHandler;
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
public class CIAwsVmCleanupTaskHandler implements CICleanupTaskHandler {
  @NotNull private Type type = CICleanupTaskHandler.Type.AWS_VM;
  @Inject private HttpHelper httpHelper;

  @Override
  public Type getType() {
    return type;
  }

  public AwsVmTaskExecutionResponse executeTaskInternal(CICleanupTaskParams ciCleanupTaskParams) {
    CIAwsVmCleanupTaskParams params = (CIAwsVmCleanupTaskParams) ciCleanupTaskParams;
    log.info("Received request to clean AWS VM with stage runtime ID {}", params.getStageRuntimeId());
    return callRunnerForCleanup(params.getStageRuntimeId());
  }

  private AwsVmTaskExecutionResponse callRunnerForCleanup(String stageExecutionId) {
    Map<String, String> params = new HashMap<>();
    params.put("stage_id", stageExecutionId);
    JSONObject obj = new JSONObject(params);
    String body = obj.toString();

    CommandExecutionStatus executionStatus = CommandExecutionStatus.FAILURE;
    String errMessage = "";
    try {
      Response response = httpHelper.post(RUNNER_CLEANUP_STAGE_URL, body, 600);
      if (response != null && response.isSuccessful()) {
        executionStatus = CommandExecutionStatus.SUCCESS;
      }
    } catch (IOException e) {
      log.error("Failed to destory VM in runner", e);
      executionStatus = CommandExecutionStatus.FAILURE;
      errMessage = e.getMessage();
    }

    return AwsVmTaskExecutionResponse.builder()
        .errorMessage(errMessage)
        .commandExecutionStatus(executionStatus)
        .build();
  }
}