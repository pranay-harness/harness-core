package software.wings.beans.stats;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Id;

/**
 * Created by anubhaw on 8/16/16.
 */
public class TopConsumer {
  @Id private String appId;
  private String appName;
  private Integer activityCount;

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
   * Gets activity count.
   *
   * @return the activity count
   */
  public Integer getActivityCount() {
    return activityCount;
  }

  /**
   * Sets activity count.
   *
   * @param activityCount the activity count
   */
  public void setActivityCount(Integer activityCount) {
    this.activityCount = activityCount;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("appName", appName)
        .add("appId", appId)
        .add("activityCount", activityCount)
        .toString();
  }
}
