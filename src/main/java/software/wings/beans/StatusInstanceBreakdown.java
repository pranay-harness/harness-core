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

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  public int getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  public List<InstanceExecutionHistory> getInstanceExecutionHistories() {
    return instanceExecutionHistories;
  }

  public void setInstanceExecutionHistories(List<InstanceExecutionHistory> instanceExecutionHistories) {
    this.instanceExecutionHistories = instanceExecutionHistories;
  }

  public static final class StatusInstanceBreakdownBuilder {
    List<InstanceExecutionHistory> instanceExecutionHistories = new ArrayList<>();
    private ExecutionStatus status;
    private int instanceCount;

    private StatusInstanceBreakdownBuilder() {}

    public static StatusInstanceBreakdownBuilder aStatusInstanceBreakdown() {
      return new StatusInstanceBreakdownBuilder();
    }

    public StatusInstanceBreakdownBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public StatusInstanceBreakdownBuilder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public StatusInstanceBreakdownBuilder withInstanceExecutionHistories(
        List<InstanceExecutionHistory> instanceExecutionHistories) {
      this.instanceExecutionHistories = instanceExecutionHistories;
      return this;
    }

    public StatusInstanceBreakdown build() {
      StatusInstanceBreakdown statusInstanceBreakdown = new StatusInstanceBreakdown();
      statusInstanceBreakdown.setStatus(status);
      statusInstanceBreakdown.setInstanceCount(instanceCount);
      statusInstanceBreakdown.setInstanceExecutionHistories(instanceExecutionHistories);
      return statusInstanceBreakdown;
    }
  }
}
