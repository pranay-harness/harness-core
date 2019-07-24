package io.harness.resources.intfc;

import com.google.inject.ImplementedBy;

import io.harness.resources.ExperimentalLogAnalysisResourceImpl;
import io.harness.rest.RestResponse;
import software.wings.service.impl.analysis.ExpAnalysisInfo;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.QueryParam;

@ImplementedBy(ExperimentalLogAnalysisResourceImpl.class)
public interface ExperimentalLogAnalysisResource {
  String ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL = "/save-analysis-records";
  String ANALYSIS_STATE_GET_EXP_ANALYSIS_INFO_URL = "/get-exp-analysis-info";
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
