package io.harness.delegate.exceptionhandler.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DataException;

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.DX)
public class DelegateMetadataTestException extends DataException {
  String taskId;
  String taskName;

  public DelegateMetadataTestException(Throwable cause) {
    super(cause);
  }
}
