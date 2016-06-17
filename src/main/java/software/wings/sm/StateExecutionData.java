package software.wings.sm;

import java.io.Serializable;
import java.util.LinkedHashMap;

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

  public Object getExecutionSummary() {
    return fillExecutionData();
  }

  public Object getExecutionDetails() {
    return fillExecutionData();
  }

  private LinkedHashMap<String, Object> fillExecutionData() {
    LinkedHashMap<String, Object> orderedMap = new LinkedHashMap<>();
    orderedMap.put("status", status);
    putNotNull(orderedMap, "startTs", startTs);
    putNotNull(orderedMap, "endTs", endTs);
    return orderedMap;
  }

  protected void putNotNull(LinkedHashMap<String, Object> orderedMap, String name, Object value) {
    if (value != null) {
      orderedMap.put(name, value);
    }
  }
}
