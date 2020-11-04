package io.harness.impl;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.execution.PlanExecution;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;

/**
 *  Orchestrate execution
 */
public interface CIPipelineExecutionService {
  PlanExecution executePipeline(NgPipelineEntity ngPipelineEntity, CIExecutionArgs ciExecutionArgs, Long buildNumber);
}
