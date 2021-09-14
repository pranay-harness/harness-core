/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.plan.execution.observers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.observer.PipelineActionObserver;
import io.harness.pms.plan.execution.service.PMSExecutionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class PipelineExecutionSummaryDeleteObserver implements PipelineActionObserver {
  @Inject PMSExecutionService pmsExecutionService;

  @Override
  public void onDelete(PipelineEntity pipelineEntity) {
    pmsExecutionService.deleteExecutionsOnPipelineDeletion(pipelineEntity);
    log.info("All executions of pipeline {} deleted", pipelineEntity.getIdentifier());
  }
}
