/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class MigrationUtils {
  /*
   * Be sure to make corresponding changes in UI that reference the old state type.
   *
   * Check:
   *   StencilConfig.js
   *   StencilModal.js
   *
   * StateTypes with StencilType CLOUD will continue to show in the list of commands available in workflows
   */
  public static void renameStateTypeAndStateClass(StateType oldStateType, StateType newStateType,
      WingsPersistence wingsPersistence, WorkflowService workflowService) {
    log.info("Renaming {} to {} in all CanaryOrchestrationWorkflows", oldStateType.name(), newStateType.name());
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    log.info("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      log.info("No applications found");
      return;
    }
    log.info("Checking {} applications", apps.size());
    for (Application app : apps) {
      List<Workflow> workflows =
          workflowService
              .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, app.getUuid()).build())
              .getResponse();
      int updateCount = 0;
      for (Workflow workflow : workflows) {
        boolean workflowModified = false;
        if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
          CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
          for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
            List<WorkflowPhase> both = new ArrayList<>();
            both.add(workflowPhase);
            WorkflowPhase rollbackPhase = coWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());
            if (rollbackPhase != null) {
              both.add(rollbackPhase);
            }
            for (WorkflowPhase phase : both) {
              for (PhaseStep phaseStep : phase.getPhaseSteps()) {
                for (GraphNode node : phaseStep.getSteps()) {
                  if (oldStateType.name().equals(node.getType())) {
                    workflowModified = true;
                    node.setType(newStateType.name());
                    Map<String, Object> properties = node.getProperties();
                    properties.put("className", newStateType.getTypeClass().getCanonicalName());
                  }
                }
              }
            }
          }
        }
        if (workflowModified) {
          try {
            log.info("... Updating workflow: {} - {}", workflow.getUuid(), workflow.getName());
            workflowService.updateWorkflow(workflow, false);
            Thread.sleep(100);
          } catch (Exception e) {
            log.error("Error updating workflow", e);
          }
          updateCount++;
        }
      }
      log.info("Application migrated: {} - {}. Updated {} out of {} workflows", app.getUuid(), app.getName(),
          updateCount, workflows.size());
    }
  }
}
