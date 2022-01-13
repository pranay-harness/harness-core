/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.RollingOrchestrationWorkflow.RollingOrchestrationWorkflowBuilder.aRollingOrchestrationWorkflow;

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

  public static final class RollingOrchestrationWorkflowBuilder {
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

    public RollingOrchestrationWorkflowBuilder withRollbackProvisioners(PhaseStep rollbackProvisioners) {
      this.rollbackProvisioners = rollbackProvisioners;
      return this;
    }

    public RollingOrchestrationWorkflowBuilder withRollbackProvisionersReverse(PhaseStep rollbackProvisionersReverse) {
      this.rollbackProvisionersReverse = rollbackProvisionersReverse;
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

    public RollingOrchestrationWorkflowBuilder withConcurrencyStrategy(ConcurrencyStrategy concurrencyStrategy) {
      this.concurrencyStrategy = concurrencyStrategy;
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
      rollingOrchestrationWorkflow.setConcurrencyStrategy(concurrencyStrategy);
      rollingOrchestrationWorkflow.setRollbackProvisioners(rollbackProvisioners);
      rollingOrchestrationWorkflow.setRollbackProvisionersReverse(rollbackProvisionersReverse);
      return rollingOrchestrationWorkflow;
    }
  }
}
