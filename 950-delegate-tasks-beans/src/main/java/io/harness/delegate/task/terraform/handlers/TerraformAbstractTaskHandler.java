package io.harness.delegate.task.terraform.handlers;

import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.exception.WingsException;
import io.harness.git.model.AuthRequest;
import io.harness.logging.CommandExecutionStatus;
import io.harness.shell.SshSessionConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class TerraformAbstractTaskHandler {
  public abstract TerraformTaskNGResponse executeTaskInternal(TerraformTaskNGParameters taskParameters,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress,
      AuthRequest authRequest, String delegateId, String taskId);

  public TerraformTaskNGResponse executeTask(TerraformTaskNGParameters taskParameters,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress,
      AuthRequest authRequest, String delegateId, String taskId) {
    try {
      TerraformTaskNGResponse terraformTaskNGResponse = executeTaskInternal(
          taskParameters, logStreamingTaskClient, commandUnitsProgress, authRequest, delegateId, taskId);
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
