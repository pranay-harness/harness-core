package software.wings.beans.stats;

import com.google.common.base.MoreObjects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Id;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopConsumer {
  @Id private String appId;
  private String appName;
  private String serviceId;
  private String serviceName;
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
   * Get Service id
   * @return
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Set Service Id
   * @param serviceId
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Get Service Name
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Set Service Name
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
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
        .add("serviceId", serviceId)
        .add("successfulActivityCount", successfulActivityCount)
        .add("serviceName", serviceName)
        .add("failedActivityCount", failedActivityCount)
        .add("totalCount", totalCount)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        appId, appName, successfulActivityCount, failedActivityCount, totalCount, serviceId, serviceName);
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
        && Objects.equals(this.totalCount, other.totalCount) && Objects.equals(this.serviceId, this.serviceId)
        && Objects.equals(this.serviceName, this.serviceName);
  }
}
