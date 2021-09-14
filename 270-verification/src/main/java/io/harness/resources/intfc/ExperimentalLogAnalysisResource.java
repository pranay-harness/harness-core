/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resources.intfc;

import io.harness.resources.ExperimentalLogAnalysisResourceImpl;
import io.harness.rest.RestResponse;

import software.wings.service.impl.analysis.ExpAnalysisInfo;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord;
import software.wings.sm.StateType;

import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.QueryParam;

@ImplementedBy(ExperimentalLogAnalysisResourceImpl.class)
public interface ExperimentalLogAnalysisResource {
  String ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL = "/save-analysis-records";
  String ANALYSIS_STATE_GET_EXP_ANALYSIS_INFO_URL = "/get-exp-analysis-info";
  String MSG_PAIRS_TO_VOTE = "/msg-pairs-to-vote";
  String ANALYSIS_STATE_RE_QUEUE_TASK = "/experimentalTask";
  String LEARNING_EXP_URL = "learning-exp";

  RestResponse<Boolean> saveLogAnalysisMLRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("logCollectionMinute") Integer logCollectionMinute,
      @QueryParam("isBaselineCreated") boolean isBaselineCreated, @QueryParam("taskId") String taskId,
      @QueryParam("stateType") StateType stateType, ExperimentalLogMLAnalysisRecord mlAnalysisResponse)
      throws IOException;

  RestResponse<List<ExpAnalysisInfo>> getLogExpAnalysisInfo(@QueryParam("accountId") String accountId)
      throws IOException;
}
