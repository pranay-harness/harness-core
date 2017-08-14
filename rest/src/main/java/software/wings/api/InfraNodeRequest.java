package software.wings.api;

import software.wings.beans.InstanceUnitType;

import java.util.List;

/**
 * Created by rishi on 5/25/17.
 */
public class InfraNodeRequest {
  private DeploymentType deploymentType;
  private boolean provisionNodes;
  private int instanceCount;
  private List<String> nodeNames;
  private PhaseElement phaseElement;
  private InstanceUnitType instanceUnitType;

  public boolean isProvisionNodes() {
    return provisionNodes;
  }

  public void setProvisionNodes(boolean provisionNodes) {
    this.provisionNodes = provisionNodes;
  }

  public int getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  public List<String> getNodeNames() {
    return nodeNames;
  }

  public void setNodeNames(List<String> nodeNames) {
    this.nodeNames = nodeNames;
  }

  public PhaseElement getPhaseElement() {
    return phaseElement;
  }

  public void setPhaseElement(PhaseElement phaseElement) {
    this.phaseElement = phaseElement;
  }

  public InstanceUnitType getInstanceUnitType() {
    return instanceUnitType;
  }

  public void setInstanceUnitType(InstanceUnitType instanceUnitType) {
    this.instanceUnitType = instanceUnitType;
  }

  @Override
  public String toString() {
    return "InfraNodeRequest{"
        + "deploymentType=" + deploymentType + ", provisionNodes=" + provisionNodes + ", instanceCount=" + instanceCount
        + ", nodeNames=" + nodeNames + ", phaseElement=" + phaseElement + '}';
  }

  public static final class InfraNodeRequestBuilder {
    private DeploymentType deploymentType;
    private boolean provisionNodes;
    private int instanceCount;
    private List<String> nodeNames;
    private PhaseElement phaseElement;
    private InstanceUnitType instanceUnitType;

    private InfraNodeRequestBuilder() {}

    public static InfraNodeRequestBuilder anInfraNodeRequest() {
      return new InfraNodeRequestBuilder();
    }

    public InfraNodeRequestBuilder withDeploymentType(DeploymentType deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public InfraNodeRequestBuilder withProvisionNodes(boolean provisionNodes) {
      this.provisionNodes = provisionNodes;
      return this;
    }

    public InfraNodeRequestBuilder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public InfraNodeRequestBuilder withNodeNames(List<String> nodeNames) {
      this.nodeNames = nodeNames;
      return this;
    }

    public InfraNodeRequestBuilder withPhaseElement(PhaseElement phaseElement) {
      this.phaseElement = phaseElement;
      return this;
    }

    public InfraNodeRequestBuilder withInstanceUnitType(InstanceUnitType instanceUnitType) {
      this.instanceUnitType = instanceUnitType;
      return this;
    }

    public InfraNodeRequest build() {
      InfraNodeRequest infraNodeRequest = new InfraNodeRequest();
      infraNodeRequest.setProvisionNodes(provisionNodes);
      infraNodeRequest.setInstanceCount(instanceCount);
      infraNodeRequest.setNodeNames(nodeNames);
      infraNodeRequest.setPhaseElement(phaseElement);
      infraNodeRequest.deploymentType = this.deploymentType;
      infraNodeRequest.setInstanceUnitType(instanceUnitType);
      return infraNodeRequest;
    }
  }
}
