package software.wings.beans;

import software.wings.sm.ExecutionStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 8/20/16.
 */
public class StatusInstanceBreakdown {
  private ExecutionStatus status;
  private int instanceCount;
  private List<InstanceExecutionHistory> instanceExecutionHistories = new ArrayList<>();

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
   * Gets instance count.
   *
   * @return the instance count
   */
  public int getInstanceCount() {
    return instanceCount;
  }

  /**
   * Sets instance count.
   *
   * @param instanceCount the instance count
   */
  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  /**
   * Gets instance execution histories.
   *
   * @return the instance execution histories
   */
  public List<InstanceExecutionHistory> getInstanceExecutionHistories() {
    return instanceExecutionHistories;
  }

  /**
   * Sets instance execution histories.
   *
   * @param instanceExecutionHistories the instance execution histories
   */
  public void setInstanceExecutionHistories(List<InstanceExecutionHistory> instanceExecutionHistories) {
    this.instanceExecutionHistories = instanceExecutionHistories;
  }

  /**
   * The type Status instance breakdown builder.
   */
  public static final class StatusInstanceBreakdownBuilder {
    /**
     * The Instance execution histories.
     */
    List<InstanceExecutionHistory> instanceExecutionHistories = new ArrayList<>();
    private ExecutionStatus status;
    private int instanceCount;

    private StatusInstanceBreakdownBuilder() {}

    /**
     * A status instance breakdown status instance breakdown builder.
     *
     * @return the status instance breakdown builder
     */
    public static StatusInstanceBreakdownBuilder aStatusInstanceBreakdown() {
      return new StatusInstanceBreakdownBuilder();
    }

    /**
     * With status status instance breakdown builder.
     *
     * @param status the status
     * @return the status instance breakdown builder
     */
    public StatusInstanceBreakdownBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With instance count status instance breakdown builder.
     *
     * @param instanceCount the instance count
     * @return the status instance breakdown builder
     */
    public StatusInstanceBreakdownBuilder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    /**
     * With instance execution histories status instance breakdown builder.
     *
     * @param instanceExecutionHistories the instance execution histories
     * @return the status instance breakdown builder
     */
    public StatusInstanceBreakdownBuilder withInstanceExecutionHistories(
        List<InstanceExecutionHistory> instanceExecutionHistories) {
      this.instanceExecutionHistories = instanceExecutionHistories;
      return this;
    }

    /**
     * Build status instance breakdown.
     *
     * @return the status instance breakdown
     */
    public StatusInstanceBreakdown build() {
      StatusInstanceBreakdown statusInstanceBreakdown = new StatusInstanceBreakdown();
      statusInstanceBreakdown.setStatus(status);
      statusInstanceBreakdown.setInstanceCount(instanceCount);
      statusInstanceBreakdown.setInstanceExecutionHistories(instanceExecutionHistories);
      return statusInstanceBreakdown;
    }
  }
}
