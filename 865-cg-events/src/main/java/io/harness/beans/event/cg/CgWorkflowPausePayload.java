/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.event.cg;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CreatedByType;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.event.cg.application.ApplicationEventData;
import io.harness.beans.event.cg.entities.EnvironmentEntity;
import io.harness.beans.event.cg.entities.InfraDefinitionEntity;
import io.harness.beans.event.cg.entities.ServiceEntity;
import io.harness.beans.event.cg.pipeline.ExecutionArgsEventData;
import io.harness.beans.event.cg.pipeline.PipelineEventData;
import io.harness.beans.event.cg.pipeline.PipelineExecData;
import io.harness.beans.event.cg.workflow.WorkflowEventData;
import io.harness.beans.event.cg.workflow.WorkflowExecData;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@OwnedBy(HarnessTeam.CDC)
public class CgWorkflowPausePayload extends CgWorkflowExecutionPayload {
  public CgWorkflowPausePayload() {}

  @Builder
  public CgWorkflowPausePayload(ApplicationEventData application, WorkflowEventData workflow,
      PipelineEventData pipeline, ExecutionArgsEventData executionArgs, EmbeddedUser triggeredBy,
      CreatedByType triggeredByType, long startedAt, List<ServiceEntity> services, List<EnvironmentEntity> environments,
      List<InfraDefinitionEntity> infraDefinitions, WorkflowExecData workflowExecution,
      PipelineExecData pipelineExecution) {
    super(application, workflow, pipeline, executionArgs, triggeredBy, triggeredByType, startedAt, services,
        environments, infraDefinitions, workflowExecution, pipelineExecution);
  }
}
