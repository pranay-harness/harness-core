package software.wings.api;

import static software.wings.sm.ContextElementType.ECS_SERVICE;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.Map;

/**
 * Created by rishi on 2/10/17.
 */
public class EcsServiceElement implements ContextElement {
  private String uuid;
  private String name;
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

  public static final class EcsServiceElementBuilder {
    private String uuid;
    private String name;
    private String clusterName;

    private EcsServiceElementBuilder() {}

    public static EcsServiceElementBuilder anEcsServiceElement() {
      return new EcsServiceElementBuilder();
    }

    public EcsServiceElementBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public EcsServiceElementBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public EcsServiceElementBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public EcsServiceElement build() {
      EcsServiceElement ecsServiceElement = new EcsServiceElement();
      ecsServiceElement.setUuid(uuid);
      ecsServiceElement.setName(name);
      ecsServiceElement.setClusterName(clusterName);
      return ecsServiceElement;
    }
  }
}
