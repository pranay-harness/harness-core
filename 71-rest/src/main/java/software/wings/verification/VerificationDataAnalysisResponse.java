package software.wings.verification;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatusResponseData;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class VerificationDataAnalysisResponse implements ExecutionStatusResponseData {
  private ExecutionStatus executionStatus;
  private VerificationStateAnalysisExecutionData stateExecutionData;
}
