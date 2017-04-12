package software.wings.api;

import static software.wings.sm.ContextElementType.CONTAINER_SERVICE;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.Map;

/**
 * Created by rishi on 4/11/17.
 */
public class ContainerServiceElement implements ContextElement {
  private String uuid;
  private String name;
  private String oldName;
  private String clusterName;
  private DeploymentType deploymentType;
  private String infraMappingId;

  @Override
  public ContextElementType getElementType() {
    return CONTAINER_SERVICE;
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

  public DeploymentType getDeploymentType() {
    return deploymentType;
  }

  public void setDeploymentType(DeploymentType deploymentType) {
    this.deploymentType = deploymentType;
  }

  public String getInfraMappingId() {
    return infraMappingId;
  }

  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  public static final class ContainerServiceElementBuilder {
    private String uuid;
    private String name;
    private String oldName;
    private String clusterName;
    private DeploymentType deploymentType;
    private String infraMappingId;

    private ContainerServiceElementBuilder() {}

    public static ContainerServiceElementBuilder aContainerServiceElement() {
      return new ContainerServiceElementBuilder();
    }

    public ContainerServiceElementBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public ContainerServiceElementBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ContainerServiceElementBuilder withOldName(String oldName) {
      this.oldName = oldName;
      return this;
    }

    public ContainerServiceElementBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public ContainerServiceElementBuilder withDeploymentType(DeploymentType deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public ContainerServiceElementBuilder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public ContainerServiceElement build() {
      ContainerServiceElement containerServiceElement = new ContainerServiceElement();
      containerServiceElement.setUuid(uuid);
      containerServiceElement.setName(name);
      containerServiceElement.setOldName(oldName);
      containerServiceElement.setClusterName(clusterName);
      containerServiceElement.setDeploymentType(deploymentType);
      containerServiceElement.setInfraMappingId(infraMappingId);
      return containerServiceElement;
    }
  }
}
