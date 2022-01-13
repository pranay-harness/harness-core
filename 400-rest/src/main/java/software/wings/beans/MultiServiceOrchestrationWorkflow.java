/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.MultiServiceOrchestrationWorkflow.MultiServiceOrchestrationWorkflowBuilder.aMultiServiceOrchestrationWorkflow;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationWorkflowType;

import software.wings.beans.concurrency.ConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sgurubelli on 5/16/17.
 */
@OwnedBy(CDC)
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

  public static final class MultiServiceOrchestrationWorkflowBuilder {
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

    public MultiServiceOrchestrationWorkflowBuilder withRollbackProvisioners(PhaseStep rollbackProvisioners) {
      this.rollbackProvisioners = rollbackProvisioners;
      return this;
    }

    public MultiServiceOrchestrationWorkflowBuilder withRollbackProvisionersReverse(
        PhaseStep rollbackProvisionersReverse) {
      this.rollbackProvisionersReverse = rollbackProvisionersReverse;
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

    public MultiServiceOrchestrationWorkflowBuilder withConcurrencyStrategy(ConcurrencyStrategy concurrencyStrategy) {
      this.concurrencyStrategy = concurrencyStrategy;
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
      multiServiceOrchestrationWorkflow.setConcurrencyStrategy(concurrencyStrategy);
      multiServiceOrchestrationWorkflow.setRollbackProvisioners(rollbackProvisioners);
      multiServiceOrchestrationWorkflow.setRollbackProvisionersReverse(rollbackProvisionersReverse);
      return multiServiceOrchestrationWorkflow;
    }
  }
}
