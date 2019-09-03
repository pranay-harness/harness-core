package software.wings.sm;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Value;

/**
 * The type Execution status data.
 */
@Value
@Builder
public class ExecutionStatusData implements ExecutionStatusResponseData {
  private ExecutionStatus executionStatus;
}