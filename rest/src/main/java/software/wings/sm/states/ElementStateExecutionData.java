package software.wings.sm.states;

import software.wings.beans.ElementExecutionSummary;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 8/19/16.
 */
public class ElementStateExecutionData extends StateExecutionData {
  private List<ElementExecutionSummary> elementStatusSummary = new ArrayList<>();
  private Map<String, ExecutionStatus> instanceIdStatusMap = new HashMap<>();

  /**
   * Gets element status summary.
   *
   * @return the element status summary
   */
  public List<ElementExecutionSummary> getElementStatusSummary() {
    return elementStatusSummary;
  }

  /**
   * Sets element status summary.
   *
   * @param elementStatusSummary the element status summary
   */
  public void setElementStatusSummary(List<ElementExecutionSummary> elementStatusSummary) {
    this.elementStatusSummary = elementStatusSummary;
  }

  public Map<String, ExecutionStatus> getInstanceIdStatusMap() {
    return instanceIdStatusMap;
  }

  public void setInstanceIdStatusMap(Map<String, ExecutionStatus> instanceIdStatusMap) {
    this.instanceIdStatusMap = instanceIdStatusMap;
  }
}
