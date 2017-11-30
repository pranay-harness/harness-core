package software.wings.beans;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.BuildWorkflow.BuildWorkflowBuilder.aBuildWorkflow;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.common.Constants.WORKFLOW_VALIDATION_MESSAGE;

import com.fasterxml.jackson.annotation.JsonTypeName;
import software.wings.common.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sgurubelli on 11/14/17.
 */
@JsonTypeName("BUILD")
public class BuildWorkflow extends CanaryOrchestrationWorkflow {
  public BuildWorkflow() {
    setOrchestrationWorkflowType(BUILD);
  }

  @Override
  public boolean validate() {
    setValid(true);
    setValidationMessage(null);
    if (getWorkflowPhases() != null) {
      String invalid = "";
      List<String> invalidChildren = getWorkflowPhases()
                                         .stream()
                                         .filter(workflowPhase -> !workflowPhase.validate())
                                         .map(WorkflowPhase::getName)
                                         .collect(toList());
      if (invalidChildren != null && !invalidChildren.isEmpty()) {
        setValid(false);
        invalid += invalidChildren.toString();
      }
      if (!invalid.isEmpty()) {
        setValidationMessage(String.format(WORKFLOW_VALIDATION_MESSAGE, invalid));
      }
    }
    return isValid();
  }

  @Override
  public OrchestrationWorkflow clone() {
    return aBuildWorkflow()
        .withGraph(getGraph())
        .withPreDeploymentSteps(getPreDeploymentSteps())
        .withWorkflowPhaseIds(getWorkflowPhaseIds())
        .withWorkflowPhases(getWorkflowPhases())
        .withWorkflowPhaseIdMap(getWorkflowPhaseIdMap())
        .withPostDeploymentSteps(getPostDeploymentSteps())
        .withRollbackWorkflowPhaseIdMap(getRollbackWorkflowPhaseIdMap())
        .withNotificationRules(getNotificationRules())
        .withFailureStrategies(getFailureStrategies())
        .withSystemVariables(getSystemVariables())
        .withUserVariables(getUserVariables())
        .withDerivedVariables(getDerivedVariables())
        .withRequiredEntityTypes(getRequiredEntityTypes())
        .build();
  }

  public static final class BuildWorkflowBuilder {
    private Graph graph;
    private PhaseStep preDeploymentSteps = new PhaseStep(PhaseStepType.PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT);
    private List<String> workflowPhaseIds = new ArrayList<>();
    private Map<String, WorkflowPhase> workflowPhaseIdMap = new HashMap<>();
    private Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = new HashMap<>();
    private List<WorkflowPhase> workflowPhases = new ArrayList<>();
    private PhaseStep postDeploymentSteps = new PhaseStep(PhaseStepType.POST_DEPLOYMENT, Constants.POST_DEPLOYMENT);
    private List<NotificationRule> notificationRules = new ArrayList<>();
    private List<FailureStrategy> failureStrategies = new ArrayList<>();
    private List<Variable> systemVariables = new ArrayList<>();
    private List<Variable> userVariables = new ArrayList<>();
    private List<Variable> derivedVariables = new ArrayList<>();
    private Set<EntityType> requiredEntityTypes;

    private BuildWorkflowBuilder() {}
    public static BuildWorkflowBuilder aBuildWorkflow() {
      return new BuildWorkflowBuilder();
    }

    public BuildWorkflowBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public BuildWorkflowBuilder withPreDeploymentSteps(PhaseStep preDeploymentSteps) {
      this.preDeploymentSteps = preDeploymentSteps;
      return this;
    }

    public BuildWorkflowBuilder withWorkflowPhaseIds(List<String> workflowPhaseIds) {
      this.workflowPhaseIds = workflowPhaseIds;
      return this;
    }

    public BuildWorkflowBuilder withWorkflowPhaseIdMap(Map<String, WorkflowPhase> workflowPhaseIdMap) {
      this.workflowPhaseIdMap = workflowPhaseIdMap;
      return this;
    }

    public BuildWorkflowBuilder withRollbackWorkflowPhaseIdMap(Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap) {
      this.rollbackWorkflowPhaseIdMap = rollbackWorkflowPhaseIdMap;
      return this;
    }

    public BuildWorkflowBuilder addWorkflowPhase(WorkflowPhase workflowPhase) {
      this.workflowPhases.add(workflowPhase);
      return this;
    }

    public BuildWorkflowBuilder withPostDeploymentSteps(PhaseStep postDeploymentSteps) {
      this.postDeploymentSteps = postDeploymentSteps;
      return this;
    }

    public BuildWorkflowBuilder withNotificationRules(List<NotificationRule> notificationRules) {
      this.notificationRules = notificationRules;
      return this;
    }

    public BuildWorkflowBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public BuildWorkflowBuilder withSystemVariables(List<Variable> systemVariables) {
      this.systemVariables = systemVariables;
      return this;
    }

    public BuildWorkflowBuilder withUserVariables(List<Variable> userVariables) {
      this.userVariables = userVariables;
      return this;
    }

    public BuildWorkflowBuilder withDerivedVariables(List<Variable> derivedVariables) {
      this.derivedVariables = derivedVariables;
      return this;
    }

    public BuildWorkflowBuilder withRequiredEntityTypes(Set<EntityType> requiredEntityTypes) {
      this.requiredEntityTypes = requiredEntityTypes;
      return this;
    }

    public BuildWorkflowBuilder withWorkflowPhases(List<WorkflowPhase> workflowPhases) {
      this.workflowPhases = workflowPhases;
      return this;
    }

    public BuildWorkflow build() {
      BuildWorkflow BuildWorkflow = new BuildWorkflow();
      BuildWorkflow.setGraph(graph);
      BuildWorkflow.setPreDeploymentSteps(preDeploymentSteps);
      BuildWorkflow.setWorkflowPhaseIds(workflowPhaseIds);
      BuildWorkflow.setWorkflowPhaseIdMap(workflowPhaseIdMap);
      BuildWorkflow.setRollbackWorkflowPhaseIdMap(rollbackWorkflowPhaseIdMap);
      BuildWorkflow.setWorkflowPhases(workflowPhases);
      BuildWorkflow.setPostDeploymentSteps(postDeploymentSteps);
      BuildWorkflow.setNotificationRules(notificationRules);
      BuildWorkflow.setFailureStrategies(failureStrategies);
      BuildWorkflow.setSystemVariables(systemVariables);
      BuildWorkflow.setUserVariables(userVariables);
      BuildWorkflow.setDerivedVariables(derivedVariables);
      BuildWorkflow.setRequiredEntityTypes(requiredEntityTypes);
      return BuildWorkflow;
    }
  }
}
