package software.wings.sm;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;

/**
 * The type Execution status data.
 */
public interface ExecutionStatusResponseData extends ResponseData { ExecutionStatus getExecutionStatus(); }