/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.common.WorkflowConstants.WORKFLOW_VALIDATION_MESSAGE;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sgurubelli on 11/14/17.
 */
@OwnedBy(CDC)
@JsonTypeName("BUILD")
public class BuildWorkflow extends CanaryOrchestrationWorkflow {
  public BuildWorkflow() {
    setOrchestrationWorkflowType(BUILD);
  }

  @Override
  public boolean needCloudProvider() {
    return false;
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
      if (isNotEmpty(invalidChildren)) {
        setValid(false);
        invalid += invalidChildren.toString();
      }
      if (!invalid.isEmpty()) {
        setValidationMessage(format(WORKFLOW_VALIDATION_MESSAGE, invalid));
      }
    }
    return isValid();
  }

  @Override
  public OrchestrationWorkflow cloneInternal() {
    return aBuildOrchestrationWorkflow()
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
        .build();
  }

  public static final class BuildOrchestrationWorkflowBuilder {
    private Graph graph;
    private PhaseStep preDeploymentSteps = new PhaseStep(PhaseStepType.PRE_DEPLOYMENT);
    private List<String> workflowPhaseIds = new ArrayList<>();
    private Map<String, WorkflowPhase> workflowPhaseIdMap = new HashMap<>();
    private Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = new HashMap<>();
    private List<WorkflowPhase> workflowPhases = new ArrayList<>();
    private PhaseStep postDeploymentSteps = new PhaseStep(PhaseStepType.POST_DEPLOYMENT);
    private List<NotificationRule> notificationRules = new ArrayList<>();
    private List<FailureStrategy> failureStrategies = new ArrayList<>();
    private List<Variable> systemVariables = new ArrayList<>();
    private List<Variable> userVariables = new ArrayList<>();
    private List<Variable> derivedVariables = new ArrayList<>();
    private Set<EntityType> requiredEntityTypes;
    private PhaseStep rollbackProvisioners;
    private PhaseStep rollbackProvisionersReverse;

    private BuildOrchestrationWorkflowBuilder() {}
    public static BuildOrchestrationWorkflowBuilder aBuildOrchestrationWorkflow() {
      return new BuildOrchestrationWorkflowBuilder();
    }

    public BuildOrchestrationWorkflowBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withPreDeploymentSteps(PhaseStep preDeploymentSteps) {
      this.preDeploymentSteps = preDeploymentSteps;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withRollbackProvisioners(PhaseStep rollbackProvisioners) {
      this.rollbackProvisioners = rollbackProvisioners;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withRollbackProvisionersReverse(PhaseStep rollbackProvisionersReverse) {
      this.rollbackProvisionersReverse = rollbackProvisionersReverse;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withWorkflowPhaseIds(List<String> workflowPhaseIds) {
      this.workflowPhaseIds = workflowPhaseIds;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withWorkflowPhaseIdMap(Map<String, WorkflowPhase> workflowPhaseIdMap) {
      this.workflowPhaseIdMap = workflowPhaseIdMap;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withRollbackWorkflowPhaseIdMap(
        Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap) {
      this.rollbackWorkflowPhaseIdMap = rollbackWorkflowPhaseIdMap;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder addWorkflowPhase(WorkflowPhase workflowPhase) {
      this.workflowPhases.add(workflowPhase);
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withPostDeploymentSteps(PhaseStep postDeploymentSteps) {
      this.postDeploymentSteps = postDeploymentSteps;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withNotificationRules(List<NotificationRule> notificationRules) {
      this.notificationRules = notificationRules;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withSystemVariables(List<Variable> systemVariables) {
      this.systemVariables = systemVariables;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withUserVariables(List<Variable> userVariables) {
      this.userVariables = userVariables;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withDerivedVariables(List<Variable> derivedVariables) {
      this.derivedVariables = derivedVariables;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withRequiredEntityTypes(Set<EntityType> requiredEntityTypes) {
      this.requiredEntityTypes = requiredEntityTypes;
      return this;
    }

    public BuildOrchestrationWorkflowBuilder withWorkflowPhases(List<WorkflowPhase> workflowPhases) {
      this.workflowPhases = workflowPhases;
      return this;
    }

    public BuildWorkflow build() {
      BuildWorkflow workflow = new BuildWorkflow();
      workflow.setGraph(graph);
      workflow.setPreDeploymentSteps(preDeploymentSteps);
      workflow.setWorkflowPhaseIds(workflowPhaseIds);
      workflow.setWorkflowPhaseIdMap(workflowPhaseIdMap);
      workflow.setRollbackWorkflowPhaseIdMap(rollbackWorkflowPhaseIdMap);
      workflow.setWorkflowPhases(workflowPhases);
      workflow.setPostDeploymentSteps(postDeploymentSteps);
      workflow.setNotificationRules(notificationRules);
      workflow.setFailureStrategies(failureStrategies);
      workflow.setSystemVariables(systemVariables);
      workflow.setUserVariables(userVariables);
      workflow.setDerivedVariables(derivedVariables);
      workflow.setRequiredEntityTypes(requiredEntityTypes);
      workflow.setRollbackProvisioners(rollbackProvisioners);
      workflow.setRollbackProvisionersReverse(rollbackProvisionersReverse);
      return workflow;
    }
  }
}
