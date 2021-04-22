package io.harness.instancesync.dto.instanceinfo;

public abstract class ContainerInfo extends InstanceInfo {
  private String clusterName;

  public ContainerInfo(String clusterName) {
    this.clusterName = clusterName;
  }
}
