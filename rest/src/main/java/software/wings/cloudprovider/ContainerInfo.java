package software.wings.cloudprovider;

/**
 * Created by brett on 4/6/17
 */
public class ContainerInfo {
  public enum Status { SUCCESS, FAILURE }

  private String hostName;
  private String containerId;
  private Status status;

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

  public String getContainerId() {
    return containerId;
  }

  public Status getStatus() {
    return status;
  }
}
