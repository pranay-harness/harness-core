package software.wings.api;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.Map;

import static software.wings.sm.ContextElementType.KUBERNETES_REPLICATION_CONTROLLER;

/**
 * Created by brett on 3/1/17
 * TODO(brett): Implement
 */
public class KubernetesReplicationControllerElement implements ContextElement {
  private String uuid;
  private String name;
  private String oldName;
  private String clusterName;

  @Override
  public ContextElementType getElementType() {
    return KUBERNETES_REPLICATION_CONTROLLER;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOldName() {
    return oldName;
  }

  public void setOldName(String oldName) {
    this.oldName = oldName;
  }

  @Override
  public Map<String, Object> paramMap() {
    return null;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }
}
