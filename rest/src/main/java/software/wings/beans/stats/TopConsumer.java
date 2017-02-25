package software.wings.beans.stats;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Id;

import java.util.Objects;

/**
 * Created by anubhaw on 8/16/16.
 */
public class TopConsumer {
  @Id private String appId;
  private String appName;
  private int successfulActivityCount;
  private int failedActivityCount;
  private int totalCount;

  /**
   * Gets app name.
   *
   * @return the app name
   */
  public String getAppName() {
    return appName;
  }

  /**
   * Sets app name.
   *
   * @param appName the app name
   */
  public void setAppName(String appName) {
    this.appName = appName;
  }

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
   * Gets successful activity count.
   *
   * @return the successful activity count
   */
  public int getSuccessfulActivityCount() {
    return successfulActivityCount;
  }

  /**
   * Sets successful activity count.
   *
   * @param successfulActivityCount the successful activity count
   */
  public void setSuccessfulActivityCount(int successfulActivityCount) {
    this.successfulActivityCount = successfulActivityCount;
  }

  /**
   * Gets failed activity count.
   *
   * @return the failed activity count
   */
  public int getFailedActivityCount() {
    return failedActivityCount;
  }

  /**
   * Sets failed activity count.
   *
   * @param failedActivityCount the failed activity count
   */
  public void setFailedActivityCount(int failedActivityCount) {
    this.failedActivityCount = failedActivityCount;
  }

  /**
   * Gets total count.
   *
   * @return the total count
   */
  public int getTotalCount() {
    return totalCount;
  }

  /**
   * Sets total count.
   *
   * @param totalCount the total count
   */
  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("appId", appId)
        .add("appName", appName)
        .add("successfulActivityCount", successfulActivityCount)
        .add("failedActivityCount", failedActivityCount)
        .add("totalCount", totalCount)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(appId, appName, successfulActivityCount, failedActivityCount, totalCount);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final TopConsumer other = (TopConsumer) obj;
    return Objects.equals(this.appId, other.appId) && Objects.equals(this.appName, other.appName)
        && Objects.equals(this.successfulActivityCount, other.successfulActivityCount)
        && Objects.equals(this.failedActivityCount, other.failedActivityCount)
        && Objects.equals(this.totalCount, other.totalCount);
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String appId;
    private String appName;
    private int successfulActivityCount;
    private int failedActivityCount;
    private int totalCount;

    private Builder() {}

    /**
     * A top consumer builder.
     *
     * @return the builder
     */
    public static Builder aTopConsumer() {
      return new Builder();
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With app name builder.
     *
     * @param appName the app name
     * @return the builder
     */
    public Builder withAppName(String appName) {
      this.appName = appName;
      return this;
    }

    /**
     * With successful activity count builder.
     *
     * @param successfulActivityCount the successful activity count
     * @return the builder
     */
    public Builder withSuccessfulActivityCount(int successfulActivityCount) {
      this.successfulActivityCount = successfulActivityCount;
      return this;
    }

    /**
     * With failed activity count builder.
     *
     * @param failedActivityCount the failed activity count
     * @return the builder
     */
    public Builder withFailedActivityCount(int failedActivityCount) {
      this.failedActivityCount = failedActivityCount;
      return this;
    }

    /**
     * With total count builder.
     *
     * @param totalCount the total count
     * @return the builder
     */
    public Builder withTotalCount(int totalCount) {
      this.totalCount = totalCount;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aTopConsumer()
          .withAppId(appId)
          .withAppName(appName)
          .withSuccessfulActivityCount(successfulActivityCount)
          .withFailedActivityCount(failedActivityCount)
          .withTotalCount(totalCount);
    }

    /**
     * Build top consumer.
     *
     * @return the top consumer
     */
    public TopConsumer build() {
      TopConsumer topConsumer = new TopConsumer();
      topConsumer.setAppId(appId);
      topConsumer.setAppName(appName);
      topConsumer.setSuccessfulActivityCount(successfulActivityCount);
      topConsumer.setFailedActivityCount(failedActivityCount);
      topConsumer.setTotalCount(totalCount);
      return topConsumer;
    }
  }
}
