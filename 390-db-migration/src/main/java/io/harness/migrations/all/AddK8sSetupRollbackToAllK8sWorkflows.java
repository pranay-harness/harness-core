/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;
import static software.wings.sm.StateType.KUBERNETES_SETUP_ROLLBACK;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.migrations.Migration;

import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddK8sSetupRollbackToAllK8sWorkflows implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    log.info("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      log.info("No applications found");
      return;
    }
    log.info("Updating {} applications.", apps.size());
    for (Application app : apps) {
      List<Workflow> workflows =
          workflowService
              .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, app.getUuid()).build())
              .getResponse();
      int updateCount = 0;
      int candidateCount = 0;
      for (Workflow workflow : workflows) {
        boolean workflowModified = false;
        boolean candidateFound = false;
        if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
          CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
          for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
            if (!workflowPhase.isRollback() && workflowPhase.getPhaseSteps().size() == 4) {
              for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
                if (CONTAINER_SETUP == phaseStep.getPhaseStepType()) {
                  for (GraphNode node : phaseStep.getSteps()) {
                    if (StateType.KUBERNETES_SETUP.name().equals(node.getType())) {
                      candidateFound = true;
                      WorkflowPhase rollbackPhase =
                          coWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());
                      if (rollbackPhase.getPhaseSteps().size() == 2) {
                        workflowModified = true;
                        rollbackPhase.getPhaseSteps().add(1,
                            aPhaseStep(CONTAINER_SETUP, WorkflowServiceHelper.SETUP_CONTAINER)
                                .addStep(GraphNode.builder()
                                             .id(generateUuid())
                                             .type(KUBERNETES_SETUP_ROLLBACK.name())
                                             .name(WorkflowServiceHelper.ROLLBACK_CONTAINERS)
                                             .rollback(true)
                                             .build())
                                .withPhaseStepNameForRollback(WorkflowServiceHelper.SETUP_CONTAINER)
                                .withStatusForRollback(ExecutionStatus.SUCCESS)
                                .withRollback(true)
                                .build());
                      }
                    }
                  }
                }
              }
            }
          }
        }
        if (candidateFound) {
          candidateCount++;
        }
        if (workflowModified) {
          try {
            log.info("--- Workflow updated: {}", workflow.getName());
            workflowService.updateWorkflow(workflow, false);
            Thread.sleep(100);
          } catch (Exception e) {
            log.error("Error updating workflow", e);
          }

          updateCount++;
        }
      }
      if (candidateCount > 0) {
        log.info("Application migrated: {} - {}. Updated {} workflows out of {} candidates.", app.getUuid(),
            app.getName(), updateCount, candidateCount);
      }
    }
  }
}
