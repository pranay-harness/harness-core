package software.wings.sm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 2/6/17.
 */
public class ServiceInstancesExecutionState implements PhaseStepExecutionState {
  private List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();

  public List<InstanceStatusSummary> getInstanceStatusSummaries() {
    return instanceStatusSummaries;
  }

  public void setInstanceStatusSummaries(List<InstanceStatusSummary> instanceStatusSummaries) {
    this.instanceStatusSummaries = instanceStatusSummaries;
  }
}
