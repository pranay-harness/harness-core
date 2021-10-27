package io.harness.pms.plan.execution;

public interface PlanExecutionResourceConstants {
  String MODULE_TYPE_PARAM_MESSAGE =
      "Module type for the entity. If its from deployments,type will be CD , if its from build type will be CI";
  String PIPELINE_IDENTIFIER_PARAM_MESSAGE =
      "Pipeline identifier for the entity. Identifier of the pipeline to be executed";
  String ORIGINAL_EXECUTION_IDENTIFIER_PARAM_MESSAGE =
      "This params containts the previous execution execution id. This is basically when we are rerunning a pipeline.";
}
