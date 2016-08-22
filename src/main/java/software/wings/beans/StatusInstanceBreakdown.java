package software.wings.beans;

import software.wings.sm.ExecutionStatus;

/**
 * Created by rishi on 8/20/16.
 */
public class StatusInstanceBreakdown {
  private ExecutionStatus status;
  private int intanceCount;

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  public int getIntanceCount() {
    return intanceCount;
  }

  public void setIntanceCount(int intanceCount) {
    this.intanceCount = intanceCount;
  }

  public static final class StatusInstanceBreakdownBuilder {
    private ExecutionStatus status;
    private int intanceCount;

    private StatusInstanceBreakdownBuilder() {}

    public static StatusInstanceBreakdownBuilder aStatusInstanceBreakdown() {
      return new StatusInstanceBreakdownBuilder();
    }

    public StatusInstanceBreakdownBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public StatusInstanceBreakdownBuilder withIntanceCount(int intanceCount) {
      this.intanceCount = intanceCount;
      return this;
    }

    public StatusInstanceBreakdown build() {
      StatusInstanceBreakdown statusInstanceBreakdown = new StatusInstanceBreakdown();
      statusInstanceBreakdown.setStatus(status);
      statusInstanceBreakdown.setIntanceCount(intanceCount);
      return statusInstanceBreakdown;
    }
  }
}
