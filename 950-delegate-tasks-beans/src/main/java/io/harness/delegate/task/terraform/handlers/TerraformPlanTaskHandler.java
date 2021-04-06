package io.harness.delegate.task.terraform.handlers;

import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.git.model.AuthRequest;

public class TerraformPlanTaskHandler extends TerraformAbstractTaskHandler {
  @Override
  public TerraformTaskNGResponse executeTaskInternal(TerraformTaskNGParameters taskParameters,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress,
      AuthRequest authRequest, String delegateId, String taskId) {
    return null;
  }
}
