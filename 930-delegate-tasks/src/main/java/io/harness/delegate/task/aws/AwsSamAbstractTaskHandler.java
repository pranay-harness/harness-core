package io.harness.delegate.task.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public abstract class AwsSamAbstractTaskHandler {
  public abstract AwsSamTaskNGResponse executeTaskInternal(AwsSamTaskParameters taskParameters, String delegateId,
      String taskId, LogCallback logCallback) throws IOException;

  public AwsSamTaskNGResponse executeTask(AwsSamTaskParameters taskParameters, String delegateId, String taskId,
      LogCallback logCallback, CommandUnitsProgress commandUnitsProgress) {
    try {
      AwsSamTaskNGResponse awsSamTaskNGResponse = executeTaskInternal(taskParameters, delegateId, taskId, logCallback);
      awsSamTaskNGResponse.setUnitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress));
      return awsSamTaskNGResponse;
    } catch (WingsException ex) {
      log.error("Failed to execute Terraform Task ", ex);
      return AwsSamTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage("Failed to execute Terraform Task. Reason: " + ex.getMessage())
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .build();
    } catch (Exception ex) {
      log.error("Failed to execute Terraform Task ", ex);
      return AwsSamTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage("Failed to execute Terraform Task.")
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .build();
    }
  }
}