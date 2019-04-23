package software.wings.verification;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatusData;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class VerificationDataAnalysisResponse extends ExecutionStatusData {
  private VerificationStateAnalysisExecutionData stateExecutionData;
}
