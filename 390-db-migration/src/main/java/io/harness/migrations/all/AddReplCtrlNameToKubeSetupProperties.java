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
import static io.harness.persistence.HQuery.excludeAuthority;

import static software.wings.sm.StateType.KUBERNETES_SETUP;

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
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddReplCtrlNameToKubeSetupProperties implements Migration {
  private static final String DEFAULT_REPLICATION_CONTROLLER_NAME = "${app.name}.${service.name}.${env.name}";
  private static final String REPLICATION_CONTROLLER_NAME_KEY = "replicationControllerName";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    log.info("Retrieving applications");
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest, excludeAuthority);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      log.info("No applications found");
      return;
    }

    log.info("Updating {} applications", apps.size());
    for (Application app : apps) {
      log.info("Updating app {}", app.getUuid());
      List<Workflow> workflows =
          workflowService
              .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, app.getUuid()).build())
              .getResponse();

      for (Workflow workflow : workflows) {
        updateWorkflowsWithReplCtrlName(workflow, KUBERNETES_SETUP);
      }
      log.info("Completed updating app {}", app.getUuid());
    }

    log.info("Updated all apps");
  }

  private void updateWorkflowsWithReplCtrlName(Workflow workflow, StateType stateType) {
    boolean workflowModified = false;
    if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
      CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
      for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
        List<WorkflowPhase> workflowPhases = new ArrayList<>();
        workflowPhases.add(workflowPhase);
        WorkflowPhase rollbackPhase = coWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());
        if (rollbackPhase != null) {
          workflowPhases.add(rollbackPhase);
        }
        for (WorkflowPhase phase : workflowPhases) {
          for (PhaseStep phaseStep : phase.getPhaseSteps()) {
            for (GraphNode node : phaseStep.getSteps()) {
              if (stateType.name().equals(node.getType())) {
                Map<String, Object> properties = node.getProperties();
                if (!properties.containsKey(REPLICATION_CONTROLLER_NAME_KEY)) {
                  workflowModified = true;
                  properties.put(REPLICATION_CONTROLLER_NAME_KEY, DEFAULT_REPLICATION_CONTROLLER_NAME);
                }
              }
            }
          }
        }
      }
    }

    if (workflowModified) {
      try {
        log.info("Updating workflow: {} - {}", workflow.getUuid(), workflow.getName());
        workflowService.updateWorkflow(workflow, false);
        Thread.sleep(100);
      } catch (Exception e) {
        log.error("Error updating workflow", e);
      }
    }
  }
}
