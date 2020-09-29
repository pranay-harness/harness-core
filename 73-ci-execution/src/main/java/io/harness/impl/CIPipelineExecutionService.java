package io.harness.impl;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import io.harness.execution.PlanExecution;

/**
 *  Orchestrate execution
 */
public interface CIPipelineExecutionService {
  PlanExecution executePipeline(CDPipelineEntity ciPipeline, CIExecutionArgs ciExecutionArgs, Long buildNumber);
}
