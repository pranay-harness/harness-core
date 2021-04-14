package io.harness.delegate.task.terraform.handlers;

import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.logging.LogCallback;

public class TerraformDestroyTaskHandler extends TerraformAbstractTaskHandler {
  @Override
  public TerraformTaskNGResponse executeTaskInternal(
      TerraformTaskNGParameters taskParameters, String delegateId, String taskId, LogCallback logCallback) {
    return null;
  }
}
