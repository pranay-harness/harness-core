package io.harness.delegate.task.terraform.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public abstract class TerraformAbstractTaskHandler {
  public abstract TerraformTaskNGResponse executeTaskInternal(TerraformTaskNGParameters taskParameters,
      String delegateId, String taskId, LogCallback logCallback) throws IOException;

  public TerraformTaskNGResponse executeTask(TerraformTaskNGParameters taskParameters, String delegateId, String taskId,
      LogCallback logCallback, CommandUnitsProgress commandUnitsProgress) {
    try {
      TerraformTaskNGResponse terraformTaskNGResponse =
          executeTaskInternal(taskParameters, delegateId, taskId, logCallback);
      terraformTaskNGResponse.setUnitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress));
      return terraformTaskNGResponse;
    } catch (WingsException ex) {
      log.error("Failed to execute Terraform Task ", ex);
      return TerraformTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage("Failed to execute Terraform Task. Reason: " + ex.getMessage())
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .build();
    } catch (Exception ex) {
      log.error("Failed to execute Terraform Task ", ex);
      return TerraformTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage("Failed to execute Terraform Task.")
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .build();
    }
  }
}
