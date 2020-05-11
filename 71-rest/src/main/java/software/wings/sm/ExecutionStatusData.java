package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.ExecutionStatusResponseData;
import lombok.Builder;
import lombok.Value;

/**
 * The type Execution status data.
 */
@OwnedBy(CDC)
@Value
@Builder
public class ExecutionStatusData implements ExecutionStatusResponseData {
  private ExecutionStatus executionStatus;
}