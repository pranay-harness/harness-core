package io.harness.pms.plan.execution;

public interface PlanExecutionResourceConstants {
  String MODULE_TYPE_PARAM_MESSAGE =
      "Module type for the entity. If its from deployments,type will be CD , if its from build type will be CI";
  String PIPELINE_IDENTIFIER_PARAM_MESSAGE =
      "Pipeline identifier for the entity. Identifier of the Pipeline to be executed";
  String ORIGINAL_EXECUTION_IDENTIFIER_PARAM_MESSAGE =
      "This param contains the previous execution execution id. This is basically when we are rerunning a Pipeline.";

  // Node Execution
  String NODE_EXECUTION_ID_PARAM_MESSAGE = "The runtime Id of the step/stage";

  String PLAN_EXECUTION_ID_PARAM_MESSAGE = "The Pipeline Execution Id";

  // RETRY CONSTANTS
  String RETRY_STAGES_PARAM_MESSAGE =
      "This param contains the identifier of stages from where to resume. It will be a list if we want to retry from parallel group ";

  String RUN_ALL_STAGES =
      "This param provides an option to run only the failed stages when Pipeline fails at parallel group. By default, it will run all the stages in the failed parallel group.";
}
