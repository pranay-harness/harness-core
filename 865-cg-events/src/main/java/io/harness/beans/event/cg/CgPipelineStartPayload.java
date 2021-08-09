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

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@OwnedBy(CDC)
public class CgPipelineStartPayload extends CgPipelineExecutionPayload {
  public CgPipelineStartPayload() {}

  @Builder
  public CgPipelineStartPayload(ApplicationEventData application, PipelineEventData pipeline,
      ExecutionArgsEventData executionArgs, EmbeddedUser triggeredBy, CreatedByType triggeredByType, long startedAt,
      List<ServiceEntity> services, List<EnvironmentEntity> environments, List<InfraDefinitionEntity> infraDefinitions,
      String executionId) {
    super(application, pipeline, executionArgs, triggeredBy, triggeredByType, startedAt, services, environments,
        infraDefinitions, executionId);
  }
}
