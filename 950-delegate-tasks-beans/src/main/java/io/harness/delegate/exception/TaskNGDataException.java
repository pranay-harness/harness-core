package io.harness.delegate.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.exception.DataException;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@OwnedBy(CDP)
@EqualsAndHashCode(callSuper = false)
public class TaskNGDataException extends DataException {
  UnitProgressData commandUnitsProgress;

  public TaskNGDataException(UnitProgressData commandUnitsProgress, Throwable cause) {
    super(cause);
    this.commandUnitsProgress = commandUnitsProgress;
  }
}
