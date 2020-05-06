package io.harness.impl;

import io.harness.beans.CIPipeline;
import io.harness.execution.PlanExecution;

/**
 *  Orchestrate execution
 */
public interface CIPipelineExecutionService { PlanExecution executePipeline(CIPipeline ciPipeline); }
