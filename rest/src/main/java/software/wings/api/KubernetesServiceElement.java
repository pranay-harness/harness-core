package software.wings.api;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.Map;

import static software.wings.sm.ContextElementType.ECS_SERVICE;

/**
 * Created by brett on 3/1/17
 * TODO(brett): Implement
 */
public class KubernetesServiceElement implements ContextElement {
  private String uuid;
  private String name;
  private String oldName;
  private String clusterName;

  @Override
  public ContextElementType getElementType() {
    return ECS_SERVICE;
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

  public static final class KubernetesServiceElementBuilder {
    private String uuid;
    private String name;
    private String oldName;
    private String clusterName;

    private KubernetesServiceElementBuilder() {}

    public static KubernetesServiceElementBuilder aKubernetesServiceElement() {
      return new KubernetesServiceElementBuilder();
    }

    public KubernetesServiceElementBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public KubernetesServiceElementBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public KubernetesServiceElementBuilder withOldName(String oldName) {
      this.oldName = oldName;
      return this;
    }

    public KubernetesServiceElementBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public KubernetesServiceElement build() {
      KubernetesServiceElement kubernetesServiceElement = new KubernetesServiceElement();
      kubernetesServiceElement.setUuid(uuid);
      kubernetesServiceElement.setName(name);
      kubernetesServiceElement.setOldName(oldName);
      kubernetesServiceElement.setClusterName(clusterName);
      return kubernetesServiceElement;
    }
  }
}
