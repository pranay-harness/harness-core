package software.wings.collect;

import software.wings.beans.AppDynamicsConfig;

/**
 * Created by rsingh on 5/18/17.
 */
public class AppdynamicsDataCollectionInfo {
  private AppDynamicsConfig appDynamicsConfig;
  private long appId;
  private long tierId;
  private int collectionTime;

  public AppdynamicsDataCollectionInfo() {}

  public AppdynamicsDataCollectionInfo(
      AppDynamicsConfig appDynamicsConfig, long appId, long tierId, int collectionTime) {
    this.appDynamicsConfig = appDynamicsConfig;
    this.appId = appId;
    this.tierId = tierId;
    this.collectionTime = collectionTime;
  }

  public AppDynamicsConfig getAppDynamicsConfig() {
    return appDynamicsConfig;
  }

  public void setAppDynamicsConfig(AppDynamicsConfig appDynamicsConfig) {
    this.appDynamicsConfig = appDynamicsConfig;
  }

  public long getAppId() {
    return appId;
  }

  public void setAppId(long appId) {
    this.appId = appId;
  }

  public long getTierId() {
    return tierId;
  }

  public void setTierId(long tierId) {
    this.tierId = tierId;
  }

  public int getCollectionTime() {
    return collectionTime;
  }

  public void setCollectionTime(int collectionTime) {
    this.collectionTime = collectionTime;
  }

  @Override
  public String toString() {
    return "AppdynamicsDataCollectionInfo{"
        + "appDynamicsConfig=" + appDynamicsConfig + ", appId=" + appId + ", tierId=" + tierId
        + ", collectionTime=" + collectionTime + '}';
  }
}
