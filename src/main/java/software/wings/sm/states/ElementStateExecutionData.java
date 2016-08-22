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

  public List<ElementExecutionSummary> getElementStatusSummary() {
    return elementStatusSummary;
  }

  public void setElementStatusSummary(List<ElementExecutionSummary> elementStatusSummary) {
    this.elementStatusSummary = elementStatusSummary;
  }

  public List<InstanceStatusSummary> getInstanceStatusSummary() {
    return instanceStatusSummary;
  }

  public void setInstanceStatusSummary(List<InstanceStatusSummary> instanceStatusSummary) {
    this.instanceStatusSummary = instanceStatusSummary;
  }
}
