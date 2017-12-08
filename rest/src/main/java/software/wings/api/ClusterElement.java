package software.wings.api;

import static software.wings.sm.ContextElementType.CLUSTER;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.Map;

/**
 * Created by brett on 4/14/17
 */
public class ClusterElement implements ContextElement {
  private String uuid;
  private String name;
  private DeploymentType deploymentType;
  private String infraMappingId;

  @Override
  public ContextElementType getElementType() {
    return CLUSTER;
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
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
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

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  public static final class ClusterElementBuilder {
    private String uuid;
    private String name;
    private DeploymentType deploymentType;
    private String infraMappingId;

    private ClusterElementBuilder() {}

    public static ClusterElementBuilder aClusterElement() {
      return new ClusterElementBuilder();
    }

    public ClusterElementBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public ClusterElementBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ClusterElementBuilder withDeploymentType(DeploymentType deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public ClusterElementBuilder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public ClusterElementBuilder but() {
      return aClusterElement()
          .withUuid(uuid)
          .withName(name)
          .withDeploymentType(deploymentType)
          .withInfraMappingId(infraMappingId);
    }

    public ClusterElement build() {
      ClusterElement clusterElement = new ClusterElement();
      clusterElement.setUuid(uuid);
      clusterElement.setName(name);
      clusterElement.setDeploymentType(deploymentType);
      clusterElement.setInfraMappingId(infraMappingId);
      return clusterElement;
    }
  }
}
