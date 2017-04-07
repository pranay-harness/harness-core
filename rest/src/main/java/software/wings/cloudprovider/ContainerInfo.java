package software.wings.cloudprovider;

import com.google.common.base.MoreObjects;

/**
 * Created by brett on 4/6/17
 */
public class ContainerInfo {
  private String hostName;
  private String containerId;

  public ContainerInfo() {}

  public ContainerInfo(String containerId) {
    this.hostName = containerId;
    this.containerId = containerId;
  }

  public ContainerInfo(String hostName, String containerId) {
    this.hostName = hostName;
    this.containerId = containerId;
  }

  public String getHostName() {
    return hostName;
  }

  public String getContainerId() {
    return containerId;
  }

  /**
   * Setter for property 'hostName'.
   *
   * @param hostName Value to set for property 'hostName'.
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Setter for property 'containerId'.
   *
   * @param containerId Value to set for property 'containerId'.
   */
  public void setContainerId(String containerId) {
    this.containerId = containerId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("hostName", hostName).add("containerId", containerId).toString();
  }
}
