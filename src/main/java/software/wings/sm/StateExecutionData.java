package software.wings.sm;

import java.io.Serializable;

/**
 * Represents state machine execution data.
 *
 * @author Rishi
 */
public class StateExecutionData implements Serializable {
  private static final long serialVersionUID = 1L;

  private String stateName;
  private long startTs;
  private long endTs;
  private ExecutionStatus status;

  public String getStateName() {
    return stateName;
  }

  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  public long getStartTs() {
    return startTs;
  }

  public void setStartTs(long startTs) {
    this.startTs = startTs;
  }

  public long getEndTs() {
    return endTs;
  }

  public void setEndTs(long endTs) {
    this.endTs = endTs;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }
}
