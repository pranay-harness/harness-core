package software.wings.sm;

import java.io.Serializable;

/**
 * Describes possible execution statuses for a state.
 *
 * @author Rishi
 */
public enum ExecutionStatus implements Serializable {
  NEW,
  RUNNING,
  SUCCESS,
  ABORTED,
  FAILED,
  ERROR;
}
