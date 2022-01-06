/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.BlueGreenOrchestrationWorkflow.BlueGreenOrchestrationWorkflowBuilder.aBlueGreenOrchestrationWorkflow;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationWorkflowType;

import software.wings.beans.concurrency.ConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(CDC)
@JsonTypeName("BLUE_GREEN")
public class BlueGreenOrchestrationWorkflow extends CanaryOrchestrationWorkflow {
  public BlueGreenOrchestrationWorkflow() {
    setOrchestrationWorkflowType(OrchestrationWorkflowType.BLUE_GREEN);
  }

  @Override
  public OrchestrationWorkflow cloneInternal() {
    return aBlueGreenOrchestrationWorkflow()
        .withGraph(getGraph())
        .withPreDeploymentSteps(getPreDeploymentSteps())
        .withRollbackProvisioners(getRollbackProvisioners())
        .withRollbackProvisionersReverse(getRollbackProvisionersReverse())
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
        .withConcurrencyStrategy(getConcurrencyStrategy())
        .build();
  }

  public static final class BlueGreenOrchestrationWorkflowBuilder {
    private Graph graph;
    private PhaseStep preDeploymentSteps = new PhaseStep(PhaseStepType.PRE_DEPLOYMENT);
    private List<String> workflowPhaseIds = new ArrayList<>();
    private Map<String, WorkflowPhase> workflowPhaseIdMap = new HashMap<>();
    private Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = new HashMap<>();
    private List<WorkflowPhase> workflowPhases = new ArrayList<>();
    private PhaseStep postDeploymentSteps = new PhaseStep(PhaseStepType.POST_DEPLOYMENT);
    private List<NotificationRule> notificationRules = new ArrayList<>();
    private ConcurrencyStrategy concurrencyStrategy;
    private List<FailureStrategy> failureStrategies = new ArrayList<>();
    private List<Variable> systemVariables = new ArrayList<>();
    private List<Variable> userVariables = new ArrayList<>();
    private List<Variable> derivedVariables = new ArrayList<>();
    private Set<EntityType> requiredEntityTypes;
    private PhaseStep rollbackProvisioners;
    private PhaseStep rollbackProvisionersReverse;

    private BlueGreenOrchestrationWorkflowBuilder() {}
    public static BlueGreenOrchestrationWorkflowBuilder aBlueGreenOrchestrationWorkflow() {
      return new BlueGreenOrchestrationWorkflowBuilder();
    }

    public BlueGreenOrchestrationWorkflowBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withPreDeploymentSteps(PhaseStep preDeploymentSteps) {
      this.preDeploymentSteps = preDeploymentSteps;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withRollbackProvisioners(PhaseStep rollbackProvisioners) {
      this.rollbackProvisioners = rollbackProvisioners;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withRollbackProvisionersReverse(
        PhaseStep rollbackProvisionersReverse) {
      this.rollbackProvisionersReverse = rollbackProvisionersReverse;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withWorkflowPhaseIds(List<String> workflowPhaseIds) {
      this.workflowPhaseIds = workflowPhaseIds;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withWorkflowPhaseIdMap(Map<String, WorkflowPhase> workflowPhaseIdMap) {
      this.workflowPhaseIdMap = workflowPhaseIdMap;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withRollbackWorkflowPhaseIdMap(
        Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap) {
      this.rollbackWorkflowPhaseIdMap = rollbackWorkflowPhaseIdMap;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder addWorkflowPhase(WorkflowPhase workflowPhase) {
      this.workflowPhases.add(workflowPhase);
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withPostDeploymentSteps(PhaseStep postDeploymentSteps) {
      this.postDeploymentSteps = postDeploymentSteps;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withNotificationRules(List<NotificationRule> notificationRules) {
      this.notificationRules = notificationRules;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withConcurrencyStrategy(ConcurrencyStrategy concurrencyStrategy) {
      this.concurrencyStrategy = concurrencyStrategy;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withSystemVariables(List<Variable> systemVariables) {
      this.systemVariables = systemVariables;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withUserVariables(List<Variable> userVariables) {
      this.userVariables = userVariables;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withDerivedVariables(List<Variable> derivedVariables) {
      this.derivedVariables = derivedVariables;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withRequiredEntityTypes(Set<EntityType> requiredEntityTypes) {
      this.requiredEntityTypes = requiredEntityTypes;
      return this;
    }

    public BlueGreenOrchestrationWorkflowBuilder withWorkflowPhases(List<WorkflowPhase> workflowPhases) {
      this.workflowPhases = workflowPhases;
      return this;
    }

    public BlueGreenOrchestrationWorkflow build() {
      BlueGreenOrchestrationWorkflow blueGreenOrchestrationWorkflow = new BlueGreenOrchestrationWorkflow();
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
      blueGreenOrchestrationWorkflow.setConcurrencyStrategy(concurrencyStrategy);
      blueGreenOrchestrationWorkflow.setRollbackProvisioners(rollbackProvisioners);
      blueGreenOrchestrationWorkflow.setRollbackProvisionersReverse(rollbackProvisionersReverse);
      return blueGreenOrchestrationWorkflow;
    }
  }
}
