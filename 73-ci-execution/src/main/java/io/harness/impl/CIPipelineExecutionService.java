package io.harness.impl;

import io.harness.beans.CIPipeline;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.execution.PlanExecution;

/**
 *  Orchestrate execution
 */
public interface CIPipelineExecutionService {
  PlanExecution executePipeline(CIPipeline ciPipeline, CIExecutionArgs ciExecutionArgs, Long buildNumber);
}
