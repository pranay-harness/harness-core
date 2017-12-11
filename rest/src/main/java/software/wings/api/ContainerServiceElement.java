package software.wings.api;

import static software.wings.sm.ContextElementType.CONTAINER_SERVICE;

import lombok.Data;
import software.wings.beans.ResizeStrategy;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.Map;

/**
 * Created by rishi on 4/11/17.
 */
@Data
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
  private String kubernetesType;

  @Override
  public ContextElementType getElementType() {
    return CONTAINER_SERVICE;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  public static final class ContainerServiceElementBuilder {
    private String uuid;
    private String name;
    private int maxInstances;
    private int serviceSteadyStateTimeout;
    private ResizeStrategy resizeStrategy;
    private String clusterName;
    private String namespace;
    private DeploymentType deploymentType;
    private String infraMappingId;
    private String kubernetesType;

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

    public ContainerServiceElementBuilder withMaxInstances(int maxInstances) {
      this.maxInstances = maxInstances;
      return this;
    }

    public ContainerServiceElementBuilder withServiceSteadyStateTimeout(int serviceSteadyStateTimeout) {
      this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
      return this;
    }

    public ContainerServiceElementBuilder withResizeStrategy(ResizeStrategy resizeStrategy) {
      this.resizeStrategy = resizeStrategy;
      return this;
    }

    public ContainerServiceElementBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public ContainerServiceElementBuilder withNamespace(String namespace) {
      this.namespace = namespace;
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

    public ContainerServiceElementBuilder withKubernetesType(String kubernetesType) {
      this.kubernetesType = kubernetesType;
      return this;
    }

    public ContainerServiceElementBuilder but() {
      return aContainerServiceElement()
          .withUuid(uuid)
          .withName(name)
          .withMaxInstances(maxInstances)
          .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
          .withResizeStrategy(resizeStrategy)
          .withClusterName(clusterName)
          .withNamespace(namespace)
          .withDeploymentType(deploymentType)
          .withInfraMappingId(infraMappingId)
          .withKubernetesType(kubernetesType);
    }

    public ContainerServiceElement build() {
      ContainerServiceElement containerServiceElement = new ContainerServiceElement();
      containerServiceElement.setUuid(uuid);
      containerServiceElement.setName(name);
      containerServiceElement.setMaxInstances(maxInstances);
      containerServiceElement.setServiceSteadyStateTimeout(serviceSteadyStateTimeout);
      containerServiceElement.setResizeStrategy(resizeStrategy);
      containerServiceElement.setClusterName(clusterName);
      containerServiceElement.setNamespace(namespace);
      containerServiceElement.setDeploymentType(deploymentType);
      containerServiceElement.setInfraMappingId(infraMappingId);
      containerServiceElement.setKubernetesType(kubernetesType);
      return containerServiceElement;
    }
  }
}
