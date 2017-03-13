package software.wings.api;

import static software.wings.sm.ContextElementType.KUBERNETES_REPLICATION_CONTROLLER;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.Map;

/**
 * Created by brett on 3/1/17
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

  public static final class KubernetesReplicationControllerElementBuilder {
    private String uuid;
    private String name;
    private String oldName;
    private String clusterName;

    private KubernetesReplicationControllerElementBuilder() {}

    public static KubernetesReplicationControllerElementBuilder aKubernetesReplicationControllerElement() {
      return new KubernetesReplicationControllerElementBuilder();
    }

    public KubernetesReplicationControllerElementBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public KubernetesReplicationControllerElementBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public KubernetesReplicationControllerElementBuilder withOldName(String oldName) {
      this.oldName = oldName;
      return this;
    }

    public KubernetesReplicationControllerElementBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public KubernetesReplicationControllerElementBuilder but() {
      return aKubernetesReplicationControllerElement()
          .withUuid(uuid)
          .withName(name)
          .withOldName(oldName)
          .withClusterName(clusterName);
    }

    public KubernetesReplicationControllerElement build() {
      KubernetesReplicationControllerElement kubernetesReplicationControllerElement =
          new KubernetesReplicationControllerElement();
      kubernetesReplicationControllerElement.setUuid(uuid);
      kubernetesReplicationControllerElement.setName(name);
      kubernetesReplicationControllerElement.setOldName(oldName);
      kubernetesReplicationControllerElement.setClusterName(clusterName);
      return kubernetesReplicationControllerElement;
    }
  }
}
