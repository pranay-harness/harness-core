package io.harness.ci.integrationstage;

import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;

public interface PipelineExecutionModifier {
  /**
   * Modifies saved pipeline execution plan by resolving references between stages.
   * References my include infrastructure and other information
   * @param ngPipeline pipeline object that holds current stages
   * @return modified ngPipeline
   */
  NgPipeline modifyExecutionPlan(NgPipeline ngPipeline, ExecutionPlanCreationContext context);
}
