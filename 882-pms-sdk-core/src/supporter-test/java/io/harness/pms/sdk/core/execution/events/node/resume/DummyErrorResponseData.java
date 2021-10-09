package io.harness.pms.sdk.core.execution.events.node.resume;

import io.harness.exception.FailureType;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.tasks.ErrorResponseData;

import java.util.EnumSet;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DummyErrorResponseData implements ErrorResponseData {
  @Override
  public String getErrorMessage() {
    return "error";
  }

  @Override
  public EnumSet<FailureType> getFailureTypes() {
    return EnumSet.of(FailureType.CONNECTIVITY);
  }

  @Override
  public WingsException getException() {
    return new InvalidRequestException("Dummy Invalid Request Exception");
  }
}
