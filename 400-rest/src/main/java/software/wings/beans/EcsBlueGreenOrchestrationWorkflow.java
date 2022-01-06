/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.OrchestrationWorkflowType.BLUE_GREEN;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.concurrency.ConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(CDC)
@JsonTypeName("BLUE_GREEN_ECS")
@EqualsAndHashCode(callSuper = true)
public class EcsBlueGreenOrchestrationWorkflow extends CanaryOrchestrationWorkflow {
  @Getter @Setter private String ecsBGType;

  public EcsBlueGreenOrchestrationWorkflow() {
    setOrchestrationWorkflowType(BLUE_GREEN);
  }

  @Override
  public OrchestrationWorkflow cloneInternal() {
    return EcsBlueGreenOrchestrationWorkflowBuilder.anEcsBlueGreenOrchestrationWorkflow()
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
        .withEcsBgType(getEcsBGType())
        .withConcurrencyStrategy(getConcurrencyStrategy())
        .build();
  }

  public static final class EcsBlueGreenOrchestrationWorkflowBuilder {
    private Graph graph;
    private PhaseStep preDeploymentSteps = new PhaseStep(PhaseStepType.PRE_DEPLOYMENT);
    private String ecsBGType;
    private Map<String, WorkflowPhase> workflowPhaseIdMap = new HashMap<>();
    private Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = new HashMap<>();
    private List<Variable> systemVariables = new ArrayList<>();
    private List<FailureStrategy> failureStrategies = new ArrayList<>();
    private List<NotificationRule> notificationRules = new ArrayList<>();
    private ConcurrencyStrategy concurrencyStrategy;
    private List<WorkflowPhase> workflowPhases = new ArrayList<>();
    private List<Variable> userVariables = new ArrayList<>();
    private List<Variable> derivedVariables = new ArrayList<>();
    private PhaseStep postDeploymentSteps = new PhaseStep(PhaseStepType.POST_DEPLOYMENT);
    private List<String> workflowPhaseIds = new ArrayList<>();
    private Set<EntityType> requiredEntityTypes;

    private EcsBlueGreenOrchestrationWorkflowBuilder() {}

    public static EcsBlueGreenOrchestrationWorkflowBuilder anEcsBlueGreenOrchestrationWorkflow() {
      return new EcsBlueGreenOrchestrationWorkflowBuilder();
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withEcsBgType(String ecsBGType) {
      this.ecsBGType = ecsBGType;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withPreDeploymentSteps(PhaseStep preDeploymentSteps) {
      this.preDeploymentSteps = preDeploymentSteps;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withWorkflowPhaseIds(List<String> workflowPhaseIds) {
      this.workflowPhaseIds = workflowPhaseIds;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withWorkflowPhaseIdMap(
        Map<String, WorkflowPhase> workflowPhaseIdMap) {
      this.workflowPhaseIdMap = workflowPhaseIdMap;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withRollbackWorkflowPhaseIdMap(
        Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap) {
      this.rollbackWorkflowPhaseIdMap = rollbackWorkflowPhaseIdMap;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder addWorkflowPhase(WorkflowPhase workflowPhase) {
      this.workflowPhases.add(workflowPhase);
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withPostDeploymentSteps(PhaseStep postDeploymentSteps) {
      this.postDeploymentSteps = postDeploymentSteps;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withNotificationRules(List<NotificationRule> notificationRules) {
      this.notificationRules = notificationRules;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withConcurrencyStrategy(ConcurrencyStrategy concurrencyStrategy) {
      this.concurrencyStrategy = concurrencyStrategy;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withSystemVariables(List<Variable> systemVariables) {
      this.systemVariables = systemVariables;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withUserVariables(List<Variable> userVariables) {
      this.userVariables = userVariables;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withDerivedVariables(List<Variable> derivedVariables) {
      this.derivedVariables = derivedVariables;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withRequiredEntityTypes(Set<EntityType> requiredEntityTypes) {
      this.requiredEntityTypes = requiredEntityTypes;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflowBuilder withWorkflowPhases(List<WorkflowPhase> workflowPhases) {
      this.workflowPhases = workflowPhases;
      return this;
    }

    public EcsBlueGreenOrchestrationWorkflow build() {
      EcsBlueGreenOrchestrationWorkflow blueGreenOrchestrationWorkflow = new EcsBlueGreenOrchestrationWorkflow();
      blueGreenOrchestrationWorkflow.setGraph(graph);
      blueGreenOrchestrationWorkflow.setPreDeploymentSteps(preDeploymentSteps);
      blueGreenOrchestrationWorkflow.setWorkflowPhaseIds(workflowPhaseIds);
      blueGreenOrchestrationWorkflow.setWorkflowPhaseIdMap(workflowPhaseIdMap);
      blueGreenOrchestrationWorkflow.setRollbackWorkflowPhaseIdMap(rollbackWorkflowPhaseIdMap);
      blueGreenOrchestrationWorkflow.setWorkflowPhases(workflowPhases);
      blueGreenOrchestrationWorkflow.setPostDeploymentSteps(postDeploymentSteps);
      blueGreenOrchestrationWorkflow.setNotificationRules(notificationRules);
      blueGreenOrchestrationWorkflow.setFailureStrategies(failureStrategies);
      blueGreenOrchestrationWorkflow.setSystemVariables(systemVariables);
      blueGreenOrchestrationWorkflow.setUserVariables(userVariables);
      blueGreenOrchestrationWorkflow.setDerivedVariables(derivedVariables);
      blueGreenOrchestrationWorkflow.setRequiredEntityTypes(requiredEntityTypes);
      blueGreenOrchestrationWorkflow.setEcsBGType(ecsBGType);
      blueGreenOrchestrationWorkflow.setConcurrencyStrategy(concurrencyStrategy);
      return blueGreenOrchestrationWorkflow;
    }
  }
}
