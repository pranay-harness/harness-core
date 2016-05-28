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
  private Long startTs;
  private Long endTs;
  private ExecutionStatus status;

  public String getStateName() {
    return stateName;
  }

  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  public Long getStartTs() {
    return startTs;
  }

  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  public Long getEndTs() {
    return endTs;
  }

  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }
}
