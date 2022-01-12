/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
      String executionId, PipelineExecData pipelineExecution) {
    super(application, pipeline, executionArgs, triggeredBy, triggeredByType, startedAt, services, environments,
        infraDefinitions, executionId, pipelineExecution);
  }
}
