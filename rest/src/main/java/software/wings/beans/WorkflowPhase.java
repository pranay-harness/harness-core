package software.wings.beans;

import static software.wings.beans.Graph.Node.Builder.aNode;

import software.wings.api.DeploymentType;
import software.wings.beans.Graph.Node;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by rishi on 12/21/16.
 */
public class WorkflowPhase {
  private String uuid = UUIDGenerator.getUuid();
  private String name;
  private @NotNull String serviceId;
  private @NotNull DeploymentType deploymentType;
  private @NotNull String computeProviderId;
  private String deploymentMasterId;

  private boolean rollback;
  private String rollbackPhaseName;

  private List<PhaseStep> phaseSteps = new ArrayList<>();

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getComputeProviderId() {
    return computeProviderId;
  }

  public void setComputeProviderId(String computeProviderId) {
    this.computeProviderId = computeProviderId;
  }

  public DeploymentType getDeploymentType() {
    return deploymentType;
  }

  public void setDeploymentType(DeploymentType deploymentType) {
    this.deploymentType = deploymentType;
  }

  public String getDeploymentMasterId() {
    return deploymentMasterId;
  }

  public void setDeploymentMasterId(String deploymentMasterId) {
    this.deploymentMasterId = deploymentMasterId;
  }

  public List<PhaseStep> getPhaseSteps() {
    return phaseSteps;
  }

  public void addPhaseStep(PhaseStep phaseStep) {
    this.phaseSteps.add(phaseStep);
  }

  public void setPhaseSteps(List<PhaseStep> phaseSteps) {
    this.phaseSteps = phaseSteps;
  }

  public boolean isRollback() {
    return rollback;
  }

  public void setRollback(boolean rollback) {
    this.rollback = rollback;
  }

  public String getRollbackPhaseName() {
    return rollbackPhaseName;
  }

  public void setRollbackPhaseName(String rollbackPhaseName) {
    this.rollbackPhaseName = rollbackPhaseName;
  }

  public Node generatePhaseNode() {
    return aNode()
        .withId(uuid)
        .withName(name)
        .withType(StateType.PHASE.name())
        .addProperty("serviceId", serviceId)
        .addProperty("deploymentType", deploymentType)
        .addProperty("computeProviderId", computeProviderId)
        .addProperty("deploymentMasterId", deploymentMasterId)
        .addProperty(Constants.SUB_WORKFLOW_ID, uuid)
        .build();
  }

  public Map<String, Object> params() {
    Map<String, Object> params = new HashMap<>();
    params.put("serviceId", serviceId);
    params.put("computeProviderId", computeProviderId);
    params.put("deploymentType", deploymentType);
    params.put("deploymentMasterId", deploymentMasterId);
    return params;
  }

  public static final class WorkflowPhaseBuilder {
    private String uuid = UUIDGenerator.getUuid();
    private String name;
    private String serviceId;
    private String computeProviderId;
    private DeploymentType deploymentType;
    private String deploymentMasterId;
    private List<PhaseStep> phaseSteps = new ArrayList<>();
    private boolean rollback;
    private String rollbackPhaseName;

    private WorkflowPhaseBuilder() {}

    public static WorkflowPhaseBuilder aWorkflowPhase() {
      return new WorkflowPhaseBuilder();
    }

    public WorkflowPhaseBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public WorkflowPhaseBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public WorkflowPhaseBuilder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public WorkflowPhaseBuilder withComputeProviderId(String computeProviderId) {
      this.computeProviderId = computeProviderId;
      return this;
    }

    public WorkflowPhaseBuilder withDeploymentType(DeploymentType deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public WorkflowPhaseBuilder withDeploymentMasterId(String deploymentMasterId) {
      this.deploymentMasterId = deploymentMasterId;
      return this;
    }

    public WorkflowPhaseBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public WorkflowPhaseBuilder withRollbackPhaseName(String rollbackPhaseName) {
      this.rollbackPhaseName = rollbackPhaseName;
      return this;
    }

    public WorkflowPhaseBuilder addPhaseStep(PhaseStep phaseStep) {
      this.phaseSteps.add(phaseStep);
      return this;
    }

    public WorkflowPhase build() {
      WorkflowPhase workflowPhase = new WorkflowPhase();
      workflowPhase.setUuid(uuid);
      workflowPhase.setName(name);
      workflowPhase.setServiceId(serviceId);
      workflowPhase.setComputeProviderId(computeProviderId);
      workflowPhase.setDeploymentType(deploymentType);
      workflowPhase.setDeploymentMasterId(deploymentMasterId);
      workflowPhase.setPhaseSteps(phaseSteps);
      workflowPhase.setRollback(rollback);
      workflowPhase.setRollbackPhaseName(rollbackPhaseName);
      return workflowPhase;
    }
  }
}
