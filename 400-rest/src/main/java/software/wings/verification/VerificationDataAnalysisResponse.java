package software.wings.verification;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.ExecutionStatusResponseData;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class VerificationDataAnalysisResponse implements ExecutionStatusResponseData {
  private ExecutionStatus executionStatus;
  private VerificationStateAnalysisExecutionData stateExecutionData;
}
