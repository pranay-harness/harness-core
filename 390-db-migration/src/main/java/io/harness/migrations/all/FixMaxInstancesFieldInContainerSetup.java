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

import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.api.DeploymentType;
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
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixMaxInstancesFieldInContainerSetup implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    log.info("Retrieving applications");
    try (HIterator<Application> iterator = new HIterator<>(wingsPersistence.createQuery(Application.class).fetch())) {
      for (Application app : iterator) {
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
              if (workflowPhase.getDeploymentType() == DeploymentType.KUBERNETES) {
                for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
                  if (CONTAINER_SETUP == phaseStep.getPhaseStepType()) {
                    for (GraphNode node : phaseStep.getSteps()) {
                      if (StateType.KUBERNETES_SETUP.name().equals(node.getType())
                          || StateType.ECS_SERVICE_SETUP.name().equals(node.getType())) {
                        Map<String, Object> properties = node.getProperties();
                        Object value = properties.get("maxInstances");
                        if (value != null) {
                          if (value instanceof String) {
                            if (((String) value).contains("randomKey")) {
                              log.info("Resetting [{}] to 2", value);
                              workflowModified = true;
                              properties.put("maxInstances", "2");
                            }
                          } else {
                            log.info("Setting [{}] to string value", value.toString());
                            workflowModified = true;
                            properties.put("maxInstances", value.toString());
                          }
                        }
                      }
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
}
