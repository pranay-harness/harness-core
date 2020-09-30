package io.harness.impl;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.execution.PlanExecution;

/**
 *  Orchestrate execution
 */
public interface CIPipelineExecutionService {
  PlanExecution executePipeline(NgPipelineEntity ngPipelineEntity, CIExecutionArgs ciExecutionArgs, Long buildNumber);
}
