/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StepType;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class K8sStatesTimeoutMigration implements Migration {
  private static final String stateTimeoutInMinutes = "stateTimeoutInMinutes";
  private static final int minTimeoutInMs = 60000;

  private static final Set<String> eligibleStates = ImmutableSet.of(StepType.K8S_SCALE.name(),
      StepType.K8S_APPLY.name(), StepType.K8S_CANARY_DEPLOY.name(), StepType.K8S_BLUE_GREEN_DEPLOY.name(),
      StepType.K8S_DEPLOYMENT_ROLLING_ROLLBACK.name(), StepType.K8S_DEPLOYMENT_ROLLING.name());

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    log.info("Running K8sStatesTimeoutMigration");
    log.info("Retrieving applications");

    try (HIterator<Application> apps = new HIterator<>(wingsPersistence.createQuery(Application.class).fetch())) {
      for (Application application : apps) {
        try (HIterator<Workflow> workflowHIterator =
                 new HIterator<>(wingsPersistence.createQuery(Workflow.class)
                                     .filter(WorkflowKeys.appId, application.getUuid())
                                     .fetch())) {
          for (Workflow workflow : workflowHIterator) {
            try {
              workflowService.loadOrchestrationWorkflow(workflow, workflow.getDefaultVersion());
              updateTimeoutInWorkflow(workflow);
            } catch (Exception e) {
              log.error("Failed to load Orchestration workflow {}", workflow.getUuid(), e);
            }
          }
        }
      }
    }

    log.info("Completed K8sStatesTimeoutMigration");
  }

  private void updateTimeoutInWorkflow(Workflow workflow) {
    boolean workflowModified = false;

    if (!(workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow)) {
      return;
    }

    // Using Canary, so that rolling, canary and BG can all be covered as they all extends CanaryOrchestrationWorkflow.
    CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    if (coWorkflow.getWorkflowPhases() == null) {
      return;
    }

    List<WorkflowPhase> workflowPhaseList = new ArrayList<>();
    final List<WorkflowPhase> workflowPhases = new ArrayList<>(coWorkflow.getWorkflowPhases());
    workflowPhases.addAll(coWorkflow.getRollbackWorkflowPhaseIdMap().values());
    for (WorkflowPhase workflowPhase : workflowPhases) {
      workflowPhaseList.add(workflowPhase);

      for (WorkflowPhase phase : workflowPhaseList) {
        for (PhaseStep phaseStep : phase.getPhaseSteps()) {
          for (GraphNode node : phaseStep.getSteps()) {
            workflowModified = updateGraphNode(node, workflow) || workflowModified;
          }
        }
      }

      workflowPhaseList.clear();
    }

    if (workflowModified) {
      try {
        log.info("Updating workflow: {} - {}", workflow.getUuid(), workflow.getName());
        workflowService.updateWorkflow(workflow, false);
      } catch (Exception e) {
        log.error("Error updating workflow", e);
      }
    }
  }

  private boolean updateGraphNode(GraphNode node, Workflow workflow) {
    boolean workflowModified = false;

    if (isEligible(node)) {
      Map<String, Object> properties = node.getProperties();
      if (properties != null && properties.containsKey(stateTimeoutInMinutes)) {
        Object timeOutObject = properties.get(stateTimeoutInMinutes);
        if (timeOutObject != null) {
          try {
            int timeout = (int) timeOutObject;
            if (timeout > minTimeoutInMs) {
              int updatedTimeout = timeout / minTimeoutInMs;
              workflowModified = true;
              properties.put(stateTimeoutInMinutes, updatedTimeout);
              log.info("Updating the timeout from {} to {} for state {} in workflowId {}", timeout, updatedTimeout,
                  node.getType(), workflow.getUuid());
            }

          } catch (ClassCastException ex) {
            log.info("Failed to convert timeout to integer for workflowId {}", workflow.getUuid());
          }
        }
      }
    }
    return workflowModified;
  }

  private boolean isEligible(GraphNode node) {
    return eligibleStates.contains(node.getType());
  }
}
