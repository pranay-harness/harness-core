package software.wings.beans;

import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;

import org.mongodb.morphia.annotations.Embedded;
import software.wings.api.DeploymentType;
import software.wings.beans.Graph.Builder;
import software.wings.beans.Graph.Node;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.sm.StateType;
import software.wings.sm.TransitionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

/**
 * Created by rishi on 12/21/16.
 */
public class WorkflowPhase implements UuidAware {
  private String uuid = UUIDGenerator.getUuid();
  private String name;
  private @NotNull String serviceId;

  @NotNull private String infraMappingId;

  private DeploymentType deploymentType;
  private String computeProviderId;
  private String infraMappingName;
  private boolean provisionNodes;

  private boolean rollback;
  private String phaseNameForRollback;

  private boolean valid;
  private String validationMessage;

  private List<TemplateExpression> templateExpressions;

  @Embedded private List<PhaseStep> phaseSteps = new ArrayList<>();

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

  public String getInfraMappingName() {
    return infraMappingName;
  }

  public void setInfraMappingName(String infraMappingName) {
    this.infraMappingName = infraMappingName;
  }

  public boolean isProvisionNodes() {
    return provisionNodes;
  }

  public void setProvisionNodes(boolean provisionNodes) {
    this.provisionNodes = provisionNodes;
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

  public String getPhaseNameForRollback() {
    return phaseNameForRollback;
  }

  public void setPhaseNameForRollback(String phaseNameForRollback) {
    this.phaseNameForRollback = phaseNameForRollback;
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public String getValidationMessage() {
    return validationMessage;
  }

  public void setValidationMessage(String validationMessage) {
    this.validationMessage = validationMessage;
  }

  /**
   * Get template expressions
   * @return
   */
  public List<TemplateExpression> getTemplateExpressions() {
    return templateExpressions;
  }

  /**
   * Set template expressions
   * @param templateExpressions
   */
  public void setTemplateExpressions(List<TemplateExpression> templateExpressions) {
    this.templateExpressions = templateExpressions;
  }
  public Node generatePhaseNode() {
    return aNode()
        .withId(uuid)
        .withName(name)
        .withType(StateType.PHASE.name())
        .addProperty("serviceId", serviceId)
        .withRollback(rollback)
        .addProperty("deploymentType", deploymentType)
        .addProperty("computeProviderId", computeProviderId)
        .addProperty("infraMappingName", infraMappingName)
        .addProperty("infraMappingId", infraMappingId)
        .addProperty(Constants.SUB_WORKFLOW_ID, uuid)
        .addProperty("phaseNameForRollback", phaseNameForRollback)
        .build();
  }

  public Map<String, Object> params() {
    Map<String, Object> params = new HashMap<>();
    params.put("serviceId", serviceId);
    params.put("computeProviderId", computeProviderId);
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

  public Map<String, Graph> generateSubworkflows() {
    Map<String, Graph> graphs = new HashMap<>();
    Builder graphBuilder = aGraph().withGraphName(name);

    String id1 = null;
    String id2;
    Node node;
    for (PhaseStep phaseStep : phaseSteps) {
      id2 = phaseStep.getUuid();
      node = phaseStep.generatePhaseStepNode();
      graphBuilder.addNodes(node);
      if (id1 == null) {
        node.setOrigin(true);
      } else {
        graphBuilder.addLinks(
            aLink().withId(getUuid()).withFrom(id1).withTo(id2).withType(TransitionType.SUCCESS.name()).build());
      }
      id1 = id2;
      Graph stepsGraph = phaseStep.generateSubworkflow(deploymentType);
      graphs.put(phaseStep.getUuid(), stepsGraph);
    }

    graphs.put(uuid, graphBuilder.build());
    return graphs;
  }

  public boolean validate() {
    valid = true;
    validationMessage = null;
    if (phaseSteps != null) {
      List<String> invalidChildren = phaseSteps.stream()
                                         .filter(phaseStep -> !phaseStep.validate())
                                         .map(PhaseStep::getName)
                                         .collect(Collectors.toList());
      if (invalidChildren != null && !invalidChildren.isEmpty()) {
        valid = false;
        validationMessage = String.format(Constants.PHASE_VALIDATION_MESSAGE, invalidChildren.toString());
      }
    }
    return valid;
  }

  public static final class WorkflowPhaseBuilder {
    private String uuid = UUIDGenerator.getUuid();
    private String name;
    private String serviceId;
    private String infraMappingId;
    private DeploymentType deploymentType;
    private String computeProviderId;
    private String infraMappingName;
    private boolean rollback;
    private String phaseNameForRollback;
    private boolean valid;
    private String validationMessage;
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

    public WorkflowPhaseBuilder withComputeProviderId(String computeProviderId) {
      this.computeProviderId = computeProviderId;
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

    public WorkflowPhaseBuilder withPhaseNameForRollback(String phaseNameForRollback) {
      this.phaseNameForRollback = phaseNameForRollback;
      return this;
    }

    public WorkflowPhaseBuilder withValid(boolean valid) {
      this.valid = valid;
      return this;
    }

    public WorkflowPhaseBuilder withValidationMessage(String validationMessage) {
      this.validationMessage = validationMessage;
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
      workflowPhase.setInfraMappingId(infraMappingId);
      workflowPhase.setDeploymentType(deploymentType);
      workflowPhase.setComputeProviderId(computeProviderId);
      workflowPhase.setInfraMappingName(infraMappingName);
      workflowPhase.setRollback(rollback);
      workflowPhase.setPhaseNameForRollback(phaseNameForRollback);
      workflowPhase.setValid(valid);
      workflowPhase.setValidationMessage(validationMessage);
      workflowPhase.setPhaseSteps(phaseSteps);
      return workflowPhase;
    }
  }
}
