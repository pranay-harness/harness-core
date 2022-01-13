/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;

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
@JsonTypeName("BASIC")
public class BasicOrchestrationWorkflow extends CanaryOrchestrationWorkflow {
  public BasicOrchestrationWorkflow() {
    setOrchestrationWorkflowType(OrchestrationWorkflowType.BASIC);
  }

  @Override
  public OrchestrationWorkflow cloneInternal() {
    return aBasicOrchestrationWorkflow()
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

  public static final class BasicOrchestrationWorkflowBuilder {
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

    private BasicOrchestrationWorkflowBuilder() {}
    public static BasicOrchestrationWorkflowBuilder aBasicOrchestrationWorkflow() {
      return new BasicOrchestrationWorkflowBuilder();
    }

    public BasicOrchestrationWorkflowBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withPreDeploymentSteps(PhaseStep preDeploymentSteps) {
      this.preDeploymentSteps = preDeploymentSteps;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withRollbackProvisioners(PhaseStep rollbackProvisioners) {
      this.rollbackProvisioners = rollbackProvisioners;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withRollbackProvisionersReverse(PhaseStep rollbackProvisionersReverse) {
      this.rollbackProvisionersReverse = rollbackProvisionersReverse;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withWorkflowPhaseIds(List<String> workflowPhaseIds) {
      this.workflowPhaseIds = workflowPhaseIds;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withWorkflowPhaseIdMap(Map<String, WorkflowPhase> workflowPhaseIdMap) {
      this.workflowPhaseIdMap = workflowPhaseIdMap;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withRollbackWorkflowPhaseIdMap(
        Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap) {
      this.rollbackWorkflowPhaseIdMap = rollbackWorkflowPhaseIdMap;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder addWorkflowPhase(WorkflowPhase workflowPhase) {
      this.workflowPhases.add(workflowPhase);
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withPostDeploymentSteps(PhaseStep postDeploymentSteps) {
      this.postDeploymentSteps = postDeploymentSteps;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withNotificationRules(List<NotificationRule> notificationRules) {
      this.notificationRules = notificationRules;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withConcurrencyStrategy(ConcurrencyStrategy concurrencyStrategy) {
      this.concurrencyStrategy = concurrencyStrategy;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withSystemVariables(List<Variable> systemVariables) {
      this.systemVariables = systemVariables;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withUserVariables(List<Variable> userVariables) {
      this.userVariables = userVariables;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withDerivedVariables(List<Variable> derivedVariables) {
      this.derivedVariables = derivedVariables;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withRequiredEntityTypes(Set<EntityType> requiredEntityTypes) {
      this.requiredEntityTypes = requiredEntityTypes;
      return this;
    }

    public BasicOrchestrationWorkflowBuilder withWorkflowPhases(List<WorkflowPhase> workflowPhases) {
      this.workflowPhases = workflowPhases;
      return this;
    }

    public BasicOrchestrationWorkflow build() {
      BasicOrchestrationWorkflow basicOrchestrationWorkflow = new BasicOrchestrationWorkflow();
      basicOrchestrationWorkflow.setGraph(graph);
      basicOrchestrationWorkflow.setPreDeploymentSteps(preDeploymentSteps);
      basicOrchestrationWorkflow.setWorkflowPhaseIds(workflowPhaseIds);
      basicOrchestrationWorkflow.setWorkflowPhaseIdMap(workflowPhaseIdMap);
      basicOrchestrationWorkflow.setRollbackWorkflowPhaseIdMap(rollbackWorkflowPhaseIdMap);
      basicOrchestrationWorkflow.setWorkflowPhases(workflowPhases);
      basicOrchestrationWorkflow.setPostDeploymentSteps(postDeploymentSteps);
      basicOrchestrationWorkflow.setNotificationRules(notificationRules);
      basicOrchestrationWorkflow.setFailureStrategies(failureStrategies);
      basicOrchestrationWorkflow.setSystemVariables(systemVariables);
      basicOrchestrationWorkflow.setUserVariables(userVariables);
      basicOrchestrationWorkflow.setDerivedVariables(derivedVariables);
      basicOrchestrationWorkflow.setRequiredEntityTypes(requiredEntityTypes);
      basicOrchestrationWorkflow.setConcurrencyStrategy(concurrencyStrategy);
      basicOrchestrationWorkflow.setRollbackProvisioners(rollbackProvisioners);
      basicOrchestrationWorkflow.setRollbackProvisionersReverse(rollbackProvisionersReverse);
      return basicOrchestrationWorkflow;
    }
  }
}
