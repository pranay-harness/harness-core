/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.sm.StateType.HELM_DEPLOY;
import static software.wings.sm.StateType.HELM_ROLLBACK;

import io.harness.annotations.dev.OwnedBy;
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

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class HelmStateTimeoutMigration implements Migration {
  private static final String steadyStateTimeout = "steadyStateTimeout";
  private static final int minTimeoutInMs = 60000;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    log.info("Running HelmStateTimeoutMigration");
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

    log.info("Completed HelmStateTimeoutMigration");
  }

  private void updateTimeoutInWorkflow(Workflow workflow) {
    boolean workflowModified = false;

    if (!(workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow)) {
      return;
    }

    CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    if (coWorkflow.getWorkflowPhases() == null) {
      return;
    }

    Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = coWorkflow.getRollbackWorkflowPhaseIdMap();

    List<WorkflowPhase> workflowPhaseList = new ArrayList<>();
    for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
      workflowPhaseList.add(workflowPhase);
      WorkflowPhase rollbackPhase = rollbackWorkflowPhaseIdMap.get(workflowPhase.getUuid());
      if (rollbackPhase != null) {
        workflowPhaseList.add(rollbackPhase);
      }

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

    if (HELM_DEPLOY.name().equals(node.getType()) || HELM_ROLLBACK.name().equals(node.getType())) {
      Map<String, Object> properties = node.getProperties();
      if (properties != null && properties.containsKey(steadyStateTimeout)) {
        Object timeOutObject = properties.get(steadyStateTimeout);
        if (timeOutObject != null) {
          try {
            int timeout = (int) timeOutObject;
            if (timeout > minTimeoutInMs) {
              int updatedTimeout = timeout / minTimeoutInMs;
              workflowModified = true;
              properties.put(steadyStateTimeout, updatedTimeout);
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
}
