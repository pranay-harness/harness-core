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

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public Integer getActivityCount() {
    return activityCount;
  }

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
