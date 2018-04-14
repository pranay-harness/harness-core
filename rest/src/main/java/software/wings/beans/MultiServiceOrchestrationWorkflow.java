package software.wings.beans;

import static software.wings.beans.MultiServiceOrchestrationWorkflow.MultiServiceOrchestrationWorkflowBuilder.aMultiServiceOrchestrationWorkflow;

import com.fasterxml.jackson.annotation.JsonTypeName;
import software.wings.common.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sgurubelli on 5/16/17.
 */
@JsonTypeName("MULTI_SERVICE")
public class MultiServiceOrchestrationWorkflow extends CanaryOrchestrationWorkflow {
  public MultiServiceOrchestrationWorkflow() {
    setOrchestrationWorkflowType(OrchestrationWorkflowType.MULTI_SERVICE);
  }

  @Override
  public OrchestrationWorkflow cloneInternal() {
    return aMultiServiceOrchestrationWorkflow()
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

  public static final class MultiServiceOrchestrationWorkflowBuilder {
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

    private MultiServiceOrchestrationWorkflowBuilder() {}
    public static MultiServiceOrchestrationWorkflowBuilder aMultiServiceOrchestrationWorkflow() {
      return new MultiServiceOrchestrationWorkflowBuilder();
    }

    public MultiServiceOrchestrationWorkflowBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withPreDeploymentSteps(PhaseStep preDeploymentSteps) {
      this.preDeploymentSteps = preDeploymentSteps;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withWorkflowPhaseIds(List<String> workflowPhaseIds) {
      this.workflowPhaseIds = workflowPhaseIds;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withWorkflowPhaseIdMap(
        Map<String, WorkflowPhase> workflowPhaseIdMap) {
      this.workflowPhaseIdMap = workflowPhaseIdMap;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withWorkflowPhases(List<WorkflowPhase> workflowPhases) {
      this.workflowPhases = workflowPhases;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withRollbackWorkflowPhaseIdMap(
        Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap) {
      this.rollbackWorkflowPhaseIdMap = rollbackWorkflowPhaseIdMap;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder addWorkflowPhase(WorkflowPhase workflowPhase) {
      this.workflowPhases.add(workflowPhase);
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withPostDeploymentSteps(PhaseStep postDeploymentSteps) {
      this.postDeploymentSteps = postDeploymentSteps;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withNotificationRules(List<NotificationRule> notificationRules) {
      this.notificationRules = notificationRules;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withSystemVariables(List<Variable> systemVariables) {
      this.systemVariables = systemVariables;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withUserVariables(List<Variable> userVariables) {
      this.userVariables = userVariables;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withDerivedVariables(List<Variable> derivedVariables) {
      this.derivedVariables = derivedVariables;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withRequiredEntityTypes(Set<EntityType> requiredEntityTypes) {
      this.requiredEntityTypes = requiredEntityTypes;
      return this;
    }

    public MultiServiceOrchestrationWorkflow build() {
      MultiServiceOrchestrationWorkflow multiServiceOrchestrationWorkflow = new MultiServiceOrchestrationWorkflow();
      multiServiceOrchestrationWorkflow.setGraph(graph);
      multiServiceOrchestrationWorkflow.setPreDeploymentSteps(preDeploymentSteps);
      multiServiceOrchestrationWorkflow.setWorkflowPhaseIds(workflowPhaseIds);
      multiServiceOrchestrationWorkflow.setWorkflowPhaseIdMap(workflowPhaseIdMap);
      multiServiceOrchestrationWorkflow.setRollbackWorkflowPhaseIdMap(rollbackWorkflowPhaseIdMap);
      multiServiceOrchestrationWorkflow.setWorkflowPhases(workflowPhases);
      multiServiceOrchestrationWorkflow.setPostDeploymentSteps(postDeploymentSteps);
      multiServiceOrchestrationWorkflow.setNotificationRules(notificationRules);
      multiServiceOrchestrationWorkflow.setFailureStrategies(failureStrategies);
      multiServiceOrchestrationWorkflow.setSystemVariables(systemVariables);
      multiServiceOrchestrationWorkflow.setUserVariables(userVariables);
      multiServiceOrchestrationWorkflow.setDerivedVariables(derivedVariables);
      multiServiceOrchestrationWorkflow.setRequiredEntityTypes(requiredEntityTypes);
      return multiServiceOrchestrationWorkflow;
    }
  }
}
