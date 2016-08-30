package software.wings.sm.states;

import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 8/19/16.
 */
public class ElementStateExecutionData extends StateExecutionData {
  private List<ElementExecutionSummary> elementStatusSummary = new ArrayList<>();
  private List<InstanceStatusSummary> instanceStatusSummary;

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

  /**
   * Gets instance status summary.
   *
   * @return the instance status summary
   */
  public List<InstanceStatusSummary> getInstanceStatusSummary() {
    return instanceStatusSummary;
  }

  /**
   * Sets instance status summary.
   *
   * @param instanceStatusSummary the instance status summary
   */
  public void setInstanceStatusSummary(List<InstanceStatusSummary> instanceStatusSummary) {
    this.instanceStatusSummary = instanceStatusSummary;
  }
}
