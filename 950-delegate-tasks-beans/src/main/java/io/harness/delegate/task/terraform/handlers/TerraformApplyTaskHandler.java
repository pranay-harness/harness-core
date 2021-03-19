package io.harness.delegate.task.terraform.handlers;

import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;

public class TerraformApplyTaskHandler extends TerraformAbstractTaskHandler {
  @Override
  public TerraformTaskNGResponse executeTaskInternal(TerraformTaskNGParameters taskParameters,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    return null;
  }
}
