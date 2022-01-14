package io.harness.delegate.task.terraform.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.terraform.TerraformBaseHelper;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.logging.LogCallback;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public abstract class TerraformAbstractTaskHandler {
  public abstract TerraformTaskNGResponse executeTaskInternal(
      TerraformTaskNGParameters taskParameters, String delegateId, String taskId, LogCallback logCallback)
      throws IOException, TimeoutException, InterruptedException;
  @Inject TerraformBaseHelper terraformBaseHelper;

  public TerraformTaskNGResponse executeTask(TerraformTaskNGParameters taskParameters, String delegateId, String taskId,
      LogCallback logCallback) throws Exception {
    try {
      return executeTaskInternal(taskParameters, delegateId, taskId, logCallback);
    } finally {
      terraformBaseHelper.performCleanupOfTfDirs(taskParameters, logCallback);
    }
  }
}
