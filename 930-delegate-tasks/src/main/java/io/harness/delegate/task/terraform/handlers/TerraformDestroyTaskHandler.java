package io.harness.delegate.task.terraform.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.logging.LogCallback;

@OwnedBy(CDP)
public class TerraformDestroyTaskHandler extends TerraformAbstractTaskHandler {
  @Override
  public TerraformTaskNGResponse executeTaskInternal(
      TerraformTaskNGParameters taskParameters, String delegateId, String taskId, LogCallback logCallback) {
    return null;
  }
}
