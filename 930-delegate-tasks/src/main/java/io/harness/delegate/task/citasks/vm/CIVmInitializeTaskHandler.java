package io.harness.delegate.task.citasks.vm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.CIInitializeTaskParams;
import io.harness.delegate.beans.ci.vm.CIVmInitializeTaskParams;
import io.harness.delegate.beans.ci.vm.VmTaskExecutionResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.citasks.CIInitializeTaskHandler;
import io.harness.delegate.task.citasks.vm.helper.HttpHelper;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIVmInitializeTaskHandler implements CIInitializeTaskHandler {
  @NotNull private Type type = CIInitializeTaskHandler.Type.VM;
  @Inject private HttpHelper httpHelper;

  @Override
  public Type getType() {
    return type;
  }

  public VmTaskExecutionResponse executeTaskInternal(
      CIInitializeTaskParams ciInitializeTaskParams, ILogStreamingTaskClient logStreamingTaskClient) {
    CIVmInitializeTaskParams CIVmInitializeTaskParams = (CIVmInitializeTaskParams) ciInitializeTaskParams;
    log.info(
        "Received request to initialize stage with stage runtime ID {}", CIVmInitializeTaskParams.getStageRuntimeId());
    return callRunnerForSetup(CIVmInitializeTaskParams.getStageRuntimeId());
  }

  private VmTaskExecutionResponse callRunnerForSetup(String stageExecutionId) {
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

    return VmTaskExecutionResponse.builder().errorMessage(errMessage).commandExecutionStatus(executionStatus).build();
  }
}