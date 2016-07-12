package software.wings.sm;

import software.wings.beans.CountsByStatuses;

import java.util.LinkedHashMap;

/**
 * Represents state machine execution data.
 *
 * @author Rishi
 */
public class StateExecutionData {
  private String stateName;
  private Long startTs;
  private Long endTs;
  private ExecutionStatus status;
  private String errorMsg;

  /**
   * Gets state name.
   *
   * @return the state name
   */
  public String getStateName() {
    return stateName;
  }

  /**
   * Sets state name.
   *
   * @param stateName the state name
   */
  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  /**
   * Gets start ts.
   *
   * @return the start ts
   */
  public Long getStartTs() {
    return startTs;
  }

  /**
   * Sets start ts.
   *
   * @param startTs the start ts
   */
  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  /**
   * Gets end ts.
   *
   * @return the end ts
   */
  public Long getEndTs() {
    return endTs;
  }

  /**
   * Sets end ts.
   *
   * @param endTs the end ts
   */
  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public ExecutionStatus getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  /**
   * Gets error msg.
   *
   * @return the error msg
   */
  public String getErrorMsg() {
    return errorMsg;
  }

  /**
   * Sets error msg.
   *
   * @param errorMsg the error msg
   */
  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  /**
   * Gets execution summary.
   *
   * @return the execution summary
   */
  public Object getExecutionSummary() {
    return fillExecutionData();
  }

  public Object getExecutionSummaryWithData() {
    return fillExecutionData();
  }
  /**
   * Gets execution details.
   *
   * @return the execution details
   */
  public Object getExecutionDetails() {
    return fillExecutionData();
  }

  private LinkedHashMap<String, Object> fillExecutionData() {
    LinkedHashMap<String, Object> orderedMap = new LinkedHashMap<>();
    putNotNull(orderedMap, "errorMsg", errorMsg);
    putNotNull(orderedMap, "startTs", startTs);
    putNotNull(orderedMap, "endTs", endTs);
    putNotNull(orderedMap, "total", 1);
    CountsByStatuses breakDown = new CountsByStatuses();
    switch (status) {
      case FAILED:
        breakDown.setFailed(1);
        break;
      case RUNNING:
        breakDown.setInprogress(1);
        break;
      case SUCCESS:
        breakDown.setSuccess(1);
        break;
    }
    putNotNull(orderedMap, "breakdown", breakDown);
    return orderedMap;
  }

  /**
   * Put not null.
   *
   * @param orderedMap the ordered map
   * @param name       the name
   * @param value      the value
   */
  protected void putNotNull(LinkedHashMap<String, Object> orderedMap, String name, Object value) {
    if (value != null) {
      orderedMap.put(name, value);
    }
  }
}
