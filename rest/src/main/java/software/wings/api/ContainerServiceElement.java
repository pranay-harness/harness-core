package software.wings.api;

import static software.wings.sm.ContextElementType.CONTAINER_SERVICE;

import software.wings.beans.ResizeStrategy;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.Map;

/**
 * Created by rishi on 4/11/17.
 */
public class ContainerServiceElement implements ContextElement {
  private String uuid;
  private String name;
  private int maxInstances;
  private int serviceSteadyStateTimeout;
  private ResizeStrategy resizeStrategy;
  private String clusterName;
  private String namespace;
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

  public int getMaxInstances() {
    return maxInstances;
  }

  public void setMaxInstances(int maxInstances) {
    this.maxInstances = maxInstances;
  }

  public ResizeStrategy getResizeStrategy() {
    return resizeStrategy;
  }

  public void setResizeStrategy(ResizeStrategy resizeStrategy) {
    this.resizeStrategy = resizeStrategy;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
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

  public int getServiceSteadyStateTimeout() {
    return serviceSteadyStateTimeout;
  }

  public void setServiceSteadyStateTimeout(int serviceSteadyStateTimeout) {
    this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  public static final class ContainerServiceElementBuilder {
    private ContainerServiceElement containerServiceElement;

    private ContainerServiceElementBuilder() {
      containerServiceElement = new ContainerServiceElement();
    }

    public static ContainerServiceElementBuilder aContainerServiceElement() {
      return new ContainerServiceElementBuilder();
    }

    public ContainerServiceElementBuilder withUuid(String uuid) {
      containerServiceElement.setUuid(uuid);
      return this;
    }

    public ContainerServiceElementBuilder withName(String name) {
      containerServiceElement.setName(name);
      return this;
    }

    public ContainerServiceElementBuilder withMaxInstances(int maxInstances) {
      containerServiceElement.setMaxInstances(maxInstances);
      return this;
    }

    public ContainerServiceElementBuilder withServiceSteadyStateTimeout(int serviceSteadyStateTimeout) {
      containerServiceElement.setServiceSteadyStateTimeout(serviceSteadyStateTimeout);
      return this;
    }

    public ContainerServiceElementBuilder withResizeStrategy(ResizeStrategy resizeStrategy) {
      containerServiceElement.setResizeStrategy(resizeStrategy);
      return this;
    }

    public ContainerServiceElementBuilder withClusterName(String clusterName) {
      containerServiceElement.setClusterName(clusterName);
      return this;
    }

    public ContainerServiceElementBuilder withNamespace(String namespace) {
      containerServiceElement.setNamespace(namespace);
      return this;
    }

    public ContainerServiceElementBuilder withDeploymentType(DeploymentType deploymentType) {
      containerServiceElement.setDeploymentType(deploymentType);
      return this;
    }

    public ContainerServiceElementBuilder withInfraMappingId(String infraMappingId) {
      containerServiceElement.setInfraMappingId(infraMappingId);
      return this;
    }

    public ContainerServiceElementBuilder but() {
      return aContainerServiceElement()
          .withUuid(containerServiceElement.getUuid())
          .withName(containerServiceElement.getName())
          .withMaxInstances(containerServiceElement.getMaxInstances())
          .withServiceSteadyStateTimeout(containerServiceElement.getServiceSteadyStateTimeout())
          .withResizeStrategy(containerServiceElement.getResizeStrategy())
          .withClusterName(containerServiceElement.getClusterName())
          .withNamespace(containerServiceElement.getNamespace())
          .withDeploymentType(containerServiceElement.getDeploymentType())
          .withInfraMappingId(containerServiceElement.getInfraMappingId());
    }

    public ContainerServiceElement build() {
      return containerServiceElement;
    }
  }
}
