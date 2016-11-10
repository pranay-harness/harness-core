package software.wings.beans.infrastructure;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Id;

import java.util.Objects;

/**
 * Created by anubhaw on 9/16/16.
 */
public class ApplicationHostUsage {
  @Id private String appId;
  private String appName;
  private int count;

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
  public int hashCode() {
    return Objects.hash(appId, appName, count);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ApplicationHostUsage other = (ApplicationHostUsage) obj;
    return Objects.equals(this.appId, other.appId) && Objects.equals(this.appName, other.appName)
        && Objects.equals(this.count, other.count);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("appId", appId).add("appName", appName).add("count", count).toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String appId;
    private String appName;
    private int count;

    private Builder() {}

    /**
     * An application host usage builder.
     *
     * @return the builder
     */
    public static Builder anApplicationHostUsage() {
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
     * With count builder.
     *
     * @param count the count
     * @return the builder
     */
    public Builder withCount(int count) {
      this.count = count;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anApplicationHostUsage().withAppId(appId).withAppName(appName).withCount(count);
    }

    /**
     * Build application host usage.
     *
     * @return the application host usage
     */
    public ApplicationHostUsage build() {
      ApplicationHostUsage applicationHostUsage = new ApplicationHostUsage();
      applicationHostUsage.setAppId(appId);
      applicationHostUsage.setAppName(appName);
      applicationHostUsage.setCount(count);
      return applicationHostUsage;
    }
  }
}
