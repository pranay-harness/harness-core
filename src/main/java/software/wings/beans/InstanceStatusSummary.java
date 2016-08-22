package software.wings.beans;

import software.wings.api.InstanceElement;
import software.wings.sm.ExecutionStatus;

/**
 * Created by rishi on 8/15/16.
 */
public class InstanceStatusSummary {
  private InstanceElement instanceElement;
  private ExecutionStatus status;

  public InstanceElement getInstanceElement() {
    return instanceElement;
  }

  public void setInstanceElement(InstanceElement instanceElement) {
    this.instanceElement = instanceElement;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  public static final class InstanceStatusSummaryBuilder {
    private InstanceElement instanceElement;
    private ExecutionStatus status;

    private InstanceStatusSummaryBuilder() {}

    public static InstanceStatusSummaryBuilder anInstanceStatusSummary() {
      return new InstanceStatusSummaryBuilder();
    }

    public InstanceStatusSummaryBuilder withInstanceElement(InstanceElement instanceElement) {
      this.instanceElement = instanceElement;
      return this;
    }

    public InstanceStatusSummaryBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public InstanceStatusSummary build() {
      InstanceStatusSummary instanceStatusSummary = new InstanceStatusSummary();
      instanceStatusSummary.setInstanceElement(instanceElement);
      instanceStatusSummary.setStatus(status);
      return instanceStatusSummary;
    }
  }
}
