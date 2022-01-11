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

import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.VERIFY_SERVICE;
import static software.wings.beans.PhaseStepType.WRAP_UP;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.migrations.Migration;

import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddVerifyToRollbackWorkflows implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    log.info("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest, excludeAuthority);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      log.info("No applications found");
      return;
    }
    log.info("Updating {} applications.", apps.size());
    for (Application app : apps) {
      migrate(app);
    }
  }

  public void migrate(Application application) {
    List<Workflow> workflows =
        workflowService
            .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, application.getUuid()).build())
            .getResponse();

    log.info("Updating {} workflows.", workflows.size());
    for (Workflow workflow : workflows) {
      migrate(workflow);
    }
  }

  public void migrate(Workflow workflow) {
    if (!(workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow)) {
      return;
    }

    boolean modified = false;

    CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
      if (workflowPhase.getPhaseSteps().stream().noneMatch(step -> step.getPhaseStepType() == VERIFY_SERVICE)) {
        continue;
      }

      WorkflowPhase rollbackPhase = coWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());
      if (rollbackPhase == null) {
        continue;
      }
      if (isEmpty(rollbackPhase.getPhaseSteps())) {
        continue;
      }
      if (rollbackPhase.getPhaseSteps().stream().anyMatch(step -> step.getPhaseStepType() == VERIFY_SERVICE)) {
        continue;
      }

      int index = rollbackPhase.getPhaseSteps().size();
      if (index <= rollbackPhase.getPhaseSteps().size()
          && rollbackPhase.getPhaseSteps().get(index - 1).getPhaseStepType() == WRAP_UP) {
        --index;
      }

      rollbackPhase.getPhaseSteps().add(index,
          aPhaseStep(VERIFY_SERVICE, WorkflowServiceHelper.VERIFY_SERVICE)
              .withRollback(true)
              .withPhaseStepNameForRollback(WorkflowServiceHelper.VERIFY_SERVICE)
              .withStatusForRollback(ExecutionStatus.SUCCESS)
              .build());

      modified = true;
    }

    if (modified) {
      try {
        log.info("--- Workflow updated: {}", workflow.getName());
        workflowService.updateWorkflow(workflow, false);
        Thread.sleep(100);
      } catch (Exception e) {
        log.error("Error updating workflow", e);
      }
    }
  }
}
