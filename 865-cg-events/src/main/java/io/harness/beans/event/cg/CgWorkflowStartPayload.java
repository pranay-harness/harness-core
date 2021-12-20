package io.harness.beans.event.cg;

import static io.harness.annotations.dev.HarnessTeam.CDC;

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
@OwnedBy(CDC)
public class CgWorkflowStartPayload extends CgWorkflowExecutionPayload {
  public CgWorkflowStartPayload() {}

  @Builder
  public CgWorkflowStartPayload(ApplicationEventData application, WorkflowEventData workflow,
      PipelineEventData pipeline, ExecutionArgsEventData executionArgs, EmbeddedUser triggeredBy,
      CreatedByType triggeredByType, long startedAt, List<ServiceEntity> services, List<EnvironmentEntity> environments,
      List<InfraDefinitionEntity> infraDefinitions, WorkflowExecData workflowExecution,
      PipelineExecData pipelineExecution) {
    super(application, workflow, pipeline, executionArgs, triggeredBy, triggeredByType, startedAt, services,
        environments, infraDefinitions, workflowExecution, pipelineExecution);
  }
}
