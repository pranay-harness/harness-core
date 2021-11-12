package io.harness.pms.pipeline;

public interface PipelineResourceConstants {
  String ACCOUNT_PARAM_MESSAGE = "Account Identifier for the entity.";
  String ORG_PARAM_MESSAGE = "Organization Identifier for the entity.";
  String PROJECT_PARAM_MESSAGE = "Project Identifier for the entity.";
  String PIPELINE_ID_PARAM_MESSAGE = "Pipeline Identifier";
  String STAGE_NODE_ID_PARAM_MESSAGE = "Stage Node Identifier to get execution stats.";
  String PIPELINE_ID_LIST_PARAM_MESSAGE = "Pipeline Identifier filter if exact pipelines needs to be filtered.";
  String PIPELINE_SEARCH_TERM_PARAM_MESSAGE =
      "Search term to filter out pipelines based on pipeline name, identifier, tags.";
  String IF_MATCH_PARAM_MESSAGE = "Version of entity to match";
  String MODULE_TYPE_PARAM_MESSAGE = "The module from which execution was triggered.";
  String ORIGINAL_EXECUTION_ID_PARAM_MESSAGE = "Id of the execution from which we are running";
  String USE_FQN_IF_ERROR_RESPONSE_ERROR_MESSAGE = "Use FQN in error response";
}
