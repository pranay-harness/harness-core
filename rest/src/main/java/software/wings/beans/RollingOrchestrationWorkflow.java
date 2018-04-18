package software.wings.beans;

import static software.wings.beans.RollingOrchestrationWorkflow.RollingOrchestrationWorkflowBuilder.aRollingOrchestrationWorkflow;

import com.fasterxml.jackson.annotation.JsonTypeName;
import software.wings.common.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonTypeName("ROLLING")
public class RollingOrchestrationWorkflow extends CanaryOrchestrationWorkflow {
  public RollingOrchestrationWorkflow() {
    setOrchestrationWorkflowType(OrchestrationWorkflowType.ROLLING);
  }

  @Override
  public OrchestrationWorkflow cloneInternal() {
    return aRollingOrchestrationWorkflow()
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

  public static final class RollingOrchestrationWorkflowBuilder {
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

    private RollingOrchestrationWorkflowBuilder() {}
    public static RollingOrchestrationWorkflowBuilder aRollingOrchestrationWorkflow() {
      return new RollingOrchestrationWorkflowBuilder();
    }

    public RollingOrchestrationWorkflowBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withPreDeploymentSteps(PhaseStep preDeploymentSteps) {
      this.preDeploymentSteps = preDeploymentSteps;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withWorkflowPhaseIds(List<String> workflowPhaseIds) {
      this.workflowPhaseIds = workflowPhaseIds;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withWorkflowPhaseIdMap(Map<String, WorkflowPhase> workflowPhaseIdMap) {
      this.workflowPhaseIdMap = workflowPhaseIdMap;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withRollbackWorkflowPhaseIdMap(
        Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap) {
      this.rollbackWorkflowPhaseIdMap = rollbackWorkflowPhaseIdMap;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder addWorkflowPhase(WorkflowPhase workflowPhase) {
      this.workflowPhases.add(workflowPhase);
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withPostDeploymentSteps(PhaseStep postDeploymentSteps) {
      this.postDeploymentSteps = postDeploymentSteps;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withNotificationRules(List<NotificationRule> notificationRules) {
      this.notificationRules = notificationRules;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withSystemVariables(List<Variable> systemVariables) {
      this.systemVariables = systemVariables;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withUserVariables(List<Variable> userVariables) {
      this.userVariables = userVariables;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withDerivedVariables(List<Variable> derivedVariables) {
      this.derivedVariables = derivedVariables;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withRequiredEntityTypes(Set<EntityType> requiredEntityTypes) {
      this.requiredEntityTypes = requiredEntityTypes;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withWorkflowPhases(List<WorkflowPhase> workflowPhases) {
      this.workflowPhases = workflowPhases;
      return this;
    }

    public RollingOrchestrationWorkflow build() {
      RollingOrchestrationWorkflow rollingOrchestrationWorkflow = new RollingOrchestrationWorkflow();
      rollingOrchestrationWorkflow.setGraph(graph);
      rollingOrchestrationWorkflow.setPreDeploymentSteps(preDeploymentSteps);
      rollingOrchestrationWorkflow.setWorkflowPhaseIds(workflowPhaseIds);
      rollingOrchestrationWorkflow.setWorkflowPhaseIdMap(workflowPhaseIdMap);
      rollingOrchestrationWorkflow.setRollbackWorkflowPhaseIdMap(rollbackWorkflowPhaseIdMap);
      rollingOrchestrationWorkflow.setWorkflowPhases(workflowPhases);
      rollingOrchestrationWorkflow.setPostDeploymentSteps(postDeploymentSteps);
      rollingOrchestrationWorkflow.setNotificationRules(notificationRules);
      rollingOrchestrationWorkflow.setFailureStrategies(failureStrategies);
      rollingOrchestrationWorkflow.setSystemVariables(systemVariables);
      rollingOrchestrationWorkflow.setUserVariables(userVariables);
      rollingOrchestrationWorkflow.setDerivedVariables(derivedVariables);
      rollingOrchestrationWorkflow.setRequiredEntityTypes(requiredEntityTypes);
      return rollingOrchestrationWorkflow;
    }
  }
}