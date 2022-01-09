/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddOrchestrationToWorkflows implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  @SuppressWarnings("PMD")
  public void migrate() {
    try (HIterator<Workflow> workflowIterator =
             new HIterator<>(wingsPersistence.createQuery(Workflow.class, excludeAuthority)
                                 .field(Workflow.ORCHESTRATION_KEY)
                                 .doesNotExist()
                                 .project(Workflow.ID_KEY2, true)
                                 .project(WorkflowKeys.appId, true)
                                 .fetch())) {
      for (Workflow workflow : workflowIterator) {
        try {
          Workflow readWorkflow = workflowService.readWorkflow(workflow.getAppId(), workflow.getUuid());
          workflowService.updateWorkflow(readWorkflow, false);
        } catch (Throwable exception) {
          log.error("Exception while migrating orchestration for {}", workflow.getUuid(), exception);
        }
      }
    }
  }
}
