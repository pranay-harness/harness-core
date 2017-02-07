package software.wings.sm;

import software.wings.api.InstanceElement;

/**
 * Created by rishi on 8/15/16.
 */
public class InstanceStatusSummary {
  private InstanceElement instanceElement;
  private ExecutionStatus status;

  /**
   * Gets instance element.
   *
   * @return the instance element
   */
  public InstanceElement getInstanceElement() {
    return instanceElement;
  }

  /**
   * Sets instance element.
   *
   * @param instanceElement the instance element
   */
  public void setInstanceElement(InstanceElement instanceElement) {
    this.instanceElement = instanceElement;
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
   * The type Instance status summary builder.
   */
  public static final class InstanceStatusSummaryBuilder {
    private InstanceElement instanceElement;
    private ExecutionStatus status;

    private InstanceStatusSummaryBuilder() {}

    /**
     * An instance status summary instance status summary builder.
     *
     * @return the instance status summary builder
     */
    public static InstanceStatusSummaryBuilder anInstanceStatusSummary() {
      return new InstanceStatusSummaryBuilder();
    }

    /**
     * With instance element instance status summary builder.
     *
     * @param instanceElement the instance element
     * @return the instance status summary builder
     */
    public InstanceStatusSummaryBuilder withInstanceElement(InstanceElement instanceElement) {
      this.instanceElement = instanceElement;
      return this;
    }

    /**
     * With status instance status summary builder.
     *
     * @param status the status
     * @return the instance status summary builder
     */
    public InstanceStatusSummaryBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * Build instance status summary.
     *
     * @return the instance status summary
     */
    public InstanceStatusSummary build() {
      InstanceStatusSummary instanceStatusSummary = new InstanceStatusSummary();
      instanceStatusSummary.setInstanceElement(instanceElement);
      instanceStatusSummary.setStatus(status);
      return instanceStatusSummary;
    }
  }
}
