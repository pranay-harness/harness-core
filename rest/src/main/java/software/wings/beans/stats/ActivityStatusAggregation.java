package software.wings.beans.stats;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Id;
import software.wings.sm.ExecutionStatus;

import java.util.List;

/**
 * Created by anubhaw on 8/20/16.
 */
public class ActivityStatusAggregation {
  @Id private String appId;
  private List<StatusCount> status;

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public List<StatusCount> getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(List<StatusCount> status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("appId", appId).add("status", status).toString();
  }

  /**
   * The type Status count.
   */
  public static class StatusCount {
    private ExecutionStatus status;
    private int count;

    public StatusCount(ExecutionStatus status, int count) {
      this.status = status;
      this.count = count;
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
     * Gets count.
     *
     * @return the count
     */
    public int getCount() {
      return count;
    }

    /**
     * Sets count.
     *
     * @param count the count
     */
    public void setCount(int count) {
      this.count = count;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("status", status).add("count", count).toString();
    }
  }

  public static final class Builder {
    private String appId;
    private List<StatusCount> status;

    private Builder() {}

    public static Builder anActivityStatusAggregation() {
      return new Builder();
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withStatus(List<StatusCount> status) {
      this.status = status;
      return this;
    }

    public Builder but() {
      return anActivityStatusAggregation().withAppId(appId).withStatus(status);
    }

    public ActivityStatusAggregation build() {
      ActivityStatusAggregation activityStatusAggregation = new ActivityStatusAggregation();
      activityStatusAggregation.setAppId(appId);
      activityStatusAggregation.setStatus(status);
      return activityStatusAggregation;
    }
  }
}
