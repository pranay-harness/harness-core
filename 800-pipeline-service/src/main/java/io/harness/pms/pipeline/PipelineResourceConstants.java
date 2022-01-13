/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
  String INPUT_SET_SEARCH_TERM_PARAM_MESSAGE = "Search term to filter out Input Sets based on name, identifier, tags.";
  String IF_MATCH_PARAM_MESSAGE = "Version of entity to match";
  String MODULE_TYPE_PARAM_MESSAGE = "The module from which execution was triggered.";
  String ORIGINAL_EXECUTION_ID_PARAM_MESSAGE = "Id of the execution from which we are running";
  String USE_FQN_IF_ERROR_RESPONSE_ERROR_MESSAGE = "Use FQN in error response";
  String INPUT_SET_ID_PARAM_MESSAGE = "Identifier of the Input Set";
  String OVERLAY_INPUT_SET_ID_PARAM_MESSAGE = "Identifier of the Overlay Input Set";

  String START_TIME_EPOCH_PARAM_MESSAGE = "Start Date Epoch time in ms";
  String END_TIME_EPOCH_PARAM_MESSAGE = "End Date Epoch time in ms";
}
