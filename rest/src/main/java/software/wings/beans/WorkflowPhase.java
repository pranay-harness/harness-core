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

  @NotNull private String infraMappingId;

  private DeploymentType deploymentType;
  private String infraMappingName;

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

  public DeploymentType getDeploymentType() {
    return deploymentType;
  }

  public void setDeploymentType(DeploymentType deploymentType) {
    this.deploymentType = deploymentType;
  }

  public String getInfraMappingName() {
    return infraMappingName;
  }

  public void setInfraMappingName(String infraMappingName) {
    this.infraMappingName = infraMappingName;
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
        .withRollback(rollback)
        .addProperty("deploymentType", deploymentType)
        .addProperty("infraMappingName", infraMappingName)
        .addProperty("infraMappingId", infraMappingId)
        .addProperty(Constants.SUB_WORKFLOW_ID, uuid)
        .build();
  }

  public Map<String, Object> params() {
    Map<String, Object> params = new HashMap<>();
    params.put("serviceId", serviceId);
    params.put("infraMappingName", infraMappingName);
    params.put("infraMappingId", infraMappingId);
    params.put("deploymentType", deploymentType);
    return params;
  }

  public String getInfraMappingId() {
    return infraMappingId;
  }

  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  public static final class WorkflowPhaseBuilder {
    private String uuid = UUIDGenerator.getUuid();
    private String name;
    private String serviceId;
    private String infraMappingId;
    private DeploymentType deploymentType;
    private String infraMappingName;
    private boolean rollback;
    private String rollbackPhaseName;
    private List<PhaseStep> phaseSteps = new ArrayList<>();

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

    public WorkflowPhaseBuilder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public WorkflowPhaseBuilder withDeploymentType(DeploymentType deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public WorkflowPhaseBuilder withInfraMappingName(String infraMappingName) {
      this.infraMappingName = infraMappingName;
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

    public WorkflowPhaseBuilder withPhaseSteps(List<PhaseStep> phaseSteps) {
      this.phaseSteps = phaseSteps;
      return this;
    }

    public WorkflowPhaseBuilder addPhaseStep(PhaseStep phaseStep) {
      this.phaseSteps.add(phaseStep);
      return this;
    }

    public WorkflowPhaseBuilder but() {
      return aWorkflowPhase()
          .withUuid(uuid)
          .withName(name)
          .withServiceId(serviceId)
          .withInfraMappingId(infraMappingId)
          .withDeploymentType(deploymentType)
          .withInfraMappingName(infraMappingName)
          .withRollback(rollback)
          .withRollbackPhaseName(rollbackPhaseName)
          .withPhaseSteps(phaseSteps);
    }

    public WorkflowPhase build() {
      WorkflowPhase workflowPhase = new WorkflowPhase();
      workflowPhase.setUuid(uuid);
      workflowPhase.setName(name);
      workflowPhase.setServiceId(serviceId);
      workflowPhase.setInfraMappingId(infraMappingId);
      workflowPhase.setDeploymentType(deploymentType);
      workflowPhase.setInfraMappingName(infraMappingName);
      workflowPhase.setRollback(rollback);
      workflowPhase.setRollbackPhaseName(rollbackPhaseName);
      workflowPhase.setPhaseSteps(phaseSteps);
      return workflowPhase;
    }
  }
}
