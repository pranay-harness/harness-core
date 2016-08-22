package software.wings.beans.stats;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Id;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Status;

import java.util.List;

/**
 * Created by anubhaw on 8/20/16.
 */
public class ActivityStatusAggregation {
  @Id private String appId;
  private List<StatusCount> status;

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public List<StatusCount> getStatus() {
    return status;
  }

  public void setStatus(List<StatusCount> status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("appId", appId).add("status", status).toString();
  }

  public static class StatusCount {
    private Activity.Status status;
    private int count;

    public Status getStatus() {
      return status;
    }

    public void setStatus(Status status) {
      this.status = status;
    }

    public int getCount() {
      return count;
    }

    public void setCount(int count) {
      this.count = count;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("status", status).add("count", count).toString();
    }
  }
}
