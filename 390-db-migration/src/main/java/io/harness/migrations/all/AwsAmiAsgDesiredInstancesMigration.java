/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_DESIRED_INSTANCES;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class AwsAmiAsgDesiredInstancesMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  private static final String MAX_INTANCES_KEY = "maxInstances";
  private static final String DESIRED_INTANCES_KEY = "desiredInstances";

  @Override
  public void migrate() {
    log.info("Retrieving applications");

    try (HIterator<Application> apps = new HIterator<>(wingsPersistence.createQuery(Application.class).fetch())) {
      while (apps.hasNext()) {
        Application application = apps.next();
        log.info("Updating app {}", application.getUuid());
        List<Workflow> workflows =
            workflowService
                .listWorkflows(
                    aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, application.getUuid()).build())
                .getResponse();

        if (isNotEmpty(workflows)) {
          for (Workflow workflow : workflows) {
            try {
              updateWorkflow(workflow);
            } catch (Exception ex) {
              log.error("Error updating workflow: [{}]", workflow.getUuid(), ex);
            }
          }
        }
        log.info("Completed updating app {}", application.getUuid());
      }
    }

    log.info("Updated all apps");
    log.info("Finished running AwsAmiAsgDesiredInstancesMigration");
  }

  private void updateWorkflow(Workflow workflow) throws Exception {
    boolean workflowModified = false;

    if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
      CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

      if (coWorkflow.getWorkflowPhases() == null) {
        return;
      }

      for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
        List<WorkflowPhase> workflowPhases = new ArrayList<>();
        workflowPhases.add(workflowPhase);

        WorkflowPhase rollbackPhase = coWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());
        if (rollbackPhase != null) {
          workflowPhases.add(rollbackPhase);
        }

        for (WorkflowPhase phase : workflowPhases) {
          List<PhaseStep> phaseSteps = phase.getPhaseSteps();
          if (isNotEmpty(phaseSteps)) {
            for (PhaseStep phaseStep : phaseSteps) {
              List<GraphNode> steps = phaseStep.getSteps();
              if (isNotEmpty(steps)) {
                for (GraphNode node : steps) {
                  if (StateType.AWS_AMI_SERVICE_SETUP.name().equals(node.getType())) {
                    workflowModified = true;
                    Map<String, Object> properties = node.getProperties();
                    if (properties == null) {
                      properties = new HashMap<>();
                    }
                    int desiredInstances = DEFAULT_AMI_ASG_DESIRED_INSTANCES;
                    if (isNotEmpty(properties)) {
                      Object o = properties.get(MAX_INTANCES_KEY);
                      if (o instanceof Integer) {
                        desiredInstances = (Integer) o;
                      }
                    }
                    properties.put(DESIRED_INTANCES_KEY, desiredInstances);
                  }
                }
              }
            }
          }
        }
      }
    }

    if (workflowModified) {
      log.info("Updating workflow: {} - {}", workflow.getUuid(), workflow.getName());
      workflowService.updateWorkflow(workflow, false);
      Thread.sleep(100);
    }
  }
}
