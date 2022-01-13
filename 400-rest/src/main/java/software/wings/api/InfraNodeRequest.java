/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import software.wings.beans.InstanceUnitType;

import java.util.List;

/**
 * Created by rishi on 5/25/17.
 */
public class InfraNodeRequest {
  private DeploymentType deploymentType;
  private String instanceCount;
  private List<String> nodeNames;
  private PhaseElement phaseElement;
  private InstanceUnitType instanceUnitType;

  public String getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(String instanceCount) {
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
        + "deploymentType=" + deploymentType + ", instanceCount=" + instanceCount + ", nodeNames=" + nodeNames
        + ", phaseElement=" + phaseElement + '}';
  }

  public static final class InfraNodeRequestBuilder {
    private DeploymentType deploymentType;
    private String instanceCount;
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

    public InfraNodeRequestBuilder withInstanceCount(String instanceCount) {
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
      infraNodeRequest.setInstanceCount(instanceCount);
      infraNodeRequest.setNodeNames(nodeNames);
      infraNodeRequest.setPhaseElement(phaseElement);
      infraNodeRequest.deploymentType = this.deploymentType;
      infraNodeRequest.setInstanceUnitType(instanceUnitType);
      return infraNodeRequest;
    }
  }
}
