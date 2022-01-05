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
import io.harness.beans.event.cg.workflow.WorkflowStepInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@OwnedBy(HarnessTeam.CDC)
public class CgWorkflowResumePayload extends CgWorkflowExecutionPayload {
  public CgWorkflowResumePayload() {}

  private List<WorkflowStepInfo> steps;

  @Builder
  public CgWorkflowResumePayload(ApplicationEventData application, WorkflowEventData workflow,
      PipelineEventData pipeline, ExecutionArgsEventData executionArgs, EmbeddedUser triggeredBy,
      CreatedByType triggeredByType, long startedAt, List<ServiceEntity> services, List<EnvironmentEntity> environments,
      List<InfraDefinitionEntity> infraDefinitions, WorkflowExecData workflowExecution,
      PipelineExecData pipelineExecution) {
    super(application, workflow, pipeline, executionArgs, triggeredBy, triggeredByType, startedAt, services,
        environments, infraDefinitions, workflowExecution, pipelineExecution);
  }
}
