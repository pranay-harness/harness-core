package software.wings.cloudprovider;

import com.google.common.base.MoreObjects;

/**
 * Created by brett on 4/6/17
 */
public class ContainerInfo {
  public enum Status { SUCCESS, FAILURE }

  private String hostName;
  private String containerId;
  private Status status;

  public ContainerInfo() {}

  public ContainerInfo(String containerId, Status status) {
    this.hostName = containerId;
    this.containerId = containerId;
    this.status = status;
  }

  public ContainerInfo(String hostName, String containerId, Status status) {
    this.hostName = hostName;
    this.containerId = containerId;
    this.status = status;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getContainerId() {
    return containerId;
  }

  public void setContainerId(String containerId) {
    this.containerId = containerId;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("hostName", hostName)
        .add("containerId", containerId)
        .add("status", status)
        .toString();
  }
}
