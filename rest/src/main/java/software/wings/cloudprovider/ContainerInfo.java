package software.wings.cloudprovider;

/**
 * Created by brett on 4/6/17
 */
public class ContainerInfo {
  private String hostName;
  private String containerId;

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
}
